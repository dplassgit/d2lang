package com.plasstech.lang.d2.interpreter;

import com.plasstech.lang.d2.common.D2RuntimeException;

public class InterpreterException extends D2RuntimeException {

  private static final long serialVersionUID = 234234L;

  public InterpreterException(String message, String name) {
    super(message, null, name);
  }
}
