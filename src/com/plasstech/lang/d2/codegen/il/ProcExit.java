package com.plasstech.lang.d2.codegen.il;

public class ProcExit extends Op {
  @Override
  public String toString() {
    return "} // end proc";
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }
}
