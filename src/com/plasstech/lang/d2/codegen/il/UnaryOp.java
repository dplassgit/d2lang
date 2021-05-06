package com.plasstech.lang.d2.codegen.il;

import com.plasstech.lang.d2.lex.Token;

public class UnaryOp extends Op {
  private final String lhs;
  private final Token.Type type;
  private final String rhs;

  // TODO: check the token type
  public UnaryOp(String lhs, Token.Type type, String rhs) {
    this.lhs = lhs;
    this.rhs = rhs;
    this.type = type;
  }

  public String lhs() {
    return lhs;
  }

  public Token.Type type() {
    return type;
  }

  public String rhs() {
    return rhs;
  }

  @Override
  public String toString() {
    return String.format("%s=%s %s", lhs, type.name(), rhs);
  }
}
