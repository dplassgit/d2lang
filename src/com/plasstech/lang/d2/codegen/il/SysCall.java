package com.plasstech.lang.d2.codegen.il;

import com.google.common.base.Preconditions;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Operand;

public class SysCall extends Op {

  public enum Call {
    MESSAGE,
    PARAMETERIZED_MESSAGE,
    PRINT,
    PRINTLN,
    INPUT
  }

  private final Call call;
  private final Operand arg;
  private int line;
  private int column;

  public SysCall(Call call, Operand arg) {
    this.call = call;
    this.arg = arg;
  }

  public SysCall(String message, int line, int column) {
    this(Call.PARAMETERIZED_MESSAGE, ConstantOperand.of(message));
    this.line = line;
    this.column = column;
  }

  public Call call() {
    return call;
  }

  public Operand arg() {
    return arg;
  }

  public int getLine() {
    Preconditions.checkState(call == Call.PARAMETERIZED_MESSAGE);
    return line;
  }

  public int getColumn() {
    Preconditions.checkState(call == Call.PARAMETERIZED_MESSAGE);
    return column;
  }

  @Override
  public String toString() {
    switch (call) {
      case PRINT:
        return String.format("printf(\"%%s\", %s)", ESCAPER.escape(arg.toString()));

      case PRINTLN:
        return String.format("printf(\"%%s\\n\", %s)", ESCAPER.escape(arg.toString()));

      case MESSAGE:
        return String.format("printf(\"ERROR: %%s\", %s)", ESCAPER.escape(arg.toString()));

      case PARAMETERIZED_MESSAGE:
        return String.format(
            "printf(\"ERROR: %s, %d, %d)", ESCAPER.escape(arg.toString()), line, column);

      case INPUT:
        return String.format("%s=_read()", arg);

      default:
        throw new IllegalArgumentException("Unknown syscall: " + call);
    }
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }
}
