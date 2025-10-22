package com.plasstech.lang.d2.common;

public class D2RuntimeException extends RuntimeException {

  private static final long serialVersionUID = 212555L;
  private final String fullMessage;

  /**
   * @param message details about the error
   * @param position where it occurred
   * @param type type of error, e.g., "Arithmetic"
   */
  public D2RuntimeException(String message, Position position, String type) {
    super(message.length() > 0 ? message : type);

    String fullMessage = type + " error";
    if (position != null) {
      fullMessage += String.format(" at %s", position.toString());
    }
    if (!getMessage().isEmpty()) {
      fullMessage += ": " + getMessage();
    }
    this.fullMessage = fullMessage;
  }

  @Override
  public String toString() {
    return fullMessage;
  }
}
