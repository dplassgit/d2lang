package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Dec;
import com.plasstech.lang.d2.codegen.il.Inc;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.testing.LocationUtils;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.VarType;

public class AdjacentIncDecOptimizerTest {
  private final Optimizer OPTIMIZERS =
      new ILOptimizer(
          ImmutableList.of(new AdjacentIncDecOptimizer(2), new NopOptimizer()))
          .setDebugLevel(2);

  private static final Location VAR1 = LocationUtils.newMemoryAddress("a", VarType.INT);
  private static final Location VAR2 = LocationUtils.newParamLocation("b", VarType.INT, 0, 0);
  private static final Location LVAR1 = LocationUtils.newMemoryAddress("a", VarType.LONG);

  @Test
  public void twoIncs_differentVarsUnchanged() {
    ImmutableList<Op> program = ImmutableList.of(
        new Inc(VAR1, null),
        new Inc(VAR2, null));

    OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isFalse();
  }

  @Test
  public void oneInc_unchanged() {
    ImmutableList<Op> program = ImmutableList.of(
        new Inc(VAR1, null));

    OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isFalse();
  }

  @Test
  public void twoIncs() {
    ImmutableList<Op> program = ImmutableList.of(
        new Inc(VAR1, null),
        new Inc(VAR1, null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    BinOp first = (BinOp) optimized.get(0);
    assertThat(first.destination()).isEqualTo(VAR1);
    assertThat(first.left()).isEqualTo(VAR1);
    assertThat(first.operator()).isEqualTo(TokenType.PLUS);
    assertThat(first.right()).isEqualTo(ConstantOperand.of(2));
  }

  @Test
  public void incAdd() {
    ImmutableList<Op> program = ImmutableList.of(
        new Inc(VAR1, null),
        new BinOp(VAR1, VAR1, TokenType.PLUS, ConstantOperand.of(2), null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    BinOp first = (BinOp) optimized.get(0);
    assertThat(first.destination()).isEqualTo(VAR1);
    assertThat(first.left()).isEqualTo(VAR1);
    assertThat(first.operator()).isEqualTo(TokenType.PLUS);
    assertThat(first.right()).isEqualTo(ConstantOperand.of(3));
  }

  @Test
  public void addInc() {
    ImmutableList<Op> program = ImmutableList.of(
        new BinOp(VAR1, VAR1, TokenType.PLUS, ConstantOperand.of(2), null),
        new Inc(VAR1, null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    BinOp first = (BinOp) optimized.get(0);
    assertThat(first.destination()).isEqualTo(VAR1);
    assertThat(first.left()).isEqualTo(VAR1);
    assertThat(first.operator()).isEqualTo(TokenType.PLUS);
    assertThat(first.right()).isEqualTo(ConstantOperand.of(3));
  }

  @Test
  public void addAdd() {
    ImmutableList<Op> program = ImmutableList.of(
        new BinOp(VAR1, VAR1, TokenType.PLUS, ConstantOperand.of(2), null),
        new BinOp(VAR1, VAR1, TokenType.PLUS, ConstantOperand.of(3), null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    BinOp first = (BinOp) optimized.get(0);
    assertThat(first.destination()).isEqualTo(VAR1);
    assertThat(first.left()).isEqualTo(VAR1);
    assertThat(first.operator()).isEqualTo(TokenType.PLUS);
    assertThat(first.right()).isEqualTo(ConstantOperand.of(5));
  }

  @Test
  public void addDec() {
    ImmutableList<Op> program = ImmutableList.of(
        new BinOp(VAR1, VAR1, TokenType.PLUS, ConstantOperand.of(2), null),
        new Dec(VAR1, null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    BinOp first = (BinOp) optimized.get(0);
    assertThat(first.destination()).isEqualTo(VAR1);
    assertThat(first.left()).isEqualTo(VAR1);
    assertThat(first.operator()).isEqualTo(TokenType.PLUS);
    assertThat(first.right()).isEqualTo(ConstantOperand.of(1));
  }

  @Test
  public void addSub() {
    ImmutableList<Op> program = ImmutableList.of(
        new BinOp(VAR1, VAR1, TokenType.PLUS, ConstantOperand.of(2), null),
        new BinOp(VAR1, VAR1, TokenType.MINUS, ConstantOperand.of(4), null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    BinOp first = (BinOp) optimized.get(0);
    assertThat(first.destination()).isEqualTo(VAR1);
    assertThat(first.left()).isEqualTo(VAR1);
    assertThat(first.operator()).isEqualTo(TokenType.PLUS);
    assertThat(first.right()).isEqualTo(ConstantOperand.of(-2));
  }

  @Test
  public void incSub() {
    ImmutableList<Op> program = ImmutableList.of(
        new Inc(VAR1, null),
        new BinOp(VAR1, VAR1, TokenType.MINUS, ConstantOperand.of(2), null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    BinOp first = (BinOp) optimized.get(0);
    assertThat(first.destination()).isEqualTo(VAR1);
    assertThat(first.left()).isEqualTo(VAR1);
    assertThat(first.operator()).isEqualTo(TokenType.PLUS);
    assertThat(first.right()).isEqualTo(ConstantOperand.of(-1));
  }

  @Test
  public void addAddLongs() {
    ImmutableList<Op> program = ImmutableList.of(
        new BinOp(LVAR1, LVAR1, TokenType.PLUS, ConstantOperand.of(2L), null),
        new BinOp(LVAR1, LVAR1, TokenType.PLUS, ConstantOperand.of(3L), null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    BinOp first = (BinOp) optimized.get(0);
    assertThat(first.destination()).isEqualTo(LVAR1);
    assertThat(first.left()).isEqualTo(LVAR1);
    assertThat(first.operator()).isEqualTo(TokenType.PLUS);
    assertThat(first.right()).isEqualTo(ConstantOperand.of(5L));
  }

  @Test
  public void twoDecs() {
    ImmutableList<Op> program = ImmutableList.of(
        new Dec(VAR1, null),
        new Dec(VAR1, null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    BinOp first = (BinOp) optimized.get(0);
    assertThat(first.destination()).isEqualTo(VAR1);
    assertThat(first.left()).isEqualTo(VAR1);
    assertThat(first.operator()).isEqualTo(TokenType.PLUS);
    assertThat(first.right()).isEqualTo(ConstantOperand.of(-2));
  }

  @Test
  public void decMinus() {
    ImmutableList<Op> program = ImmutableList.of(
        new Dec(VAR1, null),
        new BinOp(VAR1, VAR1, TokenType.MINUS, ConstantOperand.of(3L), null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    BinOp first = (BinOp) optimized.get(0);
    assertThat(first.destination()).isEqualTo(VAR1);
    assertThat(first.left()).isEqualTo(VAR1);
    assertThat(first.operator()).isEqualTo(TokenType.PLUS);
    assertThat(first.right()).isEqualTo(ConstantOperand.of(-4));
  }

  @Test
  public void decPlus() {
    ImmutableList<Op> program = ImmutableList.of(
        new Dec(VAR1, null),
        new BinOp(VAR1, VAR1, TokenType.PLUS, ConstantOperand.of(3), null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    BinOp first = (BinOp) optimized.get(0);
    assertThat(first.destination()).isEqualTo(VAR1);
    assertThat(first.left()).isEqualTo(VAR1);
    assertThat(first.operator()).isEqualTo(TokenType.PLUS);
    assertThat(first.right()).isEqualTo(ConstantOperand.of(2));
  }

  @Test
  public void incMinus() {
    ImmutableList<Op> program = ImmutableList.of(
        new Inc(VAR1, null),
        new BinOp(VAR1, VAR1, TokenType.MINUS, ConstantOperand.ONE, null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    BinOp first = (BinOp) optimized.get(0);
    assertThat(first.destination()).isEqualTo(VAR1);
    assertThat(first.left()).isEqualTo(VAR1);
    assertThat(first.operator()).isEqualTo(TokenType.PLUS);
    assertThat(first.right()).isEqualTo(ConstantOperand.ZERO);
  }
}
