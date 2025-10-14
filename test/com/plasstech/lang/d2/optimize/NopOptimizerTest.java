package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.Label;
import com.plasstech.lang.d2.codegen.il.Nop;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.ProcExit;

public class NopOptimizerTest {
  private final Optimizer optimizer = new NopOptimizer();

  private static final Op PROC_EXIT = new ProcExit("name", 0, 0);
  private static final Op LABEL = new Label("keepme");

  @Test
  public void empty() {
    assertThat(optimizer.optimize(ImmutableList.of(), null)).isEmpty();
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void nopsOnly() {
    assertThat(optimizer.optimize(ImmutableList.of(
        new Nop(),
        new Nop("; comment"),
        new Nop(LABEL)), null))
        .isEmpty();
    assertThat(optimizer.isChanged()).isTrue();
  }

  @Test
  public void noNops() {
    ImmutableList<Op> program = ImmutableList.of(LABEL, PROC_EXIT);
    assertThat(optimizer.optimize(program, null)).isEqualTo(program);
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void someNops() {
    ImmutableList<Op> program = ImmutableList.of(
        new Nop(),
        LABEL,
        new Nop("; comment"),
        PROC_EXIT,
        new Nop(LABEL));
    assertThat(optimizer.optimize(program, null)).containsExactly(LABEL, PROC_EXIT);
    assertThat(optimizer.isChanged()).isTrue();
  }
}
