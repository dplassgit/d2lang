package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.collect.ImmutableList;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.interpreter.InterpreterResult;
import com.plasstech.lang.d2.testing.TestUtils;

@RunWith(TestParameterInjector.class)
public class PrintOptimizerTest {

  private static final Optimizer OPTIMIZER =
      new ILOptimizer(
          ImmutableList.of(
              new ConstantPropagationOptimizer(0),
              new NopOptimizer(),
              new PrintOptimizer(2)));

  @Test
  public void twoInARow() {
    InterpreterResult result =
        TestUtils.optimizeAssertSameVariables("print 'hello' print 'world'", OPTIMIZER);
    assertTotalPrintCount(result, 1);
  }

  @Test
  public void notTwoInARow() {
    InterpreterResult result =
        TestUtils.optimizeAssertSameVariables("print 'hello' print 3", OPTIMIZER);
    assertTotalPrintCount(result, 1);
  }

  @Test
  public void twoInARowInAMethod() {
    InterpreterResult result =
        TestUtils.optimizeAssertSameVariables(
            "f:proc {a='hello' print a print 'world'} f()", OPTIMIZER);
    assertTotalPrintCount(result, 1);
  }

  @Test
  public void threeInARow() {
    InterpreterResult result =
        TestUtils.optimizeAssertSameVariables("print 'hello' print 'world' print 'bye'", OPTIMIZER);
    assertTotalPrintCount(result, 1);
  }

  @Test
  public void notThreeInARow() {
    InterpreterResult result =
        TestUtils.optimizeAssertSameVariables(
            // yeah yeah I know this could still be combined because constants but whatevs.
            "print 'hello' a=3 print 'world' print 'bye'", OPTIMIZER);
    assertTotalPrintCount(result, 2);
  }

  @Test
  public void println() {
    InterpreterResult result =
        TestUtils.optimizeAssertSameVariables("println 'hello' print 'world'", OPTIMIZER);
    List<String> output = result.environment().output();
    assertThat(output.get(0)).isEqualTo("hello\nworld");
    assertTotalPrintCount(result, 1);
  }

  @Test
  public void printlnTwoStrings() {
    InterpreterResult result =
        TestUtils.optimizeAssertSameVariables("println 'hello' println 'world'", OPTIMIZER);
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
    InterpreterResult result = TestUtils.optimizeAssertSameVariables("println 'hello'", OPTIMIZER);
    assertTotalPrintCount(result, 1);
  }

  @Test
  public void printConstantBool(@TestParameter boolean val) {
    InterpreterResult result =
        TestUtils.optimizeAssertSameVariables(String.format("print %s", val), OPTIMIZER);

    ImmutableList<Op> code = result.code();
    SysCall first = (SysCall) code.get(0);
    ConstantOperand<?> arg = (ConstantOperand<?>) first.arg();
    assertThat(arg.value()).isEqualTo(String.valueOf(val));
  }

  @Test
  public void printConstantInt() {
    InterpreterResult result = TestUtils.optimizeAssertSameVariables("print 3", OPTIMIZER);

    ImmutableList<Op> code = result.code();
    SysCall first = (SysCall) code.get(0);
    ConstantOperand<?> arg = (ConstantOperand<?>) first.arg();
    assertThat(arg.value()).isEqualTo("3");
  }

  @Test
  public void printConstantLong() {
    InterpreterResult result = TestUtils.optimizeAssertSameVariables("print 3L", OPTIMIZER);

    ImmutableList<Op> code = result.code();
    SysCall first = (SysCall) code.get(0);
    ConstantOperand<?> arg = (ConstantOperand<?>) first.arg();
    assertThat(arg.value()).isEqualTo("3L");
  }

  @Test
  public void printConstantNull() {
    InterpreterResult result = TestUtils.optimizeAssertSameVariables("print null", OPTIMIZER);

    ImmutableList<Op> code = result.code();
    SysCall first = (SysCall) code.get(0);
    ConstantOperand<?> arg = (ConstantOperand<?>) first.arg();
    assertThat(arg.value()).isEqualTo("null");
  }

  @Test
  public void printConstantByte() {
    InterpreterResult result = TestUtils.optimizeAssertSameVariables("print 0y03", OPTIMIZER);

    ImmutableList<Op> code = result.code();
    SysCall first = (SysCall) code.get(0);
    ConstantOperand<?> arg = (ConstantOperand<?>) first.arg();
    assertThat(arg.value()).isEqualTo("0y03");
  }

  private void assertTotalPrintCount(InterpreterResult result, long expected) {
    long actual = result.code().stream().filter(op -> (op instanceof SysCall)).count();
    assertThat(actual).isEqualTo(expected);
  }
}
