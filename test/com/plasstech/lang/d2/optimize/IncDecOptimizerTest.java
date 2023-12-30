package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.StackLocation;
import com.plasstech.lang.d2.codegen.TempLocation;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Dec;
import com.plasstech.lang.d2.codegen.il.Inc;
import com.plasstech.lang.d2.codegen.il.Nop;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.testing.LocationUtils;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.VarType;

public class IncDecOptimizerTest {
  private static final TempLocation TEMP1 = LocationUtils.newTempLocation("temp1", VarType.INT);
  private static final TempLocation TEMP2 = LocationUtils.newTempLocation("temp2", VarType.INT);
  private static final StackLocation STACK =
      LocationUtils.newStackLocation("stack", VarType.INT, 0);
  private static final TempLocation SOURCE = LocationUtils.newTempLocation("source", VarType.INT);
  private static final TempLocation DEST = LocationUtils.newTempLocation("dest", VarType.INT);

  private IncDecOptimizer optimizer = new IncDecOptimizer(2);

  @Test
  public void noOptimization() {
    // dest = 1
    // dest = source + 0
    // source = dest
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(DEST, ConstantOperand.ONE, null),
            new BinOp(DEST, SOURCE, TokenType.PLUS, ConstantOperand.ZERO, null),
            new Transfer(SOURCE, DEST, null));
    optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void incSimple() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(STACK, STACK, TokenType.PLUS, ConstantOperand.ONE, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();

    assertThat(optimized.get(0)).isInstanceOf(Inc.class);
  }

  @Test
  public void noOptimizeDouble() {
    StackLocation dbl =
        LocationUtils.newStackLocation("stack", VarType.DOUBLE, 0);

    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(dbl, dbl, TokenType.PLUS, ConstantOperand.ONE_DBL, null));

    optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void decSimple() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(STACK, STACK, TokenType.MINUS, ConstantOperand.ONE_BYTE, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();

    assertThat(optimized.get(0)).isInstanceOf(Dec.class);
  }

  @Test
  public void inc() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(TEMP1, STACK, null),
            new BinOp(TEMP2, TEMP1, TokenType.PLUS, ConstantOperand.ONE, null),
            new Transfer(STACK, TEMP2, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimized.get(0)).isInstanceOf(Nop.class);
    assertThat(optimized.get(1)).isInstanceOf(Nop.class);
    assertThat(optimized.get(2)).isInstanceOf(Inc.class);
    assertThat(optimizer.isChanged()).isTrue();
  }

  @Test
  public void incReversed() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(TEMP1, STACK, null),
            new BinOp(TEMP2, ConstantOperand.ONE, TokenType.PLUS, TEMP1, null),
            new Transfer(STACK, TEMP2, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimized.get(0)).isInstanceOf(Nop.class);
    assertThat(optimized.get(1)).isInstanceOf(Nop.class);
    assertThat(optimized.get(2)).isInstanceOf(Inc.class);
    assertThat(optimizer.isChanged()).isTrue();
  }

  @Test
  public void incShort() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(TEMP1, STACK, TokenType.PLUS, ConstantOperand.ONE, null),
            new Transfer(STACK, TEMP1, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimized.get(0)).isInstanceOf(Nop.class);
    assertThat(optimized.get(1)).isInstanceOf(Inc.class);
    assertThat(optimizer.isChanged()).isTrue();
  }

  @Test
  public void incShortReversed() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(TEMP1, ConstantOperand.ONE, TokenType.PLUS, STACK, null),
            new Transfer(STACK, TEMP1, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    System.out.println(Joiner.on('\n').join(optimized));
    assertThat(optimized.get(0)).isInstanceOf(Nop.class);
    assertThat(optimized.get(1)).isInstanceOf(Inc.class);
    assertThat(optimizer.isChanged()).isTrue();
  }

  @Test
  public void decShort() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(TEMP1, STACK, TokenType.MINUS, ConstantOperand.ONE, null),
            new Transfer(STACK, TEMP1, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimized.get(0)).isInstanceOf(Nop.class);
    assertThat(optimized.get(1)).isInstanceOf(Dec.class);
    assertThat(optimizer.isChanged()).isTrue();
  }

  @Test
  public void decShortReversed() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(TEMP1, ConstantOperand.ONE, TokenType.MINUS, STACK, null),
            new Transfer(STACK, TEMP1, null));

    optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void dec() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(TEMP1, STACK, null),
            new BinOp(TEMP2, TEMP1, TokenType.MINUS, ConstantOperand.ONE, null),
            new Transfer(STACK, TEMP2, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimized.get(0)).isInstanceOf(Nop.class);
    assertThat(optimized.get(1)).isInstanceOf(Nop.class);
    assertThat(optimized.get(2)).isInstanceOf(Dec.class);
    assertThat(optimizer.isChanged()).isTrue();
  }

  @Test
  public void decReversed() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(TEMP1, STACK, null),
            new BinOp(TEMP2, ConstantOperand.ONE, TokenType.MINUS, TEMP1, null),
            new Transfer(STACK, TEMP2, null));

    optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isFalse();
  }
}
