package com.plasstech.lang.d2.lex;

import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.lex.KeywordToken.KeywordType;
import com.plasstech.lang.d2.lex.Token.Type;

public class Lexer {
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
      // Indicates no more characters
      cc = 0;
    }
    loc++;
    return cc;
  }

  public Token nextToken() {
    // skip unwanted whitespace
    while (cc == ' ' || cc == '\n' || cc == '\t' || cc == '\r') {
      if (cc == '\n') {
        line++;
        col = 0;
      }
      advance();
    }

    Position start = new Position(line, col);
    if (Character.isDigit(cc)) {
      return makeInt(start);
    } else if (Character.isLetter(cc)) {
      return makeText(start);
    } else if (cc != 0) {
      return makeSymbol(start);
    }

    return new Token(Type.EOF, start);
  }

  private Token makeText(Position start) {
    StringBuilder sb = new StringBuilder();
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

      if (keywordType == KeywordType.TRUE) {
        return new BoolToken(start, true);
      } else if (keywordType == KeywordType.FALSE) {
        return new BoolToken(start, false);
      }

      return new KeywordToken(start, end, keywordType);
    } catch (Exception e) {
      return new Token(Type.VARIABLE, start, end, value);
    }
  }

  private IntToken makeInt(Position start) {
    int value = 0;
    while (Character.isDigit(cc)) {
      value = value * 10 + (cc - '0');
      advance();
    }
    Position end = new Position(line, col);
    return new IntToken(start, end, value);
  }

  private Token makeSymbol(Position start) {
    char oc = cc;
    switch (oc) {
      case '=':
        return startsWithEq(start);
      case '<':
        return startsWithLt(start);
      case '>':
        return startsWithGt(start);
      case '+':
        advance();
        return new Token(Type.PLUS, start, oc);
      case '-':
        advance();
        return new Token(Type.MINUS, start, oc);
      case '(':
        advance();
        return new Token(Type.LPAREN, start, oc);
      case ')':
        advance();
        return new Token(Type.RPAREN, start, oc);
      case '*':
        advance();
        return new Token(Type.MULT, start, oc);
      case '/':
        return startsWithSlash(start);
      case '%':
        advance();
        return new Token(Type.MOD, start, oc);
      case '&':
        advance();
        return new Token(Type.AND, start, oc);
      case '|':
        advance();
        return new Token(Type.OR, start, oc);
      case '!':
        return startsWithNot(start);
      case '{':
        advance();
        return new Token(Type.LBRACE, start, oc);
      case '}':
        advance();
        return new Token(Type.RBRACE, start, oc);
      case ':':
        advance();
        return new Token(Type.COLON, start, oc);
      case '"':
      case '\'':
        return makeStringToken(start, oc);
      case ',':
        advance();
        return new Token(Type.COMMA, start, oc);
      default:
        throw new ScannerException(String.format("Unknown character %c", cc), start);
    }
  }

  private Token startsWithSlash(Position start) {
    advance(); // eat the first slash
    if (cc == '/') {
      advance(); // eat the second slash
      while (cc != '\n' && cc != 0) {
        advance();
      }
      if (cc != 0) {
        advance();
      }
      line++;
      col = 0;
      return nextToken();
    }
    return new Token(Type.DIV, start, '/');
  }

  private Token startsWithNot(Position start) {
    char oc = cc;
    advance();
    if (cc == '=') {
      Position end = new Position(line, col);
      advance();
      return new Token(Type.NEQ, start, end, "!=");
    }
    return new Token(Type.NOT, start, oc);
  }

  private Token startsWithGt(Position start) {
    char oc = cc;
    advance();
    if (cc == '=') {
      Position end = new Position(line, col);
      advance();
      return new Token(Type.GEQ, start, end, ">=");
    }
    return new Token(Type.GT, start, oc);
  }

  private Token startsWithLt(Position start) {
    char oc = cc;
    advance();
    if (cc == '=') {
      Position end = new Position(line, col);
      advance();
      return new Token(Type.LEQ, start, end, "<=");
    }
    return new Token(Type.LT, start, oc);
  }

  private Token startsWithEq(Position start) {
    char oc = cc;
    advance();
    if (cc == '=') {
      Position end = new Position(line, col);
      advance();
      return new Token(Type.EQEQ, start, end, "==");
    }
    return new Token(Type.EQ, start, oc);
  }

  private Token makeStringToken(Position start, char first) {
    advance(); // eat the tick/quote
    StringBuilder sb = new StringBuilder();
    // TODO: fix backslash-escaping
    boolean escape = false;
    while (cc != first && cc != 0) {
      if (!escape) {
        sb.append(cc);
      }
      advance();
    }

    if (cc == 0) {
      throw new ScannerException("Unclosed string literal", start);
    }

    advance(); // eat the closing tick/quote
    Position end = new Position(line, col);
    return new Token(Type.STRING, start, end, sb.toString());
  }
}
