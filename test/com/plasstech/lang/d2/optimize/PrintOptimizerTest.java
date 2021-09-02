package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.interpreter.ExecutionResult;
import com.plasstech.lang.d2.testing.TestUtils;

public class PrintOptimizerTest {

  private Optimizer optimizer =
      new ILOptimizer(
          ImmutableList.of(
              new ConstantPropagationOptimizer(0), new NopOptimizer(), new PrintOptimizer(2)));

  @Test
  public void twoInARow() {
    ExecutionResult result =
        TestUtils.optimizeAssertSameVariables("print 'hello' print 'world'", optimizer);
    assertTotalPrintCount(result, 1);
  }

  @Test
  public void notTwoInARow() {
    ExecutionResult result =
        TestUtils.optimizeAssertSameVariables("print 'hello' print 3", optimizer);
    assertTotalPrintCount(result, 2);
  }

  @Test
  public void twoInARowInAMethod() {
    ExecutionResult result =
        TestUtils.optimizeAssertSameVariables(
            "f:proc {a='hello' print a print 'world'} f()", optimizer);
    assertTotalPrintCount(result, 1);
  }

  @Test
  public void threeInARow() {
    ExecutionResult result =
        TestUtils.optimizeAssertSameVariables("print 'hello' print 'world' print 'bye'", optimizer);
    assertTotalPrintCount(result, 1);
  }

  @Test
  public void notThreeInARow() {
    ExecutionResult result =
        TestUtils.optimizeAssertSameVariables(
            // yeah yeah I know this could still be combined because constants but whatevs.
            "print 'hello' a=3 print 'world' print 'bye'", optimizer);
    assertTotalPrintCount(result, 2);
  }

  @Test
  public void technicallyNotTwoInARowButStillCounts() {
    ExecutionResult result = TestUtils.optimizeAssertSameVariables("println 'hello'", optimizer);
    assertTotalPrintCount(result, 1);
  }

  private void assertTotalPrintCount(ExecutionResult result, long expected) {
    long count = result.code().stream().filter(op -> (op instanceof SysCall)).count();
    assertThat(count).isEqualTo(expected);
  }
}
