package com.plasstech.lang.d2.lex;

import com.plasstech.lang.d2.common.Position;

public class IntToken extends Token {
  private final int value;

  public IntToken(Position posStart, Position posEnd, int value) {
    super(Type.INT, posStart, posEnd, String.valueOf(value));
    this.value = value;
  }

  public Integer value() {
    return value;
  }
}
