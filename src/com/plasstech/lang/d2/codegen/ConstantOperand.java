package com.plasstech.lang.d2.codegen;

import com.google.common.base.Joiner;

public class ConstantOperand<T> implements Operand {
  private final T value;

  public ConstantOperand(T value) {
    this.value = value;
  }

  public T value() {
    return value;
  }

  @Override
  public String toString() {
    if (value instanceof String) {
      return String.format("\"%s\"", value.toString());
    } else if (value.getClass().isArray()){
      Object[] valArray = (Object[]) value;
      return String.format("[%s]", Joiner.on(", ").join(valArray));
    } else {
      return value.toString();
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof ConstantOperand)) {
      return false;
    }
    
    ConstantOperand<?> that = (ConstantOperand) obj;
    return this.value().equals(that.value());
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }
}
