package com.plasstech.lang.d2.codegen.il;


public class Store extends Op {
  private final String destAddress;
  private final String sourceRegister;

  public Store(String destAddress, String sourceRegister) {
    this.destAddress = destAddress;
    this.sourceRegister = sourceRegister;
  }

  public String destAddress() {
    return destAddress;
  }

  public String sourceRegister() {
    return sourceRegister;
  }

  @Override
  public String toString() {
    return String.format("\tstore(%s, %s)", destAddress, sourceRegister);
  }
}
