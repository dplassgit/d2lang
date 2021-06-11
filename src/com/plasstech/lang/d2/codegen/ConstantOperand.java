package com.plasstech.lang.d2.codegen;

public class ConstantOperand implements Operand {
  private final Object value;

  public ConstantOperand(Object value) {
    this.value = value;
  }

  public Object value() {
    return value;
  }

  @Override
  public String toString() {
    return "/* (ConstOperand) */ " + value.toString();
  }
}