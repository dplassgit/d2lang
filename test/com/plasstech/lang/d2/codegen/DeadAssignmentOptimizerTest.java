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
        "      rt: record{i:int} "
            + "updaterec: proc(re:rt) { "
            + "  re.i = re.i + 1 "
            + "} "
            + "recordloopinvariant: proc(rec:rt): int { "
            + "  while rec.i < 10 { "
            + "    updaterec(rec) "
            + "  } "
            + "  return rec.i "
            + "} "
            + "print recordloopinvariant(new rt) ",
        optimizer);
  }

  @Test
  public void linkedList() {
    TestUtils.optimizeAssertSameVariables(TestUtils.LINKED_LIST, optimizer);
  }
}
