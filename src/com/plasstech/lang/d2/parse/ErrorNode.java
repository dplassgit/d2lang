package com.plasstech.lang.d2.parse;

import com.plasstech.lang.d2.common.Position;

/** An error node. */
public class ErrorNode extends Node {
  private final String message;
  private final Position position;

  public ErrorNode(String message, Position position) {
    super(Type.ERROR);
    this.message = message;
    this.position = position;
  }

  public String message() {
    return message;
  }

  public Position position() {
    return position;
  }

  @Override
  public String toString() {
    return String.format("Error %s at location %s", message, position.toString());
  }
}
