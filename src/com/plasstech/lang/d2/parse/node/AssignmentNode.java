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

  public LValueNode lvalue() {
    return variable;
  }

  public ExprNode expr() {
    return expr;
  }

  @Override
  public String toString() {
    return String.format("%s = %s", variable, expr);
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }
}
