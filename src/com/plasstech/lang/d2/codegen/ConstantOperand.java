package com.plasstech.lang.d2.codegen;

import java.util.Objects;

import com.google.common.base.Joiner;
import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VarType;

public class ConstantOperand<T> implements Operand {
  // These can't use "of" because "of" sometimes uses these.
  public static final ConstantOperand<Integer> ZERO = new ConstantOperand<Integer>(0, VarType.INT);
  public static final ConstantOperand<Integer> ONE = new ConstantOperand<Integer>(1, VarType.INT);

  public static final ConstantOperand<Long> ZERO_LONG = new ConstantOperand<Long>(0L, VarType.LONG);
  public static final ConstantOperand<Long> ONE_LONG = new ConstantOperand<Long>(1L, VarType.LONG);

  public static final ConstantOperand<Byte> ZERO_BYTE =
      new ConstantOperand<Byte>((byte) 0, VarType.BYTE);
  public static final ConstantOperand<Byte> ONE_BYTE =
      new ConstantOperand<Byte>((byte) 1, VarType.BYTE);

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

  public static ConstantOperand<Long> of(long value) {
    if (value == 0L) {
      return ZERO_LONG;
    } else if (value == 1L) {
      return ONE_LONG;
    }
    return new ConstantOperand<Long>(value, VarType.LONG);
  }

  public static ConstantOperand<Byte> of(byte value) {
    if (value == 0) {
      return ZERO_BYTE;
    } else if (value == 1) {
      return ONE_BYTE;
    }
    return new ConstantOperand<Byte>(value, VarType.BYTE);
  }

  public static ConstantOperand<Double> of(double value) {
    if (value == 0.0) {
      return ZERO_DBL;
    } else if (value == 1.0) {
      return ONE_DBL;
    }
    return new ConstantOperand<Double>(value, VarType.DOUBLE);
  }

  public static ConstantOperand<Boolean> of(boolean value) {
    return value ? TRUE : FALSE;
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
    }
    if (type() == VarType.STRING) {
      String valueString = value.toString();
      if (valueString.length() > 40) {
        valueString = valueString.substring(0, 40) + "...";
      }
      return String.format("\"%s\"", valueString);
    }
    if (type().isArray()) {
      Object[] valArray = (Object[]) value;
      return String.format("[%s] [array const]", Joiner.on(", ").join(valArray));
    }
    return String.format("%s [%s const]", value.toString(), type().toString());
  }

  @Override
  public SymbolStorage storage() {
    return SymbolStorage.IMMEDIATE;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof ConstantOperand)) {
      return false;
    }
    ConstantOperand<?> that = (ConstantOperand<?>) obj;

    return this.type().equals(that.type()) && Objects.equals(this.value(), that.value());
  }

  @Override
  public int hashCode() {
    return Objects.hash(type(), value());
  }

  /** Returns true if the operand is an immediate (constant) that is more than 32 bits */
  public static boolean isImm64(Operand operand) {
    if (operand.type() == VarType.LONG && operand.isConstant()) {
      long value = valueFromConstOperand(operand).longValue();
      return value > Integer.MAX_VALUE || value < Integer.MIN_VALUE;
    }
    return false;
  }

  public static boolean isAnyZero(Operand operand) {
    return operand.equals(ZERO) // int
        || operand.equals(ZERO_LONG)
        || operand.equals(ZERO_DBL)
        || operand.equals(ZERO_BYTE);
  }

  public static boolean isAnyIntOne(Operand operand) {
    return operand.equals(ONE) // int
        || operand.equals(ONE_LONG)
        // NOTE NO DBL
        || operand.equals(ONE_BYTE);
  }

  public static Number valueFromConstOperand(Operand operand) {
    if (!(operand instanceof ConstantOperand)) {
      throw new IllegalArgumentException(
          "Cannot get String const from non-ConstantOperand: " + operand);
    }
    ConstantOperand<?> constant = (ConstantOperand<?>) operand;
    if (!(constant.value() instanceof Number)) {
      throw new IllegalArgumentException(
          "Cannot get String const from non-Number ConstantOperand: " + operand);
    }
    return (Number) constant.value();
  }

  public static String stringValueFromConstOperand(Operand operand) {
    if (!(operand instanceof ConstantOperand)) {
      throw new IllegalArgumentException(
          "Cannot get String const from non-ConstOperand: " + operand);
    }
    ConstantOperand<?> constant = (ConstantOperand<?>) operand;
    if (!(constant.value() instanceof String)) {
      throw new IllegalArgumentException(
          "Cannot get String const from non-String ConstOperand: " + operand);
    }
    return constant.value().toString();
  }

  public static ConstantOperand<? extends Number> fromValue(long value, VarType type) {
    if (type == VarType.LONG) {
      return ConstantOperand.of(value);
    }
    if (type == VarType.INT) {
      return ConstantOperand.of((int) value);
    }
    if (type == VarType.BYTE) {
      return ConstantOperand.of((byte) value);
    }
    if (type == VarType.DOUBLE) {
      return ConstantOperand.of((double) value);
    }
    throw new IllegalStateException("Cannot take fromValue of type " + type);
  }
}
