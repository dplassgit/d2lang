package com.plasstech.lang.d2.codegen.x64;

import static com.google.common.truth.Truth.assertThat;
import static com.plasstech.lang.d2.codegen.x64.testing.ExecutionSubject.assertThatCompiling;
import static org.junit.Assume.assumeTrue;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.plasstech.lang.d2.phase.State;

@RunWith(TestParameterInjector.class)
public class NasmCodeGeneratorDoubleTest {
  @TestParameter
  boolean optimize;

  @Test
  public void negate() throws Exception {
    assertThatCompiling("a=3.1 b=-a println b").withOptimize(optimize).executedEqualsInterpreted();
  }

  @Test
  public void negateParam() throws Exception {
    assertThatCompiling("f:proc(a:double) { b=-a println b} f(1.2)").withOptimize(optimize)
        .executedEqualsInterpreted();
  }

  @Test
  public void negateLocal() throws Exception {
    assertThatCompiling("f:proc(a:double) { b=a+1.0 c=-b d=-c println d} f(1.2)")
        .withOptimize(optimize).executedEqualsInterpreted();
  }

  @Test
  public void printDoubleConstant() throws Exception {
    assertThatCompiling("println 3.4").withOptimize(optimize).executedEqualsInterpreted();
  }

  @Test
  public void transferLocal() throws Exception {
    assertThatCompiling("f:proc { a=3.0 b=a println b} f()").withOptimize(optimize)
        .executedEqualsInterpreted();
  }

  @Test
  public void addToItself() throws Exception {
    assertThatCompiling("a=3.1 a=a+10.1 println a").withOptimize(optimize)
        .executedEqualsInterpreted();
  }

  @Test
  public void add() throws Exception {
    assertThatCompiling("a=3.14 b=2.0 c=a+b println c").withOptimize(optimize)
        .executedEqualsInterpreted();
  }

  @Test
  public void doubleBinOps(
      @TestParameter({"+", "-", "*", "/"}) String op,
      @TestParameter({"1234.5", "-2348.3"}) double first,
      @TestParameter({"-1234.5", "2348.3"}) double second)
      throws Exception {
    assertThatCompiling(String.format(
        "a=%f b=%f c=a %s b println c d=b %s a println d e=a %s a println e f=b %s b println f",
        first, second, op, op, op, op)).withOptimize(optimize).executedEqualsInterpreted();
  }

  @Test
  public void doubleCompOps(
      @TestParameter({"<", "<=", "==", "!=", ">=", ">"}) String op,
      @TestParameter({"1234.5", "-34567.8"}) double first,
      @TestParameter({"-1234.5", "34567.8"}) double second)
      throws Exception {
    assertThatCompiling(String.format(
        "      a=%f b=%f " //
            + "c=a %s b println c " //
            + "d=b %s a println d",
        first, second, op, op)).withOptimize(optimize).executedEqualsInterpreted();
  }

  @Test
  public void rounding() throws Exception {
    assertThatCompiling("f=6.0 k=4.0/(5.0+(4.0-5.0*f)) println k").withOptimize(optimize)
        .executedEqualsInterpreted();
  }

  @Test
  public void divisionByZeroGlobal() throws Exception {
    assumeTrue(optimize);
    String sourceCode = "a=0.0 b=1.0/a";
    assertThatCompiling(sourceCode).withOptimize(true).hasCompileTimeError("Division by 0");
    assertThatCompiling(sourceCode).withOptimize(false).withRuntimeError("Division by 0")
        .executes();
  }

  @Test
  public void divisionByZeroLocal() throws Exception {
    assumeTrue(optimize);
    String sourceCode = "f:proc:double{a=0.0 b=1.0/a return b} f()";
    assertThatCompiling(sourceCode).withOptimize(true).hasCompileTimeError("Division by 0");
    assertThatCompiling(sourceCode).withOptimize(false).withRuntimeError("Division by 0")
        .executes();
  }

  @Test
  public void stackAlignment() throws Exception {
    String sqrt =
        "      sqrt: extern proc(d:double):double "
            + "asqrt: proc(d:double):double {return sqrt(d)} "
            + "bsqrt: proc(d:double):double {f=sqrt(d) return f} "
            + "csqrt: proc(d:double):double {e=d f=sqrt(e) return f} "
            + "dd:double dsqrt: proc(d:double):double {dd=sqrt(d) return dd} "
            + "print 'aextern Should be 153.045745: ' println asqrt(23423.0) "
            + "print 'bextern Should be 153.045745: ' println bsqrt(23423.0) "
            + "print 'cextern Should be 153.045745: ' println csqrt(23423.0) "
            + "print 'dextern Should be 153.045745: ' println dsqrt(23423.0) ";
    State state = assertThatCompiling(sqrt).withOptimize(optimize).executes();
    assertThat(state.stdOut()).isEqualTo(
        "aextern Should be 153.045745: 153.0457447954696\r\n"
            + "bextern Should be 153.045745: 153.0457447954696\r\n"
            + "cextern Should be 153.045745: 153.0457447954696\r\n"
            + "dextern Should be 153.045745: 153.0457447954696\r\n");
  }

  @Test
  public void stackAlignment2() throws Exception {
    String sqrt =
        "      sqrt: extern proc(d:double):double "
            + "bsqrt: proc(a:bool, d:double):double {b=a f=sqrt(d) return f} "
            + "print 'bextern Should be 153.045745: ' println bsqrt(false, 23423.0) ";
    State state = assertThatCompiling(sqrt).withOptimize(optimize).executes();
    assertThat(state.stdOut()).isEqualTo(
        "bextern Should be 153.045745: 153.0457447954696\r\n");
  }

  @Test
  public void paramPlusConstant() throws Exception {
    assertThatCompiling("f:proc(d:double):double { d = d + 1.0 return d} println f(2.0)")
        .withOptimize(optimize).executedEqualsInterpreted();
  }

  @Test
  public void localPlusParam() throws Exception {
    assertThatCompiling("f:proc(d:double):double { e=1.0 e = e + d return e} println f(2.0)")
        .withOptimize(optimize).executedEqualsInterpreted();
  }

  @Test
  @Ignore("bug 273")
  public void printingTrailingDotZero() throws Exception {
    State state =
        assertThatCompiling("println 3.0").withOptimize(optimize).executedEqualsInterpreted();
    assertThat(state.stdOut()).isEqualTo("3.0");
  }
}
