package com.plasstech.lang.d2.codegen.il;

import com.plasstech.lang.d2.lex.Token;

public class BinOp extends Op {
  private final String lhs;
  private final Token.Type operator;
  private final String rhs1;
  private final String rhs2;

  // TODO: check the token type
  public BinOp(String lhs, String rhs1, Token.Type operator, String rhs2) {
    this.lhs = lhs;
    this.rhs1 = rhs1;
    this.operator = operator;
    this.rhs2 = rhs2;
  }

  public String lhs() {
    return lhs;
  }

  public Token.Type operator() {
    return operator;
  }

  public String rhs1() {
    return rhs1;
  }

  public String rhs2() {
    return rhs2;
  }

  @Override
  public String toString() {
    return String.format("\t%s = %s %s %s;", lhs, rhs1, operator.value(), rhs2);
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }

}
