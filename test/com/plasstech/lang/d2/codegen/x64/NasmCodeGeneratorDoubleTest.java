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
  public void assignConstant() throws Exception {
    execute("a=3.4", "assignConstant");
  }

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
    execute("print 3.4", "printDoubleConstant");
  }

  @Test
  public void printDouble() throws Exception {
    execute("a=3.4 b=a println b println a", "printDouble");
  }

  @Test
  public void transfer() throws Exception {
    execute("a=3.0 b=a", "transfer");
  }

  @Test
  public void transferLocal() throws Exception {
    execute("f:proc { a=3.0 b=a print b} f()", "transferLocal");
  }

  @Test
  public void addToItself() throws Exception {
    execute("a=3.1 a=a+10.1 print a", "addToItself");
  }

  @Test
  public void doubleAdd() throws Exception {
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
      @TestParameter({"0.0", "1234.5", "-34567.8"}) double first,
      @TestParameter({"0.0", "-1234.5", "34567.8"}) double second)
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
  public void tree() throws Exception {
    execute(
        "      a=2.0 "
            + "b=3.0 "
            + "c=-5.0 "
            + "d=7.0 "
            + "e=11.0 "
            + "f=13.0 "
            + "g = (((a+b+c+2.0)*(b+-c+d+1.0))*((c-d-e-1.0)/(d-e-f-2.0))*((e+f+a-3.0)*"
            + "      (f-a-b+4.0)))*((a+c+e-9.0)/(b-d-f+1.01)) "
            + "print g",
        "tree");
  }

  @Test
  public void allOpsGlobals() throws Exception {
    execute(
        "      a=2.0 "
            + "b=3.0 "
            + "c=-5.0 "
            + "d=7.0 "
            + "e=11.0 "
            + "f=13.0 "
            + "z=0.0 "
            + " g=a+a*(b+(b+c*(d-(c+d/(-e+(d-e*f)+a)*b)/-c)-d)) println g"
            + " k=z+4.0/(5.0+(4.0-5.0*-f)) println k"
            + " k=0.0+-d/(5.0+(4.0-5.0*f)) println k"
            + " g=a+a*(b+(b+c*(d-(c+d/(e+(d-e*f)))))) println g"
            + " h=0.0+a+(4.0+3.0*(4.0-(3.0+4.0/(4.0+(5.0-e*6.0))))) println h"
            + " j=a+a*(b+(b+c*(d-(c+d/(e+(d-e*f+0.0)))))) println j"
            + " aa=2.0+a*(3.0+(3.0+5.0*(7.0-(5.0+8.0/11.0)+(7.0-11.0*13.0))*2.0)/b) println aa",
        "allOpsGlobal");
  }

  @Test
  public void rounding() throws Exception {
    execute("f=6.0 k=4.0/(5.0+(4.0-5.0*f)) print k", "rounding");
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
            + " g=a+a*(b+(b+c*(d-(c+d/(-e+(d-e*f)+a)*b)/-c)-d)) print 'g: ' println g"
            + " k=z+4.0/(5.0+(4.0-5.0*-f)) print 'k: ' println k"
            + " k=0.0+-d/(5.0+(4.0-5.0*f)) print 'k: ' println k"
            + " g=a+a*(b+(b+c*(d-(c+d/(e+(d-e*f)))))) print 'g: ' println g"
            + " h=0.0+a+(4.0+3.0*(4.0-(3.0+4.0/(4.0+(5.0-e*6.0))))) print 'h: ' println h"
            + " j=a+a*(b+(b+c*(d-(c+d/(e+(d-e*f+0.0)))))) print 'j: ' println j"
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
  public void constantZero() throws Exception {
    String tod =
        "DS=[0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0]\r\n"
            + "\r\n"
            + "// int to double.\r\n"
            + "tod: proc(i: int): double {\r\n"
            + "  neg = false\r\n"
            + "  if i < 0 {neg = true i = -i}\r\n"
            + "  d=0.0\r\n"
            + "  place = 1.0\r\n"
            + "  while i > 0 {\r\n"
            + "    last = i%10\r\n"
            + "    d = d + place * DS[last]\r\n"
            + "    place = place * 10.0\r\n"
            + "    i = i / 10\r\n"
            + "  }\r\n"
            + "  if neg {return -d}\r\n"
            + "  return d\r\n"
            + "}\r\n"
            + "print \"Should be 2.0:\" println tod(2)\r\n"
            + "print \"Should be 0.0:\" println tod(0)\r\n";
    execute(tod, "tod");
  }

  @Test
  public void paramPlusConstant() throws Exception {
    execute("f:proc(d:double):double { d = d + 1.0 return d} print f(2.0)", "paramPlusConstant");
  }

  @Test
  public void paramPlusLocal() throws Exception {
    execute("f:proc(d:double):double { e=1.0 d = d + e return d} print f(2.0)",
        "paramPlusLocal");
  }

  @Test
  public void localPlusParam() throws Exception {
    execute("f:proc(d:double):double { e=1.0 e = e + d return e} print f(2.0)",
        "localPlusParam");
  }

  @Test
  public void localPlusConstant() throws Exception {
    execute("f:proc(d:double):double { e = d + 1.0 return e} print f(2.0)", "localPlusConstant");
  }

  @Test
  @Ignore("bug 273")
  public void printingTrailingDotZero() throws Exception {
    String stdOut = execute("print 3.0", "bug273");
    assertThat(stdOut).isEqualTo("3.0");
  }
}
