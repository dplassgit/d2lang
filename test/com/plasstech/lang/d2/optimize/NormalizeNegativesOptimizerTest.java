package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.testing.LocationUtils;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.VarType;

public class NormalizeNegativesOptimizerTest {
  private Optimizer optimizer = new NormalizeNegativesOptimizer(2);

  private static final Location V1 = LocationUtils.newMemoryAddress("a", VarType.INT);
  private static final Location V2 = LocationUtils.newParamLocation("b", VarType.INT, 0, 0);
  private static final Location VL1 = LocationUtils.newMemoryAddress("a", VarType.LONG);
  private static final Location VL2 = LocationUtils.newParamLocation("b", VarType.LONG, 0, 0);
  private static final Location S1 = LocationUtils.newMemoryAddress("a", VarType.STRING);
  private static final Location S2 = LocationUtils.newParamLocation("b", VarType.STRING, 0, 0);

  @Test
  public void ignoresPositives() {
    ImmutableList<Op> input =
        ImmutableList.of(new BinOp(V1, V2, TokenType.PLUS, ConstantOperand.of(2), null));
    optimizer.optimize(input, null);
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void ignoresMultiply() {
    ImmutableList<Op> input =
        ImmutableList.of(new BinOp(V1, V2, TokenType.MULT, ConstantOperand.of(-2), null));
    optimizer.optimize(input, null);
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void ignoresStrings() {
    ImmutableList<Op> input =
        ImmutableList.of(new BinOp(S1, S2, TokenType.PLUS, ConstantOperand.of("hi"), null));
    optimizer.optimize(input, null);
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void detectsPlus() {
    ImmutableList<Op> input =
        ImmutableList.of(new BinOp(V1, V2, TokenType.PLUS, ConstantOperand.of(-2), null));
    ImmutableList<Op> optimized = optimizer.optimize(input, null);
    assertThat(optimizer.isChanged()).isTrue();

    assertThat(optimized).hasSize(1);
    BinOp first = (BinOp) optimized.get(0);
    assertThat(first.operator()).isEqualTo(TokenType.MINUS);
    assertThat(first.right()).isEqualTo(ConstantOperand.of(2));
  }

  @Test
  public void detectsMinus() {
    ImmutableList<Op> input =
        ImmutableList.of(new BinOp(V1, V2, TokenType.MINUS, ConstantOperand.of(-2), null));
    ImmutableList<Op> optimized = optimizer.optimize(input, null);
    assertThat(optimizer.isChanged()).isTrue();

    assertThat(optimized).hasSize(1);
    BinOp first = (BinOp) optimized.get(0);
    assertThat(first.operator()).isEqualTo(TokenType.PLUS);
    assertThat(first.right()).isEqualTo(ConstantOperand.of(2));
  }

  @Test
  public void detectsPlus_long() {
    ImmutableList<Op> input =
        ImmutableList.of(new BinOp(VL1, VL2, TokenType.MINUS, ConstantOperand.of(-2L), null));
    ImmutableList<Op> optimized = optimizer.optimize(input, null);
    assertThat(optimizer.isChanged()).isTrue();

    assertThat(optimized).hasSize(1);
    BinOp first = (BinOp) optimized.get(0);
    assertThat(first.operator()).isEqualTo(TokenType.PLUS);
    assertThat(first.right()).isEqualTo(ConstantOperand.of(2L));
  }
}
