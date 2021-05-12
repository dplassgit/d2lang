package com.plasstech.lang.d2.codegen.il;

public class Return extends Op {

  @Override
  public String toString() {
    return "\treturn;";
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }
}
