package com.plasstech.lang.d2.common;

public class DivisionByZeroException extends D2RuntimeException {
  private static final long serialVersionUID = 12345L;

  public DivisionByZeroException(Position position) {
    super("Division by 0", position, "Arithmetic");
  }
}
