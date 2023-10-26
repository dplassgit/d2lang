package com.plasstech.lang.d2.codegen.x64;

import com.plasstech.lang.d2.codegen.Emitter;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.Goto;
import com.plasstech.lang.d2.codegen.il.Label;
import com.plasstech.lang.d2.codegen.il.Stop;

class LabelCodeGenerator extends DefaultOpcodeVisitor {

  private final Emitter emitter;

  public LabelCodeGenerator(Emitter emitter) {
    this.emitter = emitter;
  }

  @Override
  public void visit(Label op) {
    emitter.emitLabel(op.label());
  }

  @Override
  public void visit(Stop op) {
    emitter.emitExit(op.exitCode());
  }

  @Override
  public void visit(Goto op) {
    emitter.emit("jmp %s", op.label());
  }
}
