package com.plasstech.lang.d2.codegen.il;

public class Assignment extends Op {
  private final String lhs;
  private final String rhs;

  // TODO: only allow reg=reg or reg=constant
  public Assignment(String lhs, String rhs) {
    this.lhs = lhs;
    this.rhs = rhs;
  }

  public String lhs() {
    return lhs;
  }

  public String rhs() {
    return rhs;
  }

  @Override
  public String toString() {
    return String.format("\t%s = %s;", lhs, rhs);
  }
}
