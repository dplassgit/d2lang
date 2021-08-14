package com.plasstech.lang.d2.codegen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.il.BinOp;
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
import com.plasstech.lang.d2.type.VarType;

public class NasmCodeGenerator extends DefaultOpcodeVisitor implements Phase {

  private final List<String> asm = new ArrayList<>();
  private static FluentLogger logger = FluentLogger.forEnclosingClass();

  @Override
  public State execute(State input) {
    ImmutableList<Op> code = input.lastIlCode();
    String f = "dcode";
    if (input.filename() != null) {
      f = input.filename();
    }
    emit("; To execute:");
    // -Ox = optimize
    emit("; nasm -fwin64 -Ox %s.asm && gcc %s.obj -o %s && ./%s", f, f, f, f);

    emit("%%use altreg"); // lets us use r01 instead of rax, etc.
    emit("global main"); // required
    emit("extern puts");
    emit("extern printf");
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
        // temporarily reserve (& clear) 4 bytes per int, 8 bytes for string
        emit("  %s: dd 0", entry.getKey());
      }
    }
    // TODO: only emit these if we need to.
    emit("  __PRINTF_NUMBER_FMT: db \"%%d\", 0");
    emit("  __EXIT_MSG: db \"ERROR: \", 0");

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
    emit("  mov rcx, %d", op.exitCode());
    emit("  call exit");
  }

  @Override
  public void visit(Goto op) {
    emit("  jmp %s", op.label());
  }

  @Override
  public void visit(Transfer op) {
    // it's possible to optimize this to just be `mov [dest], [source]`
    // but we'd have to know all the allowed combinations of stack, global, immediate.

    Operand source = op.source();
    Location destination = op.destination();
    if (source.isConstant()) {
      // if source is int constant:
      // 1. mov dest, (constant)
      if (source.type() == VarType.INT) {
        ConstantOperand<Integer> sourceOp = (ConstantOperand<Integer>) source;
        int value = sourceOp.value();
        emit("  mov dword [%s], %s", destination, value);
        return;
      } else {
        // string constant (!) what to do?!
        logger.atSevere().log("Cannot retrieve string yet: %s", source);
      }
    } else
      switch (source.storage()) {
        case GLOBAL:
          // if source is global:
          // Use r15 as a temp
          // 1. mov r15, [source]
          // note, this may fail for strings, because they need to be quadwords
          emit("  mov dword r15d, [%s]", source);
          break;
        default:
          // if source is temp or local:
          // ???
          // if source is stack:
          // 1. mov r15, [ebp+???]
          logger.atSevere().log(String.format("Cannot retrieve from %s yet", source.storage()));
      }

    // if dest is global:
    switch (destination.storage()) {
      case GLOBAL:
        // 2. mov [dest], r15
        // note, this will fail for strings, because they need to be quadwords (i.e., addresses)
        emit("  mov dword [%s], r15d", destination);
        break;
        // if dest is temp or local:
        // ???
        // if dest is stack:
        // 2. mov [ebp+???], r15
        // if dest is fieldset:
        // 2. ???
      default:
        logger.atSevere().log("Cannot store in %s yet", destination.storage());
    }
  }

  @Override
  public void visit(SysCall op) {
    Operand arg = op.arg();
    switch (op.call()) {
      case INPUT:
        break;
      case MESSAGE: // exit "foo"
        emit("  sub rsp, 0x28          ; Reserve the shadow space");
        emit("  mov rcx, __EXIT_MSG");
        emit("  call printf           ; printf(__EXIT_MSG)");
        if (arg.isConstant()) {
          emit("  add rsp, 0x28          ; Remove shadow space");
          // have to store constant in memory
          logger.atSevere().log("Cannot exit string %s yet", arg);
        } else {
          // must be a string global
          emit("  mov rcx, %s", arg.toString());
          emit("  call puts             ; puts message with newline");
          emit("  add rsp, 0x28          ; Remove shadow space");
        }
        break;
      case PRINT:
        if (arg.isConstant()) {
          ConstantOperand<?> argOp = (ConstantOperand<?>) arg;
          Object value = argOp.value();
          if (value instanceof Integer) {
            emit("  sub rsp, 0x28          ; Reserve the shadow space");
            emit("  mov rcx, __PRINTF_NUMBER_FMT ; First argument is address of message");
            // this works for ints only
            emit("  mov rdx, %s         ; Second argument is parameter", arg.toString());
            emit("  call printf           ; printf(message)");
            emit("  add rsp, 0x28          ; Remove shadow space");
          } else {
            // string constant (!) what to do?!
            // might be string might be int... ugh
            logger.atSevere().log("Cannot print string yet: %s", arg);
          }
        } else {
          // might be string might be int... ugh
          emit("  sub rsp, 0x28          ; Reserve the shadow space");
          emit("  mov rcx, __PRINTF_NUMBER_FMT ; First argument is address of message");
          // this works for ints only; might fail for non-globals
          emit("  mov rdx, [%s]         ; Second argument is parameter", arg.toString());
          emit("  call printf           ; printf(message)");
          emit("  add rsp, 0x28          ; Remove shadow space");
        }
        break;
    }
  }

  @Override
  public void visit(Dec op) {
    // this will not work for non-globals
    Preconditions.checkArgument(
        op.target().storage() == SymbolStorage.GLOBAL, "Can only dec globals for now");
    emit("  dec dword [%s]", op.target());
  }

  @Override
  public void visit(Inc op) {
    Preconditions.checkArgument(
        op.target().storage() == SymbolStorage.GLOBAL, "Can only inc globals for now");
    emit("  inc dword [%s]", op.target());
  }

  @Override
  public void visit(BinOp op) {
    logger.atSevere().log("Cannot generate %s yet", op);
  }

  private void emit(String format, Object... values) {
    asm.add(String.format(format, values));
    logger.atFine().log(String.format(format, values));
  }

  private void emit(Op op) {
    emit("\n  ; %s", op.toString());
  }
}
