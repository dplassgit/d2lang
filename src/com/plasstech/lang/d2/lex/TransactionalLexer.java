package com.plasstech.lang.d2.lex;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;

/**
 * A lexer that lets you start a transaction that an be committed or rolled back.
 */
public class TransactionalLexer implements LexerInterface {
  public class Transaction {
    private final List<Token> queue = new ArrayList<>();

    public void commit() {
      Preconditions.checkState(isOpen(), "Cannot commit a transaction that is not open");
      // There's no going back.
      queue.clear();
      TransactionalLexer.this.transaction = null;
    }

    public void rollback() {
      Preconditions.checkState(isOpen(), "Cannot rollback a transaction that is not open");
      // Give all the tokens we were given back to the parent to "replay"
      TransactionalLexer.this.queue.addAll(queue);
      queue.clear();
      TransactionalLexer.this.transaction = null;
    }

    public boolean isOpen() {
      return transaction == this;
    }
  }

  private final LexerInterface source;
  private final List<Token> queue = new ArrayList<>();
  private Transaction transaction;

  public static TransactionalLexer wrap(LexerInterface source) {
    if (source instanceof TransactionalLexer tlex) {
      return tlex;
    }
    return new TransactionalLexer(source);
  }

  public TransactionalLexer(LexerInterface source) {
    this.source = source;
  }

  @Override
  public Token nextToken() {
    if (transaction != null) {
      // Store the next token on the transaction's queue.
      Token next = nextTokenInternal();
      transaction.queue.add(next);
      return next;
    }
    return nextTokenInternal();
  }

  private Token nextTokenInternal() {
    if (queue.size() > 0) {
      // There's stuff left on our queue, need to use it up before asking the source.
      return queue.removeFirst();
    }
    return source.nextToken();
  }

  public Transaction startTransaction() {
    Preconditions.checkState(transaction == null,
        "Cannot start transaction when one is already open");
    transaction = new Transaction();
    return transaction;
  }
}
