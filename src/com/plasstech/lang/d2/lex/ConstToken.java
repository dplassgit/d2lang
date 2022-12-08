package com.plasstech.lang.d2.lex;

import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.common.TokenType;

public class ConstToken<T> extends Token {
  private final T value;
  private final TokenType type;

  public ConstToken(TokenType type, T value, Position start, Position end) {
    super(TokenType.LITERAL, start, end, String.valueOf(value));
    this.type = type;
    this.value = value;
  }

  public ConstToken(TokenType type, T value, String text, Position start, Position end) {
    super(TokenType.LITERAL, start, end, text);
    this.type = type;
    this.value = value;
  }

  public T value() {
    return value;
  }

  public TokenType literalType() {
    return type;
  }
}
