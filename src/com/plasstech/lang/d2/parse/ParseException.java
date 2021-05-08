package com.plasstech.lang.d2.parse;

import com.plasstech.lang.d2.common.Position;

public class ParseException extends RuntimeException {
  private static final long serialVersionUID = 8675309L;
  private final ErrorNode errorNode;

  public ParseException(String message, Position position) {
    this.errorNode = new ErrorNode(message, position);
  }

  public ErrorNode errorNode() {
    return errorNode;
  }
}
