package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;
import static com.plasstech.lang.d2.optimize.testing.OpcodeSubject.assertThat;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.TempLocation;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.codegen.testing.LocationUtils;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.optimize.testing.OptimizerWithNop;
import com.plasstech.lang.d2.type.VarType;

public class AscChrOptimizerTest {
  private final Optimizer optimizer = new OptimizerWithNop(new AscChrOptimizer(2));

  private static final TempLocation INT_TEMP1 = LocationUtils.newTempLocation("temp1", VarType.INT);
  private static final TempLocation INT_TEMP2 = LocationUtils.newTempLocation("temp2", VarType.INT);
  private static final TempLocation STRING_TEMP1 =
      LocationUtils.newTempLocation("stemp1", VarType.STRING);
  private static final TempLocation STRING_TEMP2 =
      LocationUtils.newTempLocation("stemp2", VarType.STRING);

  @Test
  public void ascChr() {
    // temp1 = asc(stemp1) // stemp1 is a string
    // stemp2 = chr(temp1)
    // should become:
    // stemp2 = stemp1[0]
    ImmutableList<Op> program = ImmutableList.of(
        new UnaryOp(INT_TEMP1, TokenType.ASC, STRING_TEMP1, null),
        new UnaryOp(STRING_TEMP2, TokenType.CHR, INT_TEMP1, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isBinOp(STRING_TEMP2, STRING_TEMP1, TokenType.LBRACKET,
        ConstantOperand.of(0));
  }

  @Test
  public void ascZero_optimizes() {
    // stemp1 = s[0]
    // temp2 = asc(stemp1)
    // should become:
    // temp2 = asc(s)
    ImmutableList<Op> program = ImmutableList.of(
        new BinOp(STRING_TEMP1, STRING_TEMP2, TokenType.LBRACKET, ConstantOperand.of(0), null),
        new UnaryOp(INT_TEMP1, TokenType.ASC, STRING_TEMP1, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isUnaryOp(INT_TEMP1, TokenType.ASC, STRING_TEMP2);
  }

  @Test
  public void ascZero_not_temp_does_not_optimize() {
    // nottemp = s[0]
    // temp2 = asc(nottemp)
    Location nottemp = LocationUtils.newStackLocation("nottemp", VarType.STRING, 0);
    ImmutableList<Op> program = ImmutableList.of(
        new BinOp(nottemp, STRING_TEMP2, TokenType.LBRACKET, ConstantOperand.of(0), null),
        new UnaryOp(INT_TEMP1, TokenType.ASC, nottemp, null));

    optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void ascZero_dest_not_temp_optimizes() {
    // temp = s[0]
    // nottemp = asc(temp)
    Location nottemp = LocationUtils.newStackLocation("nottemp", VarType.INT, 0);
    ImmutableList<Op> program = ImmutableList.of(
        new BinOp(STRING_TEMP1, STRING_TEMP2, TokenType.LBRACKET, ConstantOperand.of(0), null),
        new UnaryOp(nottemp, TokenType.ASC, STRING_TEMP1, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isUnaryOp(nottemp, TokenType.ASC, STRING_TEMP2);
  }

  @Test
  public void ascWrongChr() {
    ImmutableList<Op> program = ImmutableList.of(
        new UnaryOp(INT_TEMP1, TokenType.ASC, STRING_TEMP1, null),
        new UnaryOp(STRING_TEMP2, TokenType.CHR, INT_TEMP2, null));

    optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void chrAsc() {
    // stemp1 = chr(temp1) // temp1 is an int
    // temp2 = asc(stemp1)
    // should become:
    // temp2 = temp1
    ImmutableList<Op> program = ImmutableList.of(
        new UnaryOp(STRING_TEMP1, TokenType.CHR, INT_TEMP1, null),
        new UnaryOp(INT_TEMP2, TokenType.ASC, STRING_TEMP1, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isTransferredFrom(INT_TEMP1);
  }

  @Test
  public void chrWrongAsc() {
    ImmutableList<Op> program = ImmutableList.of(
        new UnaryOp(STRING_TEMP1, TokenType.CHR, INT_TEMP1, null),
        new UnaryOp(INT_TEMP2, TokenType.ASC, STRING_TEMP2, null));

    optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void chrLength() {
    ImmutableList<Op> program = ImmutableList.of(
        new UnaryOp(STRING_TEMP1, TokenType.CHR, INT_TEMP1, null),
        new UnaryOp(INT_TEMP2, TokenType.LENGTH, STRING_TEMP1, null));

    optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isFalse();
  }
}
