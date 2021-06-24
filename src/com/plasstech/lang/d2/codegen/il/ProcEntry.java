package com.plasstech.lang.d2.codegen.il;

public class ProcEntry extends Op {

  private final String name;

  public ProcEntry(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    // TODO: add formals
    return String.format("\n%s() {", name());
  }

  public String name() {
    return name;
  }
}
