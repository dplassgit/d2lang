package com.plasstech.lang.d2.codegen;

import com.plasstech.lang.d2.common.NodeVisitor;
import com.plasstech.lang.d2.parse.AssignmentNode;
import com.plasstech.lang.d2.parse.BinOpNode;
import com.plasstech.lang.d2.parse.IntNode;
import com.plasstech.lang.d2.parse.Node;
import com.plasstech.lang.d2.parse.PrintNode;
import com.plasstech.lang.d2.parse.StatementsNode;
import com.plasstech.lang.d2.parse.VariableNode;

public class CodeGenerator implements NodeVisitor {
  private final StatementsNode root;

  public CodeGenerator(StatementsNode root) {
    this.root = root;
  }

  public void generate() {
    // for each child of root
    root.children().forEach(node -> node.visit(this));
  }

  // mumble something visitor pattern
  @Override
  public void accept(PrintNode printNode) {
    Node expr = printNode.expr();
    expr.visit(this);
    emit("call $ffd2 ; print r0");
  }

  @Override
  public void accept(AssignmentNode node) {
    String name = node.variable().name();
    node.expr().visit(this);
    emit(String.format("st r0, %s", name));
  }

  @Override
  public void accept(IntNode intNode) {
    // Provide constant in r0
    emit(String.format("ld r0, #%s", intNode.value()));
  }

  @Override
  public void accept(VariableNode variableNode) {
    // Retrieve location of variable and provide it in r0
    emit(String.format("ld r0, %s", variableNode.name()));
  }

  @Override
  public void accept(BinOpNode binOpNode) {
    // calculate the value and set it in r0
    Node left = binOpNode.left();
    left.visit(this);
    // by definition, "left" has emitted its value in r0. push on stack
    emit("push r0");

    Node right = binOpNode.right();
    right.visit(this);
    // by definition, "right" has emitted its value in r0. push on stack
    switch (binOpNode.opType()) {
      case MINUS: // want r1 - r0
        emit("pop r1");
        emit("sbc");
        emit("sub r1, r0");
        break;
      case DIV:
        emit("pop r1");
        emit("div r1, r0");
        break;
      case MULT:
        emit("pop r1");
        emit("mul r1, r0");
        break;
      case PLUS:
        emit("pop r1");
        emit("add r1, r0");
        break;
      default:
        break;
    }
  }

  private void emit(String string) {
    System.err.println(string);
  }
}
