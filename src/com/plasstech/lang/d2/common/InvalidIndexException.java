package com.plasstech.lang.d2.common;

public class InvalidIndexException extends D2RuntimeException {
  private static final long serialVersionUID = 3L;

  public InvalidIndexException(String message, Position position) {
    super(message, position, "Invalid index");
  }
}
