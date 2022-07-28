package com.plasstech.lang.d2.codegen;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

public class RegisterState {

  private final List<Register> registersPushed;
  private final Emitter emitter;

  private RegisterState(Emitter emitter, List<Register> pushed) {
    this.emitter = emitter;
    this.registersPushed = pushed;
  }

  public static RegisterState condPush(
      Emitter emitter, RegistersInterface registers, List<Register> toSaveIfAllocated) {
    List<Register> allocated =
        toSaveIfAllocated
            .stream()
            .filter(r -> registers.isAllocated(r))
            .collect(Collectors.toList());
    for (Register r : allocated) {
      emitter.emit("push %s", r.name64());
    }
    return new RegisterState(emitter, allocated);
  }

  public boolean pushed(Register r) {
    return registersPushed.contains(r);
  }

  public void condPop(Register r) {
    if (pushed(r)) {
      pop(r);
    }
    registersPushed.remove(r);
  }

  private void pop(Register r) {
    emitter.emit("pop %s", r.name64());
  }

  public void condPop() {
    for (Register r : Lists.reverse(registersPushed)) {
      pop(r);
    }
    registersPushed.clear();
  }
}
