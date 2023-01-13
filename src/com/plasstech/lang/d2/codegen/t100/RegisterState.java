package com.plasstech.lang.d2.codegen.t100;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.plasstech.lang.d2.codegen.Emitter;

class RegisterState {
  private final List<Register> registersPushed;
  private final Emitter emitter;

  RegisterState(Emitter emitter) {
    this(emitter, ImmutableList.of());
  }

  private RegisterState(Emitter emitter, List<Register> pushed) {
    this.emitter = emitter;
    this.registersPushed = pushed;
  }

  static RegisterState condPush(
      Emitter emitter, Registers registers, List<Register> toSaveIfAllocated) {
    List<Register> allocated =
        toSaveIfAllocated
            .stream()
            .filter(r -> registers.isAllocated(r))
            .collect(Collectors.toList());
    RegisterState registerState = new RegisterState(emitter, allocated);
    registerState.pushAll();
    return registerState;
  }

  private void pushAll() {
    for (Register r : registersPushed) {
      push(r);
    }
  }

  boolean wasPushed(Register r) {
    return registersPushed.contains(r);
  }

  void condPop(Register r) {
    if (wasPushed(r)) {
      pop(r);
    }
    registersPushed.remove(r);
  }

  public void condPop() {
    if (registersPushed.size() > 0) {
      for (Register r : Lists.reverse(registersPushed)) {
        pop(r);
      }
      registersPushed.clear();
    }
  }

  void pop(Register r) {
    emitter.emit("pop %s", r.alias);
  }

  void push(Register r) {
    emitter.emit("push %s", r.alias);
  }

  void push(ImmutableList<Register> registers) {
    for (Register r : registers) {
      push(r);
    }
  }

  void pop(ImmutableList<Register> registers) {
    for (Register r : registers) {
      pop(r);
    }
  }
}
