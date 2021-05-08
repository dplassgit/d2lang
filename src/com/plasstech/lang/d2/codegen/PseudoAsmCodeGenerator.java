package com.plasstech.lang.d2.codegen;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.common.DefaultVisitor;
import com.plasstech.lang.d2.parse.AssignmentNode;
import com.plasstech.lang.d2.parse.BinOpNode;
import com.plasstech.lang.d2.parse.BoolNode;
import com.plasstech.lang.d2.parse.IntNode;
import com.plasstech.lang.d2.parse.Node;
import com.plasstech.lang.d2.parse.PrintNode;
import com.plasstech.lang.d2.parse.BlockNode;
import com.plasstech.lang.d2.parse.UnaryNode;
import com.plasstech.lang.d2.parse.VariableNode;

public class PseudoAsmCodeGenerator extends DefaultVisitor implements CodeGenerator<String> {
  private final BlockNode root;

  public PseudoAsmCodeGenerator(BlockNode root) {
    this.root = root;
  }

  @Override
  public List<String> generate() {
    // For each child of root
    root.accept(this);
    return ImmutableList.of("; eof");
  }

  @Override
  public void visit(PrintNode node) {
    System.out.printf("\n; %s\n", node);
    Node expr = node.expr();
    expr.accept(this);
    emit("call $ffd2 ; print r0\n");
  }

  @Override
  public void visit(AssignmentNode node) {
    System.out.printf("\n; %s\n", node);
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
  public void visit(BoolNode node) {
    System.out.printf("; %s\n", node);
    // Provide constant in r0
    emit(String.format("ld r0, #%d", node.value() ? 1 : 0));
  }

  @Override
  public void visit(VariableNode node) {
    System.out.printf("; %s\n", node);
    // Retrieve location of variable and provide it in r0
    emit(String.format("ld r0, %s", node.name()));
  }

  @Override
  public void visit(BinOpNode node) {
    System.out.printf("\n; %s\n", node);
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
      case EQEQ:
        emit("pop r1");
        emit("cmp r1, r0\n");
        break;
      default:
        emit("UNKNOWN OP " + node.operator());
        break;
    }
  }

  @Override
  public void visit(UnaryNode node) {
    System.out.printf("; %s\n", node);
    // calculate the value and set it in r0
    Node expr = node.expr();
    expr.accept(this);

    switch (node.operator()) {
      case NOT:
      case MINUS: // want r0 to be 0-r1
        emit("ld r1, r0");
        emit("ld r0, #0");
        emit("sbc");
        emit("sub r0, r1\n");
        break;
      case PLUS:
        break;
      default:
        break;
    }
  }
  
  private void emit(String string) {
    System.out.println(string);
  }
}
