package com.plasstech.lang.d2.codegen.il;

import com.plasstech.lang.d2.lex.Token;

public class ArithOp extends Op {
  private final String lhs;
  private final Token.Type type;
  private final String rhs;

  public ArithOp(String lhs, Token.Type type, String rhs) {
    this.lhs = lhs;
    this.type = type;
    this.rhs = rhs;
  }

  public String lhs() {
    return lhs;
  }

  public String rhs() {
    return rhs;
  }

  public Token.Type type() {
    return type;
  }

  @Override
  public String toString() {
    return String.format("%s %s= %s", lhs, type, rhs);
  }

}
