package com.plasstech.lang.d2.codegen.il;

import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.common.TokenType;

public class UnaryOp extends Op {
  private final Location destination;
  private final TokenType operator;
  private final Operand operand;

  // TODO: check the token type
  public UnaryOp(Location destination, TokenType operator, Operand operand, Position position) {
    super(position);
    this.destination = destination;
    this.operand = operand;
    this.operator = operator;
  }

  public Location destination() {
    return destination;
  }

  public TokenType operator() {
    return operator;
  }

  public Operand operand() {
    return operand;
  }

  @Override
  public String toString() {
    return String.format("%s = %s %s", destination, operator, operand);
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }
}
