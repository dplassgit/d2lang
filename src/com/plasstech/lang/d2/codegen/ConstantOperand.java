package com.plasstech.lang.d2.codegen;

import com.google.common.base.Joiner;
import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VarType;

public class ConstantOperand<T> implements Operand {
  public static final ConstantOperand<Integer> ZERO = new ConstantOperand<Integer>(0, VarType.INT);
  public static final ConstantOperand<Integer> ONE = new ConstantOperand<Integer>(1, VarType.INT);
  public static final ConstantOperand<Double> ZERO_DBL =
      new ConstantOperand<Double>(0.0, VarType.DOUBLE);
  public static final ConstantOperand<Double> ONE_DBL =
      new ConstantOperand<Double>(1.0, VarType.DOUBLE);
  public static final ConstantOperand<Boolean> FALSE =
      new ConstantOperand<Boolean>(false, VarType.BOOL);
  public static final ConstantOperand<Boolean> TRUE =
      new ConstantOperand<Boolean>(true, VarType.BOOL);
  public static final ConstantOperand<String> EMPTY_STRING =
      new ConstantOperand<String>("", VarType.STRING);

  public static ConstantOperand<String> of(String value) {
    if (value.isEmpty()) {
      return EMPTY_STRING;
    }
    return new ConstantOperand<String>(value, VarType.STRING);
  }

  public static ConstantOperand<Integer> of(int value) {
    if (value == 0) {
      return ZERO;
    } else if (value == 1) {
      return ONE;
    }
    return new ConstantOperand<Integer>(value, VarType.INT);
  }

  public static Operand of(double value) {
    if (value == 0.0) {
      return ZERO_DBL;
    } else if (value == 1.0) {
      return ONE_DBL;
    }
    return new ConstantOperand<Double>(value, VarType.DOUBLE);
  }

  public static ConstantOperand<Boolean> of(boolean value) {
    if (value) {
      return TRUE;
    } else {
      return FALSE;
    }
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
    } else if (type() == VarType.STRING) {
      String valueString = value.toString();
      if (valueString.length() > 40) {
        valueString = valueString.substring(0, 40) + "...";
      }
      return String.format("\"%s\"", valueString);
    } else if (type().isArray()) {
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
