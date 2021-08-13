package com.plasstech.lang.d2.codegen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.Dec;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.Goto;
import com.plasstech.lang.d2.codegen.il.Inc;
import com.plasstech.lang.d2.codegen.il.Label;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Stop;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.Transfer;
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
    String f = "foo";
    if (input.filename() != null) {
      f = input.filename();
    }
    emit("; To execute:");
    // -Ox = optimize
    emit("; nasm -fwin64 -Ox %s.asm && gcc %s.obj -o %s && ./%s", f, f, f, f);

    emit("%%use altreg"); // lets us sometimes use r01 instead of rax, etc.
    emit("global main"); // required
    emit("extern puts"); // only really needed if we call print
    emit("extern printf"); // only really needed if we call print
    emit("extern exit"); // always needed

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
    emit("\t__EXIT_MSG: db \"ERROR: \", 0");

    emit("\nsection .text");
    // TODO: convert command-line arguments to ??? and send to __main
    emit("main:");
    for (Op opcode : code) {
      emit(opcode);
      opcode.accept(this);
    }

    return input.addAsmCode(ImmutableList.copyOf(asm));
  }

  @Override
  public void visit(Label op) {
    emit("%s:", op.label());
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

  @Override
  public void visit(Transfer op) {
    // it's possible to optimize this to just be `mov [dest], [source]`
    // but we'd have to know all the allowed combinations of stack, global, immediate.

    // Use r15 as a temp
    Operand source = op.source();
    if (source.storage() == SymbolStorage.GLOBAL) {
      // if source is global:
      // 1. mov r15, [source]
      emit("\tmov r15, [%s]", source);
    }
    // if source is temp or local:
    // ???
    // if source is stack:
    // 1. mov r15, [ebp+???]

    // if source is int constant:
    // 1. mov r15, (constant)
    else if (source.isConstant()) {
      ConstantOperand<?> sourceOp = (ConstantOperand<?>) source;
      Object value = sourceOp.value();
      if (value instanceof Integer) {
        // woot.
        emit("\tmov r15, %d", value);
      }
    }

    Location destination = op.destination();
    // if dest is global:
    if (destination.storage() == SymbolStorage.GLOBAL) {
      // 2. mov [dest], r15
      emit("\tmov [%s], r15", destination);
    }
    // if dest is temp or local:
    // ???
    // if dest is stack:
    // 2. mov [ebp+???], r15
    // if dest is fieldset:
    // 2. ???
  }

  @Override
  public void visit(SysCall op) {
    Operand arg = op.arg();
    switch (op.call()) {
      case INPUT:
        break;
      case MESSAGE: // exit "foo"
        emit("\tsub rsp, 28h          ; Reserve the shadow space");
        emit("\tmov rcx, __EXIT_MSG");
        emit("\tcall printf           ; printf(__EXIT_MSG)");
        if (arg.isConstant()) {
          emit("\tadd rsp, 28h          ; Remove shadow space");
          // have to store constant in memory
        } else {
          // must be a string global
          emit("\tmov rcx, %s", arg.toString());
          emit("\tcall puts             ; puts message with newline");
          emit("\tadd rsp, 28h          ; Remove shadow space");
        }
        break;
      case PRINT:
        if (arg.isConstant()) {
          // might be string might be int... ugh
          emit("\tsub rsp, 28h          ; Reserve the shadow space");
          emit("\tmov rcx, %s           ; First argument is address of message", arg.toString());
          emit("\tcall printf           ; printf(message)");
          emit("\tadd rsp, 28h          ; Remove shadow space");
        }
        break;
    }
  }

  @Override
  public void visit(Dec op) {
    emit("\tdec dword [%s]", op.target());
  }

  @Override
  public void visit(Inc op) {
    emit("\tinc dword [%s]", op.target());
  }

  private void emit(String format, Object... values) {
    asm.add(String.format(format, values));
  }

  private void emit(Op op) {
    emit("\t; %s", op.toString());
  }
}
