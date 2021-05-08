package com.plasstech.lang.d2.lex;

import com.plasstech.lang.d2.common.Position;

public class KeywordToken extends Token {
  public enum KeywordType {
    PRINT, TRUE, FALSE, IF, ELSE, ELIF, MAIN, WHILE, DO
  }

  private final KeywordType type;

  // reject if type is true or false
  public KeywordToken(Position posStart, Position posEnd, KeywordType type) {
    super(Type.KEYWORD, posStart, posEnd, type.name());
    this.type = type;
  }

  public KeywordType keyword() {
    return type;
  }
}
