package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.ProcEntry;
import com.plasstech.lang.d2.codegen.il.ProcExit;
import com.plasstech.lang.d2.interpreter.InterpreterResult;
import com.plasstech.lang.d2.testing.TestUtils;

public class DeadProcOptimizerTest {
  private static final Optimizer OPTIMIZER = new DeadProcOptimizer(2);

  @Test
  public void no_proc() {
    InterpreterResult result = TestUtils.optimizeAssertSameVariables("a=3", OPTIMIZER);
    assertThat(result.calls()).isEqualTo(0);
  }

  @Test
  public void dead_proc() {
    InterpreterResult result = TestUtils.optimizeAssertSameVariables("a=3"
        + " f:proc(b:int):int { return b + 1}"
        + " println a", OPTIMIZER);
    for (Op op : result.code()) {
      assertThat(op).isNotInstanceOf(ProcEntry.class);
      assertThat(op).isNotInstanceOf(ProcExit.class);
    }
    assertThat(result.calls()).isEqualTo(0);
  }

  @Test
  public void not_dead_proc() {
    InterpreterResult result = TestUtils.optimizeAssertSameVariables("a=3"
        + " f:proc(b:int):int { return b + 1}"
        + " println f(a)", OPTIMIZER);
    assertThat(result.calls()).isEqualTo(1);
  }
}
