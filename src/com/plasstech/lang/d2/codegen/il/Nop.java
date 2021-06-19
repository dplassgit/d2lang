package com.plasstech.lang.d2.codegen.il;

public class Nop extends Op {

  public static final Op INSTANCE = new Nop();
  private final Op original;

  public Nop() {
    this.original = null;
  }

  public Nop(Op original) {
    this.original = original;
  }

  @Override
  public String toString() {
    if (original == null) {
      return "// nop";
    } else {
      return String.format("// nop was: %s", original.toString());
    }
  }
}
