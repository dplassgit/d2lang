package com.plasstech.lang.d2.codegen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

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
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.phase.Phase;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.type.SymTab;
import com.plasstech.lang.d2.type.Symbol;
import com.plasstech.lang.d2.type.SymbolStorage;

public class NasmCodeGenerator extends DefaultOpcodeVisitor implements Phase {

  private static FluentLogger logger = FluentLogger.forEnclosingClass();

  private final List<String> asm = new ArrayList<>();
  private final Registers registers = new Registers();
  // map from name to register
  private final Map<String, Register> aliases = new HashMap<>();
  // it's possible that we've run out of registers. Push the name on the stack and deallocate
  // its register. (Maybe put this feature into Registers? Maybe make an "Aliases" object?)
  private final Stack<String> tempStack = new Stack<>();

  @Override
  public State execute(State input) {
    ImmutableList<Op> code = input.lastIlCode();
    String f = "dcode";
    if (input.filename() != null) {
      f = input.filename();
    }
    emit0("; To execute:");
    // -Ox = optimize
    emit0("; nasm -fwin64 -Ox %s.asm && gcc %s.obj -o %s && ./%s", f, f, f, f);

    emit0("%%use altreg"); // lets us use r01 instead of rax, etc.
    emit0("global main"); // required
    emit0("extern puts");
    emit0("extern printf");
    emit0("extern exit");

    // Probably what we should do is:
    // 1. emit all globals OK
    // 2. emit all string constants not done - if string constants are propagated, ???
    // maybe we shouldn't constant propagate string constants except if it's "foo"[3]
    // 3. emit all array constants (?)
    emit0("\nsection .data");
    SymTab globals = input.symbolTable();
    for (Map.Entry<String, Symbol> entry : globals.entries().entrySet()) {
      if (entry.getValue().storage() == SymbolStorage.GLOBAL) {
        // temporarily reserve (& clear) 4 bytes per int, 8 bytes for string
        emit("%s: dd 0", entry.getKey());
      }
    }
    // TODO: only emit these if we need to.
    emit("__PRINTF_NUMBER_FMT: db \"%%d\", 0");
    emit("__EXIT_MSG: db \"ERROR: \", 0");

    emit0("\nsection .text");

    // TODO: convert command-line arguments to ??? and send to __main
    emit0("main:");
    for (Op opcode : code) {
      emit(opcode);
      opcode.accept(this);
    }

    return input.addAsmCode(ImmutableList.copyOf(asm));
  }

  @Override
  public void visit(Label op) {
    emit0("%s:", op.label());
  }

  @Override
  public void visit(Stop op) {
    emit("mov rcx, %d", op.exitCode());
    emit("call exit");
  }

  @Override
  public void visit(Goto op) {
    emit("jmp %s", op.label());
  }

  @Override
  public void visit(Transfer op) {
    Operand source = op.source();
    String sourceLoc = resolve(source);
    Location destination = op.destination();
    String destLoc = resolveDest(destination);
    if (source.isConstant()) {
      // TODO: fails for strings.
      emit("mov dword %s, %s", destLoc, sourceLoc);
    } else {
      // TODO: this might be wrong.
      emit("mov r15, %s", sourceLoc);
      // TODO: this might be wrong, if it's a memory locatoin.
      emit("mov dword %s, r15d", destLoc);
    }
  }

  @Override
  public void visit(SysCall op) {
    Operand arg = op.arg();
    switch (op.call()) {
      case INPUT:
        fail("Cannot generate %s yet", op);
        break;
      case MESSAGE: // exit "foo"
        emit("sub rsp, 0x28         ; Reserve the shadow space");
        emit("mov rcx, __EXIT_MSG");
        emit("call printf           ; printf(__EXIT_MSG)");
        if (arg.isConstant()) {
          emit("add rsp, 0x28         ; Remove shadow space");
          // have to store string constant in memory
          fail("Cannot exit string %s yet", arg);
        } else {
          // must be a string global
          emit("mov rcx, %s", arg.toString());
          emit("call puts             ; puts message with newline");
          emit("add rsp, 0x28         ; Remove shadow space");
        }
        break;
      case PRINT:
        if (arg.isConstant()) {
          ConstantOperand<?> argOp = (ConstantOperand<?>) arg;
          Object value = argOp.value();
          if (value instanceof Integer) {
            emit("sub rsp, 0x28         ; Reserve the shadow space");
            emit("mov rcx, __PRINTF_NUMBER_FMT ; First argument is address of message");
            // this works for ints only
            emit("mov rdx, %s           ; Second argument is parameter", arg.toString());
            emit("call printf           ; printf(message)");
            emit("add rsp, 0x28         ; Remove shadow space");
          } else {
            // string constant (!) what to do?!
            // might be string might be int... ugh
            fail("Cannot print string yet: %s", arg);
          }
        } else {
          // might be string might be int... ugh
          emit("sub rsp, 0x28         ; Reserve the shadow space");
          emit("mov rcx, __PRINTF_NUMBER_FMT ; First argument is address of message");
          // this works for ints only; might fail for non-globals
          emit("mov rdx, [%s]         ; Second argument is parameter", arg.toString());
          emit("call printf           ; printf(message)");
          emit("add rsp, 0x28         ; Remove shadow space");
        }
        break;
    }
  }

  @Override
  public void visit(Dec op) {
    emit("dec dword %s", resolve(op.target()));
  }

  @Override
  public void visit(Inc op) {
    emit("inc dword %s", resolve(op.target()));
  }

  @Override
  public void visit(BinOp op) {
    // 1. get left
    String leftName = resolve(op.left());
    // 2. get right
    String rightName = resolve(op.right());
    // 3. determine dest location
    String destName = resolveDest(op.destination());
    // 4. mov dest, left
    // 5. {op] dest, right
    fail("Cannot generate %s yet", op);
  }

  private String resolveDest(Location loc) {
    Register reg = this.aliases.get(loc.name());
    if (reg != null) {
      return reg.toString();
    }
    switch (loc.storage()) {
      case TEMP:
        // TODO: deal with out-of-registers situation
        reg = registers.allocate();
        aliases.put(loc.name(), reg);
        return reg.name();
      case GLOBAL:
        return "[" + loc.name() + "]";
      default:
        fail("Cannot generate %s yet", loc);
        return null;
    }
  }

  @Override
  public void visit(UnaryOp op) {
    // 1. get source location name
    String sourceName = resolve(op.operand());
    // 2. apply op
    // 3. store in destination
    Location destination = op.destination();
    String destName = resolveDest(destination);
    emit("mov dword %s, %s ; unary setup", destName, sourceName);
    // apply op
    switch (op.operator()) {
      case BIT_NOT:
      case NOT:
        // NOTE: NOT TWOS COMPLEMENT NOT, just binary not.
        emit("not %s; unary operation", destName);
        break;
      case MINUS:
        emit("neg %s; unary operation", destName);
        break;
      case LENGTH:
      case ASC:
      case CHR:
      default:
        fail("Cannot generate %s yet", op);
        break;
    }
  }

  private void fail(String format, Object obj) {
    throw new UnsupportedOperationException(String.format(format, obj));
  }

  private String resolve(Operand operand) {
    if (operand.storage() == SymbolStorage.IMMEDIATE) {
      // TODO: fails for string constants.
      return operand.toString();
    }

    Register sourceReg = aliases.get(operand.toString());
    if (sourceReg != null) {
      return sourceReg.name();
    }
    switch (operand.storage()) {
      case GLOBAL:
        return "[" + operand.toString() + "]";
      default:
        fail("Cannot generate %s yet", operand);
        return operand.toString();
    }
  }

  // Emit at column 0
  private void emit0(String format, Object... values) {
    asm.add(String.format(format, values));
    logger.atFine().log(String.format(format, values));
  }

  // Emit at column 2
  private void emit(String format, Object... values) {
    emit0("  " + format, values);
  }

  private void emit(Op op) {
    emit0("\n  ; %s", op.toString());
  }
}
