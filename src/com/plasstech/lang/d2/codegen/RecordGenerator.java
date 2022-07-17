package com.plasstech.lang.d2.codegen;

import static com.plasstech.lang.d2.codegen.Register.RCX;
import static com.plasstech.lang.d2.codegen.Register.RDX;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.AllocateOp;

class RecordGenerator {

  private final Resolver resolver;
  private final Emitter emitter;
  private final Registers registers;

  public RecordGenerator(Resolver resolver, Registers registers, Emitter emitter) {
    this.resolver = resolver;
    this.registers = registers;
    this.emitter = emitter;
  }

  /** Generate asm code to allocate and assign a record. */
  public void generate(AllocateOp op) {
    RegisterState state = RegisterState.condPush(emitter, registers, ImmutableList.of(RCX, RDX));
    String dest = resolver.resolve(op.destination());
    emitter.emit("mov RCX, 1");
    // Allocate at least 1 byte
    int totalSize = Math.max(1, op.record().allocatedSize());
    emitter.emit("mov EDX, %d  ; total record size", totalSize);
    emitter.emitExternCall("calloc");
    emitter.emit("mov %s, RAX", dest);
    state.condPop();
  }
}
