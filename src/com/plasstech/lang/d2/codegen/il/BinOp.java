package com.plasstech.lang.d2.codegen.il;

import com.plasstech.lang.d2.lex.Token;

public class BinOp extends Op {
  private final Location destination;
  private final Token.Type operator;
  private final Operand rhs1;
  private final Operand rhs2;

  // TODO: check the token type
  public BinOp(Location destination, Operand rhs1, Token.Type operator, Operand rhs2) {
    this.destination = destination;
    this.rhs1 = rhs1;
    this.operator = operator;
    this.rhs2 = rhs2;
  }

  public Location destination() {
    return destination;
  }

  public Token.Type operator() {
    return operator;
  }

  public Operand rhs1() {
    return rhs1;
  }

  public Operand rhs2() {
    return rhs2;
  }

  @Override
  public String toString() {
    return String.format("%s = %s %s %s;", destination, rhs1, operator.value(), rhs2);
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }

}
