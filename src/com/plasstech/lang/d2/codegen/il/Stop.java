package com.plasstech.lang.d2.codegen.il;

public class Stop extends Op {

  private final int exitCode;

  public Stop() {
    this(0);
  }

  public Stop(int exitCode) {
    this.exitCode = exitCode;
  }

  public int exitCode() {
    return exitCode;
  }

  @Override
  public String toString() {
    return String.format("exit(%d); // a.k.a. Stop", exitCode);
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }

}
