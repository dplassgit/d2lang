package com.plasstech.lang.d2.codegen;

import java.util.Collection;

import com.google.common.collect.ImmutableList;

interface Emitter {

  /** "Emit" a line at column 0. */
  void emit0(String string, Object... values);

  /** "Emit" a line at column 2. */
  void emit(String string, Object... values);

  /** Returns all lines so far. */
  ImmutableList<String> all();

  void fail(String format, Object... values);

  void addExtern(String extern);

  Collection<String> externs();

  void emitExternCall(String call);
}
