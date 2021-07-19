package com.plasstech.lang.d2.codegen;

import static com.google.common.collect.ImmutableList.toImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.il.AllocateOp;
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
import com.plasstech.lang.d2.lex.Token;
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
import com.plasstech.lang.d2.parse.node.FieldSetNode;
import com.plasstech.lang.d2.parse.node.IfNode;
import com.plasstech.lang.d2.parse.node.InputNode;
import com.plasstech.lang.d2.parse.node.LValueNode;
import com.plasstech.lang.d2.parse.node.MainNode;
import com.plasstech.lang.d2.parse.node.NewNode;
import com.plasstech.lang.d2.parse.node.Node;
import com.plasstech.lang.d2.parse.node.PrintNode;
import com.plasstech.lang.d2.parse.node.ProcedureNode;
import com.plasstech.lang.d2.parse.node.ProcedureNode.Parameter;
import com.plasstech.lang.d2.parse.node.ReturnNode;
import com.plasstech.lang.d2.parse.node.UnaryNode;
import com.plasstech.lang.d2.parse.node.VariableNode;
import com.plasstech.lang.d2.parse.node.VariableSetNode;
import com.plasstech.lang.d2.parse.node.WhileNode;
import com.plasstech.lang.d2.type.ProcSymbol;
import com.plasstech.lang.d2.type.RecordSymbol;
import com.plasstech.lang.d2.type.SymTab;
import com.plasstech.lang.d2.type.Symbol;
import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VarType;

public class ILCodeGenerator extends DefaultVisitor implements CodeGenerator<Op> {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final Node root;
  private final SymTab symbolTable;

  private final List<Op> operations = new ArrayList<>();
  private final Stack<String> whileBreaks = new Stack<>();
  private final Stack<String> whileContinues = new Stack<>();

  private int id;
  private Stack<ProcSymbol> procedures = new Stack<>();

  public ILCodeGenerator(Node root, SymTab symbolTable) {
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

  private String newLabel(String prefix) {
    return String.format("__%s_%d", prefix, ++id);
  }

  private TempLocation allocateTemp(VarType varType) {
    String name = String.format("__temp%d", ++id);
    symbolTable().declare(name, varType);
    // TODO: keep the vartype with the location
    return new TempLocation(name);
  }

  private StackLocation allocateStack(VarType varType) {
    String name = String.format("__stack%d", ++id);
    symbolTable().declare(name, varType);
    // TODO: keep the vartype with the location
    return new StackLocation(name);
  }

  private MemoryAddress allocateMemory(VarType varType) {
    String name = String.format("__memory%d", ++id);
    symbolTable().declare(name, varType);
    // TODO: keep the vartype with the location
    return new MemoryAddress(name);
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
    LValueNode variable = node.variable();
    variable.accept(
        new LValueNode.Visitor() {

          @Override
          public void visit(VariableSetNode variableNode) {
            String name = variableNode.name();

            // Look up storage in current symbol table
            // this may be a global or a local/parameter (stack)
            Location dest = lookupLocation(name);
            variableNode.setLocation(dest);

            Node expr = node.expr();
            expr.accept(ILCodeGenerator.this);
            Location source = expr.location();
            if (dest == null || source == null) {
              logger.atSevere().log(
                  "lvalue source or dest is null: dest = %s, source=%s at %s",
                  dest, source, expr.position());
              return;
            }
            emit(new Transfer(dest, source));
          }

          @Override
          public void visit(FieldSetNode fieldSetNode) {
            // Look up storage in current symbol table
            Symbol sym = symbolTable().getRecursive(fieldSetNode.variableName());
            FieldSetAddress dest;
            if (sym != null) {
              // this is wrong, probably
              dest =
                  new FieldSetAddress(
                      fieldSetNode.variableName(), fieldSetNode.fieldName(), sym.storage());
            } else {
              // ???
              logger.atSevere().log(
                  "Could not find record symbol %s in symtab", fieldSetNode.variableName());
              dest =
                  new FieldSetAddress(
                      fieldSetNode.variableName(), fieldSetNode.fieldName(), SymbolStorage.HEAP);
            }

            fieldSetNode.setLocation(dest);
            Node expr = node.expr();
            expr.accept(ILCodeGenerator.this);
            Location source = expr.location();

            emit(new Transfer(dest, source));
          }
        });
  }

  @Override
  public void visit(NewNode node) {
    Symbol symbol = symbolTable().getRecursive(node.recordName());
    if (!(symbol instanceof RecordSymbol)) {
      logger.atSevere().log(
          "Cannot call NEW on non-record type %s at %s on line %s", symbol, node, node.position());
      return;
    }
    MemoryAddress location = allocateMemory(symbol.varType());
    node.setLocation(location);
    emit(new AllocateOp(location, (RecordSymbol) symbol));
  }

  private Location lookupLocation(String name) {
    Symbol variable = symbolTable().getRecursive(name);
    switch (variable.storage()) {
      case HEAP:
      case GLOBAL:
        return new MemoryAddress(name);
      default:
        return new StackLocation(name);
    }
  }

  @Override
  public <T> void visit(ConstNode<T> node) {
    TempLocation destination = allocateTemp(node.varType());

    // TODO: Should this be a thing? should we just set the "location" of the node to a constant
    // "location"?
    node.setLocation(destination);

    // TODO: figure out storage for strings
    ConstantOperand constOperand = new ConstantOperand(node.value());
    Transfer transfer = new Transfer(destination, constOperand);
    emit(transfer);
  }

  /**
   * This winds up generating code that may be difficult to translate to assembly/bytecode, e.g.,
   *
   * <pre>
   * __temp32 = a + b
   * </pre>
   *
   * where both a and b are stack or globals.
   *
   * <p>The only reason I did this was to get the loop invariant optimizer to work with records,
   * because this is the code it used to generate:
   *
   * <pre>
   * __temp1 = rec
   * __temp2 = __temp1.fieldname
   * </pre>
   *
   * This was causing the loop optimizer to move BOTH ops out of the loop, because it thought
   * __temp1, and therefore __temp2 was invariant.
   *
   * <p>So now this version generates:
   *
   * <pre>
   * __temp2 = rec.fieldname
   * </pre>
   *
   * which makes the optimizer work.
   */
  @Override
  public void visit(VariableNode node) {
    // This may be a global or a local/parameter (stack)
    Location source = lookupLocation(node.name());
    node.setLocation(source);
  }

  @Override
  public void visit(InputNode node) {
    TempLocation destination = allocateTemp(VarType.STRING);
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
      leftSrc = left.location();
    }

    // Source for the value of right - either a register or memory location or a
    // value or a constant.
    Operand rightSrc;
    // Calculate the value and put it somewhere
    Node right = node.right();

    if (node.operator() == Token.Type.DOT) {
      // the RHS is a field reference
      VariableNode rightVarNode = (VariableNode) right;
      rightSrc = new ConstantOperand<String>(rightVarNode.name());
    } else {
      // if left and right are "simple", just get it.
      if (right.isSimpleType()) {
        ConstNode<?> simpleRight = (ConstNode<?>) right;
        rightSrc = new ConstantOperand(simpleRight.value());
      } else {
        right.accept(this);
        rightSrc = right.location();
      }
    }

    TempLocation location = allocateTemp(node.varType());
    node.setLocation(location);
    emit(new BinOp(location, leftSrc, node.operator(), rightSrc));
  }

  @Override
  public void visit(UnaryNode node) {
    // calculate the value and put it somewhere.
    Node expr = node.expr();
    expr.accept(this);

    TempLocation destination = allocateTemp(node.varType());
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
        logger.atSevere().log("No code generated for node %s", node);
        break;
    }
  }

  @Override
  public void visit(IfNode node) {
    String after = newLabel("after_if");

    for (IfNode.Case ifCase : node.cases()) {
      String nextLabel = newLabel("elif");

      Node cond = ifCase.condition();
      cond.accept(this);

      TempLocation temp = allocateTemp(cond.varType());
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
    // pattern is: if (condition) { do loop while condition}

    // emit test
    // if while condition is false goto loop_end
    // loop_begin:
    // ..emit loop code
    // loop_increment: ("continue" target)
    // ..emit increment code
    // ..emit test
    // ..if while condition is true, goto loop_begin
    // loop_end: ("break" target)

    String before = newLabel(Label.LOOP_BEGIN_PREFIX);
    String increment = newLabel(Label.LOOP_INCREMENT_PREFIX);
    String after = newLabel(Label.LOOP_END_PREFIX);
    whileContinues.push(increment);
    whileBreaks.push(after);

    // Pre-check
    Node condition = node.condition();
    condition.accept(this);
    TempLocation notCondition = allocateTemp(condition.varType());
    emit(new UnaryOp(notCondition, Type.NOT, condition.location()));
    emit(new IfOp(notCondition, after));

    emit(new Label(before));

    node.block().accept(this);

    emit(new Label(increment));
    if (node.doStatement().isPresent()) {
      node.doStatement().get().accept(this);
    }

    // post-increment test
    condition.setLocation(null);
    condition.accept(this);
    emit(new IfOp(condition.location(), before));

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
      StackLocation returnValueDestination = allocateStack(node.returnType());
      node.setLocation(returnValueDestination);
    }
    // Guard to prevent just falling into this method
    String afterLabel = newLabel("after_user_proc_" + node.name());

    emit(new Goto(afterLabel));

    // This is the real entry point.
    emit(new Label(node.name()));
    emit(
        new ProcEntry(
            node.name(),
            node.parameters().stream().map(Parameter::name).collect(toImmutableList())));

    // Also TODO: how to reference arguments???
    node.block().accept(this);

    // there should have already been a regular "return" with the value.
    if (node.returnType() == VarType.VOID) {
      emit(new Return());
    }
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
      Location location = allocateTemp(node.varType());
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
