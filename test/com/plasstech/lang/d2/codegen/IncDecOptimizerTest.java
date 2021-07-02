package com.plasstech.lang.d2.codegen;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Dec;
import com.plasstech.lang.d2.codegen.il.Inc;
import com.plasstech.lang.d2.codegen.il.Nop;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.lex.Token;

public class IncDecOptimizerTest {

  private IncDecOptimizer optimizer = new IncDecOptimizer(0);

  @Test
  public void noOptimization() {
    TempLocation source = new TempLocation("source");
    TempLocation dest = new TempLocation("dest");
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(dest, ConstantOperand.ONE),
            new BinOp(dest, source, Token.Type.PLUS, ConstantOperand.ONE),
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
    assertThat(optimized.get(0)).isInstanceOf(Inc.class);
    assertThat(optimized.get(1)).isInstanceOf(Nop.class);
    assertThat(optimized.get(2)).isInstanceOf(Nop.class);
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
    assertThat(optimized.get(0)).isInstanceOf(Dec.class);
    assertThat(optimized.get(1)).isInstanceOf(Nop.class);
    assertThat(optimized.get(2)).isInstanceOf(Nop.class);
    assertThat(optimizer.isChanged()).isTrue();
  }
}
