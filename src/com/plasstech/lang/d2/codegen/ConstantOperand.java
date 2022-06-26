package com.plasstech.lang.d2.codegen;

import com.google.common.base.Joiner;
import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VarType;

public class ConstantOperand<T> implements Operand {
  public static final ConstantOperand<Integer> ZERO = of(0);
  public static final ConstantOperand<Integer> ONE = of(1);
  public static final ConstantOperand<Boolean> FALSE = of(false);
  public static final ConstantOperand<Boolean> TRUE = of(true);

  public static ConstantOperand<String> of(String value) {
    return new ConstantOperand<String>(value, VarType.STRING);
  }

  public static ConstantOperand<Integer> of(int value) {
    return new ConstantOperand<Integer>(value, VarType.INT);
  }

  public static ConstantOperand<Boolean> of(boolean value) {
    return new ConstantOperand<Boolean>(value, VarType.BOOL);
  }

  private final T value;
  private final VarType type;

  public ConstantOperand(T value, VarType varType) {
    this.value = value;
    this.type = varType;
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
