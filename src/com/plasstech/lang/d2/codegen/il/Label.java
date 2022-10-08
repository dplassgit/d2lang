package com.plasstech.lang.d2.codegen.il;

import java.util.Objects;

public class Label extends Op {
  public static final String LOOP_BEGIN_PREFIX = "loop_begin";
  public static final String LOOP_END_PREFIX = "loop_end";
  public static final String LOOP_INCREMENT_PREFIX = "loop_increment";

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
  public boolean equals(Object that) {
    if (that == null || !(that instanceof Label)) {
      return false;
    }
    return this.hashCode() == that.hashCode();
  }

  @Override
  public int hashCode() {
    return Objects.hash(getClass().getName(), label());
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }
}
