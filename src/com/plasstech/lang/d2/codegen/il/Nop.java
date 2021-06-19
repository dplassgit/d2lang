package com.plasstech.lang.d2.codegen.il;

public class Nop extends Op {

  public static final Op INSTANCE = new Nop();

  @Override
  public String toString() {
    return "// nop";
  }
}
