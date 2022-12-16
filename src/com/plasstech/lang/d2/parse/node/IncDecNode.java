package com.plasstech.lang.d2.parse.node;

import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.VarType;

public class IncDecNode extends AssignmentNode {
  public enum Direction {
    Inc,
    Dec;
  }

  public IncDecNode(VariableSetNode variable, Direction op) {
    // the expression is variable = variable +/- 1
    super(variable, makeExpr(variable, op));
  }

  private static ExprNode makeExpr(VariableSetNode variable, Direction op) {
    ExprNode one = new ConstNode<Integer>(1, VarType.INT, variable.position());
    return new BinOpNode(variable, (op == Direction.Inc) ? TokenType.PLUS : TokenType.MINUS, one);
  }
}
