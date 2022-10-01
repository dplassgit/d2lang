package com.plasstech.lang.d2.parse.node;

import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.VarType;

public class UnaryNode extends AbstractNode implements ExprNode {

  private final TokenType operator;

  private final ExprNode expr;

  public UnaryNode(TokenType operator, ExprNode expr, Position position) {
    super(position);
    this.operator = operator;
    this.expr = expr;
  }

  public TokenType operator() {
    return operator;
  }

  public ExprNode expr() {
    return expr;
  }

  @Override
  public void setVarType(VarType varType) {
    super.setVarType(varType);
    // This is weird.
    if (expr.varType().isUnknown()) {
      expr.setVarType(varType);
    }
  }

  @Override
  public String toString() {
    return String.format("%s %s", operator.name(), expr);
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }
}
