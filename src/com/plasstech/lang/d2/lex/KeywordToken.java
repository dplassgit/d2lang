package com.plasstech.lang.d2.lex;

import com.google.common.base.Preconditions;
import com.plasstech.lang.d2.common.Position;

public class KeywordToken extends Token {
  public enum KeywordType {
    // maybe move all these into Token.Type and have bits for "is keyword"
    PRINT, PRINTLN, TRUE, FALSE, IF, ELSE, ELIF, MAIN, PROC, RETURN, WHILE, DO, BREAK,
    CONTINUE, INT, BOOL, STRING, LENGTH(Token.Type.LENGTH), ASC(Token.Type.ASC),
    CHR(Token.Type.CHR);

    // TODO: I hate this. It's stupid.
    private Token.Type unaryTokenType;

    KeywordType(Token.Type tokenType) {
      this.unaryTokenType = tokenType;
    }

    KeywordType() {
      this(null);
    }

    public boolean isUnary() {
      return unaryOperator() != null;
    }

    public Token.Type unaryOperator() {
      return unaryTokenType;
    }
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
