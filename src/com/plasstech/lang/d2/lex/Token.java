package com.plasstech.lang.d2.lex;

import com.plasstech.lang.d2.common.Position;

public class Token {

  public enum Type {
    INT(true), BOOL(true), // indicates the "bool" keyword"
    STRING(true), VARIABLE, //
    PRINT(true), PRINTLN(true), TRUE(true), FALSE(true), IF(true), ELSE(true), ELIF(true), //
    MAIN(true), PROC(true), RETURN(true), WHILE(true), DO(true), BREAK(true), CONTINUE(true), //
    // Unary operators:
    LENGTH(true), CHR(true), ASC(true), //
    EQ, //
    EQEQ, LT, GT, LEQ, GEQ, NEQ, //
    // Booleans
    NOT, AND, OR, //
    // Binary
    PLUS, MINUS, MULT, DIV, MOD, //
    LPAREN, RPAREN, //
    LBRACE, RBRACE, //
    LBRACKET, RBRACKET, //
    COLON, COMMA, //
    EOF;

    private final boolean keyword;

    Type() {
      this(false);
    }

    Type(boolean keyword) {
      this.keyword = keyword;
    }

    public boolean isKeyword() {
      return keyword;
    }
  }

  private final Type type;
  private final Position start;
  private final Position end;
  private final String value;

  // simple token, no extra text. Example: EOF
  public Token(Type type, Position pos) {
    this(type, pos, pos, type.name());
  }

  public Token(Type type, Position pos, char cc) {
    this(type, pos, pos, String.valueOf(cc));
  }

  public Token(Type type, Position start, Position end, String value) {
    this.type = type;
    this.start = start;
    this.end = end;
    this.value = value;
  }

  public Type type() {
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
    return String.format("%s (%s, [%s - %s])", value, type.name(), start.toString(),
            end.toString());
  }
}
