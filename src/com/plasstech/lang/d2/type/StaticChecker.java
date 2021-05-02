package com.plasstech.lang.d2.type;

import com.plasstech.lang.d2.common.NodeVisitor;
import com.plasstech.lang.d2.parse.AssignmentNode;
import com.plasstech.lang.d2.parse.BinOpNode;
import com.plasstech.lang.d2.parse.IntNode;
import com.plasstech.lang.d2.parse.Node;
import com.plasstech.lang.d2.parse.PrintNode;
import com.plasstech.lang.d2.parse.StatementNode;
import com.plasstech.lang.d2.parse.StatementsNode;
import com.plasstech.lang.d2.parse.VariableNode;

public class StaticChecker implements NodeVisitor {
  private final StatementsNode root;

  public StaticChecker(StatementsNode root) {
    this.root = root;
  }

  public void execute() {
    // for each child of root
    root.children().forEach(node -> node.visit(this));
  }

  @Override
  public void accept(PrintNode printNode) {
    // NOP
  }

  @Override
  public void accept(AssignmentNode node) {
    // Make sure that the left = right
    Node rhs = node.expr();
    rhs.visit(this);
    if (rhs.varType() == VarType.UNKNOWN) {
      // this is bad.
      throw new IllegalStateException("Cannot determine type of " + rhs);
    }
    Node variable = node.variable();
    if (variable.varType() == VarType.UNKNOWN) {
      variable.setVarType(rhs.varType());
      // Enter it in the local symbol table
    }
    if (variable.varType() != rhs.varType()) {
      throw new IllegalStateException(String.format("Type mismatch; lhs %s is %s; rhs %s is %s",
              variable.toString(), variable.varType(), rhs.toString(), rhs.varType()));
    }
  }

  @Override
  public void accept(IntNode intNode) {
    // All is good.
  }

  @Override
  public void accept(VariableNode variableNode) {
  }

  @Override
  public void accept(StatementNode statementNode) {
  }

  @Override
  public void accept(BinOpNode binOpNode) {
    // Make sure that the left = right
    Node left = binOpNode.left();
    left.visit(this);
    if (left.varType() == VarType.UNKNOWN) {
      // this is bad.
      throw new IllegalStateException("Cannot determine type of " + left);
    }
    Node right = binOpNode.right();
    right.visit(this);
    if (right.varType() == VarType.UNKNOWN) {
      // this is bad.
      throw new IllegalStateException("Cannot determine type of " + right);
    }
    if (left.varType() != right.varType()) {
      throw new IllegalStateException(String.format("Type mismatch; lhs %s is %s; rhs %s is %s",
              left.toString(), left.varType(), right.toString(), right.varType()));
    }
  }
}
