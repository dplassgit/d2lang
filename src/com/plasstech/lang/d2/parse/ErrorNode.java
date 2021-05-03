package com.plasstech.lang.d2.parse;

import com.plasstech.lang.d2.common.Position;

/** An error node. */
public class ErrorNode extends Node {
  private final String message;

  public ErrorNode(String message, Position position) {
    super(Type.ERROR, position);
    this.message = message;
  }

  public String message() {
    return message;
  }

  @Override
  public String toString() {
    return String.format("Error %s at location %s", message, position().toString());
  }
}
