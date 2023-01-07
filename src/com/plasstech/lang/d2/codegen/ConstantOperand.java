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
  public SymbolStorage storage() {
    return SymbolStorage.IMMEDIATE;
  }

  @Override
  public boolean equals(Object that) {
    if (that == null || !(that instanceof ConstantOperand)) {
      return false;
    }

    return this.hashCode() == that.hashCode();
  }

  @Override
  public int hashCode() {
    return Objects.hash(type(), value());
  }

  /** Returns true if the operand is an immediate (constant) that is more than 32 bits */
  public static boolean isImm64(Operand operand) {
    if (operand.type() == VarType.LONG && operand.isConstant()) {
      ConstantOperand<Long> longOperand = (ConstantOperand<Long>) operand;
      long value = longOperand.value();
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
}
