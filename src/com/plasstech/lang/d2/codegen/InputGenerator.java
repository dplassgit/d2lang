package com.plasstech.lang.d2.codegen;

import static com.plasstech.lang.d2.codegen.IntRegister.R8;
import static com.plasstech.lang.d2.codegen.IntRegister.R9;
import static com.plasstech.lang.d2.codegen.IntRegister.RCX;
import static com.plasstech.lang.d2.codegen.IntRegister.RDX;

import com.google.common.collect.ImmutableList;

class InputGenerator {
  private static final int ONE_MB = 1024 * 1024;
  private final Resolver resolver;
  private final Registers registers;
  private final Emitter emitter;

  public InputGenerator(Resolver resolver, Registers registers, Emitter emitter) {
    this.resolver = resolver;
    this.registers = registers;
    this.emitter = emitter;
  }

  /**
   * Generates the "input" command by reading up to 1mb from stdin.
   *
   * <p>TODO(https://github.com/dplassgit/d2lang/issues/105): Make this a function in dlib.
   *
   * @param operand destination for the input
   */
  void generate(Operand operand) {
    String destLoc = resolver.resolve(operand);

    RegisterState state =
        RegisterState.condPush(emitter, registers, ImmutableList.of(RCX, RDX, R8, R9));
    emitter.emitExternCall("_flushall");

    // 1. calloc 1mb
    emitter.emit("mov RDX, %d; allocate 1mb", ONE_MB);
    emitter.emit("mov RCX, 1");
    emitter.emitExternCall("calloc");
    Register tempReg = resolver.allocate();
    emitter.emit("; allocated %s as temp reg", tempReg);
    // TODO: this register might be munged by subsequent calls...
    emitter.emit("mov %s, RAX", tempReg.name64());

    // 3. _read up to 1mb
    emitter.emit0("");
    emitter.emit("; int _read(int fd, void *buffer, count size)");
    emitter.emit("mov RCX, 0  ; 0=stdio");
    emitter.emit("mov RDX, %s  ; destination", tempReg.name64());
    emitter.emit("mov R8, %d; count", ONE_MB);
    emitter.emitExternCall("_read");

    // 4. capture total size (from RAX)
    emitter.emit("mov RDX, RAX  ; bytes read");

    // 5. allocate the correct size + 1
    emitter.emit0("");
    emitter.emit("; alloc a new buffer with the # of bytes read + 1");
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
    emitter.emit("mov RDX, %s  ; source", tempReg.name64());
    emitter.emit("pop R8  ; size, was pushed before as RDX");
    emitter.emitExternCall("memcpy");

    // 8. deallocate the original
    emitter.emit("; deallocate the original 1mb buffer");
    emitter.emit("mov RCX, %s", tempReg.name64());
    emitter.emitExternCall("free");
    resolver.deallocate(tempReg);
    state.condPop();
  }
}
