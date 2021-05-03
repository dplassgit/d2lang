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
    root.children().forEach(node -> node.visit(this));
    return symbolTable;
  }

  @Override
  public void accept(PrintNode printNode) {
    // NOP
  }

  @Override
  public void accept(AssignmentNode node) {
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
    right.visit(this);
    if (right.varType().isUnknown()) {
      if (!variable.varType().isUnknown()) {
        right.setVarType(variable.varType());
      } else {
        // this is bad.
        throw new IllegalStateException(
                String.format("Cannot determine type of %s at %s", right, node));
      }
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
  public void accept(IntNode intNode) {
    // All is good.
  }

  @Override
  public void accept(VariableNode variableNode) {
    // Look up variable in the (local) symbol table, and set it in the node.
    if (variableNode.varType().isUnknown()) {
      VarType existingType = symbolTable.lookup(variableNode.name());
      if (!existingType.isUnknown()) {
        variableNode.setVarType(existingType);
      }
    }
  }

  @Override
  public void accept(BinOpNode binOpNode) {
    // Make sure that the left = right
    Node left = binOpNode.left();
    left.visit(this);

    Node right = binOpNode.right();
    right.visit(this);

    if (left.varType().isUnknown() && right.varType().isUnknown()) {
      throw new IllegalStateException("Cannot determine type of " + binOpNode);
    }
    // Go back down the tree
    if (left.varType().isUnknown()) {
      left.setVarType(right.varType());

      // This is a hack that will not scale...
      if (left.nodeType() == Node.Type.VARIABLE) {
        VariableNode node = (VariableNode) left;
        symbolTable.add(node.name(), left.varType());
      }
    }
    if (right.varType().isUnknown()) {
      right.setVarType(left.varType());

      if (right.nodeType() == Node.Type.VARIABLE) {
        VariableNode node = (VariableNode) right;
        symbolTable.add(node.name(), right.varType());
      }
    }
    if (left.varType() != right.varType()) {
      throw new IllegalStateException(String.format("Type mismatch; lhs %s is %s; rhs %s is %s",
              left.toString(), left.varType(), right.toString(), right.varType()));
    }
    binOpNode.setVarType(left.varType());
  }
}
