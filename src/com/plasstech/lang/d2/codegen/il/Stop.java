package com.plasstech.lang.d2.codegen.il;

public class Stop extends Op {
  @Override
  public String toString() {
    return "\texit(0);";
  }
}
