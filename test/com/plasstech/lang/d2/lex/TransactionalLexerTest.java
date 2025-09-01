package com.plasstech.lang.d2.lex;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.lex.TransactionalLexer.Transaction;

public class TransactionalLexerTest {

  private TransactionalLexer sut = new TransactionalLexer(new Lexer("< <= == >= > != = ( ) [ ]"));

  @Test
  public void nextToken_noTransaction() {
    TransactionalLexer lexer = new TransactionalLexer(new Lexer("a 1 <"));
    Token token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.VARIABLE);
    assertThat(token.text()).isEqualTo("a");
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.LITERAL);
    assertThat(token.text()).isEqualTo("1");
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.LT);
    token = lexer.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.EOF);
  }

  @Test
  public void rollback() {
    assertThat(sut.nextToken().type()).isEqualTo(TokenType.LT);

    Transaction t = sut.startTransaction();
    assertThat(sut.nextToken().type()).isEqualTo(TokenType.LEQ);
    assertThat(sut.nextToken().type()).isEqualTo(TokenType.EQEQ);

    t.rollback();

    // Goes back to the start of the transaction
    assertThat(sut.nextToken().type()).isEqualTo(TokenType.LEQ);
    assertThat(sut.nextToken().type()).isEqualTo(TokenType.EQEQ);
  }

  @Test
  public void commit() {
    assertThat(sut.nextToken().type()).isEqualTo(TokenType.LT);

    Transaction t = sut.startTransaction();
    assertThat(sut.nextToken().type()).isEqualTo(TokenType.LEQ);

    t.commit();

    assertThat(sut.nextToken().type()).isEqualTo(TokenType.EQEQ);
    assertThat(sut.nextToken().type()).isEqualTo(TokenType.GEQ);
  }

  @Test
  public void commit_then_new_transaction() {
    assertThat(sut.nextToken().type()).isEqualTo(TokenType.LT);

    Transaction t = sut.startTransaction();
    assertThat(sut.nextToken().type()).isEqualTo(TokenType.LEQ);

    t.commit();

    assertThat(sut.nextToken().type()).isEqualTo(TokenType.EQEQ);
    t = sut.startTransaction();
    // should just keep going.
    assertThat(sut.nextToken().type()).isEqualTo(TokenType.GEQ);
    t.commit();
    assertThat(sut.nextToken().type()).isEqualTo(TokenType.GT);
    assertThat(sut.nextToken().type()).isEqualTo(TokenType.NEQ);
  }

  @Test
  public void rollback_then_start_transaction_then_rollback() {
    assertThat(sut.nextToken().type()).isEqualTo(TokenType.LT);
    assertThat(sut.nextToken().type()).isEqualTo(TokenType.LEQ);

    Transaction t = sut.startTransaction();
    assertThat(sut.nextToken().type()).isEqualTo(TokenType.EQEQ);
    assertThat(sut.nextToken().type()).isEqualTo(TokenType.GEQ);
    assertThat(sut.nextToken().type()).isEqualTo(TokenType.GT);

    t.rollback();
    assertThat(sut.nextToken().type()).isEqualTo(TokenType.EQEQ);
    assertThat(sut.nextToken().type()).isEqualTo(TokenType.GEQ);

    t = sut.startTransaction();
    assertThat(sut.nextToken().type()).isEqualTo(TokenType.GT);
    assertThat(sut.nextToken().type()).isEqualTo(TokenType.NEQ);

    t.rollback();
    assertThat(sut.nextToken().type()).isEqualTo(TokenType.GT);
    assertThat(sut.nextToken().type()).isEqualTo(TokenType.NEQ);
    assertThat(sut.nextToken().type()).isEqualTo(TokenType.ASSIGN);
  }

  @Test
  public void rollback_then_start_transaction_then_commit() {
    assertThat(sut.nextToken().type()).isEqualTo(TokenType.LT);
    assertThat(sut.nextToken().type()).isEqualTo(TokenType.LEQ);

    Transaction t = sut.startTransaction();
    assertThat(sut.nextToken().type()).isEqualTo(TokenType.EQEQ);
    assertThat(sut.nextToken().type()).isEqualTo(TokenType.GEQ);
    assertThat(sut.nextToken().type()).isEqualTo(TokenType.GT);

    t.rollback();
    assertThat(sut.nextToken().type()).isEqualTo(TokenType.EQEQ);
    assertThat(sut.nextToken().type()).isEqualTo(TokenType.GEQ);

    t = sut.startTransaction();
    assertThat(sut.nextToken().type()).isEqualTo(TokenType.GT);
    assertThat(sut.nextToken().type()).isEqualTo(TokenType.NEQ);

    t.commit();
    assertThat(sut.nextToken().type()).isEqualTo(TokenType.ASSIGN);
    assertThat(sut.nextToken().type()).isEqualTo(TokenType.LPAREN);
  }

  @Test
  public void isOpen_after_commit() {
    Transaction t = sut.startTransaction();
    assertThat(t.isOpen()).isTrue();
    t.commit();
    assertThat(t.isOpen()).isFalse();
  }

  @Test
  public void isOpen_after_rollback() {
    Transaction t = sut.startTransaction();
    assertThat(t.isOpen()).isTrue();
    t.rollback();
    assertThat(t.isOpen()).isFalse();
  }

  @Test
  public void start_transaction_at_beginning() {
    Transaction t = sut.startTransaction();
    assertThat(sut.nextToken().type()).isEqualTo(TokenType.LT);
    assertThat(sut.nextToken().type()).isEqualTo(TokenType.LEQ);

    t.rollback();
    assertThat(sut.nextToken().type()).isEqualTo(TokenType.LT);
  }

  @Test
  public void rollback_then_eof() {
    TransactionalLexer one = new TransactionalLexer(new Lexer("a"));
    Transaction t = one.startTransaction();
    Token token = one.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.VARIABLE);
    assertThat(token.text()).isEqualTo("a");

    assertThat(one.nextToken().type()).isEqualTo(TokenType.EOF);

    t.rollback();
    token = one.nextToken();
    assertThat(token.type()).isEqualTo(TokenType.VARIABLE);
    assertThat(token.text()).isEqualTo("a");

    assertThat(one.nextToken().type()).isEqualTo(TokenType.EOF);
  }

  @Test
  public void double_start() {
    sut.startTransaction();
    assertThrows(IllegalStateException.class, () -> sut.startTransaction());
  }

  @Test
  public void double_rollback() {
    Transaction t = sut.startTransaction();
    t.rollback();
    assertThrows(IllegalStateException.class, () -> t.rollback());
  }

  @Test
  public void double_commit() {
    Transaction t = sut.startTransaction();
    t.commit();
    assertThrows(IllegalStateException.class, () -> t.commit());
  }
}
