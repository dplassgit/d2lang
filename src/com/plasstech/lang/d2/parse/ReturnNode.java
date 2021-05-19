package com.plasstech.lang.d2.parse;

import com.plasstech.lang.d2.common.NodeVisitor;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.VarType;

public class ReturnNode extends StatementNode {

  private final ExprNode expr;

  ReturnNode(Position position, ExprNode expr) {
    super(position);
    this.expr = expr;
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
    return String.format("{ReturnNode: %s}", expr.toString());
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }
}
