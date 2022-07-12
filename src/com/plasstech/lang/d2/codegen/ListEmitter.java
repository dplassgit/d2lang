package com.plasstech.lang.d2.codegen;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.common.D2RuntimeException;

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

  @Override
  public void fail(String format, Object[] values) {
    throw new D2RuntimeException("UnsupportedOperation", null, String.format(format, values));
  }
}
