package com.plasstech.lang.d2.codegen.x64;

import static com.plasstech.lang.d2.codegen.x64.IntRegister.RCX;
import static com.plasstech.lang.d2.codegen.x64.IntRegister.RDX;

import com.plasstech.lang.d2.codegen.Emitter;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.SysCall.Call;
import com.plasstech.lang.d2.codegen.x64.Resolver.ResolvedOperand;
import com.plasstech.lang.d2.type.VarType;

/** Generates NASM code for printing things. */
class PrintCodeGenerator extends DefaultOpcodeVisitor {
  private static final String EXIT_MSG = "EXIT_MSG: db \"ERROR: %s\", 10, 0";

  private final Resolver resolver;
  private final Emitter emitter;

  PrintCodeGenerator(Resolver resolver, Emitter emitter) {
    this.resolver = resolver;
    this.emitter = emitter;
  }

  @Override
  public void visit(SysCall op) {
    RegisterState registerState =
        RegisterState.condPush(emitter, resolver, Register.VOLATILE_REGISTERS);

    Operand arg = op.arg();
    ResolvedOperand argRo = resolver.resolveFully(arg);
    String argName = argRo.name();
    boolean isNewline = op.call() == Call.PRINTLN || op.call() == Call.MESSAGE;
    if (arg.type() == VarType.INT || arg.type() == VarType.BYTE) {
      // Move with sign extend. Intentionally set rdx first, in case the arg is in ecx
      Size size = Size.of(arg.type());
      emitter.emit("movsx RDX, %s %s  ; parameter", size.asmType, argName);
      setUpFormat(Format.INT, isNewline);
      emitter.emitExternCall("printf");
    } else if (arg.type() == VarType.LONG) {
      emitter.emit("mov RDX, %s  ; parameter", argName);
      setUpFormat(Format.LONG, isNewline);
      emitter.emitExternCall("printf");

    } else if (arg.type() == VarType.BOOL) {

      if (argName.equals("1")) {
        setUpFormat(Format.TRUE, isNewline);
      } else if (argName.equals("0")) {
        setUpFormat(Format.FALSE, isNewline);
      } else {

        // Translate dynamically from 0/1 to false/true
        emitter.emit("cmp BYTE %s, 1", argName);
        setUpFormat(Format.FALSE, isNewline);

        // This puts pattern into RDX
        emitter.addData(Format.TRUE.constData(isNewline));
        emitter.emit("mov RDX, %s  ; pattern", Format.TRUE.constName(isNewline));
        // Conditionally move the pattern from either RCX or RDS
        emitter.emit("cmovz RCX, RDX");
      }

      emitter.emitExternCall("printf");

    } else if (arg.type() == VarType.STRING) {

      if (op.call() == SysCall.Call.MESSAGE) {
        // Intentionally set rdx first in case the arg is in rcx
        resolver.mov(arg, RDX);
        emitter.addData(EXIT_MSG);
        emitter.emit("mov RCX, EXIT_MSG  ; pattern");
      } else {
        // arg may not be in rcx (or rdx) yet
        if (arg.isConstant() && op.call() == SysCall.Call.PRINT) {
          // no newline needed, just print the constant.
          resolver.mov(arg, RCX);
        } else {
          resolver.mov(arg, RDX);
          // pre-set the format as string
          setUpFormat(Format.STRING, isNewline);

          if (!arg.isConstant()) {
            // check for null; if null, change format to printing null
            emitter.emit("cmp QWORD %s, 0", RDX);
            String notNullLabel = resolver.nextLabel("not_null");
            emitter.emit("jne %s", notNullLabel);
            setUpFormat(Format.NULL, isNewline);
            emitter.emitLabel(notNullLabel);
          }
        }
      }
      emitter.emitExternCall("printf");

    } else if (arg.type() == VarType.NULL) {
      setUpFormat(Format.NULL, isNewline);
      emitter.emitExternCall("printf");
    } else if (arg.type() == VarType.DOUBLE) {
      if (arg.isConstant()) {
        // Only needed if argName is a constant
        Register tempReg = resolver.allocate(VarType.DOUBLE);
        emitter.emit("movsd %s, %s", tempReg, argName);
        emitter.emit("movq RDX, %s", tempReg);
        resolver.deallocate(tempReg);
      } else if (resolver.isInAnyRegister(arg)) {
        // arg is an xmm register
        emitter.emit("movq RDX, %s", argName);
      } else {
        // arg is in memory. NOTE: do not use resolver.mov because it will try to use movsd which is
        // wrong for non-registers or something like that
        emitter.emit("mov RDX, %s", argName);
      }
      setUpFormat(Format.DOUBLE, isNewline);
      emitter.emitExternCall("printf");
    } else {
      emitter.fail("Cannot print %ss yet", arg.type());
    }
    emitter.emitExternCall("_flushall");
    registerState.condPop();
    resolver.deallocate(arg);
  }

  private enum Format {
    // TODO: print bytes with 0y prefix?
    INT("%d"),
    LONG("%lld"),
    DOUBLE("%f"),
    TRUE("true"),
    FALSE("false"),
    STRING("%s"),
    NULL("null");

    private final String spec;

    Format(String spec) {
      this.spec = spec;
    }

    String constName(boolean newline) {
      if (newline) {
        return String.format("PRINTLN_%s", this.name());
      } else {
        return String.format("PRINT_%s", this.name());
      }
    }

    String constData(boolean newline) {
      String suffix = "0";
      if (newline) {
        suffix = "10, 0";
      }
      return String.format("%s: db \"%s\", %s", constName(newline), this.spec, suffix);
    }
  }

  private void setUpFormat(Format format, boolean isNewline) {
    emitter.addData(format.constData(isNewline));
    emitter.emit("mov RCX, %s  ; pattern", format.constName(isNewline));
  }
}
