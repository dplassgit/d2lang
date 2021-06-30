package com.plasstech.lang.d2.parse.node;

/** Assignment: variable = <expression> */
public class AssignmentNode extends AbstractNode implements StatementNode {
  private final LValueNode variable;
  private final ExprNode expr;

  public AssignmentNode(LValueNode variable, ExprNode expr) {
    super(variable.position());
    this.variable = variable;
    this.expr = expr;
  }

  public LValueNode variable() {
    return variable;
  }

  public ExprNode expr() {
    return expr;
  }

  @Override
  public String toString() {
    return String.format(
        "AssignmentNode: %s (%s) = %s", variable.toString(), variable.varType(), expr);
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }
}
