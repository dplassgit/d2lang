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

    // Probably what we should do is:
    // 1. emit all globals
    // 2. emit all string constants
    // 3. emit all array constants (?)
    emit("\nSECTION .data");
    SymTab globals = input.symbolTable();
    for (Map.Entry<String, Symbol> entry : globals.entries().entrySet()) {
      if (entry.getValue().storage() == SymbolStorage.GLOBAL) {
        // for now, reserve 4 bytes per entry.
        emit("%s: dd 0", entry.getKey());
      }
    }

    emit("\nSECTION .text");
    for (Op opcode : code) {
      emit(opcode);
      opcode.accept(this);
    }

    return input.addAsmCode(ImmutableList.copyOf(asm));
  }

  private void emit(String statement) {
    asm.add(statement);
  }

  private void emit(String format, String value) {
    asm.add(String.format(format, value));
  }

  @Override
  public void visit(Label op) {
    emit("%s:", op.label());
  }

  private void emit(Op op) {
    emit("; %s", op.toString());
  }

  @Override
  public void visit(Stop op) {
    emit("ret");
  }

  @Override
  public void visit(Goto op) {
    emit("jmp %s", op.label());
  }
}
