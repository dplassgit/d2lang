package com.plasstech.lang.d2.common;

public class InvalidIndexException extends D2RuntimeException {
  private static final long serialVersionUID = 3L;

  public InvalidIndexException(Position position, String format, Object... parameters) {
    super(String.format(format, parameters), position, "Invalid index");
  }
}
