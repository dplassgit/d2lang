package com.plasstech.lang.d2.codegen;

import static com.plasstech.lang.d2.codegen.Register.R8;
import static com.plasstech.lang.d2.codegen.Register.R9;
import static com.plasstech.lang.d2.codegen.Register.RCX;
import static com.plasstech.lang.d2.codegen.Register.RDX;

import com.google.common.collect.ImmutableList;

class InputGenerator {

  private final Resolver resolver;
  private final Registers registers;
  private final Emitter emitter;

  public InputGenerator(Resolver resolver, Registers registers, Emitter emitter) {
    this.resolver = resolver;
    this.registers = registers;
    this.emitter = emitter;
  }

  void generate(Operand operand) {
    String destLoc = resolver.resolve(operand);
    // TODO: maybe make this a function and only call it once?

    RegisterState state =
        RegisterState.condPush(emitter, registers, ImmutableList.of(RCX, RDX, R8, R9));
    emitter.emitExternCall("_flushall");

    // 1. calloc 1mb
    emitter.emit("mov RDX, 1000000  ; allocate 1mb");
    emitter.emit("mov RCX, 1");
    emitter.emitExternCall("calloc");
    Register tempReg = resolver.allocate();
    emitter.emit("; allocated %s as temp reg", tempReg);
    // TODO: this register might be munged by the various calls...
    emitter.emit("mov %s, RAX", tempReg.name64);

    // 3. fread up to 1mb
    emitter.emit0("");
    emitter.emit("; int _read(int fd, void *buffer, count size)");
    emitter.emit("mov RCX, 0  ; 0=stdio");
    emitter.emit("mov RDX, %s  ; destination", tempReg.name64);
    emitter.emit("mov R8, 1000000  ; count");
    emitter.emitExternCall("_read");
    // 4. capture total size (from RAX)
    emitter.emit("mov RDX, RAX  ; bytes read");
    emitter.emit0("");
    // 5. allocate the correct size + 1
    emitter.emit("inc RDX  ; extra byte for null");
    emitter.emit("mov RCX, 1  ; size");
    emitter.emit("push RDX");
    // 6. calloc a buffer with the new size
    emitter.emitExternCall("calloc");
    // 7. assign new place to destination
    emitter.emit("mov %s, RAX  ; new buffer", destLoc);
    // 8. copy from temp location to new location
    emitter.emit0("");
    emitter.emit("; memcpy(dest, source, size)");
    emitter.emit("mov RCX, %s  ; dest", destLoc);
    emitter.emit("mov RDX, %s  ; source", tempReg.name64);
    emitter.emit("pop R8  ; size, was pushed before as RDX");
    emitter.emitExternCall("memcpy");
    // 8. deallocate the original
    emitter.emit0("");
    emitter.emit("mov RCX, %s", tempReg.name64);
    emitter.emitExternCall("free");
    resolver.deallocate(tempReg);
    state.condPop();
  }
}
