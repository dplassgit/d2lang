package com.plasstech.lang.d2.lex;

import com.plasstech.lang.d2.common.Position;

public class BoolToken extends Token {

  private final boolean value;

  public BoolToken(Position pos, boolean value) {
    super(Type.BOOL, pos, pos, String.valueOf(value));
    this.value = value;
  }

  public boolean value() {
    return value;
  }
}
