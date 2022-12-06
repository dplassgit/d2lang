package com.plasstech.lang.d2.lex;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import com.plasstech.lang.d2.common.TokenType;

public class LexerTest {
  @Test
  public void singleSymbols() {
    Lexer lexer = new Lexer("+-*/%()! =<>|&{}:[].");
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.PLUS);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.MINUS);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.MULT);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.DIV);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.MOD);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.LPAREN);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.RPAREN);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.BIT_NOT);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.EQ);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.LT);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.GT);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.BIT_OR);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.BIT_AND);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.LBRACE);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.RBRACE);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.COLON);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.LBRACKET);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.RBRACKET);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.DOT);
  }

  @Test
  public void doubleSymbols() {
    Lexer lexer = new Lexer("== <= >= != >> <<");
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.EQEQ);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.LEQ);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.GEQ);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.NEQ);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.SHIFT_RIGHT);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.SHIFT_LEFT);
  }

  @Test
  public void invalidSingleChars() {
    assertThrows(ScannerException.class, () -> new Lexer("@").nextToken());
    assertThrows(ScannerException.class, () -> new Lexer(";").nextToken());
    assertThrows(ScannerException.class, () -> new Lexer("�").nextToken());
    assertThrows(ScannerException.class, () -> new Lexer("�").nextToken());
  }

  @Test
  public void longerInt() {
    Lexer lexer = new Lexer("1234");
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.INT);
    assertThat(token.text()).isEqualTo("1234");
    ConstToken<Integer> itt = (ConstToken<Integer>) token;
    assertThat(itt.value()).isEqualTo(1234);
  }

  @Test
  public void byteConstant() {
    Lexer lexer = new Lexer("0y3F");
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.BYTE);
    assertThat(token.text()).isEqualTo("3F");
    ConstToken<Byte> itt = (ConstToken<Byte>) token;
    assertThat(itt.value()).isEqualTo((byte) 0x3F);
  }

  @Test
  public void byteConstantOneDigit() {
    Lexer lexer = new Lexer("0y3 f");
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.BYTE);
    assertThat(token.text()).isEqualTo("3");
    ConstToken<Byte> itt = (ConstToken<Byte>) token;
    assertThat(itt.value()).isEqualTo(3);
  }

  @Test
  public void tooBigByte() {
    Lexer lexer = new Lexer("0y1234567890123");
    ScannerException exception = assertThrows(ScannerException.class, () -> lexer.nextToken());
    assertThat(exception).hasMessageThat().contains("Byte constant too big");
  }

  @Test
  public void tooShortByte() {
    Lexer lexer = new Lexer("0y 123");
    ScannerException exception = assertThrows(ScannerException.class, () -> lexer.nextToken());
    assertThat(exception).hasMessageThat().contains("Invalid byte constant");
  }

  @Test
  public void caseInsensitiveByteConstant() {
    Lexer lexer = new Lexer("0yeA");
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.BYTE);
    assertThat(token.text()).isEqualTo("eA");
    ConstToken<Byte> itt = (ConstToken<Byte>) token;
    assertThat(itt.value()).isEqualTo((byte) 0xea);
  }

  @Test
  public void doubleToken() {
    Lexer lexer = new Lexer("1234.5");
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.DOUBLE);
    assertThat(token.text()).isEqualTo("1234.5");
    ConstToken<Double> itt = (ConstToken<Double>) token;
    assertThat(itt.value()).isEqualTo(1234.5);
  }

  @Test
  public void doubleTokenJustADot() {
    Lexer lexer = new Lexer("1234. next");
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.DOUBLE);
    assertThat(token.text()).isEqualTo("1234.0");
    ConstToken<Double> itt = (ConstToken<Double>) token;
    assertThat(itt.value()).isEqualTo(1234.);
  }

  @Test
  public void tooLongInt() {
    Lexer lexer = new Lexer("1234567890123");
    assertThrows(ScannerException.class, () -> lexer.nextToken());
  }

  @Test
  public void eof() {
    Lexer lexer = new Lexer("1");
    lexer.nextToken();
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.EOF);
  }

  @Test
  public void twoNumbers() {
    Lexer lexer = new Lexer("1 2");
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.INT);
    assertThat(token.text()).isEqualTo("1");

    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.INT);
    assertThat(token.text()).isEqualTo("2");
  }

  @Test
  public void whiteSpace() {
    Lexer lexer = new Lexer("1\n\t 23");
    ConstToken<Integer> token = (ConstToken<Integer>) lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.INT);
    assertThat(token.text()).isEqualTo("1");
    assertThat(token.value()).isEqualTo(1);
    assertThat(token.start().line()).isEqualTo(1);
    assertThat(token.start().column()).isEqualTo(1);
    assertThat(token.end().line()).isEqualTo(1);
    assertThat(token.end().column()).isEqualTo(2);

    token = (ConstToken<Integer>) lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.INT);
    assertThat(token.text()).isEqualTo("23");
    assertThat(token.value()).isEqualTo(23);
    assertThat(token.start().line()).isEqualTo(2);
    assertThat(token.start().column()).isEqualTo(3);
    assertThat(token.end().line()).isEqualTo(2);
    assertThat(token.end().column()).isEqualTo(4);
  }

  @Test
  public void trueFalse() {
    Lexer lexer = new Lexer("true false True FALSE");
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.TRUE);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.FALSE);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.TRUE);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.FALSE);
  }

  @Test
  public void keyword() {
    Lexer lexer =
        new Lexer(
            "print PrintLN IF Else elif do while break continue int bool string double byte long "
                + " proc return length asc chr exit and or not xor length new record");

    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.PRINT);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.PRINTLN);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.IF);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.ELSE);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.ELIF);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.DO);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.WHILE);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.BREAK);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.CONTINUE);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.INT);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.BOOL);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.STRING);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.DOUBLE);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.BYTE);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.LONG);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.PROC);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.RETURN);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.LENGTH);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.ASC);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.CHR);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.EXIT);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.AND);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.OR);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.NOT);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.XOR);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.LENGTH);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.NEW);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.RECORD);
    assertThat(token.type().isKeyword()).isTrue();
  }

  @Test
  public void mixed() {
    Lexer lexer = new Lexer("print 3 p");
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.PRINT);
    assertThat(token.type().isKeyword()).isTrue();

    ConstToken<Integer> intToken = (ConstToken<Integer>) lexer.nextToken();
    assertThat(intToken.type()).isEqualTo(TokenType.INT);
    assertThat(intToken.value()).isEqualTo(3);

    Token varToken = lexer.nextToken();
    assertThat(varToken.type()).isEqualTo(TokenType.VARIABLE);
    assertThat(varToken.text()).isEqualTo("p");
  }

  @Test
  public void longNotKeyword() {
    Lexer lexer = new Lexer("printed");
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.VARIABLE);
    assertThat(token.text()).isEqualTo("printed");
  }

  @Test
  public void variable() {
    Lexer lexer = new Lexer("prin");
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.VARIABLE);
    assertThat(token.text()).isEqualTo("prin");
  }

  @Test
  public void variableAlnum() {
    Lexer lexer = new Lexer("prin2");
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.VARIABLE);
    assertThat(token.text()).isEqualTo("prin2");
  }

  @Test
  public void underscore() {
    Lexer lexer = new Lexer("TYPE_token token_ token_with_lots_of_");
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.VARIABLE);
    assertThat(token.text()).isEqualTo("TYPE_token");
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.VARIABLE);
    assertThat(token.text()).isEqualTo("token_");
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.VARIABLE);
    assertThat(token.text()).isEqualTo("token_with_lots_of_");
  }

  @Test
  public void leading_underscore_not_allowed() {
    assertThrows(ScannerException.class, () -> new Lexer("_token").nextToken());
    assertThrows(ScannerException.class, () -> new Lexer("__token").nextToken());
    assertThrows(ScannerException.class, () -> new Lexer("_").nextToken());
    assertThrows(ScannerException.class, () -> new Lexer("__").nextToken());
  }

  @Test
  public void assign() {
    Lexer lexer = new Lexer("a = 3");

    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.VARIABLE);
    assertThat(token.text()).isEqualTo("a");

    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.EQ);

    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.INT);
    assertThat(token.text()).isEqualTo("3");
  }

  @Test
  public void assign_expr() {
    Lexer lexer = new Lexer("a=3+4");

    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.VARIABLE);
    assertThat(token.text()).isEqualTo("a");

    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.EQ);

    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.INT);
    assertThat(token.text()).isEqualTo("3");

    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.PLUS);
    assertThat(token.text()).isEqualTo("+");

    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.INT);
    assertThat(token.text()).isEqualTo("4");
  }

  @Test
  public void comment() {
    Lexer lexer = new Lexer("1// ignored\na");

    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.INT);
    assertThat(token.text()).isEqualTo("1");

    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.VARIABLE);
    assertThat(token.text()).isEqualTo("a");

    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.EOF);
  }

  @Test
  public void commentEom() {
    Lexer lexer = new Lexer("1// ignored\n");

    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.INT);
    assertThat(token.text()).isEqualTo("1");

    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.EOF);
  }

  @Test
  public void commentEol() {
    Lexer lexer = new Lexer("1// ignored");

    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.INT);
    assertThat(token.text()).isEqualTo("1");

    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.EOF);
  }

  @Test
  public void commentCrLf() {
    Lexer lexer = new Lexer("1// ignored\r\n");

    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.INT);
    assertThat(token.text()).isEqualTo("1");

    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.EOF);
  }

  @Test
  public void commentLfCr() {
    Lexer lexer = new Lexer("1// ignored\n\r");

    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.INT);
    assertThat(token.text()).isEqualTo("1");

    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.EOF);
  }

  @Test
  public void commentsEol() {
    Lexer lexer = new Lexer("1// ignored\n// so is this...\nb");

    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.INT);
    assertThat(token.text()).isEqualTo("1");

    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.VARIABLE);
    assertThat(token.text()).isEqualTo("b");

    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.EOF);
  }

  @Test
  public void stringTick() {
    Lexer lexer = new Lexer("'Hi'");
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.STRING);
    assertThat(token.text()).isEqualTo("Hi");
  }

  @Test
  public void stringQuotes() {
    Lexer lexer = new Lexer("\"Hi\"");
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.STRING);
    assertThat(token.text()).isEqualTo("Hi");
  }

  @Test
  public void stringEmpty() {
    Lexer lexer = new Lexer("''");
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.STRING);
    assertThat(token.text()).isEqualTo("");
  }

  @Test
  public void stringSpace() {
    Lexer lexer = new Lexer("' '");
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.STRING);
    assertThat(token.text()).isEqualTo(" ");
  }

  @Test
  public void stringOpen_error() {
    Lexer lexer = new Lexer("\"Hi");
    assertThrows(ScannerException.class, () -> lexer.nextToken());
  }

  @Test
  public void backslashEscapes() {
    // Trust me.
    String input = "'\\n \\\\ \\' \\\"'";
    Lexer lexer = new Lexer(input);
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.STRING);
    assertThat(token.text()).isEqualTo("\n \\ ' \"");
  }

  @Test
  public void backslashEscapesDoubleQuotes() {
    // Trust me.
    String input = "\" \\n \\\\ \\' \\\" \"";
    Lexer lexer = new Lexer(input);
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.STRING);
    assertThat(token.text()).isEqualTo(" \n \\ ' \" ");
  }

  @Test
  public void bad_backslashes() {
    assertThrows(ScannerException.class, () -> new Lexer("'\\a'").nextToken());
    assertThrows(ScannerException.class, () -> new Lexer("'\\0'").nextToken());
    assertThrows(ScannerException.class, () -> new Lexer("'\\v'").nextToken());
    assertThrows(ScannerException.class, () -> new Lexer("'\\N'").nextToken());
    ScannerException exception =
        assertThrows(ScannerException.class, () -> new Lexer("'\\").nextToken());
    assertThat(exception).hasMessageThat().contains("Unclosed");
  }
}
