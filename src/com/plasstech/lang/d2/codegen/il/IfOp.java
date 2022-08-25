package com.plasstech.lang.d2.codegen.il;

import com.plasstech.lang.d2.codegen.Operand;

public class IfOp extends Op {
  private final Operand condition;
  private final String dest;

  public IfOp(Operand condition, String dest) {
    this.condition = condition;
    this.dest = dest;
  }

  public Operand condition() {
    return condition;
  }

  public String destination() {
    return dest;
  }

  @Override
  public String toString() {
    return String.format("if (%s) goto %s", condition, dest);
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }
}
