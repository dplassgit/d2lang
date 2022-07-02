package com.plasstech.lang.d2.codegen;

import static com.google.common.collect.ImmutableList.toImmutableList;

import java.util.List;

import com.google.common.collect.Lists;

public class RegisterState {

  private final List<Register> registersPushed;
  private final Emitter emitter;

  private RegisterState(Emitter emitter, List<Register> pushed) {
    this.emitter = emitter;
    this.registersPushed = pushed;
  }

  public static RegisterState condPush(
      Emitter emitter, Registers registers, List<Register> toSaveIfAllocated) {
    List<Register> allocated =
        toSaveIfAllocated.stream().filter(r -> registers.isAllocated(r)).collect(toImmutableList());
    for (Register r : allocated) {
      emitter.emit("  push %s", r.name64);
    }
    return new RegisterState(emitter, allocated);
  }

  public void condPop() {
    for (Register r : Lists.reverse(registersPushed)) {
      emitter.emit("  pop %s", r.name64);
    }
  }
}
