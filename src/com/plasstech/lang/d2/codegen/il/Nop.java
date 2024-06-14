package com.plasstech.lang.d2.codegen.il;

public class Nop extends Op {
  private final String message;

  public Nop() {
    this("");
  }

  public Nop(Op original) {
    this.message = String.format("was: %s", original.toString());
  }

  public Nop(String message) {
    this.message = message;
  }

  @Override
  public String toString() {
    return String.format("// nop %s", message).trim();
  }
}
