package com.plasstech.lang.d2.codegen.il;

import com.plasstech.lang.d2.lex.Token;

public class BinOp extends Op {
  private final String lhs;
  private final Token.Type type;
  private final String rhs1;
  private final String rhs2;

  public BinOp(String lhs, String rhs1, Token.Type type, String rhs2) {
    this.lhs = lhs;
    this.rhs1 = rhs1;
    this.type = type;
    this.rhs2 = rhs2;
  }

  public String lhs() {
    return lhs;
  }

  public Token.Type type() {
    return type;
  }

  public String rhs1() {
    return rhs1;
  }

  public String rhs2() {
    return rhs2;
  }

  @Override
  public String toString() {
    return String.format("%s=%s %s %s", lhs, rhs1, type.name(), rhs2);
  }
}
