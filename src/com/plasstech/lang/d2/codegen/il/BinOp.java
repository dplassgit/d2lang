package com.plasstech.lang.d2.codegen.il;

import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.common.TokenType;

/** Represents a binary operation: destination = left (operator) right */
public class BinOp extends Op {
  private final Location destination;
  private final TokenType operator;
  private final Operand left;
  private final Operand right;

  public BinOp(
      Location destination, Operand left, TokenType operator, Operand right, Position position) {
    super(position);
    this.destination = destination;
    this.left = left;
    this.operator = operator;
    this.right = right;
  }

  public Location destination() {
    return destination;
  }

  public TokenType operator() {
    return operator;
  }

  public Operand left() {
    return left;
  }

  public Operand right() {
    return right;
  }

  @Override
  public String toString() {
    return String.format("%s = %s %s %s", destination, left, operator, right);
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }
}
