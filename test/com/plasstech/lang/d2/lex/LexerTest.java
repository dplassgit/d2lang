package com.plasstech.lang.d2.lex;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.plasstech.lang.d2.lex.KeywordToken.KeywordType;
import com.plasstech.lang.d2.lex.Token.Type;

public class LexerTest {
  @Test
  public void nextToken_singleSymbols() {
    Lexer lexer = new Lexer("+-*/%()! =<>|&{}");
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
  }

  @Test
  public void nextToken_doubleSymbols() {
    Lexer lexer = new Lexer("== <= >= !=");
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.EQEQ);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.LEQ);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.GEQ);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.NEQ);
  }

  @Test
  public void nextToken_int() {
    Lexer lexer = new Lexer("3");
    IntToken token = (IntToken) lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.INT);
    assertThat(token.value()).isEqualTo(3);
  }

  @Test
  public void nextToken_longerint() {
    Lexer lexer = new Lexer("1234");
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.INT);
    assertThat(token.text()).isEqualTo("1234");
  }

  @Test
  public void nextToken_eof() {
    Lexer lexer = new Lexer("1");
    lexer.nextToken();
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.EOF);
  }

  @Test
  public void nextToken_twoNumbers() {
    Lexer lexer = new Lexer("1 2");
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.INT);
    assertThat(token.text()).isEqualTo("1");

    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.INT);
    assertThat(token.text()).isEqualTo("2");
  }

  @Test
  public void nextToken_whiteSpace() {
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
  public void nextToken_trueFalse() {
    Lexer lexer = new Lexer("true false True FALSE");
    BoolToken token = (BoolToken) lexer.nextToken();
    assertThat(token.value()).isTrue();
    token = (BoolToken) lexer.nextToken();
    assertThat(token.value()).isFalse();
    token = (BoolToken) lexer.nextToken();
    assertThat(token.value()).isTrue();
    token = (BoolToken) lexer.nextToken();
    assertThat(token.value()).isFalse();
  }

  @Test
  public void nextToken_keyword() {
    Lexer lexer = new Lexer("print IF Else elif");

    KeywordToken token = (KeywordToken) lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.KEYWORD);
    assertThat(token.keyword()).isEqualTo(KeywordType.PRINT);
    token = (KeywordToken) lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.KEYWORD);
    assertThat(token.keyword()).isEqualTo(KeywordType.IF);
    token = (KeywordToken) lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.KEYWORD);
    assertThat(token.keyword()).isEqualTo(KeywordType.ELSE);
    token = (KeywordToken) lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.KEYWORD);
    assertThat(token.keyword()).isEqualTo(KeywordType.ELIF);

  }

  @Test
  public void nextToken_mixed() {
    Lexer lexer = new Lexer("print 3 p");
    KeywordToken token = (KeywordToken) lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.KEYWORD);
    assertThat(token.keyword()).isEqualTo(KeywordType.PRINT);

    IntToken intToken = (IntToken) lexer.nextToken();
    assertThat(intToken.type()).isEqualTo(Type.INT);
    assertThat(intToken.value()).isEqualTo(3);

    Token varToken = lexer.nextToken();
    assertThat(varToken.type()).isEqualTo(Type.VARIABLE);
    assertThat(varToken.text()).isEqualTo("p");
  }

  @Test
  public void nextToken_longNotKeyword() {
    Lexer lexer = new Lexer("printed");
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.VARIABLE);
    assertThat(token.text()).isEqualTo("printed");
  }

  @Test
  public void nextToken_variable() {
    Lexer lexer = new Lexer("prin");
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.VARIABLE);
    assertThat(token.text()).isEqualTo("prin");
  }

  @Test
  public void nextToken_variableAlnum() {
    Lexer lexer = new Lexer("prin2");
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.VARIABLE);
    assertThat(token.text()).isEqualTo("prin2");
  }

  @Test
  public void nextToken_assign() {
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
  public void nextToken_assign_expr() {
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
  public void nextToken_comment() {
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
  public void nextToken_commentEom() {
    Lexer lexer = new Lexer("1// ignored\n");

    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.INT);
    assertThat(token.text()).isEqualTo("1");

    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.EOF);
  }

  @Test
  public void nextToken_commentEol() {
    Lexer lexer = new Lexer("1// ignored");

    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.INT);
    assertThat(token.text()).isEqualTo("1");

    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(Type.EOF);
  }

  @Test
  public void nextToken_commentsEol() {
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
}
