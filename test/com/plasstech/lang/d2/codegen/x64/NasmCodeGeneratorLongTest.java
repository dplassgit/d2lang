package com.plasstech.lang.d2.codegen.x64;

import static com.plasstech.lang.d2.codegen.x64.testing.ExecutionSubject.assertThatCompiling;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;

@RunWith(TestParameterInjector.class)
public class NasmCodeGeneratorLongTest {
  @Test
  public void longUnaryOps(@TestParameter({"-", "!"}) String op) throws Exception {
    assertThatCompiling(String.format("a=3L b=%sa println b", op)).executedEqualsInterpreted();
  }

  @Test
  public void longBinOps(
      @TestParameter({"+", "-", "*", "&", "|", "^", "/" /* , "%" */ }) String op,
      @TestParameter({"12345678901234", "-234567"}) String first,
      @TestParameter({"-12345678901234", "234567"}) String second)
      throws Exception {
    String program =
        String.format(
            "      a=%sL "
                + "b=%sL "
                + "c=a %s 3L println c " // this should fail
                + "c=a %s b println c "
                + "d=b %s a println d "
                + "e=a %s a println e "
                + "f=b %s b println f",
            first, second, op, op, op, op, op);
    assertThatCompiling(program).executedEqualsInterpreted();
  }

  @Test
  public void longCompOps(
      @TestParameter({"<=", "!=", ">"}) String op,
      @TestParameter({"12345678901234", "-12345"}) String first,
      @TestParameter({"123456", "-3456778901234"}) String second)
      throws Exception {
    assertThatCompiling(String.format(
        "      a=%sL b=%sL " //
            + "c=a %s b println c " //
            + "d=b %s a println d " //
            + "e=a %s %sL println e " //
            + "f=%sL %s b println f",
        first, second, op, op, op, second, first, op)).executedEqualsInterpreted();
  }

  @Test
  public void longCompOpsArgs(
      @TestParameter({"<", "==", ">="}) String op,
      @TestParameter({"12345678901234", "-12345"}) String first,
      @TestParameter({"123456", "-3456778901234"}) String second)
      throws Exception {
    String program =
        String.format(
            "    bb:long g:bool f:proc(a:long, b:long) { " //
                + "aa=a " // to test locals vs args.
                + "c=a %s b println c " //
                + "d=b %s aa println d " //
                + "e=aa %s %sL println e " //
                + "g=%sL %s aa println g " // global g
                + "bb=b "
                + "h=bb < 3L println h"
                + "} "
                + "f(%sL, %sL)",
            op, op, op, first, second, op, first, second);
    assertThatCompiling(program).executedEqualsInterpreted();
  }

  @Test
  public void shiftOps() throws Exception {
    String op = ">>";
    assertThatCompiling(
        String.format("a=123L b=4L c=a%sb println c a=-234L d=b%sa println d", op, op))
        .executedEqualsInterpreted();
  }

  @Test
  public void shiftConstant() throws Exception {
    String op = "<<";
    assertThatCompiling(
        String.format("a=123L c=a %s 4L println c a=-234L d=a %s 4L println d", op, op))
        .executedEqualsInterpreted();
  }

  @Test
  public void shiftOpsProc(@TestParameter({"<<", ">>"}) String op) throws Exception {
    String program =
        String.format(
            "f:proc(a:long) {b=4L b=b %s a println a a=a%sb println a c=a<<2L println c} f(2L)",
            op, op);
    assertThatCompiling(program).executedEqualsInterpreted();
  }

  @Test
  public void shiftSelf() throws Exception {
    assertThatCompiling("f:proc(a:long) {a=a<<a println a} f(2L)").executedEqualsInterpreted();
  }

  @Test
  public void rounding() throws Exception {
    assertThatCompiling("f=6 k=4/(5+(4-5*f)) println k").executedEqualsInterpreted();
  }

  @Test
  public void divLoop() throws Exception {
    assertThatCompiling("a=10000L while a > 0L {println a a = a / 10L }")
        .executedEqualsInterpreted();
  }

  @Test
  public void incDec() throws Exception {
    assertThatCompiling("a=3L a-- println a a++ println a").executedEqualsInterpreted();
  }

  @Test
  public void divisionByZeroGlobal() throws Exception {
    String sourceCode = "a=0L b=1L/a";
    assertThatCompiling(sourceCode).hasCompileTimeError("Division by 0");
  }

  @Test
  public void divisionByZeroLocal() throws Exception {
    String sourceCode = "f:proc:long {a=0L b=1L/a return b} f()";
    assertThatCompiling(sourceCode).hasCompileTimeError("Division by 0");
    assertThatCompiling(sourceCode).withOptimize(false).withRuntimeError("Division by 0")
        .executes();
  }
}
