package com.plasstech.lang.d2.codegen;

import com.plasstech.lang.d2.codegen.il.Op;

public class Goto extends Op {

  private final String label;

  public Goto(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  @Override
  public String toString() {
    return String.format("\tgoto %s;", label);
  }
}
