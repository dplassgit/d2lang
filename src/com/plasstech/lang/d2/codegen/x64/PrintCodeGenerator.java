package com.plasstech.lang.d2.codegen.x64;

import static com.plasstech.lang.d2.codegen.Codegen.fail;
import static com.plasstech.lang.d2.codegen.x64.IntRegister.RCX;
import static com.plasstech.lang.d2.codegen.x64.IntRegister.RDX;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.plasstech.lang.d2.codegen.ConstEntry;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Emitter;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.StringTable;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.SysCall.Call;
import com.plasstech.lang.d2.common.Labels;
import com.plasstech.lang.d2.type.PrintFormats;
import com.plasstech.lang.d2.type.PrintFormats.Format;
import com.plasstech.lang.d2.type.VarType;

/** Generates NASM code for printing things. */
class PrintCodeGenerator extends DefaultOpcodeVisitor {
  private static final String EXIT_MSG = "EXIT_MSG: db \"ERROR: %s\", 10, 0";

  private static final Set<Call> PRINT_CALLS =
      ImmutableSet.of(Call.PRINT, Call.PRINTLN, Call.MESSAGE, Call.PARAMETERIZED_MESSAGE);

  private final Resolver resolver;
  private final Emitter emitter;
  private final StringTable stringTable;

  PrintCodeGenerator(Resolver resolver, StringTable table, Emitter emitter) {
    this.resolver = resolver;
    this.stringTable = table;
    this.emitter = emitter;
  }

  @Override
  public void visit(SysCall op) {
    if (!PRINT_CALLS.contains(op.call())) {
      return;
    }
    RegisterState registerState =
        RegisterState.condPush(emitter, resolver, Register.VOLATILE_REGISTERS);

    Operand arg = op.arg();
    ResolvedOperand argRo = resolver.resolveFully(arg);
    String argName = argRo.name();

    boolean isNewline =
        op.call() == Call.PRINTLN
            || op.call() == Call.MESSAGE
            || op.call() == Call.PARAMETERIZED_MESSAGE;
    Format format = PrintFormats.getFormat(arg.type());
    if (arg.type() == VarType.INT) {

      Size size = Size.of(arg.type());
      // Move with sign extend. Intentionally set rdx first, in case the arg is in rcx
      emitter.emit("movsx RDX, %s %s", size.asmType, argName);
      setUpFormat(format, isNewline);

    } else if (arg.type() == VarType.BYTE) {

      emitter.emit("xor RDX, RDX");
      // note: no sign extend; otherwise it will prepend ffs
      emitter.emit("mov DL, BYTE %s", argName);
      setUpFormat(format, isNewline);

    } else if (arg.type() == VarType.LONG) {

      emitter.emit("mov RDX, %s", argName);
      setUpFormat(format, isNewline);

    } else if (arg.type() == VarType.BOOL) {

      if (argName.equals("1")) {
        // This usually wouldn't happen; the print optimizer would have already translated
        // 1 to "true" and 0 to "false".
        setUpFormat(Format.TRUE, isNewline);
      } else if (argName.equals("0")) {
        setUpFormat(Format.FALSE, isNewline);
      } else {
        // Translate dynamically from 0/1 to false/true
        emitter.emit("cmp BYTE %s, 1", argName);
        setUpFormat(Format.FALSE, isNewline);

        // This puts pattern into RDX
        emitter.addData(constData(Format.TRUE, isNewline));
        emitter.emit("mov RDX, %s", constName(Format.TRUE, isNewline));
        // Conditionally move the pattern from either RCX or RDS
        emitter.emit("cmovz RCX, RDX");
      }

    } else if (arg.type() == VarType.STRING) {
      if (op.call() == SysCall.Call.MESSAGE) {
        // Intentionally set rdx first in case the arg is in rcx
        resolver.mov(arg, RDX);
        emitter.addData(EXIT_MSG);
        emitter.emit("mov RCX, EXIT_MSG");
      } else if (op.call() == SysCall.Call.PARAMETERIZED_MESSAGE) {
        // The arg is a string constant. Need to get its entry so we can get its name.
        String constValue = ConstantOperand.stringValueFromConstOperand(op.arg());
        ConstEntry<String> entry = stringTable.lookup(constValue);
        List<Operand> operands = op.operands();

        for (int i = 1; i < operands.size(); ++i) {
          Operand operand = operands.get(i);
          if (i < 4) {
            // Yes, we skip 1 (RCX)
            // TODO: this is broken in the same way proccalls *were* broken when it's not
            // the "easy case":
            /**
             * We need to do gymnastics when a param register needs to be copied to a LATER param
             * register, e.g., RCX needs to be in RDX or RDX needs to be in R9
             */
            Register register = Register.INT_PARAM_REGISTERS.get(i);
            resolver.mov(operand, register);
          } else {
            //  push the rest
            // I don't love this, but it's a start.
            resolver.mov(operand, IntRegister.RCX);
            // bug: it never fixes the stack, but... parameterized messages are always followed
            // by an "exit" so... we don't quite care. but it's still a bug.
            emitter.emit("push RCX");
          }
        }

        emitter.emit("mov RCX, %s  ; pattern", entry.name());
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
            emitter.emit("cmp QWORD RDX, 0");
            String notNullLabel = Labels.nextLabel("not_null");
            emitter.emit("jne %s", notNullLabel);
            setUpFormat(Format.NULL, isNewline);
            emitter.emitLabel(notNullLabel);
          }
        }
      }

    } else if (arg.type() == VarType.NULL) {

      setUpFormat(format, isNewline);

    } else if (arg.type() == VarType.DOUBLE) {
      if (arg.isConstant()) {
        // this shouldn't happen in optimization mode because the arithmetic optimizer
        // should have already converted to string.
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
      // TODO: If arg is a round number, print a trailing .0
    } else {
      fail(op.position(), "Cannot print %ss yet", arg.type());
    }
    emitter.emitExternCall("printf");
    emitter.emitExternCall("_flushall");
    registerState.condPop();
    resolver.deallocate(arg);
  }

  private static String constName(Format format, boolean newline) {
    if (newline) {
      return String.format("PRINTLN_%s", format.name());
    } else {
      return String.format("PRINT_%s", format.name());
    }
  }

  private static String constData(Format format, boolean newline) {
    String suffix = "0";
    if (newline) {
      suffix = "10, 0";
    }
    return String.format("%s: db \"%s\", %s", constName(format, newline), format.spec, suffix);
  }

  private void setUpFormat(Format format, boolean isNewline) {
    emitter.addData(constData(format, isNewline));
    emitter.emit("mov RCX, %s", constName(format, isNewline));
  }
}
