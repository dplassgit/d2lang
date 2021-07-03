package com.plasstech.lang.d2.codegen;

import org.junit.Test;

public class LoopInvariantOptimizerTest {
  private LoopInvariantOptimizer optimizer = new LoopInvariantOptimizer(2);

  @Test
  public void oneloop() {
    TestUtils.optimizeAssertSameVariables(
        "      n = 3\n"
            + "sum = 0\n"
            + "i = 0 while i < 10 do i = i + 1 {\n"
            + "  x = n + 1 sum = sum + 1\n"
            + "}\n"
            + "println sum",
        optimizer);
  }

  @Test
  public void nestedLoops() {
    TestUtils.optimizeAssertSameVariables(
        "      sum = 0\n"
            + "n = 10\n"
            + "i = 0 while i < n do i = i + 1 {\n"
            + "  y = n + 1\n"
            + "  j = 0 while j < n do j = j + 1 {\n"
            + "    x = n * 3\n"
            + "    k = 0 while k < n do k = k + 1 {\n"
            + "      z = n * 3\n"
            + "      sum = sum + i\n"
            + "    }\n"
            + "    sum = sum + i\n"
            + "  }\n"
            + "  sum = sum + i\n"
            + "}\n"
            + "println sum",
        optimizer);
  }
}
