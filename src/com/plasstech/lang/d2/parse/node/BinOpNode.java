package com.plasstech.lang.d2.parse.node;

import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.VarType;

/** Binary operation: leftexpr <operation> rightexpr */
public class BinOpNode extends AbstractNode implements ExprNode {
  private static final Set<TokenType> BINARY_OPERATORS =
      ImmutableSet.of(
          TokenType.PLUS,
          TokenType.MINUS,
          TokenType.MULT,
          TokenType.DIV,
          TokenType.MOD,
          TokenType.AND,
          TokenType.OR,
          TokenType.XOR,
          TokenType.BIT_AND,
          TokenType.BIT_OR,
          TokenType.BIT_XOR,
          TokenType.EQEQ,
          TokenType.GT,
          TokenType.LT,
          TokenType.GEQ,
          TokenType.LEQ,
          TokenType.NEQ,
          TokenType.LBRACKET,
          TokenType.SHIFT_LEFT,
          TokenType.SHIFT_RIGHT,
          TokenType.DOT);

  private final TokenType operator;
  private final ExprNode left;
  private final ExprNode right;

  public BinOpNode(ExprNode left, TokenType operator, ExprNode right) {
    super(left.position());
    Preconditions.checkArgument(
        BINARY_OPERATORS.contains(operator), "Invalid opType " + operator.name());
    this.left = left;
    this.operator = operator;
    this.right = right;
  }

  public TokenType operator() {
    return operator;
  }

  public ExprNode left() {
    return left;
  }

  public ExprNode right() {
    return right;
  }

  @Override
  public void setVarType(VarType varType) {
    super.setVarType(varType);
    // This is weird.
    if (left.varType().isUnknown()) {
      left.setVarType(varType);
    }
    if (right.varType().isUnknown()) {
      right.setVarType(varType);
    }
  }

  @Override
  public String toString() {
    return String.format(
        "BinOpNode: {%s} %s {%s}", left.toString(), operator.name(), right.toString());
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }
}
