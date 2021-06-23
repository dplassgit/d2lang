package com.plasstech.lang.d2.lex;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import com.plasstech.lang.d2.lex.Token.Type;

public class LexerTest {
  @Test
  public void singleSymbols() {
    Lexer lexer = new Lexer("+-*/%()! =<>|&{}:[]");
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.PLUS);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.MINUS);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.MULT);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.DIV);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.MOD);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.LPAREN);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.RPAREN);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.NOT);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.EQ);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.LT);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.GT);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.OR);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.AND);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.LBRACE);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.RBRACE);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.COLON);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.LBRACKET);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.RBRACKET);
  }

  @Test
  public void doubleSymbols() {
    Lexer lexer = new Lexer("== <= >= != >> <<");
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.EQEQ);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.LEQ);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.GEQ);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.NEQ);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.SHIFT_RIGHT);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.SHIFT_LEFT);
  }

  @Test
  public void longerInt() {
    Lexer lexer = new Lexer("1234");
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.INT);
    assertThat(token.text()).isEqualTo("1234");
    IntToken itt = (IntToken) token;
    assertThat(itt.value()).isEqualTo(1234);
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
    assertThat(token.type()).isEqualTo(Type.EOF);
  }

  @Test
  public void twoNumbers() {
    Lexer lexer = new Lexer("1 2");
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.INT);
    assertThat(token.text()).isEqualTo("1");

    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.INT);
    assertThat(token.text()).isEqualTo("2");
  }

  @Test
  public void whiteSpace() {
    Lexer lexer = new Lexer("1\n\t 23");
    IntToken token = (IntToken) lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.INT);
    assertThat(token.text()).isEqualTo("1");
    assertThat(token.value()).isEqualTo(1);
    assertThat(token.start().line()).isEqualTo(1);
    assertThat(token.start().column()).isEqualTo(1);
    assertThat(token.end().line()).isEqualTo(1);
    assertThat(token.end().column()).isEqualTo(2);

    token = (IntToken) lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.INT);
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
    assertThat(token.type()).isEqualTo(Token.Type.TRUE);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Token.Type.FALSE);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Token.Type.TRUE);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Token.Type.FALSE);
  }

  @Test
  public void keyword() {
    Lexer lexer =
        new Lexer(
            "print PrintLN IF Else elif do while break continue int bool proc return length asc"
                + " chr exit");

    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Token.Type.PRINT);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Token.Type.PRINTLN);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Token.Type.IF);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Token.Type.ELSE);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Token.Type.ELIF);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Token.Type.DO);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Token.Type.WHILE);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Token.Type.BREAK);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Token.Type.CONTINUE);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Token.Type.INT);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Token.Type.BOOL);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Token.Type.PROC);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Token.Type.RETURN);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Token.Type.LENGTH);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Token.Type.ASC);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Token.Type.CHR);
    assertThat(token.type().isKeyword()).isTrue();
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Token.Type.EXIT);
    assertThat(token.type().isKeyword()).isTrue();
  }

  @Test
  public void mixed() {
    Lexer lexer = new Lexer("print 3 p");
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Token.Type.PRINT);
    assertThat(token.type().isKeyword()).isTrue();

    IntToken intToken = (IntToken) lexer.nextToken();
    assertThat(intToken.type()).isEqualTo(Type.INT);
    assertThat(intToken.value()).isEqualTo(3);

    Token varToken = lexer.nextToken();
    assertThat(varToken.type()).isEqualTo(Type.VARIABLE);
    assertThat(varToken.text()).isEqualTo("p");
  }

  @Test
  public void longNotKeyword() {
    Lexer lexer = new Lexer("printed");
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.VARIABLE);
    assertThat(token.text()).isEqualTo("printed");
  }

  @Test
  public void variable() {
    Lexer lexer = new Lexer("prin");
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.VARIABLE);
    assertThat(token.text()).isEqualTo("prin");
  }

  @Test
  public void variableAlnum() {
    Lexer lexer = new Lexer("prin2");
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.VARIABLE);
    assertThat(token.text()).isEqualTo("prin2");
  }

  @Test
  public void underscore() {
    Lexer lexer = new Lexer("TYPE_token token_ token_with_lots_of_ _leading_ok _");
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.VARIABLE);
    assertThat(token.text()).isEqualTo("TYPE_token");
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.VARIABLE);
    assertThat(token.text()).isEqualTo("token_");
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.VARIABLE);
    assertThat(token.text()).isEqualTo("token_with_lots_of_");
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.VARIABLE);
    assertThat(token.text()).isEqualTo("_leading_ok");
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.VARIABLE);
    assertThat(token.text()).isEqualTo("_");
  }

  @Test
  public void double_underscore_not_allowed() {
    assertThrows(ScannerException.class, () -> new Lexer("__token").nextToken());
    assertThrows(ScannerException.class, () -> new Lexer("__").nextToken());
  }

  @Test
  public void assign() {
    Lexer lexer = new Lexer("a = 3");

    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.VARIABLE);
    assertThat(token.text()).isEqualTo("a");

    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.EQ);

    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.INT);
    assertThat(token.text()).isEqualTo("3");
  }

  @Test
  public void assign_expr() {
    Lexer lexer = new Lexer("a=3+4");

    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.VARIABLE);
    assertThat(token.text()).isEqualTo("a");

    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.EQ);

    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.INT);
    assertThat(token.text()).isEqualTo("3");

    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.PLUS);
    assertThat(token.text()).isEqualTo("+");

    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.INT);
    assertThat(token.text()).isEqualTo("4");
  }

  @Test
  public void comment() {
    Lexer lexer = new Lexer("1// ignored\na");

    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.INT);
    assertThat(token.text()).isEqualTo("1");

    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.VARIABLE);
    assertThat(token.text()).isEqualTo("a");

    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.EOF);
  }

  @Test
  public void commentEom() {
    Lexer lexer = new Lexer("1// ignored\n");

    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.INT);
    assertThat(token.text()).isEqualTo("1");

    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.EOF);
  }

  @Test
  public void commentEol() {
    Lexer lexer = new Lexer("1// ignored");

    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.INT);
    assertThat(token.text()).isEqualTo("1");

    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.EOF);
  }

  @Test
  public void commentCrLf() {
    Lexer lexer = new Lexer("1// ignored\r\n");

    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.INT);
    assertThat(token.text()).isEqualTo("1");

    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.EOF);
  }

  @Test
  public void commentLfCr() {
    Lexer lexer = new Lexer("1// ignored\n\r");

    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.INT);
    assertThat(token.text()).isEqualTo("1");

    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.EOF);
  }

  @Test
  public void commentsEol() {
    Lexer lexer = new Lexer("1// ignored\n// so is this...\nb");

    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.INT);
    assertThat(token.text()).isEqualTo("1");

    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.VARIABLE);
    assertThat(token.text()).isEqualTo("b");

    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.EOF);
  }

  @Test
  public void stringTick() {
    Lexer lexer = new Lexer("'Hi'");
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.STRING);
    assertThat(token.text()).isEqualTo("Hi");
  }

  @Test
  public void stringQuotes() {
    Lexer lexer = new Lexer("\"Hi\"");
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.STRING);
    assertThat(token.text()).isEqualTo("Hi");
  }

  @Test
  public void stringEmpty() {
    Lexer lexer = new Lexer("''");
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.STRING);
    assertThat(token.text()).isEqualTo("");
  }

  @Test
  public void stringSpace() {
    Lexer lexer = new Lexer("' '");
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.STRING);
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
    assertThat(token.type()).isEqualTo(Type.STRING);
    assertThat(token.text()).isEqualTo("\n \\ ' \"");
  }

  @Test
  public void backslashEscapesDoubleQuotes() {
    // Trust me.
    String input = "\" \\n \\\\ \\' \\\" \"";
    Lexer lexer = new Lexer(input);
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.STRING);
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
