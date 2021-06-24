package com.plasstech.lang.d2.common;

public class D2RuntimeException extends RuntimeException {

  private static final long serialVersionUID = 212555L;
  private final Position position;
  private final String name;

  public D2RuntimeException(String message, Position position, String name) {
    super(message);
    this.position = position;
    this.name = name;
  }

  @Override
  public String toString() {
    if (position != null) {
      return String.format("%s error at %s: %s", name, position, getMessage());
    } else {
      return String.format("%s error: %s", name, getMessage());
    }
  }
}
