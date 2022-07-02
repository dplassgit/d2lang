package com.plasstech.lang.d2.codegen;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;

public class ListEmitter implements Emitter {

  private List<String> code = new ArrayList<>();

  @Override
  public void emit(String format, Object... values) {
    code.add(String.format(format, values));
  }

  @Override
  public ImmutableList<String> all() {
    return ImmutableList.copyOf(code);
  }
}
