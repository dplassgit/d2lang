package com.plasstech.lang.d2.codegen;

import com.plasstech.lang.d2.common.DefaultVisitor;
import com.plasstech.lang.d2.parse.AssignmentNode;
import com.plasstech.lang.d2.parse.BinOpNode;
import com.plasstech.lang.d2.parse.IntNode;
import com.plasstech.lang.d2.parse.Node;
import com.plasstech.lang.d2.parse.PrintNode;
import com.plasstech.lang.d2.parse.StatementsNode;
import com.plasstech.lang.d2.parse.VariableNode;

public class CodeGenerator extends DefaultVisitor {
  private final StatementsNode root;

  public CodeGenerator(StatementsNode root) {
    this.root = root;
  }

  public void generate() {
    // For each child of root
    root.accept(this);
  }

  // mumble something visitor pattern
  @Override
  public void visit(PrintNode node) {
    System.out.printf("; %s\n", node);
    Node expr = node.expr();
    expr.accept(this);
    emit("call $ffd2 ; print r0\n");
  }

  @Override
  public void visit(AssignmentNode node) {
    System.out.printf("; %s\n", node);
    String name = node.variable().name();
    node.expr().accept(this);
    emit(String.format("st r0, %s", name));
  }

  @Override
  public void visit(IntNode node) {
    System.out.printf("; %s\n", node);
    // Provide constant in r0
    emit(String.format("ld r0, #%s", node.value()));
  }

  @Override
  public void visit(VariableNode node) {
    System.out.printf("; %s\n", node);
    // Retrieve location of variable and provide it in r0
    emit(String.format("ld r0, %s", node.name()));
  }

  @Override
  public void visit(BinOpNode node) {
    System.out.printf("; %s\n", node);
    // calculate the value and set it in r0
    Node left = node.left();
    left.accept(this);
    // by definition, "left" has emitted its value in r0. push on stack
    emit("push r0\n");

    Node right = node.right();
    right.accept(this);
    // by definition, "right" has emitted its value in r0. push on stack
    switch (node.operator()) {
      case MINUS: // want r1 - r0
        emit("pop r1");
        emit("sbc");
        emit("sub r1, r0\n");
        break;
      case DIV:
        emit("pop r1");
        emit("div r1, r0\n");
        break;
      case MULT:
        emit("pop r1");
        emit("mul r1, r0\n");
        break;
      case PLUS:
        emit("pop r1");
        emit("add r1, r0\n");
        break;
      default:
        break;
    }
  }

  private void emit(String string) {
    System.out.println(string);
  }
}
