package com.plasstech.lang.d2.codegen.x64;

import com.plasstech.lang.d2.codegen.Emitter;
import com.plasstech.lang.d2.codegen.Labels;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.common.Position;

/** This generator should be replaced with a d library. */
class NullPointerCheckGenerator {

  private final Resolver resolver;
  private final Emitter emitter;

  NullPointerCheckGenerator(Resolver resolver, Emitter emitter) {
    this.resolver = resolver;
    this.emitter = emitter;
  }

  private static final String NPE_ERR = "NPE_ERR: db \"Null pointer error at line %d.\", 10, 0";

  private void generateNPEMessage(Position position) {
    emitter.addData(NPE_ERR);
    emitter.emit("mov EDX, %d  ; line number", position.line());
    emitter.emit("mov RCX, NPE_ERR");
    emitter.emitExternCall("printf");
    emitter.emitExit(-1);
  }

  void generateNullPointerCheck(Position position, Operand source) {
    String sourceName = resolver.resolve(source);
    if (sourceName.equals("0")) {
      // immediately zero. right to jail.
      emitter.emit("; forcing NPE check for constant null");
      generateNPEMessage(position);
    } else if (!source.isConstant()) {
      String dest = Labels.nextLabel("not_npe");
      emitter.emit("cmp QWORD %s, 0", sourceName);
      emitter.emit("jne %s", dest);
      generateNPEMessage(position);
      emitter.emitLabel(dest);
    } else {
      emitter.emit("; skipping NPE check for constant non-null %s", sourceName);
    }
  }
}
