package com.plasstech.lang.d2.codegen.il;

public class Stop extends Op {
  @Override
  public String toString() {
    return "exit(0);";
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }
}
