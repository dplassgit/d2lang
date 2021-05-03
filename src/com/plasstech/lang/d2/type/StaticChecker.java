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

  public StaticChecker(StatementsNode root) {
    this.root = root;
  }

  public SymTab execute() {
    // for each child of root
    root.children().forEach(node -> node.accept(this));
    return symbolTable;
  }

  @Override
  public void visit(PrintNode printNode) {
    // NOP
  }

  @Override
  public void visit(AssignmentNode node) {
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
      throw new IllegalStateException(
              String.format("Cannot determine type of %s in %s", right, node));
    }

    if (variable.varType().isUnknown()) {
      variable.setVarType(right.varType());
      // Enter it in the local symbol table
      symbolTable.add(variable.name(), right.varType());
    } else if (variable.varType() != right.varType()) {
      throw new IllegalStateException(String.format("Type mismatch; lhs %s is %s; rhs %s is %s",
              variable.toString(), variable.varType(), right.toString(), right.varType()));
    }
  }

  @Override
  public void visit(IntNode intNode) {
    // NOP
  }

  @Override
  public void visit(VariableNode node) {
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
    // Make sure that the left = right
    Node left = binOpNode.left();
    left.accept(this);

    Node right = binOpNode.right();
    right.accept(this);

    if (left.varType().isUnknown() || right.varType().isUnknown()) {
      throw new IllegalStateException("Cannot determine type of " + binOpNode);
    }

    if (left.varType() != right.varType()) {
      throw new IllegalStateException(String.format("Type mismatch; lhs %s is %s; rhs %s is %s",
              left.toString(), left.varType(), right.toString(), right.varType()));
    }
    binOpNode.setVarType(left.varType());
  }
}
