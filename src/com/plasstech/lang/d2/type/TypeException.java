package com.plasstech.lang.d2.type;

import com.plasstech.lang.d2.common.D2RuntimeException;
import com.plasstech.lang.d2.common.Position;

public class TypeException extends D2RuntimeException {
  private static final long serialVersionUID = 314159L;

  public TypeException(String message, Position position) {
    super(message, position, "Type");
  }
}
