package com.plasstech.lang.d2.codegen.il;

import com.plasstech.lang.d2.lex.Token;

public class UnaryOp extends Op {
  private final String lhs;
  private final Token.Type operator;
  private final String rhs;

  // TODO: check the token type
  public UnaryOp(String lhs, Token.Type operator, String rhs) {
    this.lhs = lhs;
    this.rhs = rhs;
    this.operator = operator;
  }

  public String lhs() {
    return lhs;
  }

  public Token.Type operator() {
    return operator;
  }

  public String rhs() {
    return rhs;
  }

  @Override
  public String toString() {
    return String.format("\t%s = %s %s;", lhs, operator.value(), rhs);
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }
}
