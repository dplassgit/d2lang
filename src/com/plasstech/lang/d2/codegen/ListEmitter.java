package com.plasstech.lang.d2.codegen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.common.D2RuntimeException;

public class ListEmitter implements Emitter {

  private final List<String> code = new ArrayList<>();
  private final Set<String> externs = new TreeSet<>();

  @Override
  public void emit0(String format, Object... values) {
    code.add(String.format(format, values));
  }

  @Override
  public void emit(String string, Object... values) {
    emit0("  " + string, values);
  }

  @Override
  public ImmutableList<String> all() {
    return ImmutableList.copyOf(code);
  }

  @Override
  public void fail(String format, Object... values) {
    throw new D2RuntimeException("UnsupportedOperation", null, String.format(format, values));
  }

  @Override
  public void addExtern(String extern) {
    externs.add(extern);
  }

  @Override
  public Collection<String> externs() {
    return externs;
  }

  @Override
  public void emitExternCall(String call) {
    addExtern(call);
    emit("sub RSP, 0x20");
    emit("call %s", call);
    emit("add RSP, 0x20");
  }
}
