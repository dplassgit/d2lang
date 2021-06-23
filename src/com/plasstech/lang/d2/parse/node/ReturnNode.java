package com.plasstech.lang.d2.parse.node;

import java.util.Optional;

import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.VarType;

public class ReturnNode extends AbstractNode implements StatementNode {

  private final Optional<ExprNode> expr;

  public ReturnNode(Position position, ExprNode expr) {
    super(position);
    this.expr = Optional.of(expr);
  }

  public ReturnNode(Position position) {
    super(position);
    this.expr = Optional.empty();
    this.setVarType(VarType.VOID);
  }

  public Optional<ExprNode> expr() {
    return expr;
  }

  @Override
  public void setVarType(VarType varType) {
    super.setVarType(varType);
    // This is weird.
    if (expr.isPresent()) {
      if (expr.get().varType().isUnknown()) {
        expr.get().setVarType(varType);
      }
    }
  }

  @Override
  public String toString() {
    if (expr.isPresent()) {
      return String.format("{ReturnNode: return %s}", expr.get().toString());
    } else {
      return "{ReturnNode: return}";
    }
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }
}
