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
  public void emitLabel(String label) {
    if (label != null) {
      emit("");
      emit0("%s:", label);
    }
  }
}
