package com.plasstech.lang.d2.lex;

import com.plasstech.lang.d2.common.D2RuntimeException;
import com.plasstech.lang.d2.common.Position;

public class ScannerException extends D2RuntimeException {
  private static final long serialVersionUID = 5L;

  public ScannerException(Position position, String format, Object... params) {
    super(String.format(format, params), position, "Scanner");
  }
}
