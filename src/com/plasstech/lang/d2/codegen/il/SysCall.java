package com.plasstech.lang.d2.codegen.il;

public class SysCall extends Op {
  private final String callName;
  private final String arg;

  public SysCall(String callName, String arg) {
    this.callName = callName;
    this.arg = arg;
  }

  public String callName() {
    return callName;
  }

  public String arg() {
    return arg;
  }

  @Override
  public String toString() {
    if (callName.equals("$ffd2")) {
      // TODO: need type of "arg"
      // print
      return String.format("\tprintf(\"%%s\", %s);", arg);
    }
    return String.format("\tcall(%s, %s);", callName, arg);
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }
}
