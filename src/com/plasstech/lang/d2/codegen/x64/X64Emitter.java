package com.plasstech.lang.d2.codegen.x64;

import com.plasstech.lang.d2.codegen.ListEmitter;

/**
 * X64 (nasm) implementation of Emitter.
 */
class X64Emitter extends ListEmitter {
  private boolean needsGc;

  @Override
  public void emitExternCall(String call) {
    emit("sub RSP, 0x20");
    if (call.equals("calloc") || call.equals("malloc")) {
      emit("push RCX");
      emit("push RDX");
      addExtern("gc_run");
      // hm, is it possible that this is munging registers?
      emit("call gc_run");
      emit("pop RDX");
      emit("pop RCX");
      needsGc = true;
      call = "gc_" + call;
    }
    addExtern(call);
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

  public boolean needsGc() {
    return needsGc;
  }
}
