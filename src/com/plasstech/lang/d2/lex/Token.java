package com.plasstech.lang.d2.lex;

import com.plasstech.lang.d2.common.Position;

public class Token {

  public enum Type {
    KEYWORD, INT, BOOL, STRING, VARIABLE, EQ("="), NOT("!"), PLUS("+"), MINUS("-"), LPAREN("("),
    RPAREN(")"), MULT("*"), DIV("/"), MOD("%"), AND("&&"), OR("||"), EQEQ("=="), LT("<"),
    GT(">"), LEQ("<="), GEQ(">="), NEQ("!="), LBRACE, RBRACE, COLON(":"), COLON(":"), COMMA(","),
    LBRACKET("["), RBRACKET("]"), EOF;

    private final String val;

    public String value() {
      return val;
    }

    Type(String val) {
      this.val = val;
    }

    Type() {
      this.val = name();
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
