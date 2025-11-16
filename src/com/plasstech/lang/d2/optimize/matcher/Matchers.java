package com.plasstech.lang.d2.optimize.matcher;

import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.type.VarType;

/** Grouping of prebuilt Operand matchers. */
public class Matchers {
  public static Matcher any() {
    return op -> true;
  }

  public static Matcher isEqualTo(Operand other) {
    return op -> other.equals(op);
  }

  public static Matcher hasType(VarType type) {
    return op -> op.type().equals(type);
  }

  public static Matcher isIntegral() {
    return op -> op.type().isIntegral();
  }

  public static Matcher isNumeric() {
    return op -> op.type().isNumeric();
  }

  public static Matcher isConstant() {
    return op -> op.isConstant();
  }

  public static Matcher isArray() {
    return op -> op.type().isArray();
  }

  public static Matcher isNull() {
    return op -> op.isNull();
  }

  public static Matcher isIntegralConstant() {
    return and(isConstant(), isIntegral());
  }

  public static Matcher isDoubleConstant() {
    return and(isConstant(), hasType(VarType.DOUBLE));
  }

  public static final Matcher isAnyZero() {
    return operand -> ConstantOperand.isAnyZero(operand);
  }

  public static final Matcher isAnyOne() {
    return operand -> ConstantOperand.isAnyOne(operand);
  }

  public static final Matcher isAnyNegativeOne() {
    return or(
        isEqualTo(ConstantOperand.of((byte) (-1))),
        isEqualTo(ConstantOperand.of(-1L)),
        isEqualTo(ConstantOperand.of(-1)),
        isEqualTo(ConstantOperand.of(-1.0)));
  }

  public static Matcher not(Matcher m) {
    return op -> !m.matches(op);
  }

  public static Matcher and(Matcher... matchers) {
    // Surely there is a way to do this via a stream
    return op -> {
      for (Matcher m : matchers) {
        if (!m.matches(op)) {
          return false;
        }
      }
      return true;
    };
  }

  public static Matcher or(Matcher... matchers) {
    // Surely there is a way to do this via a stream
    return op -> {
      for (Matcher m : matchers) {
        if (m.matches(op)) {
          return true;
        }
      }
      return false;
    };
  }

  public static Matcher isPowerOf2() {
    return and(
        isIntegralConstant(),
        op -> {
          int value = ConstantOperand.valueFromConstOperand(op).intValue();
          return (value >= 2) && ((value & (value - 1)) == 0);
        });
  }

  public static Matcher isNegativeConstant() {
    return and(
        isConstant(),
        isNumeric(),
        op -> ConstantOperand.valueFromConstOperand(op).doubleValue() < 0);
  }
}
