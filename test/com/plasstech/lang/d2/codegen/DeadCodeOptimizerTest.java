package com.plasstech.lang.d2.codegen;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class DeadCodeOptimizerTest {
  private Optimizer optimizer =
      new ILOptimizer(
              ImmutableList.of(new ConstantPropagationOptimizer(2), new DeadCodeOptimizer(2)))
          .setDebugLevel(2);

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
}
