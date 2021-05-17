package com.plasstech.lang.d2.lex;

import com.plasstech.lang.d2.common.Position;

public class KeywordToken extends Token {
  public enum KeywordType {
    PRINT, PRINTLN, TRUE, FALSE, IF, ELSE, ELIF, MAIN, WHILE, DO, BREAK, CONTINUE, INT, BOOL,
    STRING;
  }

  private final KeywordType type;

  // TODO: reject if type is true or false
  public KeywordToken(Position posStart, Position posEnd, KeywordType type) {
    super(Type.KEYWORD, posStart, posEnd, type.name());
    this.type = type;
  }

  public KeywordType keyword() {
    return type;
  }
}
