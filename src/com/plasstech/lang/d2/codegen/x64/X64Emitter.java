package com.plasstech.lang.d2.codegen.x64;

import com.plasstech.lang.d2.codegen.ListEmitter;

/**
 * X64 (nasm) implementation of Emitter.
 */
class X64Emitter extends ListEmitter {
  @Override
  public void emitExternCall(String call) {
    addExtern(call);
    emit("sub RSP, 0x20");
    emit("call %s", call);
    emit("add RSP, 0x20");
  }

  @Override
  // Why is this a thing?
  public void emitExit(int exitCode) {
    addExtern("exit");
    if (exitCode == 0) {
      emit("xor RCX, RCX");
    } else {
      emit("mov RCX, %d", exitCode);
    }
    emit("call exit");
  }

  @Override
  public void emitLabel(String label) {
    if (label != null) {
      emit("");
      emit0("%s:", label);
    }
  }
}
