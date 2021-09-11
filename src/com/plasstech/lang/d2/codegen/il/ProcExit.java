package com.plasstech.lang.d2.codegen.il;

public class ProcExit extends Op {

  private final String procName;

  public ProcExit(String procName) {
    this.procName = procName;
  }

  @Override
  public String toString() {
    return "} // end " + procName();
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }

  public String procName() {
    return procName;
  }
}
