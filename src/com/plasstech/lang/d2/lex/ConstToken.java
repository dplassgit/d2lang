package com.plasstech.lang.d2.lex;

import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.common.TokenType;

public class ConstToken<T> extends Token {
  private final T value;

  public ConstToken(TokenType type, T value, Position start, Position end) {
    super(type, start, end, String.valueOf(value));
    this.value = value;
  }

  public T value() {
    return value;
  }
}
