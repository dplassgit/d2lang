package com.plasstech.lang.d2.codegen.il;

public class Label extends Op {
  private final String label;

  public Label(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  @Override
  public String toString() {
    return String.format("\n%s:\n\tt0=t0;", label);
  }
}
