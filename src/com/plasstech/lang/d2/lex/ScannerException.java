package com.plasstech.lang.d2.lex;

import com.plasstech.lang.d2.common.D2RuntimeException;
import com.plasstech.lang.d2.common.Position;

public class ScannerException extends D2RuntimeException {
  private static final long serialVersionUID = 212555L;

  public ScannerException(String message, Position position) {
    super(message, position, "Scanner");
  }
}
