package com.plasstech.lang.d2.common;

public class D2RuntimeException extends RuntimeException {

  private static final long serialVersionUID = 212555L;
  private final Position position;
  private final String type;

  /**
   * @param message details about the error
   * @param position where it occurred
   * @param type type of error, e.g., "Arithmetic"
   */
  public D2RuntimeException(String message, Position position, String type) {
    super(message);
    this.position = position;
    this.type = type;
  }

  @Override
  public String toString() {
    if (position != null) {
      return String.format("%s error at %s: %s", type, position, getMessage());
    } else {
      return String.format("%s error: %s", type, getMessage());
    }
  }
}
