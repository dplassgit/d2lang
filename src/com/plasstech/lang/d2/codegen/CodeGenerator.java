package com.plasstech.lang.d2.codegen;

import com.plasstech.lang.d2.common.NodeVisitor;
import com.plasstech.lang.d2.parse.AssignmentNode;
import com.plasstech.lang.d2.parse.BinOpNode;
import com.plasstech.lang.d2.parse.IntNode;
import com.plasstech.lang.d2.parse.PrintNode;
import com.plasstech.lang.d2.parse.StatementNode;
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
  }

  @Override
  public void accept(AssignmentNode node) {
  }

  @Override
  public void accept(IntNode intNode) {
  }

  @Override
  public void accept(VariableNode variableNode) {
  }

  @Override
  public void accept(StatementNode statementNode) {
  }

  @Override
  public void accept(BinOpNode binOpNode) {
  }
}
