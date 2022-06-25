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
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.VarType;

public class IncDecOptimizerTest {
  private static final ConstantOperand<Integer> TWO = new ConstantOperand<Integer>(2);
  private static final TempLocation TEMP1 = new TempLocation("temp1", VarType.INT);
  private static final TempLocation TEMP2 = new TempLocation("temp2", VarType.INT);
  private static final StackLocation STACK = new StackLocation("stack", VarType.INT, 0);
  private static final TempLocation SOURCE = new TempLocation("source", VarType.INT);
  private static final TempLocation DEST = new TempLocation("dest", VarType.INT);

  private IncDecOptimizer optimizer = new IncDecOptimizer(2);

  @Test
  public void noOptimization() {
    // dest = 1
    // dest = source + 0
    // source = dest
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(DEST, ConstantOperand.ONE),
            new BinOp(DEST, SOURCE, TokenType.PLUS, ConstantOperand.ZERO),
            new Transfer(SOURCE, DEST));
    optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void optimizeInc() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(TEMP1, STACK),
            new BinOp(TEMP2, TEMP1, TokenType.PLUS, ConstantOperand.ONE),
            new Transfer(STACK, TEMP2));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    System.out.println(Joiner.on('\n').join(optimized));
    assertThat(optimized.get(0)).isInstanceOf(Nop.class);
    assertThat(optimized.get(1)).isInstanceOf(Nop.class);
    assertThat(optimized.get(2)).isInstanceOf(Inc.class);
    assertThat(optimizer.isChanged()).isTrue();
  }

  @Test
  public void optimizeIncReversed() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(TEMP1, STACK),
            new BinOp(TEMP2, ConstantOperand.ONE, TokenType.PLUS, TEMP1),
            new Transfer(STACK, TEMP2));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    System.out.println(Joiner.on('\n').join(optimized));
    assertThat(optimized.get(0)).isInstanceOf(Nop.class);
    assertThat(optimized.get(1)).isInstanceOf(Nop.class);
    assertThat(optimized.get(2)).isInstanceOf(Inc.class);
    assertThat(optimizer.isChanged()).isTrue();
  }

  @Test
  public void optimizeIncShort() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(TEMP1, STACK, TokenType.PLUS, ConstantOperand.ONE),
            new Transfer(STACK, TEMP1));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    System.out.println(Joiner.on('\n').join(optimized));
    assertThat(optimized.get(0)).isInstanceOf(Nop.class);
    assertThat(optimized.get(1)).isInstanceOf(Inc.class);
    assertThat(optimizer.isChanged()).isTrue();
  }

  @Test
  public void optimizeIncShortReversed() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(TEMP1, ConstantOperand.ONE, TokenType.PLUS, STACK),
            new Transfer(STACK, TEMP1));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    System.out.println(Joiner.on('\n').join(optimized));
    assertThat(optimized.get(0)).isInstanceOf(Nop.class);
    assertThat(optimized.get(1)).isInstanceOf(Inc.class);
    assertThat(optimizer.isChanged()).isTrue();
  }

  @Test
  public void optimizeInc2Short() {
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(TEMP1, STACK, TokenType.PLUS, TWO), new Transfer(STACK, TEMP1));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    System.out.println(Joiner.on('\n').join(optimized));
    assertThat(optimized.get(0)).isInstanceOf(Inc.class);
    assertThat(optimized.get(1)).isInstanceOf(Inc.class);
    assertThat(optimizer.isChanged()).isTrue();
  }

  @Test
  public void optimizeInc2ShortReversed() {
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(TEMP1, TWO, TokenType.PLUS, STACK), new Transfer(STACK, TEMP1));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    System.out.println(Joiner.on('\n').join(optimized));
    assertThat(optimized.get(0)).isInstanceOf(Inc.class);
    assertThat(optimized.get(1)).isInstanceOf(Inc.class);
    assertThat(optimizer.isChanged()).isTrue();
  }

  @Test
  public void optimizeDecShort() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(TEMP1, STACK, TokenType.MINUS, ConstantOperand.ONE),
            new Transfer(STACK, TEMP1));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    System.out.println(Joiner.on('\n').join(optimized));
    assertThat(optimized.get(0)).isInstanceOf(Nop.class);
    assertThat(optimized.get(1)).isInstanceOf(Dec.class);
    assertThat(optimizer.isChanged()).isTrue();
  }

  @Test
  public void optimizeDecShortReversed() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(TEMP1, ConstantOperand.ONE, TokenType.MINUS, STACK),
            new Transfer(STACK, TEMP1));

    optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void optimizeMinus2Short() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(TEMP1, STACK, TokenType.MINUS, TWO), new Transfer(STACK, TEMP1));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    System.out.println(Joiner.on('\n').join(optimized));
    assertThat(optimized.get(0)).isInstanceOf(Dec.class);
    assertThat(optimized.get(1)).isInstanceOf(Dec.class);
    assertThat(optimizer.isChanged()).isTrue();
  }

  @Test
  public void optimizePlus2() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(TEMP1, STACK),
            new BinOp(TEMP2, TEMP1, TokenType.PLUS, TWO),
            new Transfer(STACK, TEMP2));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    System.out.println(Joiner.on('\n').join(optimized));

    assertThat(optimized.get(0)).isInstanceOf(Nop.class);
    assertThat(optimized.get(1)).isInstanceOf(Inc.class);
    assertThat(optimized.get(2)).isInstanceOf(Inc.class);
    assertThat(optimizer.isChanged()).isTrue();
  }

  @Test
  public void optimizePlus2Reversed() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(TEMP1, STACK),
            new BinOp(TEMP2, TWO, TokenType.PLUS, TEMP1),
            new Transfer(STACK, TEMP2));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    System.out.println(Joiner.on('\n').join(optimized));

    assertThat(optimized.get(0)).isInstanceOf(Nop.class);
    assertThat(optimized.get(1)).isInstanceOf(Inc.class);
    assertThat(optimized.get(2)).isInstanceOf(Inc.class);
    assertThat(optimizer.isChanged()).isTrue();
  }

  @Test
  public void optimizeDec() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(TEMP1, STACK),
            new BinOp(TEMP2, TEMP1, TokenType.MINUS, ConstantOperand.ONE),
            new Transfer(STACK, TEMP2));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    System.out.println(Joiner.on('\n').join(optimized));
    assertThat(optimized.get(0)).isInstanceOf(Nop.class);
    assertThat(optimized.get(1)).isInstanceOf(Nop.class);
    assertThat(optimized.get(2)).isInstanceOf(Dec.class);
    assertThat(optimizer.isChanged()).isTrue();
  }

  @Test
  public void optimizeDecReversed() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(TEMP1, STACK),
            new BinOp(TEMP2, ConstantOperand.ONE, TokenType.MINUS, TEMP1),
            new Transfer(STACK, TEMP2));

    optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void optimizeMinus2() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(TEMP1, STACK),
            new BinOp(TEMP2, TEMP1, TokenType.MINUS, TWO),
            new Transfer(STACK, TEMP2));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    System.out.println(Joiner.on('\n').join(optimized));

    assertThat(optimized.get(0)).isInstanceOf(Nop.class);
    assertThat(optimized.get(1)).isInstanceOf(Dec.class);
    assertThat(optimized.get(2)).isInstanceOf(Dec.class);
    assertThat(optimizer.isChanged()).isTrue();
  }
}
