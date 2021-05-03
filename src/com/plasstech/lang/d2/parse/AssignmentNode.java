package com.plasstech.lang.d2.parse;

import com.plasstech.lang.d2.common.NodeVisitor;

/**
 * Assignment: variable = <expression>
 */
public class AssignmentNode extends StatementNode {
  private final VariableNode variable;
  private final Node expr;

  AssignmentNode(VariableNode variable, Node expr) {
    super(Type.ASSIGNMENT, variable.position());
    this.variable = variable;
    this.expr = expr;
  }

  public VariableNode variable() {
    return variable;
  }

  public Node expr() {
    return expr;
  }

  @Override
  public String toString() {
    return String.format("AssignmentNode: %s = %s", variable.name(), expr.toString());
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }
}
