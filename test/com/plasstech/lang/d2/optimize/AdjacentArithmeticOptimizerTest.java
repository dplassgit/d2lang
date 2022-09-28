package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.TempLocation;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.VarType;

public class AdjacentArithmeticOptimizerTest {
  private final Optimizer optimizer = new AdjacentArithmeticOptimizer(2);
  private final Optimizer OPTIMIZERS =
      new ILOptimizer(ImmutableList.of(optimizer, new NopOptimizer())).setDebugLevel(2);

  private static final TempLocation TEMP1 = new TempLocation("temp1", VarType.INT);
  private static final TempLocation TEMP2 = new TempLocation("temp2", VarType.INT);
  private static final TempLocation TEMP3 = new TempLocation("temp3", VarType.INT);
  private static final TempLocation DTEMP1 = new TempLocation("temp1", VarType.DOUBLE);
  private static final TempLocation DTEMP2 = new TempLocation("temp2", VarType.DOUBLE);
  private static final TempLocation DTEMP3 = new TempLocation("temp3", VarType.DOUBLE);

  @Test
  public void plusPlus() {
    ImmutableList<Op> program =
        ImmutableList.of(
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
    ConstantOperand<Integer> right = (ConstantOperand<Integer>) first.right();
    assertThat(right.value()).isEqualTo(2);
  }

  @Test
  public void plusPlusDouble() {
    ImmutableList<Op> program =
        ImmutableList.of(
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
    ConstantOperand<Double> right = (ConstantOperand<Double>) first.right();
    assertThat(right.value()).isEqualTo(2);
  }

  @Test
  public void multMult() {
    ImmutableList<Op> program =
        ImmutableList.of(
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
    ConstantOperand<Integer> right = (ConstantOperand<Integer>) first.right();
    assertThat(right.value()).isEqualTo(6);
  }

  @Test
  public void plusMult() {
    ImmutableList<Op> program =
        ImmutableList.of(
            // should become temp3=temp1+2
            new BinOp(TEMP2, TEMP1, TokenType.PLUS, ConstantOperand.ONE, null),
            new BinOp(TEMP3, TEMP2, TokenType.MULT, ConstantOperand.ONE, null));

    assertThat(OPTIMIZERS.isChanged()).isFalse();
  }

  @Test
  public void plusMinus() {
    ImmutableList<Op> program =
        ImmutableList.of(
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
    ConstantOperand<Integer> right = (ConstantOperand<Integer>) first.right();
    assertThat(right.value()).isEqualTo(0);
  }

  @Test
  public void minusPlus() {
    ImmutableList<Op> program =
        ImmutableList.of(
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
    ConstantOperand<Integer> right = (ConstantOperand<Integer>) first.right();
    assertThat(right.value()).isEqualTo(0);
  }

  @Test
  public void minusPlus2() {
    ImmutableList<Op> program =
        ImmutableList.of(
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
    ConstantOperand<Integer> right = (ConstantOperand<Integer>) first.right();
    assertThat(right.value()).isEqualTo(1);
  }

  @Test
  public void minusMinus() {
    ImmutableList<Op> program =
        ImmutableList.of(
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
    ConstantOperand<Integer> right = (ConstantOperand<Integer>) first.right();
    assertThat(right.value()).isEqualTo(2);
  }
}
