package com.plasstech.lang.d2.codegen;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.plasstech.lang.d2.codegen.il.Assignment;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Goto;
import com.plasstech.lang.d2.codegen.il.IfOp;
import com.plasstech.lang.d2.codegen.il.Label;
import com.plasstech.lang.d2.codegen.il.Load;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Return;
import com.plasstech.lang.d2.codegen.il.Stop;
import com.plasstech.lang.d2.codegen.il.Store;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.common.DefaultVisitor;
import com.plasstech.lang.d2.lex.Token.Type;
import com.plasstech.lang.d2.parse.AssignmentNode;
import com.plasstech.lang.d2.parse.BinOpNode;
import com.plasstech.lang.d2.parse.BoolNode;
import com.plasstech.lang.d2.parse.BreakNode;
import com.plasstech.lang.d2.parse.ContinueNode;
import com.plasstech.lang.d2.parse.IfNode;
import com.plasstech.lang.d2.parse.IntNode;
import com.plasstech.lang.d2.parse.MainNode;
import com.plasstech.lang.d2.parse.Node;
import com.plasstech.lang.d2.parse.PrintNode;
import com.plasstech.lang.d2.parse.ProcedureNode;
import com.plasstech.lang.d2.parse.ProgramNode;
import com.plasstech.lang.d2.parse.SimpleNode;
import com.plasstech.lang.d2.parse.UnaryNode;
import com.plasstech.lang.d2.parse.VariableNode;
import com.plasstech.lang.d2.parse.WhileNode;
import com.plasstech.lang.d2.type.SymTab;

public class ILCodeGenerator extends DefaultVisitor implements CodeGenerator<Op> {

  private final ProgramNode root;
  private final SymTab symTab;

  private final List<Op> operations = new ArrayList<>();
  private final Stack<String> whileStarts = new Stack<>();
  private final Stack<String> whileEnds = new Stack<>();

  private int tempId;
  private int labelId;

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
      for (String name : symTab.entries().keySet()) {
        System.out.printf("int %s;\n", name);
      }
    }
    System.out.printf("int t0;\n");
    root.accept(this);
    System.out.println("}");
    return operations;
  }

  @Override
  public void visit(PrintNode node) {
    System.out.printf("\n// %s\n", node);
    Node expr = node.expr();
    expr.accept(this);
    emit(new SysCall("$ffd2", "t0"));
//    System.out.println("\tprintf(\"%d\\n\", t0);");
  }

  private String generateLabel(String prefix) {
    labelId++;
    return String.format("%s%d", prefix, labelId);
  }

  private String generateTemp() {
    tempId++;
    System.out.printf("int t%d;\n", tempId);
    return String.format("t%d", tempId);
  }

  @Override
  public void visit(AssignmentNode node) {
    System.out.printf("\n// %s\n", node);

    String name = node.variable().name();
    Node expr = node.expr();
    expr.accept(this);
    emit(new Store(name, "t0"));
  }

  @Override
  public void visit(IntNode node) {
    // Provide constant in t0
    emit(new Assignment("t0", String.valueOf(node.value())));
  }

  @Override
  public void visit(BoolNode node) {
    // Provide constant in t0
    emit(new Assignment("t0", String.valueOf(node.value())));
  }

  @Override
  public void visit(VariableNode node) {
    // Retrieve location of variable and provide it in t0
    emit(new Load("t0", node.name()));
  }

  @Override
  public void visit(BinOpNode node) {
    System.out.printf("\n// %s\n", node);

    // Calculate the value and set it in t0
    Node left = node.left();
    // Source for the value of left - either a register or memory location or a value.
    String leftSrc;
    // if left and right are "simple", just get it.
    if (left.isSimpleType()) {
      SimpleNode simpleLeft = (SimpleNode) left;
      leftSrc = simpleLeft.simpleValue();
    } else {
      leftSrc = generateTemp();
      left.accept(this);
      // by definition, "left" has emitted its value in t0. store in rx
      emit(new Assignment(leftSrc, "t0"));
    }

    // Calculate the value and set it in t0
    Node right = node.right();
    // Source for the value of right - either a register or memory location or a
    // value.
    String rightSrc;
    // if left and right are "simple", just get it.
    if (right.isSimpleType()) {
      SimpleNode simpleRight = (SimpleNode) right;
      rightSrc = simpleRight.simpleValue();
    } else {
      rightSrc = generateTemp();
      right.accept(this);
      // by definition, "left" has emitted its value in t0. store in rx
      emit(new Assignment(rightSrc, "t0"));
    }

//    switch (node.operator()) {
//      case MINUS:
//      case DIV:
//      case MULT:
//      case PLUS:
//      case EQEQ:
    emit(new BinOp("t0", leftSrc, node.operator(), rightSrc));
//        break;
//      default:
//        emit("UNKNOWN OP " + node.operator());
//        break;
//    }

  }

  @Override
  public void visit(UnaryNode node) {
    System.out.printf("\n// %s\n", node);
    // calculate the value and set it in t0
    Node expr = node.expr();
    expr.accept(this);

    switch (node.operator()) {
      case NOT:
      case MINUS: // want t0 to be 0-t0
        emit(new UnaryOp("t0", node.operator(), "t0"));
        break;
      case PLUS:
        // Intentionally do nothing.
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
      Node cond = ifCase.condition();
      cond.accept(this);
      // This is very inefficient, but understandable.
      String thisLabel = generateLabel("then");
      String nextLabel = generateLabel("elif");
      // if it's true, jump to this block
      emit(new IfOp("t0", thisLabel));
      // else, jump to the next one
      emit(new Goto(nextLabel));
      emit(new Label(thisLabel));
      ifCase.block().statements().forEach(stmt -> stmt.accept(this));
      // We're in a block , now jump completely after.
      emit(new Goto(after));
      emit(new Label(nextLabel));
    }
    if (node.elseBlock() != null) {
      System.out.printf("\n// else:");
      node.elseBlock().statements().forEach(stmt -> stmt.accept(this));
    }

    emit(new Label(after));
  }

  @Override
  public void visit(WhileNode node) {
    System.out.printf("\n// %s\n", node);

    // push before and after on the stacks, for possible use by the "break" and "continue" nodes.
    String before = generateLabel("beforeWhile");
    whileStarts.push(before);
    String after = generateLabel("afterWhile");
    whileEnds.push(after);

    System.out.println("// while");
    emit(new Label(before));
    node.condition().accept(this);
    emit(new UnaryOp("t0", Type.NOT, "t0"));
    emit(new IfOp("t0", after));

    System.out.println("// do");
    node.block().accept(this);
    if (node.assignment().isPresent()) {
      node.assignment().get().accept(this);
    }
    emit(new Goto(before));

    emit(new Label(after));
    whileStarts.pop();
    whileEnds.pop();
  }

  @Override
  public void visit(BreakNode node) {
    // break = go to the end
    System.out.println("// break");
    String after = whileEnds.peek();
    emit(new Goto(after));
  }

  @Override
  public void visit(ContinueNode node) {
    System.out.println("// continue");
    // continue = go to the "increment"
    String before = whileStarts.peek();
    emit(new Goto(before));
  }

  @Override
  public void visit(MainNode node) {
    emit(new Label("_main"));
    // TODO: something about arguments? probably add to local symbol table
    // Also TODO: how to reference arguments
    if (node.statements() != null) {
      node.statements().accept(this);
    }
    emit(new Stop());
  }

  @Override
  public void visit(ProcedureNode node) {
    // Guard to prevent just falling into this method
    String afterLabel = generateLabel("afterProc");
    emit(new Goto(afterLabel));

    // note different mangling
    emit(new Label("d_" + node.name()));
    // TODO: something about arguments? probably add to local symbol table
    // Also TODO: how to reference arguments
    if (node.statements() != null) {
      node.statements().accept(this);
    }
    emit(new Return());

    emit(new Label(afterLabel));
  }

  private void emit(Op op) {
    System.out.println(op);
    operations.add(op);
  }
}
