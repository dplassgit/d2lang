package com.plasstech.lang.d2.common;

public class ArraySizeException extends D2RuntimeException {

  public ArraySizeException(String message, Position position) {
    super(message, position, "Array size");
  }

  private static final long serialVersionUID = 5L;
}
