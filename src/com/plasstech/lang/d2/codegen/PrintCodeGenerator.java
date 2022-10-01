package com.plasstech.lang.d2.codegen;

import static com.plasstech.lang.d2.codegen.IntRegister.RCX;
import static com.plasstech.lang.d2.codegen.IntRegister.RDX;

import com.plasstech.lang.d2.codegen.Resolver.ResolvedOperand;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.type.VarType;

/** Generates NASM code for printing things. */
class PrintCodeGenerator {
  private static final String EXIT_MSG = "EXIT_MSG: db \"ERROR: %s\", 0";
  private static final String PRINTF_INT_FMT = "PRINTF_INT_FMT: db \"%d\", 0";
  private static final String PRINTF_DOUBLE_FMT = "PRINTF_DOUBLE_FMT: db \"%f\", 0";
  private static final String CONST_FALSE = "CONST_FALSE: db \"false\", 0";
  private static final String CONST_TRUE = "CONST_TRUE: db \"true\", 0";
  private static final String CONST_NULL = "CONST_NULL: db \"null\", 0";

  private final Resolver resolver;
  private final Emitter emitter;

  PrintCodeGenerator(Resolver resolver, Emitter emitter) {
    this.resolver = resolver;
    this.emitter = emitter;
  }

  void generate(SysCall op) {
    Operand arg = op.arg();
    ResolvedOperand argRo = resolver.resolveFully(arg);
    String argName = argRo.name();
    if (arg.type() == VarType.INT || arg.type() == VarType.BYTE) {
      // TODO: print bytes with 0y prefix?
      // move with sign extend. intentionally set rdx first, in case the arg is in ecx
      Size size = Size.of(arg.type());
      emitter.emit("movsx RDX, %s %s  ; parameter", size.asmType, argName);
      emitter.addData(PRINTF_INT_FMT);
      emitter.emit("mov RCX, PRINTF_INT_FMT  ; pattern");
      emitter.emitExternCall("printf");
    } else if (arg.type() == VarType.BOOL) {
      if (argName.equals("1")) {
        emitter.addData(CONST_TRUE);
        emitter.emit("mov RCX, CONST_TRUE");
      } else if (argName.equals("0")) {
        emitter.addData(CONST_FALSE);
        emitter.emit("mov RCX, CONST_FALSE");
      } else {
        // translate dynamically from 0/1 to false/true
        // Intentionally do the comp first, in case the arg is in dl or cl
        emitter.emit("cmp BYTE %s, 1", argName);
        emitter.addData(CONST_FALSE);
        emitter.emit("mov RCX, CONST_FALSE");
        emitter.addData(CONST_TRUE);
        emitter.emit("mov RDX, CONST_TRUE");
        // Conditional move
        emitter.emit("cmovz RCX, RDX");
      }
      emitter.emitExternCall("printf");
    } else if (arg.type() == VarType.STRING) {
      // String
      if (op.call() == SysCall.Call.MESSAGE) {
        // Intentionally set rdx first in case the arg is in rcx
        resolver.mov(arg, RDX);
        emitter.addData(EXIT_MSG);
        emitter.emit("mov RCX, EXIT_MSG  ; pattern");
      } else {
        // arg is not in rcx yet
        resolver.mov(arg, RCX);
      }
      if (!arg.isConstant()) {
        // if null, print null
        emitter.emit("cmp QWORD RCX, 0");
        String notNullLabel = resolver.nextLabel("not_null");
        emitter.emit("jne %s", notNullLabel);
        emitter.addData(CONST_NULL);
        emitter.emit("mov RCX, CONST_NULL  ; constant 'null'");
        emitter.emitLabel(notNullLabel);
      }
      emitter.emitExternCall("printf");
    } else if (arg.type() == VarType.NULL) {
      emitter.addData(CONST_NULL);
      emitter.emit("mov RCX, CONST_NULL  ; constant 'null'");
      emitter.emitExternCall("printf");
    } else if (arg.type() == VarType.DOUBLE) {
      if (arg.isConstant()) {
        // this step is only really needed if argName is a constant.
        Register tempReg = resolver.allocate(VarType.DOUBLE);
        emitter.emit("movsd %s, %s", tempReg, argName);
        emitter.emit("movq RDX, %s", tempReg);
        resolver.deallocate(tempReg);
      } else if (resolver.isInAnyRegister(arg)) {
        // argval is an xmm register
        emitter.emit("movq RDX, %s", argName);
      } else {
        // arg is in memory.
        emitter.emit("mov RDX, %s", argName);
      }
      emitter.addData(PRINTF_DOUBLE_FMT);
      emitter.emit("mov RCX, PRINTF_DOUBLE_FMT  ; address of pattern");
      emitter.emitExternCall("printf");
    } else {
      emitter.fail("Cannot print %ss yet", arg.type());
    }
    emitter.emitExternCall("_flushall");
  }
}
