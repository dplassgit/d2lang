package com.plasstech.lang.d2.codegen;

import com.google.common.collect.ImmutableList;

interface Emitter {

  /** "Emit" a line at column 0. */
  void emit(String string, Object... values);

  /** Returns all lines so far. */
  ImmutableList<String> all();

  void fail(String format, Object... values);
}
