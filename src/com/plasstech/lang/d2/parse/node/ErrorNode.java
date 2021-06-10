package com.plasstech.lang.d2.parse.node;

import com.plasstech.lang.d2.common.Position;

/** An error node. */
public class ErrorNode extends AbstractNode {
  private final String message;

  public ErrorNode(String message, Position position) {
    super(position);
    this.message = message;
  }

  @Override
  public boolean isError() {
    return true;
  }

  @Override
  public String message() {
    return toString();
  }

  @Override
  public String toString() {
    return String.format("Error at %s: %s ", position(), message);
  }
}
