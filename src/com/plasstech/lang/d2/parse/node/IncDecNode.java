package com.plasstech.lang.d2.parse.node;

import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.VarType;

/** Represents an increment or decrement statement, which is syntactic sugar for i=i=1 or i=i-1. */
public class IncDecNode extends AssignmentNode {

  public IncDecNode(VariableSetNode variableSet, boolean increment) {
    // the expression is variable = variable +/- 1
    super(variableSet, makeExpr(variableSet, increment));
  }

  private static ExprNode makeExpr(VariableSetNode theThis, boolean increment) {
    ExprNode one = new ConstNode<Integer>(1, VarType.INT, theThis.position());
    VariableNode variable = new VariableNode(theThis.name(), theThis.position());
    return new BinOpNode(variable, increment ? TokenType.PLUS : TokenType.MINUS, one);
  }
}
