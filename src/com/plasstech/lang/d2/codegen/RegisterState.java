package com.plasstech.lang.d2.codegen;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class RegisterState {
  private final List<Register> registersPushed;
  private final Emitter emitter;
  private final RegisterVisitor popVisitor = new PopVisitor();
  private final RegisterVisitor pushVisitor = new PushVisitor();

  public RegisterState(Emitter emitter) {
    this(emitter, ImmutableList.of());
  }

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
    if (registersPushed.size() > 0) {
      emitter.emit("; pushing allocated registers");
      for (Register r : registersPushed) {
        r.accept(pushVisitor);
      }
    }
  }

  public boolean wasPushed(Register r) {
    return registersPushed.contains(r);
  }

  public void condPop(Register r) {
    if (wasPushed(r)) {
      pop(r);
    }
    registersPushed.remove(r);
  }

  public void condPop() {
    if (registersPushed.size() > 0) {
      emitter.emit("; popping allocated registers");
      for (Register r : Lists.reverse(registersPushed)) {
        pop(r);
      }
      registersPushed.clear();
    }
  }

  public void pop(Register r) {
    r.accept(popVisitor);
  }

  public void push(Register r) {
    r.accept(pushVisitor);
  }

  private class PushVisitor implements RegisterVisitor {
    @Override
    public void visit(IntRegister r) {
      emitter.emit("push %s", r.name64());
    }

    @Override
    public void visit(MmxRegister r) {
      emitter.emit("sub RSP, 0x10"); // 16 bytes, to store the whole 128 bits
      emitter.emit("movq [RSP], %s", r.name());
    }
  }

  private class PopVisitor implements RegisterVisitor {
    @Override
    public void visit(IntRegister r) {
      emitter.emit("pop %s", r.name64());
    }

    @Override
    public void visit(MmxRegister r) {
      emitter.emit("movq %s, [RSP]", r.name64());
      emitter.emit("add RSP, 0x10");
    }
  }

  public void push(ImmutableList<Register> registers) {
    for (Register r : registers) {
      push(r);
    }
  }

  public void pop(ImmutableList<Register> registers) {
    for (Register r : registers) {
      pop(r);
    }
  }
}
