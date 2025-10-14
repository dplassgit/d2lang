package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;
import static com.plasstech.lang.d2.optimize.testing.OptimizerSubject.assertThatInterpreting;

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

@RunWith(TestParameterInjector.class)
public class PrintOptimizerTest {

  private final Optimizer optimizer =
      new ILOptimizer(
          ImmutableList.of(
              new NopOptimizer(),
              // This is needed since print("hi") becomes
              // _temp="hi"
              // syscall(PRINT, _temp)
              // which won't get optimized by the PrintOptimizer. So we have to make sure it becomes
              // syscall(PRINT, "hi")
              new ConstantPropagationOptimizer(0),
              new PrintOptimizer(2)),
          0);

  @Test
  public void twoInARow() {
    assertThatInterpreting("print 'hello' print 'world'").withOptimizer(optimizer)
        .hasExpectedSysCallCount(1);
  }

  @Test
  public void notTwoInARow() {
    assertThatInterpreting("print 'hello' print 3").withOptimizer(optimizer)
        .hasExpectedSysCallCount(1);
  }

  @Test
  public void twoInARowInAMethod() {
    assertThatInterpreting("f:proc {a='hello' print a print 'world'} f()")
        .withOptimizer(optimizer).hasExpectedSysCallCount(1);
  }

  @Test
  public void threeInARow() {
    assertThatInterpreting("print 'hello' print 'world' print 'bye'").withOptimizer(optimizer)
        .hasExpectedSysCallCount(1);
  }

  @Test
  public void notThreeInARow() {
    assertThatInterpreting("print 'hello' a=3 print 'world' print 'bye'")
        .withOptimizer(optimizer).hasExpectedSysCallCount(2);
  }

  @Test
  public void println() {
    InterpreterResult result =
        assertThatInterpreting("println 'hello' print 'world'").withOptimizer(optimizer)
            .hasExpectedSysCallCount(1);
    List<String> output = result.environment().output();
    assertThat(output.get(0)).isEqualTo("hello\nworld");
  }

  @Test
  public void printlnTwoStrings() {
    InterpreterResult result =
        assertThatInterpreting("println 'hello' println 'world'").withOptimizer(optimizer)
            .hasExpectedSysCallCount(1);

    SysCall call = (SysCall) result.code().get(0);
    assertThat(call.call()).isEqualTo(SysCall.Call.PRINTLN);
    List<String> output = result.environment().output();
    assertThat(output.get(0)).isEqualTo("hello\nworld");
  }

  @Test
  public void printlnInts() {
    InterpreterResult result =
        assertThatInterpreting("println 3 print 4").withOptimizer(new ILOptimizer(
            ImmutableList.of(
                // need this to allow the "adjacent" test to work
                new NopOptimizer(),
                // need this to propagate the __temp1=3+println __temp1 to println 3
                new ConstantPropagationOptimizer(0),
                // need this to get rid of dead temp assignments
                new DeadAssignmentOptimizer(0),
                // need this to convert println 3 to println "3"
                new ArithmeticOptimizer(2),
                new PrintOptimizer(2)),
            0)).hasExpectedSysCallCount(1);

    List<String> output = result.environment().output();
    assertThat(output.get(0)).isEqualTo("3\n4");
  }

  @Test
  public void technicallyNotTwoInARowButStillCounts() {
    assertThatInterpreting("println 'hello'").withOptimizer(optimizer).hasExpectedSysCallCount(1);
  }

  @Test
  public void printConstantBool(@TestParameter boolean val) {
    InterpreterResult result =
        assertThatInterpreting(String.format("print %s", val)).withOptimizer(optimizer)
            .hasSameVariables();

    ImmutableList<Op> code = result.code();
    SysCall first = (SysCall) code.get(0);
    ConstantOperand<?> arg = (ConstantOperand<?>) first.arg();
    assertThat(arg.value()).isEqualTo(String.valueOf(val));
  }

  @Test
  public void printConstantInt() {
    InterpreterResult result =
        assertThatInterpreting("print 3").withOptimizer(optimizer).hasSameVariables();

    ImmutableList<Op> code = result.code();
    SysCall first = (SysCall) code.get(0);
    ConstantOperand<?> arg = (ConstantOperand<?>) first.arg();
    assertThat(arg.value()).isEqualTo("3");
  }

  @Test
  public void printConstantLong() {
    InterpreterResult result =
        assertThatInterpreting("print 3L").withOptimizer(optimizer).hasSameVariables();

    ImmutableList<Op> code = result.code();
    SysCall first = (SysCall) code.get(0);
    ConstantOperand<?> arg = (ConstantOperand<?>) first.arg();
    assertThat(arg.value()).isEqualTo("3L");
  }

  @Test
  public void printConstantNull() {
    InterpreterResult result =
        assertThatInterpreting("print null").withOptimizer(optimizer).hasSameVariables();

    ImmutableList<Op> code = result.code();
    SysCall first = (SysCall) code.get(0);
    ConstantOperand<?> arg = (ConstantOperand<?>) first.arg();
    assertThat(arg.value()).isEqualTo("null");
  }

  @Test
  public void printConstantByte() {
    InterpreterResult result =
        assertThatInterpreting("print 0y03").withOptimizer(optimizer).hasSameVariables();

    ImmutableList<Op> code = result.code();
    SysCall first = (SysCall) code.get(0);
    ConstantOperand<?> arg = (ConstantOperand<?>) first.arg();
    assertThat(arg.value()).isEqualTo("0y03");
  }
}
