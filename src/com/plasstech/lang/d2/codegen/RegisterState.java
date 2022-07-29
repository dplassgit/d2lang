package com.plasstech.lang.d2.codegen;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

public class RegisterState {
  private final List<Register> registersPushed;
  private final Emitter emitter;
  private final RegisterVisitor popVisitor = new PopVisitor();

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
    RegisterState registerState = new RegisterState(emitter, allocated);
    registerState.pushAll();
    return registerState;
  }

  private void pushAll() {
    RegisterVisitor pushVisitor = new PushVisitor();
    for (Register r : registersPushed) {
      r.accept(pushVisitor);
    }
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

  public void condPop() {
    for (Register r : Lists.reverse(registersPushed)) {
      pop(r);
    }
    registersPushed.clear();
  }

  private void pop(Register r) {
    r.accept(popVisitor);
  }

  private class PushVisitor implements RegisterVisitor {
    @Override
    public void visit(IntRegister r) {
      emitter.emit("push %s", r.name64());
    }

    @Override
    public void visit(MmxRegister r) {
      emitter.emit("sub RSP, 0x10"); // 16 bytes, to store the whole 128 bits
      emitter.emit("movdqu [RSP], %s", r.name());
    }
  }

  private class PopVisitor implements RegisterVisitor {
    @Override
    public void visit(IntRegister r) {
      emitter.emit("pop %s", r.name64());
    }

    @Override
    public void visit(MmxRegister r) {
      emitter.emit("movdqu %s, [RSP]", r.name64());
      emitter.emit("add RSP, 0x10");
    }
  }
}
