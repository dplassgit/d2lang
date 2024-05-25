package com.plasstech.lang.d2.codegen.x64;

import static com.google.common.truth.Truth.assertThat;
import static com.plasstech.lang.d2.codegen.x64.testing.ExecutionSubject.assertThatCompiling;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.plasstech.lang.d2.phase.State;

@RunWith(TestParameterInjector.class)
public class NasmCodeGeneratorDoubleTest {

  @Test
  public void negate() throws Exception {
    assertThatCompiling("a=3.1 b=-a println b").executedEqualsInterpreted();
  }

  @Test
  public void negateParam() throws Exception {
    assertThatCompiling("f:proc(a:double) { b=-a println b} f(1.2)").executedEqualsInterpreted();
  }

  @Test
  public void negateLocal() throws Exception {
    assertThatCompiling("f:proc(a:double) { b=a+1.0 c=-b d=-c println d} f(1.2)")
        .executedEqualsInterpreted();
  }

  @Test
  public void printDoubleConstant() throws Exception {
    assertThatCompiling("println 3.4").executedEqualsInterpreted();
  }

  @Test
  public void transferLocal() throws Exception {
    assertThatCompiling("f:proc { a=3.0 b=a println b} f()").executedEqualsInterpreted();
  }

  @Test
  public void addToItself() throws Exception {
    assertThatCompiling("a=3.1 a=a+10.1 println a").executedEqualsInterpreted();
  }

  @Test
  public void add() throws Exception {
    assertThatCompiling("a=3.14 b=2.0 c=a+b println c").executedEqualsInterpreted();
  }

  @Test
  public void doubleBinOps(
      @TestParameter({"+", "-", "*", "/"}) String op,
      @TestParameter({"1234.5", "-2348.3"}) double first,
      @TestParameter({"-1234.5", "2348.3"}) double second)
      throws Exception {
    assertThatCompiling(String.format(
        "a=%f b=%f c=a %s b println c d=b %s a println d e=a %s a println e f=b %s b println f",
        first, second, op, op, op, op)).executedEqualsInterpreted();
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
        first, second, op, op)).executedEqualsInterpreted();
  }

  @Test
  public void rounding() throws Exception {
    assertThatCompiling("f=6.0 k=4.0/(5.0+(4.0-5.0*f)) println k").executedEqualsInterpreted();
  }

  @Test
  public void allOpsLocals() throws Exception {
    assertThatCompiling("f1:proc(a:double, b:double): double { \n"
        + "a=2.0 "
        + "b=3.0 "
        + "c=-5.0 "
        + "d=7.0 "
        + "e=11.0 "
        + "f=13.0 "
        + "z=0.0 "
        + " g=a+a*(b+(b+c*(d-(c+d/(-e+(d-e*f)+a)*b)/-c)-d)) println 'g: ' println g"
        + " k=z+4.0/(5.0+(4.0-5.0*-f)) println 'k: ' println k"
        + " k=0.0+-d/(5.0+(4.0-5.0*f)) println 'k: ' println k"
        + " g=a+a*(b+(b+c*(d-(c+d/(e+(d-e*f)))))) println 'g: ' println g"
        + " h=0.0+a+(4.0+3.0*(4.0-(3.0+4.0/(4.0+(5.0-e*6.0))))) println 'h: ' println h"
        + " j=a+a*(b+(b+c*(d-(c+d/(e+(d-e*f+0.0)))))) println 'j: ' println j"
        + " aa=2.0+a*(3.0+(3.0+5.0*(7.0-(5.0+8.0/11.0)+(7.0-11.0*13.0))*2.0)/b) println aa"
        + " return aa "
        + "} \n"
        + "f2:proc(a:double, b:double):double { return f1(b, a) } "
        + "println f2(1.0, 2.0)").executedEqualsInterpreted();
  }

  @Test
  public void divisionByZeroGlobal() throws Exception {
    String sourceCode = "a=0.0 b=1.0/a";
    assertThatCompiling(sourceCode).hasCompileTimeError("Division by 0");
    assertThatCompiling(sourceCode).withOptimize(false).withRuntimeError("Division by 0")
        .executes();
  }

  @Test
  public void divisionByZeroLocal(@TestParameter boolean optimize) throws Exception {
    assertThatCompiling("f:proc:double{a=0.0 b=1.0/a return b} f()")
        .withOptimize(optimize)
        .withRuntimeError("Division by 0")
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
    State state = assertThatCompiling(sqrt).executes();
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
    State state = assertThatCompiling(sqrt).executes();
    assertThat(state.stdOut()).isEqualTo(
        "bextern Should be 153.045745: 153.0457447954696\r\n");
  }

  @Test
  public void paramPlusConstant() throws Exception {
    assertThatCompiling("f:proc(d:double):double { d = d + 1.0 return d} println f(2.0)")
        .executedEqualsInterpreted();
  }

  @Test
  public void localPlusParam() throws Exception {
    assertThatCompiling("f:proc(d:double):double { e=1.0 e = e + d return e} println f(2.0)")
        .executedEqualsInterpreted();
  }

  @Test
  @Ignore("bug 273")
  public void printingTrailingDotZero() throws Exception {
    State state = assertThatCompiling("println 3.0").executedEqualsInterpreted();
    assertThat(state.stdOut()).isEqualTo("3.0");
  }
}
