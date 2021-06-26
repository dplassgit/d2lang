package com.plasstech.lang.d2.codegen.il;

// An opcode.
public abstract class Op {

  public void accept(OpcodeVisitor visitor) {
    // does nothing by default.
  }
}
