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
    return String.format("%s:", label);
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }

}
