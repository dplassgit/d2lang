package com.plasstech.lang.d2.codegen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.common.D2RuntimeException;

public class ListEmitter implements Emitter {

  private final List<String> code = new ArrayList<>();
  private final Set<String> externs = new TreeSet<>();
  private final Set<String> data = new TreeSet<>();

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
    throw new D2RuntimeException(String.format(format, values), null, "UnsupportedOperation");
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
  public void emitExternCall(String call) {
    addExtern(call);
    emit("sub RSP, 0x28");
    emit("call %s", call);
    emit("add RSP, 0x28");
  }

  @Override
  public void addData(String datum) {
    data.add(datum);
  }

  @Override
  public ImmutableList<String> data() {
    return ImmutableList.copyOf(data);
  }

  @Override
  public void emitExit(int exitCode) {
    addExtern("exit");
    emit("mov RCX, %d", exitCode);
    emit("call exit");
  }

  @Override
  public void emitLabel(String label) {
    if (label != null) {
      emit0("\n%s:", label);
    }
  }
}
