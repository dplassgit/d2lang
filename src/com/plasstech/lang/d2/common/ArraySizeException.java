package com.plasstech.lang.d2.common;

public class ArraySizeException extends D2RuntimeException {

  public ArraySizeException(Position position, String format, Object... parameters) {
    super(String.format(format, parameters), position, "Array size");
  }

  private static final long serialVersionUID = 5L;
}
