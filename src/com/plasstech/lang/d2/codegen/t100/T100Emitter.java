package com.plasstech.lang.d2.codegen.t100;

import com.plasstech.lang.d2.codegen.ListEmitter;

public class T100Emitter extends ListEmitter {
  @Override
  public void emitExternCall(String call) {}

  @Override
  public void emitExit(int exitCode) {
    emit("call 0x0502  ; drop back into BASIC");
    emit("hlt");
  }

  @Override
  public void emitLabel(String label) {
    if (label != null) {
      emit0("\n%s:", label);
    }
  }
}
