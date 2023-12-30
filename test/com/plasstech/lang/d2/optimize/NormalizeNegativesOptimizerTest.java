package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;
import static com.plasstech.lang.d2.optimize.OpcodeSubject.assertThat;

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

  private static final Location INTVAR1 = LocationUtils.newMemoryAddress("a", VarType.INT);
  private static final Location INTVAR2 = LocationUtils.newParamLocation("b", VarType.INT, 0, 0);
  private static final Location LONGVAR1 = LocationUtils.newMemoryAddress("a", VarType.LONG);
  private static final Location LONGVAR2 = LocationUtils.newParamLocation("b", VarType.LONG, 0, 0);
  private static final Location DBLVAR1 = LocationUtils.newMemoryAddress("a", VarType.DOUBLE);
  private static final Location DBLVAR2 = LocationUtils.newParamLocation("b", VarType.DOUBLE, 0, 0);
  private static final Location STRVAR1 = LocationUtils.newMemoryAddress("a", VarType.STRING);
  private static final Location STRVAR2 = LocationUtils.newParamLocation("b", VarType.STRING, 0, 0);

  @Test
  public void ignoresPositives() {
    ImmutableList<Op> input =
        ImmutableList.of(new BinOp(INTVAR1, INTVAR2, TokenType.PLUS, ConstantOperand.of(2), null));
    optimizer.optimize(input, null);
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void ignoresMultiply() {
    ImmutableList<Op> input =
        ImmutableList.of(new BinOp(INTVAR1, INTVAR2, TokenType.MULT, ConstantOperand.of(-2), null));
    optimizer.optimize(input, null);
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void ignoresStrings() {
    ImmutableList<Op> input =
        ImmutableList.of(new BinOp(STRVAR1, STRVAR2, TokenType.PLUS, ConstantOperand.of("hi"), null));
    optimizer.optimize(input, null);
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void detectsPlus() {
    ImmutableList<Op> input =
        ImmutableList.of(new BinOp(INTVAR1, INTVAR2, TokenType.PLUS, ConstantOperand.of(-2), null));
    ImmutableList<Op> optimized = optimizer.optimize(input, null);
    assertThat(optimizer.isChanged()).isTrue();

    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isBinOp(INTVAR1, INTVAR2, TokenType.MINUS, ConstantOperand.of(2));
  }

  @Test
  public void detectsMinus() {
    ImmutableList<Op> input =
        ImmutableList.of(new BinOp(INTVAR1, INTVAR2, TokenType.MINUS, ConstantOperand.of(-2), null));
    ImmutableList<Op> optimized = optimizer.optimize(input, null);
    assertThat(optimizer.isChanged()).isTrue();

    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isBinOp(INTVAR1, INTVAR2, TokenType.PLUS, ConstantOperand.of(2));
  }

  @Test
  public void detectsPlus_long() {
    ImmutableList<Op> input =
        ImmutableList.of(new BinOp(LONGVAR1, LONGVAR2, TokenType.MINUS, ConstantOperand.of(-2L), null));
    ImmutableList<Op> optimized = optimizer.optimize(input, null);
    assertThat(optimizer.isChanged()).isTrue();

    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isBinOp(LONGVAR1, LONGVAR2, TokenType.PLUS, ConstantOperand.of(2L));
  }

  @Test
  public void detectsPlus_double() {
    ImmutableList<Op> input =
        ImmutableList.of(new BinOp(DBLVAR1, DBLVAR2, TokenType.MINUS, ConstantOperand.of(-2.0), null));
    ImmutableList<Op> optimized = optimizer.optimize(input, null);
    assertThat(optimizer.isChanged()).isTrue();

    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isBinOp(DBLVAR1, DBLVAR2, TokenType.PLUS, ConstantOperand.of(2.0));
  }
}
