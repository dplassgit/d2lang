package com.plasstech.lang.d2.codegen.x64;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;

@RunWith(TestParameterInjector.class)
public class NasmCodeGeneratorDoubleTest extends NasmCodeGeneratorTestBase {

  @Test
  public void negate() throws Exception {
    execute("a=3.1 b=-a println b", "negate");
  }

  @Test
  public void negateParam() throws Exception {
    execute("f:proc(a:double) { b=-a println b} f(1.2)", "negateParam");
  }

  @Test
  public void negateLocal() throws Exception {
    execute("f:proc(a:double) { b=a+1.0 c=-b d=-c println d} f(1.2)", "negateLocal");
  }

  @Test
  public void printDoubleConstant() throws Exception {
    execute("println 3.4", "printDoubleConstant");
  }

  @Test
  public void transferLocal() throws Exception {
    execute("f:proc { a=3.0 b=a println b} f()", "transferLocal");
  }

  @Test
  public void addToItself() throws Exception {
    execute("a=3.1 a=a+10.1 println a", "addToItself");
  }

  @Test
  public void add() throws Exception {
    execute("a=3.14 b=2.0 c=a+b println c", "doubleAdd");
  }

  @Test
  public void doubleBinOps(
      @TestParameter({"+", "-", "*", "/"}) String op,
      @TestParameter({"1234.5", "-2348.3"}) double first,
      @TestParameter({"-1234.5", "2348.3"}) double second)
      throws Exception {
    execute(
        String.format(
            "a=%f b=%f c=a %s b println c d=b %s a println d e=a %s a println e f=b %s b println f",
            first, second, op, op, op, op),
        "doubleBinOps");
  }

  @Test
  public void doubleCompOps(
      @TestParameter({"<", "<=", "==", "!=", ">=", ">"}) String op,
      @TestParameter({"1234.5", "-34567.8"}) double first,
      @TestParameter({"-1234.5", "34567.8"}) double second)
      throws Exception {
    execute(
        String.format(
            "      a=%f b=%f " //
                + "c=a %s b println c " //
                + "d=b %s a println d",
            first, second, op, op),
        "doubleCompOps");
  }

  @Test
  public void rounding() throws Exception {
    execute("f=6.0 k=4.0/(5.0+(4.0-5.0*f)) println k", "rounding");
  }

  @Test
  public void allOpsLocals() throws Exception {
    execute(
        "f1:proc(a:double, b:double): double { \n"
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
            + "println f2(1.0, 2.0)",
        "allOpsLocals");
  }

  @Test
  public void divisionByZeroGlobal() throws Exception {
    String program = "a=0.0 b=1.0/a";
    assertGenerateError(program, "Division by 0");
    assertRuntimeError(program, "divisionByZeroLocal", "Division by 0");
  }

  @Test
  public void divisionByZeroLocal() throws Exception {
    String program = "f:proc:double{a=0.0 b=1.0/a return b} f()";
    assertGenerateError(program, "Division by 0");
    assertRuntimeError(program, "divisionByZeroLocal", "Division by 0");
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
    // note: cannot use "execute" because that compares interpreter to compiled, and
    // the interpreter can't deal with externs.
    assertThat(compile(sqrt, "sqrt").stdOut()).isEqualTo(
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
    assertThat(compile(sqrt, "sqrt").stdOut()).isEqualTo(
        "bextern Should be 153.045745: 153.0457447954696\r\n");
  }

  @Test
  public void paramPlusConstant() throws Exception {
    execute("f:proc(d:double):double { d = d + 1.0 return d} println f(2.0)", "paramPlusConstant");
  }

  @Test
  public void localPlusParam() throws Exception {
    execute("f:proc(d:double):double { e=1.0 e = e + d return e} println f(2.0)",
        "localPlusParam");
  }

  @Test
  @Ignore("bug 273")
  public void printingTrailingDotZero() throws Exception {
    String stdOut = execute("println 3.0", "bug273");
    assertThat(stdOut).isEqualTo("3.0");
  }
}
