package com.plasstech.lang.d2.lex;

import com.plasstech.lang.d2.common.Position;

public class ScannerException extends RuntimeException {
  private static final long serialVersionUID = 212555L;
  private final Position position;

  public ScannerException(String message, Position position) {
    super(message);
    this.position = position;
  }

  @Override
  public String toString() {
    return String.format("Scanner exception at %s: %s", position, getMessage());
  }
}
