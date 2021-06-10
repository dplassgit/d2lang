package com.plasstech.lang.d2.codegen.il;

import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.lex.Token;

public class UnaryOp extends Op {
  private final Location destination;
  private final Token.Type operator;
  private final Operand operand;

  // TODO: check the token type
  public UnaryOp(Location destination, Token.Type operator, Operand operand) {
    this.destination = destination;
    this.operand = operand;
    this.operator = operator;
  }

  public Location destination() {
    return destination;
  }

  public Token.Type operator() {
    return operator;
  }

  public Operand rhs() {
    return operand;
  }

  @Override
  public String toString() {
    return String.format("%s = %s %s;", destination, operator.value(), operand);
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }
}
