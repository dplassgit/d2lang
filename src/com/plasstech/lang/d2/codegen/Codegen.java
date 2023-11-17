package com.plasstech.lang.d2.codegen;

import com.plasstech.lang.d2.common.D2RuntimeException;
import com.plasstech.lang.d2.common.Position;

/** A utility class */
public class Codegen {
  /** Throws D2RuntimeException with the given information. */
  public static void fail(Position position, String format, Object... values) {
    fail("UnsupportedOperation", position, format, values);
  }

  /** Throws D2RuntimeException with the given information. */
  public static void fail(String type, Position position, String format, Object... values) {
    throw new D2RuntimeException(String.format(format, values), position, type);
  }

  private Codegen() {}
}
