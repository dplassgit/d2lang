package com.plasstech.lang.d2.codegen;

import static com.plasstech.lang.d2.codegen.Register.RCX;
import static com.plasstech.lang.d2.codegen.Register.RDX;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.AllocateOp;
import com.plasstech.lang.d2.codegen.il.FieldSetOp;

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
  void generate(AllocateOp op) {
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

  void generate(FieldSetOp op) {
    String recordLoc = resolver.resolve(op.recordLocation());
    Register calcReg = resolver.allocate();
    // 1. if not already in register, put record location into a register
    emitter.emit(
        "mov %s, %s  ; put record location in register for calculations", calcReg, recordLoc);

    // 2. get offset of field
    int offset = op.recordSymbol().getField(op.field()).offset();
    // 3. add to get actual field location
    if (offset > 0) {
      if (offset == 1) {
        emitter.emit("inc %s  ; get actual field location via offset", calcReg);
      } else {
        emitter.emit("add %s, %d  ; get actual field location via offset", calcReg, offset);
      }
    } else {
      emitter.emit("; offset was 0");
    }

    Operand source = op.source();
    String sourceName = resolver.resolve(source);
    // 4. mov register, source - take heed of size of RHS
    Size size = Size.of(source.type());
    if (source.isConstant() || resolver.isInAnyRegister(source)) {
      emitter.emit("mov %s [%s], %s  ; store it!", size, calcReg, sourceName);
    } else {
      // need an indirection, ugh.
      Register indirectReg = resolver.allocate();
      emitter.emit("; allocated %s for calculations", indirectReg);
      emitter.emit(
          "mov %s %s, %s  ; get value to store",
          size, indirectReg.sizeByType(source.type()), sourceName);
      emitter.emit("mov [%s], %s  ; store it!", calcReg, indirectReg);
      resolver.deallocate(indirectReg);
    }
    resolver.deallocate(source);
    resolver.deallocate(calcReg);
  }
}
