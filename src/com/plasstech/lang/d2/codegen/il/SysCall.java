package com.plasstech.lang.d2.codegen.il;

import com.plasstech.lang.d2.codegen.Operand;

public class SysCall extends Op {

  public enum Call {
    MESSAGE,
    PRINT,
    PRINTLN,
    INPUT
  }

  private final Call call;
  private final Operand arg;

  public SysCall(Call call, Operand arg) {
    this.call = call;
    this.arg = arg;
  }

  public Call call() {
    return call;
  }

  public Operand arg() {
    return arg;
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
