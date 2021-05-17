package com.plasstech.lang.d2.lex;

import com.google.common.base.Preconditions;
import com.plasstech.lang.d2.common.Position;

public class KeywordToken extends Token {
  public enum KeywordType {
    PRINT, PRINTLN, TRUE, FALSE, IF, ELSE, ELIF, MAIN, PROC, RETURNS, RETURN, WHILE, DO, BREAK,
    CONTINUE, INT, BOOL, STRING;
  }

  private final KeywordType type;

  public KeywordToken(Position posStart, Position posEnd, KeywordType type) {
    super(Type.KEYWORD, posStart, posEnd, type.name());
    Preconditions.checkArgument(type != KeywordType.TRUE && type != KeywordType.FALSE,
            "Cannot use TRUE or FALSE as KeywordToken type");
    this.type = type;
  }

  public KeywordType keyword() {
    return type;
  }
}
