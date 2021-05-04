package com.plasstech.lang.d2.type;

import com.plasstech.lang.d2.common.NodeVisitor;
import com.plasstech.lang.d2.parse.AssignmentNode;
import com.plasstech.lang.d2.parse.BinOpNode;
import com.plasstech.lang.d2.parse.IntNode;
import com.plasstech.lang.d2.parse.Node;
import com.plasstech.lang.d2.parse.PrintNode;
import com.plasstech.lang.d2.parse.StatementsNode;
import com.plasstech.lang.d2.parse.VariableNode;

public class StaticChecker implements NodeVisitor {
  private final StatementsNode root;
  private final SymTab symbolTable = new SymTab();
  private String error;

  public StaticChecker(StatementsNode root) {
    this.root = root;
  }

  public TypeCheckResult execute() {
    root.accept(this);
    if (error != null) {
      return new TypeCheckResult(error);
    }
    return new TypeCheckResult(symbolTable);
  }

  @Override
  public void visit(PrintNode printNode) {
    // NOP
  }

  @Override
  public void visit(AssignmentNode node) {
    if (error != null) {
      return;
    }
    // Make sure that the left = right
    VariableNode variable = node.variable();
    VarType existingType = variable.varType();
    if (existingType.isUnknown()) {
      existingType = symbolTable.lookup(variable.name());
      if (!existingType.isUnknown()) {
        variable.setVarType(existingType);
      }
    }

    Node right = node.expr();
    right.accept(this);
    if (right.varType().isUnknown()) {
      // this is bad.
      error = String.format("Type error at %s: Indeterminable type for %s", right.position(),
              right);
      return;
    }

    if (variable.varType().isUnknown()) {
      variable.setVarType(right.varType());
      // Enter it in the local symbol table
      symbolTable.add(variable.name(), right.varType());
    } else if (variable.varType() != right.varType()) {
      error = String.format("Type mismatch at %s: lhs (%s) is %s; rhs (%s) is %s",
              variable.position(),
              variable,
              variable.varType(), right, right.varType());
    }
  }

  @Override
  public void visit(IntNode intNode) {
    // NOP
  }

  @Override
  public void visit(VariableNode node) {
    if (error != null) {
      return;
    }
    if (node.varType().isUnknown()) {
      // Look up variable in the (local) symbol table, and set it in the node.
      VarType existingType = symbolTable.lookup(node.name());
      if (!existingType.isUnknown()) {
        node.setVarType(existingType);
      }
    }
  }

  @Override
  public void visit(BinOpNode binOpNode) {
    if (error != null) {
      return;
    }
    // Make sure that the left = right
    Node left = binOpNode.left();
    left.accept(this);

    Node right = binOpNode.right();
    right.accept(this);

    if (left.varType().isUnknown() || right.varType().isUnknown()) {
      error = String.format("Cannot determine type of %s", binOpNode);
      return;
    }

    if (left.varType() != right.varType()) {
      error = String.format("Type mismatch at %s: lhs %s is %s; rhs %s is %s", left.position(),
              left, left.varType(),
              right, right.varType());
      return;
    }
    binOpNode.setVarType(left.varType());
  }
}
