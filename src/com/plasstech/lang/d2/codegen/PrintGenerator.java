package com.plasstech.lang.d2.codegen;

import static com.plasstech.lang.d2.codegen.Register.RCX;
import static com.plasstech.lang.d2.codegen.Register.RDX;

import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.type.VarType;

/** Generates NASM code for printing things. */
class PrintGenerator {
  private static final String EXIT_MSG = "EXIT_MSG: db \"ERROR: %s\", 0";
  private static final String PRINTF_NUMBER_FMT = "PRINTF_NUMBER_FMT: db \"%d\", 0";
  private static final String CONST_FALSE = "CONST_FALSE: db \"false\", 0";
  private static final String CONST_TRUE = "CONST_TRUE: db \"true\", 0";
  private static final String CONST_NULL = "CONST_NULL: db \"null\", 0";

  private final Resolver resolver;
  private final Emitter emitter;

  public PrintGenerator(Resolver resolver, Emitter emitter) {
    this.resolver = resolver;
    this.emitter = emitter;
  }

  void generate(SysCall op) {
    Operand arg = op.arg();
    String argVal = resolver.resolve(arg);
    if (arg.type() == VarType.INT) {
      // move with sign extend. intentionally set rdx first, in case the arg is in ecx
      emitter.emit("movsx RDX, DWORD %s  ; Second argument is parameter", argVal);
      emitter.addData(PRINTF_NUMBER_FMT);
      emitter.emit("mov RCX, PRINTF_NUMBER_FMT  ; First argument is address of pattern");
      emitter.emitExternCall("printf");
    } else if (arg.type() == VarType.BOOL) {
      if (argVal.equals("1")) {
        emitter.addData(CONST_TRUE);
        emitter.emit("mov RCX, CONST_TRUE");
      } else if (argVal.equals("0")) {
        emitter.addData(CONST_FALSE);
        emitter.emit("mov RCX, CONST_FALSE");
      } else {
        // translate dynamically from 0/1 to false/true
        // Intentionally do the comp first, in case the arg is in dl or cl
        emitter.emit("cmp BYTE %s, 1", argVal);
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
        if (!resolver.isInRegister(arg, RDX)) {
          // arg is not in rdx yet
          emitter.emit("mov RDX, %s  ; Second argument is parameter/string to print", argVal);
        }
        emitter.addData(EXIT_MSG);
        emitter.emit("mov RCX, EXIT_MSG  ; First argument is address of pattern");
      } else if (!resolver.isInRegister(arg, RCX)) {
        // arg is not in rcx yet
        emitter.emit("mov RCX, %s  ; String to print", argVal);
      }
      if (!arg.isConstant()) {
        // if null, print null
        emitter.emit("cmp QWORD RCX, 0");
        String notNullLabel = resolver.nextLabel("not_null");
        emitter.emit("jne _%s", notNullLabel);
        emitter.addData(CONST_NULL);
        emitter.emit("mov RCX, CONST_NULL  ; constant 'null'");
        emitter.emit0("_%s:", notNullLabel);
      }
      emitter.emitExternCall("printf");
    } else if (arg.type() == VarType.NULL) {
      emitter.addData(CONST_NULL);
      emitter.emit("mov RCX, CONST_NULL  ; constant 'null'");
      emitter.emitExternCall("printf");
    } else {
      emitter.fail("Cannot print %ss yet", arg.type());
    }
  }
}
