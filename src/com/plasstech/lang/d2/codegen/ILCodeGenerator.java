package com.plasstech.lang.d2.codegen;

import static com.google.common.collect.ImmutableList.toImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Call;
import com.plasstech.lang.d2.codegen.il.ConstantOperand;
import com.plasstech.lang.d2.codegen.il.Goto;
import com.plasstech.lang.d2.codegen.il.IfOp;
import com.plasstech.lang.d2.codegen.il.Label;
import com.plasstech.lang.d2.codegen.il.Location;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Operand;
import com.plasstech.lang.d2.codegen.il.ProcEntry;
import com.plasstech.lang.d2.codegen.il.ProcExit;
import com.plasstech.lang.d2.codegen.il.Return;
import com.plasstech.lang.d2.codegen.il.StackLocation;
import com.plasstech.lang.d2.codegen.il.Stop;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.TempLocation;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.common.DefaultVisitor;
import com.plasstech.lang.d2.lex.Token.Type;
import com.plasstech.lang.d2.parse.AssignmentNode;
import com.plasstech.lang.d2.parse.BinOpNode;
import com.plasstech.lang.d2.parse.BreakNode;
import com.plasstech.lang.d2.parse.CallNode;
import com.plasstech.lang.d2.parse.ConstNode;
import com.plasstech.lang.d2.parse.ContinueNode;
import com.plasstech.lang.d2.parse.ExprNode;
import com.plasstech.lang.d2.parse.IfNode;
import com.plasstech.lang.d2.parse.MainNode;
import com.plasstech.lang.d2.parse.Node;
import com.plasstech.lang.d2.parse.PrintNode;
import com.plasstech.lang.d2.parse.ProcedureNode;
import com.plasstech.lang.d2.parse.ProgramNode;
import com.plasstech.lang.d2.parse.ReturnNode;
import com.plasstech.lang.d2.parse.UnaryNode;
import com.plasstech.lang.d2.parse.VariableNode;
import com.plasstech.lang.d2.parse.WhileNode;
import com.plasstech.lang.d2.type.SymTab;
import com.plasstech.lang.d2.type.Symbol;
import com.plasstech.lang.d2.type.VarType;

public class ILCodeGenerator extends DefaultVisitor implements CodeGenerator<Op> {

  private final ProgramNode root;
  private final SymTab symTab;

  private final List<Op> operations = new ArrayList<>();
  private final Stack<String> whileBreaks = new Stack<>();
  private final Stack<String> whileContinues = new Stack<>();

  private int tempId;
  private int labelId;
  private Stack<ProcedureNode> procedures = new Stack<>();

  public ILCodeGenerator(ProgramNode root, SymTab symTab) {
    this.root = root;
    this.symTab = symTab;
  }

  @Override
  public List<Op> generate() {
    System.out.println("#include <stdlib.h>");
    System.out.println("#include <stdio.h>");
    System.out.println("main() {");
    if (symTab != null) {
      for (Symbol symbol : symTab.entries().values()) {
        emitTypeDeclaration(symbol);
      }
    }
    root.accept(this);
    System.out.println("}");
    return operations;
  }

  private void emitTypeDeclaration(Symbol symbol) {
    switch (symbol.type()) {
      case INT:
        System.out.printf("int %s;\n", symbol.name());
        break;
      case BOOL:
        System.out.printf("int %s; // (bool)\n", symbol.name());
        break;
      case STRING:
        System.out.printf("char *%s;\n", symbol.name());
        break;
      default:
        System.out.printf("void *%s;\n", symbol.name());
        break;
    }
  }

  private String generateLabel(String prefix) {
    labelId++;
    return String.format("%s%d", prefix, labelId);
  }

  private TempLocation generateTemp(VarType varType) {
    tempId++;
    String name = String.format("temp%d", tempId);
    switch (varType) {
      case INT:
        System.out.printf("int %s;\n", name);
        break;
      case BOOL:
        System.out.printf("int %s; // (bool)\n", name);
        break;
      case STRING:
        System.out.printf("char *%s;\n", name);
        break;
      default:
        System.out.printf("void *%s;\n", name);
        break;
    }
    symTab.declare(name, varType);
    return new TempLocation(name);
  }

  @Override
  public void visit(PrintNode node) {
    System.out.printf("\n// %s\n", node);
    Node expr = node.expr();
    expr.accept(this);
    emit(new SysCall(SysCall.Call.PRINT, expr.location()));
    if (node.isPrintln()) {
      emit(new SysCall(SysCall.Call.PRINT, new ConstantOperand("\n")));
    }
  }

  @Override
  public void visit(AssignmentNode node) {
    System.out.printf("\n// %s\n", node);

    String name = node.variable().name();
    Location dest = new StackLocation(name); // ???
    node.variable().setLocation(dest);

    Node expr = node.expr();
    expr.accept(this);
    Location source = expr.location();

    emit(new Transfer(dest, source));
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
    StackLocation source = new StackLocation(node.name());
    TempLocation destination = generateTemp(node.varType());
    node.setLocation(destination);
    // Retrieve location of variable and provide it in a temp
    emit(new Transfer(destination, source));
  }

  @Override
  public void visit(BinOpNode node) {
    // Calculate the value and set it in t0
    Node left = node.left();
    // Source for the value of left - either a register or memory location or a value.
    Operand leftSrc;
    // if left and right are "simple", just get it.
    if (left.isSimpleType()) {
      ConstNode<?> simpleLeft = (ConstNode<?>) left;
      leftSrc = new ConstantOperand(simpleLeft.value());
    } else {
      left.accept(this);
      // should we copy left's location to a new temp?!
      leftSrc = left.location();
    }

    // Calculate the value and set it in t0
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
      // should we copy right's location to a new temp?!
      rightSrc = right.location();
    }

//    switch (node.operator()) {
//      case MINUS:
//      case DIV:
//      case MULT:
//      case PLUS:
//      case EQEQ:
    TempLocation location = generateTemp(node.varType());
    node.setLocation(location);
    emit(new BinOp(location, leftSrc, node.operator(), rightSrc));
//        break;
//      default:
//        emit("UNKNOWN OP " + node.operator());
//        break;
//    }

  }

  @Override
  public void visit(UnaryNode node) {
    // calculate the value and set it in t0
    Node expr = node.expr();
    expr.accept(this);

    TempLocation destination = generateTemp(node.varType());
    node.setLocation(destination);

    switch (node.operator()) {
      case NOT:
      case MINUS:
        emit(new UnaryOp(destination, node.operator(), expr.location()));
        break;
      case PLUS:
        emit(new Transfer(destination, expr.location()));
        break;
      default:
        break;
    }
  }

  @Override
  public void visit(IfNode node) {
    System.out.printf("\n// %s\n", node);

    String after = generateLabel("afterIf");

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
      System.out.printf("\n// else:");
      node.elseBlock().accept(this);
    }

    emit(new Label(after));
  }

  @Override
  public void visit(WhileNode node) {
    System.out.printf("\n// %s\n", node);

    // before:
    // ..test
    // ..if done, goto after
    // ..(loop code)
    // increment: ("continue" target)
    // ..(increment code)
    // ..goto before
    // after: ("break" target)
    String before = generateLabel("beforeWhile");
    String increment = generateLabel("increment");
    String after = generateLabel("afterWhile");
    whileContinues.push(increment);
    whileBreaks.push(after);

    System.out.println("// while");
    emit(new Label(before));
    Node condition = node.condition();
    condition.accept(this);

    TempLocation notCondition = generateTemp(condition.varType());
    emit(new UnaryOp(notCondition, Type.NOT, condition.location()));
    emit(new IfOp(notCondition, after));

    System.out.println("// do");
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
    System.out.println("// break");
    String after = whileBreaks.peek();
    emit(new Goto(after));
  }

  @Override
  public void visit(ContinueNode node) {
    System.out.println("// continue");
    // continue = go to the "increment"
    String before = whileContinues.peek();
    emit(new Goto(before));
  }

  @Override
  public void visit(MainNode node) {
    System.out.println("// main");
    emit(new Label("_main"));
    // TODO: something about arguments? probably add to local symbol table
    // Also TODO: how to reference arguments
    if (node.block() != null) {
      node.block().accept(this);
    }
    emit(new Stop());
  }

  @Override
  public void visit(ReturnNode node) {
    if (node.expr().isPresent()) {
      node.expr().get().accept(this);
      // copy the location of the result to the current procedure's destination.
      Location exprLoc = node.expr().get().location();
      Location procDest = procedures.peek().location();
      // copy the exprloc to the dest
      emit(new Transfer(procDest, exprLoc));
      emit(new Return(procDest));
    } else {
      emit(new Return());
    }
  }

  @Override
  public void visit(ProcedureNode node) {
    procedures.push(node);
    if (node.returnType() != VarType.VOID) {
      // generate a destination
      TempLocation tempDestination = generateTemp(node.returnType());
      node.setLocation(tempDestination);
    }
    // Guard to prevent just falling into this method
    String afterLabel = generateLabel("afterProc");

    emit(new Goto(afterLabel));

    // note mangling
    emit(new ProcEntry("d_" + node.name()));

    // TODO: something about arguments? probably add to local symbol table
    // Also TODO: how to reference arguments???
    node.block().accept(this);

    // there should have already been a regular "return" with the value.
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
    ImmutableList<Location> actualLocations = node.actuals().stream().map(Node::location)
            .collect(toImmutableList());
    if (node.isStatement()) {
      // No return value
      call = new Call("d_" + node.functionToCall(), actualLocations);
    } else {
      // 3. put result location into node.location
      Location location = generateTemp(node.varType());
      node.setLocation(location);
      call = new Call(location, "d_" + node.functionToCall(), actualLocations);
    }
    emit(call);
  }

  private void emit(Op op) {
    System.out.println(op);
    operations.add(op);
  }
}
