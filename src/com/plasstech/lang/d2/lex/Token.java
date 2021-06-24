package com.plasstech.lang.d2.lex;

import com.plasstech.lang.d2.common.Position;

public class Token {

  public enum Type {
    // Keywords:
    INT(true), // indicates the "int" keyword
    BOOL(true), // indicates the "bool" keyword
    STRING(true), // indicates the "string" keyword
    VARIABLE,
    PRINT(true),
    PRINTLN(true),
    TRUE(true),
    FALSE(true),
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
    RECORD(true), // for future expansion
    NEW(true), // for future expansion
    NULL(true), // for future expansion
    DELETE(true), // for future expansion
    MAP(true), // for future expansion
    INPUT(true),
    EXIT(true),
    // Unary operators (& keywords)
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
    // Booleans operators (& keywords)
    NOT(true),
    AND(true),
    OR(true),
    // Binary operators
    PLUS("+"),
    MINUS("-"),
    MULT("*"),
    DIV("/"),
    MOD("%"),
    SHIFT_LEFT("<<"),
    SHIFT_RIGHT(">>"),
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

    Type() {
      this(false, null);
    }

    Type(String abbreviation) {
      this(false, abbreviation);
    }

    Type(boolean keyword) {
      this(keyword, null);
    }

    Type(boolean keyword, String abbreviation) {
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
    return String.format(
        "%s (%s, [%s - %s])", value, type.name(), start.toString(), end.toString());
  }
}
