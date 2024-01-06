package com.plasstech.lang.d2.codegen;

import org.junit.Test;

import com.google.common.testing.EqualsTester;
import com.plasstech.lang.d2.type.VarType;

public class ConstantOperandTest {
  @Test
  public void equalsTester() {
    new EqualsTester()
        .addEqualityGroup(ConstantOperand.fromValue(-2, VarType.BYTE))
        .addEqualityGroup(ConstantOperand.fromValue(-2, VarType.INT), ConstantOperand.of(-2))
        .addEqualityGroup(ConstantOperand.fromValue(-2, VarType.LONG), ConstantOperand.of(-2L))
        .addEqualityGroup(ConstantOperand.fromValue(-2, VarType.DOUBLE), ConstantOperand.of(-2.0))
        .addEqualityGroup(ConstantOperand.of(-2.2))
        .addEqualityGroup(ConstantOperand.fromValue(2, VarType.BYTE))
        .addEqualityGroup(ConstantOperand.fromValue(2, VarType.INT), ConstantOperand.of(2))
        .addEqualityGroup(ConstantOperand.fromValue(2, VarType.LONG), ConstantOperand.of(2L))
        .addEqualityGroup(ConstantOperand.fromValue(2, VarType.DOUBLE), ConstantOperand.of(2.0))
        .addEqualityGroup(ConstantOperand.of(2.1))
        .addEqualityGroup(ConstantOperand.fromValue(-1, VarType.BYTE))
        .addEqualityGroup(ConstantOperand.fromValue(-1, VarType.INT), ConstantOperand.of(-1))
        .addEqualityGroup(ConstantOperand.fromValue(-1, VarType.LONG), ConstantOperand.of(-1L))
        .addEqualityGroup(ConstantOperand.fromValue(-1, VarType.DOUBLE), ConstantOperand.of(-1.0))
        .addEqualityGroup(ConstantOperand.ONE_BYTE, ConstantOperand.fromValue(1, VarType.BYTE))
        .addEqualityGroup(ConstantOperand.ONE, ConstantOperand.fromValue(1, VarType.INT),
            ConstantOperand.of(1))
        .addEqualityGroup(ConstantOperand.ONE_LONG, ConstantOperand.fromValue(1, VarType.LONG),
            ConstantOperand.of(1L))
        .addEqualityGroup(ConstantOperand.ONE_DBL, ConstantOperand.fromValue(1, VarType.DOUBLE),
            ConstantOperand.of(1.0))
        .addEqualityGroup(ConstantOperand.ZERO_BYTE, ConstantOperand.fromValue(0, VarType.BYTE))
        .addEqualityGroup(ConstantOperand.ZERO, ConstantOperand.fromValue(0, VarType.INT),
            ConstantOperand.of(0))
        .addEqualityGroup(ConstantOperand.ZERO_LONG, ConstantOperand.fromValue(0, VarType.LONG),
            ConstantOperand.of(0L))
        .addEqualityGroup(ConstantOperand.ZERO_DBL, ConstantOperand.fromValue(0, VarType.DOUBLE),
            ConstantOperand.of(0.0))
        .testEquals();
  }
}
