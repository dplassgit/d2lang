package com.plasstech.lang.d2.codegen.il;

public class ProcExit extends Op {

  private final String procName;
  private final int localBytes;

  public ProcExit(String procName, int localBytes) {
    this.procName = procName;
    this.localBytes = localBytes;
  }

  @Override
  public String toString() {
    return "end " + procName();
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }

  public String procName() {
    return procName;
  }

  public int localBytes() {
    return localBytes;
  }
}
