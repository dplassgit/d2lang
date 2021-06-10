package com.plasstech.lang.d2.parse;

import com.plasstech.lang.d2.common.D2RuntimeException;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.parse.node.ErrorNode;

public class ParseException extends D2RuntimeException {
  private static final long serialVersionUID = 8675309L;
  private final ErrorNode errorNode;

  public ParseException(String message, Position position) {
    super(message, position, "Parse");
    this.errorNode = new ErrorNode(message, position);
  }

  public ErrorNode errorNode() {
    return errorNode;
  }
}
