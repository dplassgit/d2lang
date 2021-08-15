package com.plasstech.lang.d2.codegen;

import com.google.common.base.Joiner;
import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VarType;

public class ConstantOperand<T> implements Operand {
  public static final ConstantOperand<Integer> ZERO = new ConstantOperand<Integer>(0);
  public static final ConstantOperand<Integer> ONE = new ConstantOperand<Integer>(1);
  public static final ConstantOperand<Boolean> FALSE = new ConstantOperand<Boolean>(false);
  public static final ConstantOperand<Boolean> TRUE = new ConstantOperand<Boolean>(true);

  private final T value;
  private final VarType type;

  public ConstantOperand(T value) {
    this.value = value;
    if (value instanceof Integer) {
      this.type = VarType.INT;
    } else if (value instanceof String) {
      this.type = VarType.STRING;
    } else if (value instanceof Boolean) {
      this.type = VarType.BOOL;
    } else {
      this.type = VarType.UNKNOWN;
    }
  }

  @Override
  public VarType type() {
    return type;
  }

  public T value() {
    return value;
  }

  @Override
  public boolean isConstant() {
    return true;
  }

  @Override
  public String toString() {
    if (value == null) {
      return "__null";
    } else if (value instanceof String) {
      String valueString = value.toString();
      if (valueString.length() > 40) {
        valueString = valueString.substring(0, 40) + "...";
      }
      return String.format("\"%s\"", valueString);
    } else if (value.getClass().isArray()) {
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

    ConstantOperand<?> that = (ConstantOperand<?>) obj;
    return this.value().equals(that.value());
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  @Override
  public SymbolStorage storage() {
    return SymbolStorage.IMMEDIATE;
  }
}
