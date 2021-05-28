package com.plasstech.lang.d2.codegen.il;

public class SysCall extends Op {
  public enum Call {
    PRINT
  }

  private final Call call;
  private final String arg;

  public SysCall(Call call, String arg) {
    this.call = call;
    this.arg = arg;
  }

  public Call call() {
    return call;
  }

  public String arg() {
    return arg;
  }

  @Override
  public String toString() {
    if (call == Call.PRINT) {
      // TODO: need type of "arg", because it may be an int variable or a string constant.
      return String.format("\tprintf(\"%%s\", %s);", arg);
    }
    return String.format("\tcall(%s, %s);", call.name(), arg);
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }
}
