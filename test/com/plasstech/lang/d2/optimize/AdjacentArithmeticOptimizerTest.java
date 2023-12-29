package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.TempLocation;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Dec;
import com.plasstech.lang.d2.codegen.il.Inc;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.testing.LocationUtils;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.VarType;

public class AdjacentArithmeticOptimizerTest {
  private final Optimizer OPTIMIZERS =
      new ILOptimizer(
          ImmutableList.of(new AdjacentArithmeticOptimizer(2), new NopOptimizer()))
          .setDebugLevel(2);

  private static final TempLocation TEMP1 = LocationUtils.newTempLocation("temp1", VarType.INT);
  private static final TempLocation TEMP2 = LocationUtils.newTempLocation("temp2", VarType.INT);
  private static final TempLocation TEMP3 = LocationUtils.newTempLocation("temp3", VarType.INT);
  private static final TempLocation LTEMP1 = LocationUtils.newTempLocation("temp1", VarType.LONG);
  private static final TempLocation LTEMP2 = LocationUtils.newTempLocation("temp2", VarType.LONG);
  private static final TempLocation LTEMP3 = LocationUtils.newTempLocation("temp3", VarType.LONG);
  private static final TempLocation DTEMP1 = LocationUtils.newTempLocation("temp1", VarType.DOUBLE);
  private static final TempLocation DTEMP2 = LocationUtils.newTempLocation("temp2", VarType.DOUBLE);
  private static final TempLocation DTEMP3 = LocationUtils.newTempLocation("temp3", VarType.DOUBLE);
  private static final TempLocation BTEMP1 = LocationUtils.newTempLocation("temp1", VarType.BYTE);
  private static final TempLocation BTEMP2 = LocationUtils.newTempLocation("temp2", VarType.BYTE);
  private static final TempLocation BTEMP3 = LocationUtils.newTempLocation("temp3", VarType.BYTE);
  private static final Location VAR1 = LocationUtils.newMemoryAddress("a", VarType.INT);
  private static final Location VAR2 = LocationUtils.newMemoryAddress("b", VarType.INT);

  @Test
  public void plusPlus() {
    ImmutableList<Op> program = ImmutableList.of(
        // should become temp3=temp1+2
        new BinOp(TEMP2, TEMP1, TokenType.PLUS, ConstantOperand.ONE, null),
        new BinOp(TEMP3, TEMP2, TokenType.PLUS, ConstantOperand.ONE, null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    BinOp first = (BinOp) optimized.get(0);
    assertThat(first.destination()).isEqualTo(TEMP3);
    assertThat(first.left()).isEqualTo(TEMP1);
    assertThat(first.operator()).isEqualTo(TokenType.PLUS);
    assertThat(first.right()).isEqualTo(ConstantOperand.of(2));
  }

  @Test
  public void plusInc() {
    ImmutableList<Op> program = ImmutableList.of(
        // should become temp2=temp2+2, which yes, isn't possible in the 'real world'
        new BinOp(TEMP2, TEMP2, TokenType.PLUS, ConstantOperand.ONE, null),
        new Inc(TEMP2, null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    BinOp first = (BinOp) optimized.get(0);
    assertThat(first.destination()).isEqualTo(TEMP2);
    assertThat(first.left()).isEqualTo(TEMP2);
    assertThat(first.operator()).isEqualTo(TokenType.PLUS);
    assertThat(first.right()).isEqualTo(ConstantOperand.of(2));
  }

  @Test
  public void plusDec() {
    ImmutableList<Op> program = ImmutableList.of(
        // should become temp2=temp2+0, which yes, isn't possible in the 'real world'
        new BinOp(TEMP2, TEMP2, TokenType.PLUS, ConstantOperand.ONE, null),
        new Dec(TEMP2, null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    BinOp first = (BinOp) optimized.get(0);
    assertThat(first.destination()).isEqualTo(TEMP2);
    assertThat(first.left()).isEqualTo(TEMP2);
    assertThat(first.operator()).isEqualTo(TokenType.PLUS);
    assertThat(first.right()).isEqualTo(ConstantOperand.ZERO);
  }

  @Test
  public void incPlusDifferent_noChange() {
    ImmutableList<Op> program = ImmutableList.of(new Inc(VAR1, null),
        new BinOp(TEMP2, VAR1, TokenType.PLUS, ConstantOperand.ONE, null));

    OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isFalse();
  }

  @Test
  public void plusPlusDifferent_noChange() {
    ImmutableList<Op> program = ImmutableList.of(
        // var=var+1
        // temp2=var+1
        // should have no change.
        new BinOp(VAR1, VAR1, TokenType.PLUS, ConstantOperand.ONE, null),
        new BinOp(TEMP2, VAR1, TokenType.PLUS, ConstantOperand.ONE, null));

    OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isFalse();
  }

  @Test
  public void plusPlusSame() {
    ImmutableList<Op> program = ImmutableList.of(
        // var=var+1
        // var=var+1
        // should become var = var + 2
        new BinOp(VAR1, VAR1, TokenType.PLUS, ConstantOperand.ONE, null),
        new BinOp(VAR1, VAR1, TokenType.PLUS, ConstantOperand.ONE, null));

    OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isFalse();
  }

  @Test
  public void decIncDifferent() {
    ImmutableList<Op> program = ImmutableList.of(new Dec(VAR1, null), new Inc(VAR2, null));

    OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isFalse();
  }

  @Test
  public void incDecDifferent() {
    ImmutableList<Op> program = ImmutableList.of(new Inc(VAR1, null), new Dec(VAR2, null));

    OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isFalse();
  }

  @Test
  public void plusPlusDouble() {
    ImmutableList<Op> program = ImmutableList.of(
        // should become temp3=temp1+2
        new BinOp(DTEMP2, DTEMP1, TokenType.PLUS, ConstantOperand.ONE_DBL, null),
        new BinOp(DTEMP3, DTEMP2, TokenType.PLUS, ConstantOperand.ONE_DBL, null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    BinOp first = (BinOp) optimized.get(0);
    assertThat(first.destination()).isEqualTo(DTEMP3);
    assertThat(first.left()).isEqualTo(DTEMP1);
    assertThat(first.operator()).isEqualTo(TokenType.PLUS);
    assertThat(first.right()).isEqualTo(ConstantOperand.of(2.0));
  }

  @Test
  public void orOr() {
    ImmutableList<Op> program = ImmutableList.of(
        // should become temp3=temp1|3
        new BinOp(TEMP2, TEMP1, TokenType.BIT_OR, ConstantOperand.ONE, null),
        new BinOp(TEMP3, TEMP2, TokenType.BIT_OR, ConstantOperand.of(3), null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    BinOp first = (BinOp) optimized.get(0);
    assertThat(first.destination()).isEqualTo(TEMP3);
    assertThat(first.left()).isEqualTo(TEMP1);
    assertThat(first.operator()).isEqualTo(TokenType.BIT_OR);
    assertThat(first.right()).isEqualTo(ConstantOperand.of(3));
  }

  @Test
  public void andAndByte() {
    ImmutableList<Op> program = ImmutableList.of(
        // should become temp3=temp1&1 because 1&3=1. is that right?
        new BinOp(BTEMP2, BTEMP1, TokenType.BIT_AND, ConstantOperand.ONE_BYTE, null),
        new BinOp(BTEMP3, BTEMP2, TokenType.BIT_AND, ConstantOperand.of((byte) 3), null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    BinOp first = (BinOp) optimized.get(0);
    assertThat(first.destination()).isEqualTo(BTEMP3);
    assertThat(first.left()).isEqualTo(BTEMP1);
    assertThat(first.operator()).isEqualTo(TokenType.BIT_AND);
    assertThat(first.right()).isEqualTo(ConstantOperand.ONE_BYTE);
  }

  @Test
  public void xorXorLong() {
    ImmutableList<Op> program = ImmutableList.of(
        // should become temp3=temp1&1 because 1&3=1. is that right?
        new BinOp(LTEMP2, LTEMP1, TokenType.BIT_XOR, ConstantOperand.ONE_LONG, null),
        new BinOp(LTEMP3, LTEMP2, TokenType.BIT_XOR, ConstantOperand.of(4L), null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    BinOp first = (BinOp) optimized.get(0);
    assertThat(first.destination()).isEqualTo(LTEMP3);
    assertThat(first.left()).isEqualTo(LTEMP1);
    assertThat(first.operator()).isEqualTo(TokenType.BIT_XOR);
    assertThat(first.right()).isEqualTo(ConstantOperand.of(5L));
  }

  @Test
  public void multMultLong() {
    ImmutableList<Op> program = ImmutableList.of(
        new BinOp(LTEMP2, LTEMP1, TokenType.MULT, ConstantOperand.of(123123L), null),
        new BinOp(LTEMP3, LTEMP2, TokenType.MULT, ConstantOperand.of(234234L), null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    BinOp first = (BinOp) optimized.get(0);
    assertThat(first.destination()).isEqualTo(LTEMP3);
    assertThat(first.left()).isEqualTo(LTEMP1);
    assertThat(first.operator()).isEqualTo(TokenType.MULT);

    ConstantOperand<Long> right = (ConstantOperand<Long>) first.right();
    assertThat(right.value()).isGreaterThan(Integer.MAX_VALUE);
    assertThat(right.value()).isEqualTo(123123L * 234234L);
  }

  @Test
  public void multMult() {
    ImmutableList<Op> program = ImmutableList.of(
        // should become temp3=temp1*6
        new BinOp(TEMP2, TEMP1, TokenType.MULT, ConstantOperand.of(2), null),
        new BinOp(TEMP3, TEMP2, TokenType.MULT, ConstantOperand.of(3), null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    BinOp first = (BinOp) optimized.get(0);
    assertThat(first.destination()).isEqualTo(TEMP3);
    assertThat(first.left()).isEqualTo(TEMP1);
    assertThat(first.operator()).isEqualTo(TokenType.MULT);
    assertThat(first.right()).isEqualTo(ConstantOperand.of(6));
  }

  @Test
  public void plusMult() {
    ImmutableList<Op> program = ImmutableList.of(
        // should become temp3=temp1+2
        new BinOp(TEMP2, TEMP1, TokenType.PLUS, ConstantOperand.ONE, null),
        new BinOp(TEMP3, TEMP2, TokenType.MULT, ConstantOperand.ONE, null));

    OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isFalse();
  }

  @Test
  public void plusMinus() {
    ImmutableList<Op> program = ImmutableList.of(
        // should become temp3=temp1+0
        new BinOp(TEMP2, TEMP1, TokenType.PLUS, ConstantOperand.ONE, null),
        new BinOp(TEMP3, TEMP2, TokenType.MINUS, ConstantOperand.ONE, null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    BinOp first = (BinOp) optimized.get(0);
    assertThat(first.destination()).isEqualTo(TEMP3);
    assertThat(first.left()).isEqualTo(TEMP1);
    // it can be temp1+0 or temp1-0, so we don't care.
    assertThat(first.right()).isEqualTo(ConstantOperand.ZERO);
  }

  @Test
  public void minusPlus() {
    ImmutableList<Op> program = ImmutableList.of(
        // should become temp3=temp1+0
        new BinOp(TEMP2, TEMP1, TokenType.MINUS, ConstantOperand.ONE, null),
        new BinOp(TEMP3, TEMP2, TokenType.PLUS, ConstantOperand.ONE, null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    BinOp first = (BinOp) optimized.get(0);
    assertThat(first.destination()).isEqualTo(TEMP3);
    assertThat(first.left()).isEqualTo(TEMP1);
    // it can be temp1+0 or temp1-0, so we don't care.
    assertThat(first.right()).isEqualTo(ConstantOperand.ZERO);
  }

  @Test
  public void minusPlus2() {
    ImmutableList<Op> program = ImmutableList.of(
        // should become temp3=temp1=2+1 = -1
        new BinOp(TEMP2, TEMP1, TokenType.MINUS, ConstantOperand.of(2), null),
        new BinOp(TEMP3, TEMP2, TokenType.PLUS, ConstantOperand.ONE, null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    BinOp first = (BinOp) optimized.get(0);
    assertThat(first.destination()).isEqualTo(TEMP3);
    assertThat(first.left()).isEqualTo(TEMP1);
    assertThat(first.operator()).isEqualTo(TokenType.MINUS);
    assertThat(first.right()).isEqualTo(ConstantOperand.of(1));
  }

  @Test
  public void minusMinus() {
    ImmutableList<Op> program = ImmutableList.of(
        // should be temp3 = temp1 - 2
        new BinOp(TEMP2, TEMP1, TokenType.MINUS, ConstantOperand.ONE, null),
        new BinOp(TEMP3, TEMP2, TokenType.MINUS, ConstantOperand.ONE, null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    BinOp first = (BinOp) optimized.get(0);
    assertThat(first.destination()).isEqualTo(TEMP3);
    assertThat(first.left()).isEqualTo(TEMP1);
    assertThat(first.operator()).isEqualTo(TokenType.MINUS);
    assertThat(first.right()).isEqualTo(ConstantOperand.of(2));
  }

  @Test
  public void divDiv() {
    ImmutableList<Op> program = ImmutableList.of(
        // should be temp3 = temp1 / 10
        new BinOp(TEMP2, TEMP1, TokenType.DIV, ConstantOperand.of(5), null),
        new BinOp(TEMP3, TEMP2, TokenType.DIV, ConstantOperand.of(2), null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    BinOp first = (BinOp) optimized.get(0);
    assertThat(first.destination()).isEqualTo(TEMP3);
    assertThat(first.left()).isEqualTo(TEMP1);
    assertThat(first.operator()).isEqualTo(TokenType.DIV);
    assertThat(first.right()).isEqualTo(ConstantOperand.of(10));
  }

  @Test
  public void divMult() {
    ImmutableList<Op> program = ImmutableList.of(
        // should be temp3 = temp1 / (20/10) = temp1 / 2
        new BinOp(TEMP2, TEMP1, TokenType.DIV, ConstantOperand.of(20), null),
        new BinOp(TEMP3, TEMP2, TokenType.MULT, ConstantOperand.of(10), null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    BinOp first = (BinOp) optimized.get(0);
    assertThat(first.destination()).isEqualTo(TEMP3);
    assertThat(first.left()).isEqualTo(TEMP1);
    assertThat(first.operator()).isEqualTo(TokenType.DIV);
    assertThat(first.right()).isEqualTo(ConstantOperand.of(2));
  }

  @Test
  public void divMultTooSmall() {
    ImmutableList<Op> program = ImmutableList.of(
        // should be temp3 = temp1 / (2/10) = temp1 / 0 behnt.
        new BinOp(TEMP2, TEMP1, TokenType.DIV, ConstantOperand.of(2), null),
        new BinOp(TEMP3, TEMP2, TokenType.MULT, ConstantOperand.of(10), null));

    OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isFalse();
  }

  @Test
  public void multDiv() {
    ImmutableList<Op> program = ImmutableList.of(
        // should be temp3 = temp1 * (20/5) = temp1 * 4
        new BinOp(TEMP2, TEMP1, TokenType.MULT, ConstantOperand.of(20), null),
        new BinOp(TEMP3, TEMP2, TokenType.DIV, ConstantOperand.of(5), null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    BinOp first = (BinOp) optimized.get(0);
    assertThat(first.destination()).isEqualTo(TEMP3);
    assertThat(first.left()).isEqualTo(TEMP1);
    assertThat(first.operator()).isEqualTo(TokenType.MULT);
    assertThat(first.right()).isEqualTo(ConstantOperand.of(4));
  }
}
