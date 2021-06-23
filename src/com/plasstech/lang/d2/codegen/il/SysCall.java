package com.plasstech.lang.d2.codegen.il;

import com.plasstech.lang.d2.codegen.Operand;

public class SysCall extends Op {
  public enum Call {
    MESSAGE,
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
    switch(call) {
      case PRINT:
        return String.format("printf(\"%%s\", %s);", arg);
      case MESSAGE:
        return String.format("printf(\"ERROR: %%s\", %s);", arg);
      default:
        return String.format("call(%s, %s);", call.name(), arg);
    }
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }
}
