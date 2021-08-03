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
import com.plasstech.lang.d2.lex.Token;

public class IncDecOptimizerTest {
  private static final ConstantOperand<Integer> TWO = new ConstantOperand<Integer>(2);

  private IncDecOptimizer optimizer = new IncDecOptimizer(2);

  @Test
  public void noOptimization() {
    TempLocation source = new TempLocation("source");
    TempLocation dest = new TempLocation("dest");
    // dest = 1
    // dest = source + 0
    // source = dest
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(dest, ConstantOperand.ONE),
            new BinOp(dest, source, Token.Type.PLUS, ConstantOperand.ZERO),
            new Transfer(source, dest));
    optimizer.optimize(program);
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void optimizeInc() {
    TempLocation temp1 = new TempLocation("temp1");
    TempLocation temp2 = new TempLocation("temp2");
    StackLocation stack = new StackLocation("stack");
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(temp1, stack),
            new BinOp(temp2, temp1, Token.Type.PLUS, ConstantOperand.ONE),
            new Transfer(stack, temp2));

    ImmutableList<Op> optimized = optimizer.optimize(program);

    System.out.println(Joiner.on('\n').join(optimized));
    assertThat(optimized.get(0)).isInstanceOf(Nop.class);
    assertThat(optimized.get(1)).isInstanceOf(Nop.class);
    assertThat(optimized.get(2)).isInstanceOf(Inc.class);
    assertThat(optimizer.isChanged()).isTrue();
  }

  @Test
  public void optimizeIncReversed() {
    TempLocation temp1 = new TempLocation("temp1");
    TempLocation temp2 = new TempLocation("temp2");
    StackLocation stack = new StackLocation("stack");
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(temp1, stack),
            new BinOp(temp2, ConstantOperand.ONE, Token.Type.PLUS, temp1),
            new Transfer(stack, temp2));

    ImmutableList<Op> optimized = optimizer.optimize(program);

    System.out.println(Joiner.on('\n').join(optimized));
    assertThat(optimized.get(0)).isInstanceOf(Nop.class);
    assertThat(optimized.get(1)).isInstanceOf(Nop.class);
    assertThat(optimized.get(2)).isInstanceOf(Inc.class);
    assertThat(optimizer.isChanged()).isTrue();
  }

  @Test
  public void optimizeIncShort() {
    TempLocation temp1 = new TempLocation("temp1");
    StackLocation stack = new StackLocation("stack");
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(temp1, stack, Token.Type.PLUS, ConstantOperand.ONE),
            new Transfer(stack, temp1));

    ImmutableList<Op> optimized = optimizer.optimize(program);

    System.out.println(Joiner.on('\n').join(optimized));
    assertThat(optimized.get(0)).isInstanceOf(Nop.class);
    assertThat(optimized.get(1)).isInstanceOf(Inc.class);
    assertThat(optimizer.isChanged()).isTrue();
  }

  @Test
  public void optimizeIncShortReversed() {
    TempLocation temp1 = new TempLocation("temp1");
    StackLocation stack = new StackLocation("stack");
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(temp1, ConstantOperand.ONE, Token.Type.PLUS, stack),
            new Transfer(stack, temp1));

    ImmutableList<Op> optimized = optimizer.optimize(program);

    System.out.println(Joiner.on('\n').join(optimized));
    assertThat(optimized.get(0)).isInstanceOf(Nop.class);
    assertThat(optimized.get(1)).isInstanceOf(Inc.class);
    assertThat(optimizer.isChanged()).isTrue();
  }

  @Test
  public void optimizeInc2Short() {
    TempLocation temp1 = new TempLocation("temp1");
    StackLocation stack = new StackLocation("stack");
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(temp1, stack, Token.Type.PLUS, TWO), new Transfer(stack, temp1));

    ImmutableList<Op> optimized = optimizer.optimize(program);

    System.out.println(Joiner.on('\n').join(optimized));
    assertThat(optimized.get(0)).isInstanceOf(Inc.class);
    assertThat(optimized.get(1)).isInstanceOf(Inc.class);
    assertThat(optimizer.isChanged()).isTrue();
  }

  @Test
  public void optimizeInc2ShortReversed() {
    TempLocation temp1 = new TempLocation("temp1");
    StackLocation stack = new StackLocation("stack");
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(temp1, TWO, Token.Type.PLUS, stack), new Transfer(stack, temp1));

    ImmutableList<Op> optimized = optimizer.optimize(program);

    System.out.println(Joiner.on('\n').join(optimized));
    assertThat(optimized.get(0)).isInstanceOf(Inc.class);
    assertThat(optimized.get(1)).isInstanceOf(Inc.class);
    assertThat(optimizer.isChanged()).isTrue();
  }

  @Test
  public void optimizeDecShort() {
    TempLocation temp1 = new TempLocation("temp1");
    StackLocation stack = new StackLocation("stack");
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(temp1, stack, Token.Type.MINUS, ConstantOperand.ONE),
            new Transfer(stack, temp1));

    ImmutableList<Op> optimized = optimizer.optimize(program);

    System.out.println(Joiner.on('\n').join(optimized));
    assertThat(optimized.get(0)).isInstanceOf(Nop.class);
    assertThat(optimized.get(1)).isInstanceOf(Dec.class);
    assertThat(optimizer.isChanged()).isTrue();
  }

  @Test
  public void optimizeDecShortReversed() {
    TempLocation temp1 = new TempLocation("temp1");
    StackLocation stack = new StackLocation("stack");
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(temp1, ConstantOperand.ONE, Token.Type.MINUS, stack),
            new Transfer(stack, temp1));

    optimizer.optimize(program);

    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void optimizeMinus2Short() {
    TempLocation temp1 = new TempLocation("temp1");
    StackLocation stack = new StackLocation("stack");
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(temp1, stack, Token.Type.MINUS, TWO), new Transfer(stack, temp1));

    ImmutableList<Op> optimized = optimizer.optimize(program);

    System.out.println(Joiner.on('\n').join(optimized));
    assertThat(optimized.get(0)).isInstanceOf(Dec.class);
    assertThat(optimized.get(1)).isInstanceOf(Dec.class);
    assertThat(optimizer.isChanged()).isTrue();
  }

  @Test
  public void optimizePlus2() {
    TempLocation temp1 = new TempLocation("temp1");
    TempLocation temp2 = new TempLocation("temp2");
    StackLocation stack = new StackLocation("stack");
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(temp1, stack),
            new BinOp(temp2, temp1, Token.Type.PLUS, TWO),
            new Transfer(stack, temp2));

    ImmutableList<Op> optimized = optimizer.optimize(program);

    System.out.println(Joiner.on('\n').join(optimized));

    assertThat(optimized.get(0)).isInstanceOf(Nop.class);
    assertThat(optimized.get(1)).isInstanceOf(Inc.class);
    assertThat(optimized.get(2)).isInstanceOf(Inc.class);
    assertThat(optimizer.isChanged()).isTrue();
  }

  @Test
  public void optimizePlus2Reversed() {
    TempLocation temp1 = new TempLocation("temp1");
    TempLocation temp2 = new TempLocation("temp2");
    StackLocation stack = new StackLocation("stack");
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(temp1, stack),
            new BinOp(temp2, TWO, Token.Type.PLUS, temp1),
            new Transfer(stack, temp2));

    ImmutableList<Op> optimized = optimizer.optimize(program);

    System.out.println(Joiner.on('\n').join(optimized));

    assertThat(optimized.get(0)).isInstanceOf(Nop.class);
    assertThat(optimized.get(1)).isInstanceOf(Inc.class);
    assertThat(optimized.get(2)).isInstanceOf(Inc.class);
    assertThat(optimizer.isChanged()).isTrue();
  }

  @Test
  public void optimizeDec() {
    TempLocation temp1 = new TempLocation("temp1");
    TempLocation temp2 = new TempLocation("temp2");
    StackLocation stack = new StackLocation("stack");
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(temp1, stack),
            new BinOp(temp2, temp1, Token.Type.MINUS, ConstantOperand.ONE),
            new Transfer(stack, temp2));
    
    ImmutableList<Op> optimized = optimizer.optimize(program);

    System.out.println(Joiner.on('\n').join(optimized));
    assertThat(optimized.get(0)).isInstanceOf(Nop.class);
    assertThat(optimized.get(1)).isInstanceOf(Nop.class);
    assertThat(optimized.get(2)).isInstanceOf(Dec.class);
    assertThat(optimizer.isChanged()).isTrue();
  }

  @Test
  public void optimizeDecReversed() {
    TempLocation temp1 = new TempLocation("temp1");
    TempLocation temp2 = new TempLocation("temp2");
    StackLocation stack = new StackLocation("stack");
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(temp1, stack),
            new BinOp(temp2, ConstantOperand.ONE, Token.Type.MINUS, temp1),
            new Transfer(stack, temp2));

    optimizer.optimize(program);

    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void optimizeMinus2() {
    TempLocation temp1 = new TempLocation("temp1");
    TempLocation temp2 = new TempLocation("temp2");
    StackLocation stack = new StackLocation("stack");
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(temp1, stack),
            new BinOp(temp2, temp1, Token.Type.MINUS, TWO),
            new Transfer(stack, temp2));

    ImmutableList<Op> optimized = optimizer.optimize(program);

    System.out.println(Joiner.on('\n').join(optimized));

    assertThat(optimized.get(0)).isInstanceOf(Nop.class);
    assertThat(optimized.get(1)).isInstanceOf(Dec.class);
    assertThat(optimized.get(2)).isInstanceOf(Dec.class);
    assertThat(optimizer.isChanged()).isTrue();
  }
}
