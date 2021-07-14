package com.plasstech.lang.d2.codegen;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class DeadAssignmentOptimizerTest {

  private Optimizer optimizer =
      new ILOptimizer(ImmutableList.of(new DeadAssignmentOptimizer(2))).setDebugLevel(2);

  @Test
  public void deadTemps() {
    TestUtils.optimizeAssertSameVariables(
        "      p:proc(n:int):int {"
            + "  sum = 0 i=0 while i < n do i = i + 1 {"
            + "    y = n * (2-1)"
            + "    y = n * (n-1) + n"
            + "    sum = sum + i"
            + "  }"
            + "  return sum"
            + "}"
            + "println p(10)",
        optimizer);
  }
}
