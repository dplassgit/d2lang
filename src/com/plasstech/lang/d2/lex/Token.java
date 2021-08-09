package com.plasstech.lang.d2.lex;

import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.common.TokenType;

public class Token {

  private final TokenType type;
  private final Position start;
  private final Position end;
  private final String value;

  // simple token, no extra text. Example: EOF
  public Token(TokenType type, Position pos) {
    this(type, pos, pos, type.name());
  }

  public Token(TokenType type, Position pos, char cc) {
    this(type, pos, pos, String.valueOf(cc));
  }

  public Token(TokenType type, Position start, Position end, String value) {
    this.type = type;
    this.start = start;
    this.end = end;
    this.value = value;
  }

  public TokenType type() {
    return type;
  }

  public Position start() {
    return start;
  }

  public Position end() {
    return end;
  }

  public String text() {
    return value;
  }

  @Override
  public String toString() {
    return String.format(
        "%s (%s, [%s - %s])", value, type.name(), start.toString(), end.toString());
  }
}
