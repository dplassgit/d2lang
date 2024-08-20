package com.plasstech.lang.d2.optimize;

import static com.plasstech.lang.d2.optimize.testing.OptimizerSubject.assertThatInterpreting;

import org.junit.Test;

public class DeadProcOptimizerTest {
  private static final Optimizer OPTIMIZER = new DeadProcOptimizer(2);

  @Test
  public void no_proc() {
    assertThatInterpreting("a=3").withOptimizer(OPTIMIZER).hasNoCalls();
  }

  @Test
  public void dead_proc() {
    assertThatInterpreting("a=3"
        + " f:proc(b:int):int { return b + 1}"
        + " println a").withOptimizer(OPTIMIZER).hasNoCalls();
  }

  @Test
  public void not_dead_proc() {
    assertThatInterpreting("a=3"
        + " f:proc(b:int):int { return b + 1}"
        + " println f(a)").withOptimizer(OPTIMIZER).hasCallsTo("f");
  }
}
