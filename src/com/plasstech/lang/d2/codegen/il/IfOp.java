package com.plasstech.lang.d2.codegen.il;

public class IfOp extends Op {
  private final String cond;
  private final String dest;

  public IfOp(String cond, String dest) {
    this.cond = cond;
    this.dest = dest;
  }

  public String condition() {
    return cond;
  }

  public String destination() {
    return dest;
  }

  @Override
  public String toString() {
    return String.format("\tif (%s) goto %s;", cond, dest);
  }
}
