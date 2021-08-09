package com.plasstech.lang.d2.common;

public enum TokenType {
  // Keywords:
  INT(true), // indicates the "int" keyword
  BOOL(true), // indicates the "bool" keyword
  STRING(true), // indicates the "string" keyword
  VARIABLE,
  PRINT(true),
  PRINTLN(true),
  TRUE(true),
  FALSE(true),
  NULL(true),
  IF(true),
  ELSE(true),
  ELIF(true),
  MAIN(true),
  PROC(true),
  RETURN(true),
  WHILE(true),
  DO(true),
  BREAK(true),
  CONTINUE(true),
  RECORD(true),
  DELETE(true), // for future expansion
  MAP(true), // for future expansion
  INPUT(true),
  EXIT(true),
  // Unary operators (& keywords)
  NEW(true),
  LENGTH(true),
  CHR(true),
  ASC(true),
  // Comparisons:
  EQ("="),
  EQEQ("=="),
  LT("<"),
  GT(">"),
  LEQ("<="),
  GEQ(">="),
  NEQ("!="),
  // Boolean operators (& keywords)
  NOT(true), // it won't accept ! as not boolean anymore.
  AND(true),
  OR(true),
  XOR(true),
  // Binary operators
  PLUS("+"),
  MINUS("-"),
  MULT("*"),
  DIV("/"),
  MOD("%"),
  SHIFT_LEFT("<<"),
  SHIFT_RIGHT(">>"),
  BIT_AND("&"),
  BIT_XOR("^"),
  BIT_OR("|"),
  BIT_NOT("!"),
  // Separators
  LPAREN("("),
  RPAREN(")"),
  LBRACE("{"),
  RBRACE("}"),
  LBRACKET("["),
  RBRACKET("]"),
  COLON(":"),
  COMMA(","),
  DOT("."),
  EOF;

  private final String abbreviation;
  private final boolean keyword;

  TokenType() {
    this(false, null);
  }

  TokenType(String abbreviation) {
    this(false, abbreviation);
  }

  TokenType(boolean keyword) {
    this(keyword, null);
  }

  TokenType(boolean keyword, String abbreviation) {
    this.keyword = keyword;
    if (abbreviation == null) {
      this.abbreviation = name();
    } else {
      this.abbreviation = abbreviation;
    }
  }

  public boolean isKeyword() {
    return keyword;
  }

  @Override
  public String toString() {
    return abbreviation;
  }
}