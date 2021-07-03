package com.plasstech.lang.d2.codegen;

import static com.google.common.collect.ImmutableList.toImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Call;
import com.plasstech.lang.d2.codegen.il.Goto;
import com.plasstech.lang.d2.codegen.il.IfOp;
import com.plasstech.lang.d2.codegen.il.Label;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.ProcEntry;
import com.plasstech.lang.d2.codegen.il.ProcExit;
import com.plasstech.lang.d2.codegen.il.Return;
import com.plasstech.lang.d2.codegen.il.Stop;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.lex.Token.Type;
import com.plasstech.lang.d2.parse.node.AssignmentNode;
import com.plasstech.lang.d2.parse.node.BinOpNode;
import com.plasstech.lang.d2.parse.node.BreakNode;
import com.plasstech.lang.d2.parse.node.CallNode;
import com.plasstech.lang.d2.parse.node.ConstNode;
import com.plasstech.lang.d2.parse.node.ContinueNode;
import com.plasstech.lang.d2.parse.node.DefaultVisitor;
import com.plasstech.lang.d2.parse.node.ExitNode;
import com.plasstech.lang.d2.parse.node.ExprNode;
import com.plasstech.lang.d2.parse.node.IfNode;
import com.plasstech.lang.d2.parse.node.InputNode;
import com.plasstech.lang.d2.parse.node.MainNode;
import com.plasstech.lang.d2.parse.node.Node;
import com.plasstech.lang.d2.parse.node.PrintNode;
import com.plasstech.lang.d2.parse.node.ProcedureNode;
import com.plasstech.lang.d2.parse.node.ProgramNode;
import com.plasstech.lang.d2.parse.node.ReturnNode;
import com.plasstech.lang.d2.parse.node.UnaryNode;
import com.plasstech.lang.d2.parse.node.VariableNode;
import com.plasstech.lang.d2.parse.node.WhileNode;
import com.plasstech.lang.d2.type.ProcSymbol;
import com.plasstech.lang.d2.type.SymTab;
import com.plasstech.lang.d2.type.Symbol;
import com.plasstech.lang.d2.type.VarType;

public class ILCodeGenerator extends DefaultVisitor implements CodeGenerator<Op> {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ProgramNode root;
  private final SymTab symbolTable;

  private final List<Op> operations = new ArrayList<>();
  private final Stack<String> whileBreaks = new Stack<>();
  private final Stack<String> whileContinues = new Stack<>();

  private int tempId;
  private int labelId;
  private Stack<ProcSymbol> procedures = new Stack<>();

  public ILCodeGenerator(ProgramNode root, SymTab symbolTable) {
    this.root = root;
    this.symbolTable = symbolTable;
  }

  @Override
  public ImmutableList<Op> generate() {
    root.accept(this);
    return ImmutableList.copyOf(operations);
  }

  private SymTab symbolTable() {
    if (procedures.isEmpty()) {
      return symbolTable;
    }
    return procedures.peek().symTab();
  }

  private String generateLabel(String prefix) {
    labelId++;
    return String.format("__%s_%d", prefix, labelId);
  }

  private TempLocation generateTemp(VarType varType) {
    tempId++;
    String name = String.format("__temp%d", tempId);
    symbolTable().declare(name, varType);
    // TODO: keep the vartype with the location
    return new TempLocation(name);
  }

  private StackLocation generateStack(VarType varType) {
    tempId++;
    String name = String.format("__stack%d", tempId);
    symbolTable().declare(name, varType);
    // TODO: keep the vartype with the location
    return new StackLocation(name);
  }

  @Override
  public void visit(PrintNode node) {
    Node expr = node.expr();
    expr.accept(this);
    emit(new SysCall(SysCall.Call.PRINT, expr.location()));
    if (node.isPrintln()) {
      emit(new SysCall(SysCall.Call.PRINT, new ConstantOperand<String>("\n")));
    }
  }

  @Override
  public void visit(AssignmentNode node) {
    String name = node.variable().name();

    // Look up storage in current symbol table
    // this may be a global or a local/parameter (stack)
    Location dest = lookupLocation(name);
    node.variable().setLocation(dest);

    Node expr = node.expr();
    expr.accept(this);
    Location source = expr.location();

    emit(new Transfer(dest, source));
  }

  private Location lookupLocation(String name) {
    Symbol variable = symbolTable().getRecursive(name);
    switch (variable.storage()) {
      case GLOBAL:
        return new MemoryAddress(name);
      default:
        return new StackLocation(name);
    }
  }

  @Override
  public <T> void visit(ConstNode<T> node) {
    TempLocation destination = generateTemp(node.varType());

    // TODO: Should this be a thing? should we just set the "location" of the node to a constant
    // "location"?
    node.setLocation(destination);

    // TODO: figure out storage for strings
    ConstantOperand constOperand = new ConstantOperand(node.value());
    Transfer transfer = new Transfer(destination, constOperand);
    emit(transfer);
  }

  @Override
  public void visit(VariableNode node) {
    // this may be a global or a local/parameter (stack)
    Location source = lookupLocation(node.name());

    TempLocation destination = generateTemp(node.varType());
    node.setLocation(destination);

    // Retrieve location of variable and provide it in a temp
    emit(new Transfer(destination, source));
  }

  @Override
  public void visit(InputNode node) {
    TempLocation destination = generateTemp(VarType.STRING);
    node.setLocation(destination);
    emit(new SysCall(SysCall.Call.INPUT, destination));
  }

  @Override
  public void visit(BinOpNode node) {
    // Calculate the value and put it somewhere
    Node left = node.left();
    // Source for the value of left - either a register or memory location or a value.
    Operand leftSrc;
    // if left and right are "simple", just get it.
    if (left.isSimpleType()) {
      ConstNode<?> simpleLeft = (ConstNode<?>) left;
      leftSrc = new ConstantOperand(simpleLeft.value());
    } else {
      left.accept(this);

      // should we copy to a new temp?!
      leftSrc = left.location();
    }

    // Calculate the value and put it somewhere
    Node right = node.right();
    // Source for the value of right - either a register or memory location or a
    // value or a constant.
    Operand rightSrc;
    // if left and right are "simple", just get it.
    if (right.isSimpleType()) {
      ConstNode<?> simpleRight = (ConstNode<?>) right;
      rightSrc = new ConstantOperand(simpleRight.value());
    } else {
      right.accept(this);
      // should we copy to a new temp?!
      rightSrc = right.location();
    }

    TempLocation location = generateTemp(node.varType());
    node.setLocation(location);
    // location = leftSrc (operator) rightSrc
    emit(new BinOp(location, leftSrc, node.operator(), rightSrc));
  }

  @Override
  public void visit(UnaryNode node) {
    // calculate the value and put it somewhere.
    Node expr = node.expr();
    expr.accept(this);

    TempLocation destination = generateTemp(node.varType());
    node.setLocation(destination);

    switch (node.operator()) {
      case BIT_NOT:
      case NOT:
      case MINUS:
      case LENGTH:
      case ASC:
      case CHR:
        emit(new UnaryOp(destination, node.operator(), expr.location()));
        break;
      case PLUS:
        // tiny optimization
        emit(new Transfer(destination, expr.location()));
        break;
      default:
        logger.atWarning().log("No code generated for operator %s", node.operator());
        break;
    }
  }

  @Override
  public void visit(IfNode node) {
    String after = generateLabel("after_if");

    for (IfNode.Case ifCase : node.cases()) {
      String nextLabel = generateLabel("elif");

      Node cond = ifCase.condition();
      cond.accept(this);

      TempLocation temp = generateTemp(cond.varType());
      // if it's not true, jump to the next block
      // temp = !cond
      // if temp skip to next block.
      emit(new UnaryOp(temp, Type.NOT, cond.location()));
      emit(new IfOp(temp, nextLabel));

      ifCase.block().accept(this);
      // We're in a block , now jump completely after.
      emit(new Goto(after));

      emit(new Label(nextLabel));
    }
    if (node.elseBlock() != null) {
      node.elseBlock().accept(this);
    }

    emit(new Label(after));
  }

  @Override
  public void visit(WhileNode node) {
    // loop_begin:
    // ..test
    // ..if done, goto loop_end
    // ..(loop code)
    // loop_increment: ("continue" target)
    // ..(increment code)
    // ..goto loop_begin
    // loop_end: ("break" target)
    String before = generateLabel(Label.LOOP_BEGIN_PREFIX);
    String increment = generateLabel(Label.LOOP_INCREMENT_PREFIX);
    String after = generateLabel(Label.LOOP_END_PREFIX);
    whileContinues.push(increment);
    whileBreaks.push(after);

    emit(new Label(before));
    Node condition = node.condition();
    condition.accept(this);

    TempLocation notCondition = generateTemp(condition.varType());
    emit(new UnaryOp(notCondition, Type.NOT, condition.location()));
    emit(new IfOp(notCondition, after));

    node.block().accept(this);

    emit(new Label(increment));
    if (node.doStatement().isPresent()) {
      node.doStatement().get().accept(this);
    }
    emit(new Goto(before));

    emit(new Label(after));
    whileBreaks.pop();
    whileContinues.pop();
  }

  @Override
  public void visit(BreakNode node) {
    // break = go to the end
    String after = whileBreaks.peek();
    emit(new Goto(after));
  }

  @Override
  public void visit(ContinueNode node) {
    // continue = go to the "increment" label
    String before = whileContinues.peek();
    emit(new Goto(before));
  }

  @Override
  public void visit(MainNode node) {
    emit(new Label("__main"));
    // TODO: something about arguments? probably add to local symbol table
    // Also TODO: how to reference arguments
    if (node.block() != null) {
      node.block().accept(this);
    }
    emit(new Stop());
  }

  @Override
  public void visit(ExitNode node) {
    int code = 0;
    if (node.exitMessage().isPresent()) {
      code = -1;
      node.exitMessage().get().accept(this);
      Location messageLocation = node.exitMessage().get().location();
      emit(new SysCall(SysCall.Call.MESSAGE, messageLocation));
    }
    emit(new Stop(code));
  }

  @Override
  public void visit(ReturnNode node) {
    if (node.expr().isPresent()) {
      node.expr().get().accept(this);
      // copy the location of the result to the current procedure's destination.
      Location exprLoc = node.expr().get().location();
      Location procDest = procedures.peek().node().location();
      // copy the exprloc to the dest
      emit(new Transfer(procDest, exprLoc));
      emit(new Return(procDest));
    } else {
      emit(new Return());
    }
  }

  @Override
  public void visit(ProcedureNode node) {
    // Look up procedure in current scope
    ProcSymbol procSym = (ProcSymbol) symbolTable().getRecursive(node.name());
    assert (procSym != null);

    procedures.push(procSym);

    if (node.returnType() != VarType.VOID) {
      // generate a destination
      StackLocation returnValueDestination = generateStack(node.returnType());
      node.setLocation(returnValueDestination);
    }
    // Guard to prevent just falling into this method
    String afterLabel = generateLabel("after_user_proc_" + node.name());

    emit(new Goto(afterLabel));

    emit(new ProcEntry(node.name()));
    // This is the real entry point.
    emit(new Label(node.name()));

    // Also TODO: how to reference arguments???
    node.block().accept(this);

    // there should have already been a regular "return" with the value.
    emit(new Return());
    emit(new ProcExit());
    emit(new Label(afterLabel));

    procedures.pop();
  }

  @Override
  public void visit(CallNode node) {
    // 1. generate each actual
    for (ExprNode actual : node.actuals()) {
      actual.accept(this);
    }

    // 2. emit call(parameters)
    Call call;
    ImmutableList<Operand> actualLocations =
        node.actuals().stream().map(Node::location).collect(toImmutableList());
    if (node.isStatement()) {
      // No return value
      call = new Call(node.functionToCall(), actualLocations);
    } else {
      // 3. put result location into node.location
      Location location = generateTemp(node.varType());
      node.setLocation(location);
      call = new Call(location, node.functionToCall(), actualLocations);
    }
    emit(call);
  }

  private void emit(Op op) {
    logger.atFine().log(op.toString());
    operations.add(op);
  }
}
