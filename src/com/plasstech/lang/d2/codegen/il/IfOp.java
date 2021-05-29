package com.plasstech.lang.d2.codegen.il;

public class IfOp extends Op {
  private final Operand condition;
  private final String dest;

  public IfOp(Operand notCondition, String dest) {
    this.condition = notCondition;
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
    return String.format("\tif (%s) goto %s;", condition, dest);
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }
}
