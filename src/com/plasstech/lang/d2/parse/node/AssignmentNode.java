package com.plasstech.lang.d2.parse.node;

/**
 * Assignment: variable = <expression>
 */
public class AssignmentNode extends AbstractNode implements StatementNode {
  private final VariableNode variable;
  private final ExprNode expr;

  public AssignmentNode(VariableNode variable, ExprNode expr) {
    super(variable.position());
    this.variable = variable;
    this.expr = expr;
  }

  public VariableNode variable() {
    return variable;
  }

  public ExprNode expr() {
    return expr;
  }

  @Override
  public String toString() {
    return String.format("AssignmentNode: %s (%s) = %s", variable.name(), variable.varType(), expr);
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }
}
