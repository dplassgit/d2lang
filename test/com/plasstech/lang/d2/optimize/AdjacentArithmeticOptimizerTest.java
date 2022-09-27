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

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isFalse();
  }

  @Test
  public void minusPlus() {
    ImmutableList<Op> program =
        ImmutableList.of(
            // should not change (yet)
            new BinOp(TEMP1, TEMP2, TokenType.MINUS, ConstantOperand.ONE, null),
            new BinOp(TEMP3, TEMP1, TokenType.PLUS, ConstantOperand.ONE, null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(optimizer.isChanged()).isFalse();
    assertThat(optimized).hasSize(2);
  }
}
