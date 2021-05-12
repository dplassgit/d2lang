package com.plasstech.lang.d2.codegen.il;

public class Load extends Op {
  private final String destRegister;
  private final String sourceAddress;

  public Load(String destRegister, String source) {
    this.destRegister = destRegister;
    this.sourceAddress = source;
  }

  public String destRegister() {
    return destRegister;
  }

  public String sourceAddress() {
    return sourceAddress;
  }

  @Override
  public String toString() {
    return String.format("\t%s = %s; // load", destRegister, sourceAddress);
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }
}
