package com.plasstech.lang.d2.codegen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.collect.ImmutableList;

/**
 * Partial implementation of Emitter that stores most everything in a list. Or a set.
 */
public abstract class ListEmitter implements Emitter {

  private final List<String> code = new ArrayList<>();
  private final Set<String> externs = new TreeSet<>();
  private final Set<String> data = new TreeSet<>();

  @Override
  public void emit0(String format, Object... values) {
    code.add(String.format(format, values));
  }

  @Override
  public void emit(String string, Object... values) {
    if (string.length() == 0) {
      return;
    }
    emit0("  " + string, values);
  }

  @Override
  public ImmutableList<String> all() {
    return ImmutableList.copyOf(code);
  }

  @Override
  public void addExtern(String extern) {
    externs.add(extern);
  }

  @Override
  public ImmutableList<String> externs() {
    return ImmutableList.copyOf(externs);
  }

  @Override
  public void addData(String datum) {
    data.add(datum);
  }

  @Override
  public ImmutableList<String> data() {
    return ImmutableList.copyOf(data);
  }
}
