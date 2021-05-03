package com.plasstech.lang.d2.lex;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.lex.KeywordToken.KeywordType;
import com.plasstech.lang.d2.lex.Token.Type;

public class Lexer {
  private static final Set<
          Character> SYMBOL_STARTS = ImmutableSet.of('=', '+', '-', '(', ')', '*', '/');

  private final String text;
  private int line, col;
  // location inside text
  private int loc;
  private char cc; // current character

  public Lexer(String text) {
    this.text = text;
    this.loc = 0;
    this.line = 1;
    this.col = 0;
    advance();
  }

  private char advance() {
    if (loc < text.length()) {
      cc = text.charAt(loc);
      col++;
    } else {
      cc = 0;
    }
    loc++;
    return cc;
  }

  public Token nextToken() {
    // skip unwanted whitespace
    while (cc == ' ' || cc == '\n' || cc == '\t') {
      if (cc == '\n') {
        line++;
        col = 0;
      }
      advance();
    }
    if (Character.isDigit(cc)) {
      return makeInt();
    } else if (Character.isLetter(cc)) {
      return makeText();
    } else if (SYMBOL_STARTS.contains(cc)) {
      return makeSymbol();
    } else if (cc != 0) {
      Position loc = new Position(line, col);
      throw new RuntimeException(String.format("Unknown character %c at location %s", cc, loc));
    }
    Position start = new Position(line, col);
    return new Token(Type.EOF, start);
  }

  private Token makeText() {
    StringBuilder sb = new StringBuilder();
    Position start = new Position(line, col);
    if (Character.isLetter(cc)) {
      sb.append(cc);
      advance();
    }
    while (Character.isLetterOrDigit(cc)) {
      sb.append(cc);
      advance();
    }
    String value = sb.toString();
    Position end = new Position(line, col);
    try {
      // Figure out which keyword it is
      KeywordType keywordType = KeywordType.valueOf(value.toUpperCase());
      return new KeywordToken(start, end, keywordType);
    } catch (Exception e) {
      return new Token(Type.VARIABLE, start, end, value);
    }
  }

  private IntToken makeInt() {
    int value = 0;
    Position start = new Position(line, col);
    while (Character.isDigit(cc)) {
      value = value * 10 + (cc - '0');
      advance();
    }
    Position end = new Position(line, col);
    return new IntToken(start, end, value);
  }

  private Token makeSymbol() {
    Position loc = new Position(line, col);
    char oc = cc;
    switch (oc) {
      case '=':
      advance();
      return new Token(Type.EQ, loc, oc);
    case '+':
      advance();
      return new Token(Type.PLUS, loc, oc);
    case '-':
      advance();
      return new Token(Type.MINUS, loc, oc);
    case '(':
      advance();
      return new Token(Type.LPAREN, loc, oc);
    case ')':
      advance();
      return new Token(Type.RPAREN, loc, oc);
    case '*':
      advance();
      return new Token(Type.MULT, loc, oc);
    case '/':
      advance();
      return new Token(Type.DIV, loc, oc);
    default:
      throw new RuntimeException(String.format("Unknown character %c at location %s", cc, loc));
    }
  }
}
