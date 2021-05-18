package com.plasstech.lang.d2.parse;

import com.plasstech.lang.d2.common.NodeVisitor;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.lex.Token;
import com.plasstech.lang.d2.type.VarType;

public class UnaryNode extends ExprNode {

  private final Token.Type operator;

  private final ExprNode expr;

  UnaryNode(Token.Type operator, ExprNode expr, Position position) {
    super(position);
    this.operator = operator;
    this.expr = expr;
  }

  public Token.Type operator() {
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
    return String.format("{UnaryNode: %s %s}", operator.name(), expr.toString());
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }
}
