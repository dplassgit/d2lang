package com.plasstech.lang.d2.codegen;

import com.google.common.collect.ImmutableList;

public interface Emitter {

  /** "Emit" a line at column 0. */
  void emit0(String format, Object... values);

  /** "Emit" a line at column 2. */
  void emit(String format, Object... values);

  /** Returns all lines so far. */
  ImmutableList<String> all();

  /** Add an extern, e.g., "extern printf" */
  void addExtern(String extern);

  ImmutableList<String> externs();

  void emitExternCall(String call);

  /** Add a data definition, e.g., "NAME: db \"Name here\", 0" */
  void addData(String data);

  ImmutableList<String> data();

  void emitExit(int exitCode);

  void emitLabel(String label);
}
