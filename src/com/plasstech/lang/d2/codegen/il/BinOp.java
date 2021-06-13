package com.plasstech.lang.d2.codegen.il;

import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.lex.Token;

public class BinOp extends Op {
  private final Location destination;
  private final Token.Type operator;
  private final Operand left;
  private final Operand right;

  // TODO: add types, to aid in codegen
  public BinOp(Location destination, Operand left, Token.Type operator, Operand right) {
    this.destination = destination;
    this.left = left;
    this.operator = operator;
    this.right = right;
  }

  public Location destination() {
    return destination;
  }

  public Token.Type operator() {
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
    return String.format("%s = %s %s %s;", destination, left, operator, right);
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }

}
