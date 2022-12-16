package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.interpreter.InterpreterResult;
import com.plasstech.lang.d2.testing.TestUtils;

public class PrintOptimizerTest {

  private Optimizer optimizer =
      new ILOptimizer(
          ImmutableList.of(
              new ConstantPropagationOptimizer(0),
              new DeadAssignmentOptimizer(0),
              new NopOptimizer(),
              new PrintOptimizer(2)));

  @Test
  public void twoInARow() {
    InterpreterResult result =
        TestUtils.optimizeAssertSameVariables("print 'hello' print 'world'", optimizer);
    assertTotalPrintCount(result, 1);
  }

  @Test
  public void notTwoInARow() {
    InterpreterResult result =
        TestUtils.optimizeAssertSameVariables("print 'hello' print 3", optimizer);
    assertTotalPrintCount(result, 2);
  }

  @Test
  public void twoInARowInAMethod() {
    InterpreterResult result =
        TestUtils.optimizeAssertSameVariables(
            "f:proc {a='hello' print a print 'world'} f()", optimizer);
    assertTotalPrintCount(result, 1);
  }

  @Test
  public void threeInARow() {
    InterpreterResult result =
        TestUtils.optimizeAssertSameVariables("print 'hello' print 'world' print 'bye'", optimizer);
    assertTotalPrintCount(result, 1);
  }

  @Test
  public void notThreeInARow() {
    InterpreterResult result =
        TestUtils.optimizeAssertSameVariables(
            // yeah yeah I know this could still be combined because constants but whatevs.
            "print 'hello' a=3 print 'world' print 'bye'", optimizer);
    assertTotalPrintCount(result, 2);
  }

  @Test
  public void println() {
    InterpreterResult result =
        TestUtils.optimizeAssertSameVariables("println 'hello' print 'world'", optimizer);
    List<String> output = result.environment().output();
    assertThat(output.get(0)).isEqualTo("hello\nworld");
    assertTotalPrintCount(result, 1);
  }

  @Test
  public void printlnTwoStrings() {
    InterpreterResult result =
        TestUtils.optimizeAssertSameVariables("println 'hello' println 'world'", optimizer);
    SysCall call = (SysCall) result.code().get(0);
    assertThat(call.call()).isEqualTo(SysCall.Call.PRINTLN);
    List<String> output = result.environment().output();
    assertThat(output.get(0)).isEqualTo("hello\nworld");
    assertTotalPrintCount(result, 1);
  }

  @Test
  public void printlnInts() {
    InterpreterResult result =
        TestUtils.optimizeAssertSameVariables(
            "println 3 print 4",
            new ILOptimizer(
                ImmutableList.of(
                    // need this to allow the "adjacent" test to work
                    new NopOptimizer(),
                    // need this to propagate the __temp1=3+println __temp1 to println 3
                    new ConstantPropagationOptimizer(0),
                    // need this to get rid of dead temp assignments
                    new DeadAssignmentOptimizer(0),
                    // need this to convert println 3 to println "3"
                    new ArithmeticOptimizer(2),
                    new PrintOptimizer(2))));
    List<String> output = result.environment().output();
    assertThat(output.get(0)).isEqualTo("3\n4");
    assertTotalPrintCount(result, 1);
  }

  @Test
  public void technicallyNotTwoInARowButStillCounts() {
    InterpreterResult result = TestUtils.optimizeAssertSameVariables("println 'hello'", optimizer);
    assertTotalPrintCount(result, 1);
  }

  private void assertTotalPrintCount(InterpreterResult result, long expected) {
    long count = result.code().stream().filter(op -> (op instanceof SysCall)).count();
    assertThat(count).isEqualTo(expected);
  }
}
