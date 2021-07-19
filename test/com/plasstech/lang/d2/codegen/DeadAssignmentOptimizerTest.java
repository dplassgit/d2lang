package com.plasstech.lang.d2.codegen;

import org.junit.Test;

public class DeadAssignmentOptimizerTest {

  private Optimizer optimizer = new ILOptimizer(new DeadAssignmentOptimizer(2)).setDebugLevel(2);

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

  @Test
  public void recordLoopInvariant() {
    TestUtils.optimizeAssertSameVariables(
        "      r: record{i:int} "
            + "updaterec: proc(rec:r) { "
            + "  rec.i = rec.i + 1 "
            + "} "
            + "recordloopinvariant: proc(rec:r): int { "
            + "  rec.i = 0 "
            + "  while rec.i < 10 { "
            + "    updaterec(rec) "
            + "  } "
            + "  return rec.i "
            + "} "
            + "val = recordloopinvariant(new r) "
            + "println val ",
        optimizer);
  }

  @Test
  public void linkedList() {
    TestUtils.optimizeAssertSameVariables(TestUtils.LINKED_LIST, optimizer);
  }
}
