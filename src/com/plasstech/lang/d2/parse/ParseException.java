package com.plasstech.lang.d2.parse;

import com.plasstech.lang.d2.common.D2RuntimeException;
import com.plasstech.lang.d2.common.Position;

public class ParseException extends D2RuntimeException {
  private static final long serialVersionUID = 8675309L;

  public ParseException(String message, Position position) {
    super(message, position, "Parse");
  }
}
