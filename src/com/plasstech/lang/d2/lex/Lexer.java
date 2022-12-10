package com.plasstech.lang.d2.lex;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.common.TokenType;

public class Lexer {
  private final String text;

  private int line, col; // current line & colum=n
  private int loc; // location inside text
  private char cc; // current character

  private Map<Character, Character> BACKSLASH_ESCAPE_MAP =
      ImmutableMap.<Character, Character>builder()
          .put('n', '\n')
          .put('r', '\r')
          .put('t', '\t')
          .put('"', '"')
          .put('\'', '\'')
          .put('\\', '\\')
          .build();

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
      return makeNumber(start);
    } else if (Character.isLetter(cc) || cc == '_') {
      return makeText(start);
    } else if (cc != 0) {
      return makeSymbol(start);
    }

    return new Token(TokenType.EOF, start);
  }

  /**
   * Read letters/numbers/underscore until whitespace. Then figure out if it's a keyword or a
   * variable.
   */
  private Token makeText(Position start) {
    StringBuilder sb = new StringBuilder();
    if (Character.isLetter(cc) || cc == '_') {
      sb.append(cc);
      advance();
    }
    while (Character.isLetterOrDigit(cc) || cc == '_') {
      sb.append(cc);
      advance();
    }

    String value = sb.toString();
    if (value.startsWith("_")) {
      throw new ScannerException(String.format("Illegal variable name %s", value), start);
    }
    Position end = new Position(line, col);
    try {
      // Figure out which keyword it is
      TokenType keywordType = TokenType.valueOf(value.toUpperCase());
      if (keywordType.isKeyword()) {
        return new Token(keywordType, start);
      }
    } catch (Exception e) {
    }
    // Not a keyword, must be a variable.
    return new Token(TokenType.VARIABLE, start, end, value);
  }

  private Token makeNumber(Position start) {
    StringBuilder sb = new StringBuilder();
    // This is non-standard but I don't care. It's my language and you can suck it.
    // 0y7f means a byte (in hex)
    if (cc == '0') {
      sb.append(cc);
      advance(); // eat the '0'
      if (cc == 'y') {
        advance(); // eat the 'y'
        // byte constant. clear the stringbuilder.
        sb = new StringBuilder();
        while (Character.isDigit(cc) || (cc >= 'A' && cc <= 'F') || (cc >= 'a' && cc <= 'f')) {
          sb.append(cc);
          advance();
        }
        if (sb.length() == 0) {
          throw new ScannerException("Invalid byte constant", start);
        }
        if (sb.length() > 2) {
          throw new ScannerException(String.format("Byte constant too big 0y%s", sb), start);
        }
        try {
          Position end = new Position(line, col);
          // Cannot use Byte.parseByte because it rejects "some" values, huh.
          int value = Integer.parseInt(sb.toString(), 16);
          return new ConstToken<Byte>(TokenType.BYTE, (byte) value, sb.toString(), start, end);
        } catch (Exception e) {
          throw new ScannerException(String.format("Byte constant out of range 0y%s", sb), start);
        }
      }
    }
    while (Character.isDigit(cc)) {
      sb.append(cc);
      advance();
    }
    // if cc is a dot, we're making a double
    if (cc == '.') {
      sb.append(cc);
      advance();
      while (Character.isDigit(cc)) {
        sb.append(cc);
        advance();
      }
      try {
        Position end = new Position(line, col);
        double value = Double.parseDouble(sb.toString());
        return new ConstToken<Double>(TokenType.DOUBLE, value, start, end);
      } catch (Exception e) {
        throw new ScannerException(String.format("Double constant too big %s", sb), start);
      }
    }
    try {
      Position end = new Position(line, col);
      int value = Integer.parseInt(sb.toString());
      return new ConstToken<Integer>(TokenType.INT, value, start, end);
    } catch (Exception e) {
      throw new ScannerException(String.format("Integer constant too big %s", sb), start);
    }
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
        return startsWithPlus(start);
      case '-':
        return startsWithMinus(start);
      case '(':
        advance();
        return new Token(TokenType.LPAREN, start, oc);
      case ')':
        advance();
        return new Token(TokenType.RPAREN, start, oc);
      case '*':
        advance();
        return new Token(TokenType.MULT, start, oc);
      case '/':
        return startsWithSlash(start);
      case '%':
        advance();
        return new Token(TokenType.MOD, start, oc);
      case '&':
        advance();
        return new Token(TokenType.BIT_AND, start, oc);
      case '|':
        advance();
        return new Token(TokenType.BIT_OR, start, oc);
      case '!':
        return startsWithNot(start);
      case '{':
        advance();
        return new Token(TokenType.LBRACE, start, oc);
      case '}':
        advance();
        return new Token(TokenType.RBRACE, start, oc);
      case ':':
        advance();
        return new Token(TokenType.COLON, start, oc);
      case '"':
      case '\'':
        return makeStringToken(start, oc);
      case ',':
        advance();
        return new Token(TokenType.COMMA, start, oc);
      case '[':
        advance();
        return new Token(TokenType.LBRACKET, start, oc);
      case ']':
        advance();
        return new Token(TokenType.RBRACKET, start, oc);
      case '^':
        advance();
        return new Token(TokenType.BIT_XOR, start, oc);
      case '.':
        advance();
        return new Token(TokenType.DOT, start, oc);
      default:
        throw new ScannerException(String.format("Unexpected character '%c'", cc), start);
    }
  }

  private Token startsWithPlus(Position start) {
    char oc = cc;
    advance(); // eat the first +
    if (cc == '+') {
      Position end = new Position(line, col);
      advance(); // eat the second +
      return new Token(TokenType.INCREMENT, start, end, "++");
    } else {
      return new Token(TokenType.PLUS, start, oc);
    }
  }

  private Token startsWithMinus(Position start) {
    char oc = cc;
    advance(); // eat the first -
    if (cc == '-') {
      Position end = new Position(line, col);
      advance(); // eat the second -
      return new Token(TokenType.DECREMENT, start, end, "--");
    } else {
      return new Token(TokenType.MINUS, start, oc);
    }
  }

  private Token startsWithSlash(Position start) {
    advance(); // eat the first slash
    if (cc == '/') {
      // It's a comment! Advance until the next line or EOF.
      advance(); // eat the second slash
      while (cc != '\n' && cc != 0) {
        advance(); // advance until newline or EOF.
      }
      if (cc != 0) {
        advance(); // eat the newline
      }
      line++;
      col = 0;
      return nextToken(); // risky, but /shrug.
    }
    return new Token(TokenType.DIV, start, '/');
  }

  private Token startsWithNot(Position start) {
    char oc = cc;
    advance(); // eat the !
    if (cc == '=') {
      Position end = new Position(line, col);
      advance(); // eat the =
      return new Token(TokenType.NEQ, start, end, "!=");
    }
    return new Token(TokenType.BIT_NOT, start, oc);
  }

  private Token startsWithGt(Position start) {
    char oc = cc;
    advance(); // eat the >
    if (cc == '=') {
      Position end = new Position(line, col);
      advance(); // eat the =
      return new Token(TokenType.GEQ, start, end, ">=");
    } else if (cc == '>') {
      Position end = new Position(line, col);
      advance(); // eat the >
      return new Token(TokenType.SHIFT_RIGHT, start, end, ">>");
    }
    return new Token(TokenType.GT, start, oc);
  }

  private Token startsWithLt(Position start) {
    char oc = cc;
    advance(); // eat the <
    if (cc == '=') {
      Position end = new Position(line, col);
      advance(); // eat the =
      return new Token(TokenType.LEQ, start, end, "<=");
    } else if (cc == '<') {
      Position end = new Position(line, col);
      advance(); // eat the <
      return new Token(TokenType.SHIFT_LEFT, start, end, "<<");
    }
    return new Token(TokenType.LT, start, oc);
  }

  private Token startsWithEq(Position start) {
    char oc = cc;
    advance(); // eat the =
    if (cc == '=') {
      Position end = new Position(line, col);
      advance(); // eat the second =
      return new Token(TokenType.EQEQ, start, end, "==");
    }
    return new Token(TokenType.EQ, start, oc);
  }

  private Token makeStringToken(Position start, char openingChar) {
    advance(); // eat the opening tick/quote
    StringBuilder sb = new StringBuilder();
    boolean escape = false;
    // Take all characters until the closing tick/quote
    while ((cc != openingChar || escape) && cc != 0) {
      if (cc == '\n') {
        line++;
        col = 0;
      }

      if (!escape) {
        escape = (cc == '\\');
        if (!escape) {
          sb.append(cc);
        }
      } else {
        Character escaped = BACKSLASH_ESCAPE_MAP.get(cc);
        if (escaped == null) {
          throw new ScannerException("Unknown backslash escape: \\" + cc, start);
        }
        sb.append(escaped);
        escape = false;
      }
      advance();
    }

    if (cc == 0) {
      throw new ScannerException("Unclosed string literal", start);
    }

    advance(); // eat the closing tick/quote
    Position end = new Position(line, col);
    return new ConstToken<String>(TokenType.STRING, sb.toString(), start, end);
  }
}
