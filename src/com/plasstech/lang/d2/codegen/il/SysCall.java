package com.plasstech.lang.d2.codegen.il;

public class SysCall extends Op {
  public enum Call {
    PRINT
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
    if (call == Call.PRINT) {
      // TODO: need type of "arg", because it may be an int variable or a string constant.
      return String.format("printf(\"%%s\", %s);", arg);
    }
    return String.format("call(%s, %s);", call.name(), arg);
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }
}
