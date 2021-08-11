package com.plasstech.lang.d2.codegen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.Goto;
import com.plasstech.lang.d2.codegen.il.Label;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Stop;
import com.plasstech.lang.d2.phase.Phase;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.type.SymTab;
import com.plasstech.lang.d2.type.Symbol;
import com.plasstech.lang.d2.type.SymbolStorage;

public class NasmCodeGenerator extends DefaultOpcodeVisitor implements Phase {

  private final List<String> asm = new ArrayList<>();

  @Override
  public State execute(State input) {
    ImmutableList<Op> code = input.lastIlCode();
    if (input.filename() != null) {
      emit("; Compiled from %s", input.filename());
    }
    emit("; To execute: nasm -fwin64 foo.asm && gcc foo.obj -o foo && ./foo");

    emit("global main");
    emit("extern puts");
    emit("extern exit");
    // Probably what we should do is:
    // 1. emit all globals OK
    // 2. emit all string constants not done - if string constants are propagated, ???
    // maybe we shouldn't constant propagate string constants except if it's "foo"[3]
    // 3. emit all array constants (?)
    emit("\nsection .data");
    SymTab globals = input.symbolTable();
    for (Map.Entry<String, Symbol> entry : globals.entries().entrySet()) {
      if (entry.getValue().storage() == SymbolStorage.GLOBAL) {
        // temporarily reserve (& clear) 4 bytes per entry
        emit("\t%s: dd 0", entry.getKey());
      }
    }

    emit("\nsection .text");
    // TODO: convert command-line arguments to ??? and send to __main
    emit("main:");
    for (Op opcode : code) {
      emit(opcode);
      opcode.accept(this);
    }

    return input.addAsmCode(ImmutableList.copyOf(asm));
  }

  private void emit(String statement) {
    asm.add(statement);
  }

  private void emit(String format, Object value) {
    asm.add(String.format(format, value));
  }
  
  @Override
  public void visit(Label op) {
    emit("%s:", op.label());
  }

  private void emit(Op op) {
    emit("\t; %s", op.toString());
  }

  @Override
  public void visit(Stop op) {
    emit("\tmov rcx, %d", op.exitCode());
    emit("\tcall exit");
  }

  @Override
  public void visit(Goto op) {
    emit("\tjmp %s", op.label());
  }
}
