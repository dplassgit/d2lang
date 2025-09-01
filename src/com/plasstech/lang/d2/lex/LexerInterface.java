package com.plasstech.lang.d2.lex;

public interface LexerInterface {

  /**
   * Returns the next token. If there is no next token, returns the EOF token. If it can't parse the
   * next token, throws (unchecked) {@link(ScannerException}
   */
  Token nextToken();
}