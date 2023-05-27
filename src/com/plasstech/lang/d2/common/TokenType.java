package com.plasstech.lang.d2.common;

public enum TokenType {
  VARIABLE, // all variables are tagged with this type
  LITERAL, // all literal values are tagged with this type
  // Keywords:
  INT(true), // indicates the "int" keyword
  BOOL(true), // indicates the "bool" keyword
  STRING(true), // indicates the "string" keyword
  DOUBLE(true), // indicates the "double" keyword
  LONG(true), // indicates the "long" keyword
  CHAR(true), // indicates the "char" keyword
  BYTE(true), // indicates the "byte" keyword
  TRUE(true), // boolean literal
  FALSE(true), // boolean literal
  PRINT(true),
  PRINTLN(true),
  NULL(true),
  IF(true),
  ELSE(true),
  ELIF(true),
  ARGS(true), // for command line arguments
  PROC(true),
  EXTERN(true), // reference an externally defined symbol
  RETURN(true),
  WHILE(true),
  DO(true),
  BREAK(true),
  CONTINUE(true),
  RECORD(true),
  INPUT(true),
  EXIT(true),
  // Unary operators (& keywords)
  NEW(true),
  LENGTH(true),
  CHR(true),
  ASC(true),
  INCREMENT("++"),
  DECREMENT("--"),
  // Comparisons:
  ASSIGN("="),
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
  // For future expansion:
  DELETE(true), // free a new
  FOR(true), // for (x in array/list)
  IN(true),
  GET(true), // get one character
  THIS(true), // for primitive classes
  PRIVATE(true), // for primitive classes
  LOAD(true), // load a file, maybe including binary type
  SAVE(true), // save a file
  EXPORT(true), // expose a symbol to externally
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
