package com.plasstech.lang.d2.codegen.il;

import com.plasstech.lang.d2.codegen.Operand;

public class IfOp extends Op {
  private final Operand condition;
  private final String dest;
  private final boolean jumpNot;

  public IfOp(Operand condition, String dest, boolean jumpNot) {
    this.condition = condition;
    this.dest = dest;
    this.jumpNot = jumpNot;
  }

  public Operand condition() {
    return condition;
  }

  public String destination() {
    return dest;
  }

  public boolean isNot() {
    return jumpNot;
  }

  @Override
  public String toString() {
    return String.format("if %s%s goto %s", jumpNot ? "not " : "", condition, dest);
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }
}
