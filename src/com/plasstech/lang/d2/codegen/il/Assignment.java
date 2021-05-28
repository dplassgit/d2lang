package com.plasstech.lang.d2.codegen.il;

public class Assignment<T> extends Op {
  private final String lhs;
  private final T rhs;

  // TODO: only allow reg=reg or reg=constant
  public Assignment(String lhs, T rhs) {
    this.lhs = lhs;
    this.rhs = rhs;
  }

  public String lhs() {
    return lhs;
  }

  public T rhs() {
    return rhs;
  }

  @Override
  public String toString() {
    if (rhs instanceof String) {
      return String.format("\t%s = \"%s\";", lhs, rhs);
    } else {
      return String.format("\t%s = %s;", lhs, rhs);
    }
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }
}
