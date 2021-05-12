package com.plasstech.lang.d2.codegen.il;

public class Goto extends Op {

  private final String label;

  public Goto(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return String.format("\tgoto %s;", label);
  }
}
