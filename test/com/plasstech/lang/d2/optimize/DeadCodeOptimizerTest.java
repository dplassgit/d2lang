package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.TempLocation;
import com.plasstech.lang.d2.codegen.il.Dec;
import com.plasstech.lang.d2.codegen.il.Inc;
import com.plasstech.lang.d2.codegen.il.Nop;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.testing.TestUtils;
import com.plasstech.lang.d2.type.VarType;

public class DeadCodeOptimizerTest {
  private Optimizer optimizer =
      new ILOptimizer(
              ImmutableList.of(new ConstantPropagationOptimizer(2), new DeadCodeOptimizer(2)))
          .setDebugLevel(2);
  private static final TempLocation TEMP1 = new TempLocation("temp1", VarType.INT);
  private static final TempLocation TEMP2 = new TempLocation("temp2", VarType.INT);

  @Test
  public void oneLoopBreak() {
    TestUtils.optimizeAssertSameVariables(
        "      oneLoopBreakDCO:proc(n:int):int {\n"
            + "  sum = 0\n"
            + "  i = 0 "
            + "  while i < 10 do i = i + 1 {"
            + "    sum = sum + 1\n"
            + "    break"
            + "  }"
            + "  return sum"
            + "}"
            + "println oneLoopBreakDCO(10)",
        optimizer);
  }

  @Test
  public void incDec() {
    ImmutableList<Op> program = ImmutableList.of(new Inc(TEMP1), new Dec(TEMP1));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimized.get(0)).isInstanceOf(Nop.class);
    assertThat(optimized.get(1)).isInstanceOf(Nop.class);
  }

  @Test
  public void incDecDifferent() {
    ImmutableList<Op> program = ImmutableList.of(new Inc(TEMP1), new Dec(TEMP2));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isFalse();
    assertThat(optimized.get(0)).isInstanceOf(Inc.class);
    assertThat(optimized.get(1)).isInstanceOf(Dec.class);
  }

  @Test
  public void decInc() {
    ImmutableList<Op> program = ImmutableList.of(new Dec(TEMP1), new Inc(TEMP1));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimized.get(0)).isInstanceOf(Nop.class);
    assertThat(optimized.get(1)).isInstanceOf(Nop.class);
  }

  @Test
  public void decIncDec() {
    ImmutableList<Op> program = ImmutableList.of(new Dec(TEMP1), new Inc(TEMP1), new Dec(TEMP1));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized.get(0)).isInstanceOf(Nop.class);
    assertThat(optimized.get(1)).isInstanceOf(Nop.class);
    assertThat(optimized.get(2)).isInstanceOf(Dec.class);
  }
}
