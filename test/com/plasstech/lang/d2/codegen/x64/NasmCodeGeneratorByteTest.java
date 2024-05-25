package com.plasstech.lang.d2.codegen.x64;

import static com.plasstech.lang.d2.codegen.x64.testing.ExecutionSubject.assertThatCompiling;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;

@RunWith(TestParameterInjector.class)
public class NasmCodeGeneratorByteTest {
  @Test
  public void byteUnaryOps(@TestParameter({"-", "!"}) String op) throws Exception {
    assertThatCompiling(String.format("a=0y3 b=%sa print b", op)).executedEqualsInterpreted();
  }

  @Test
  public void byteBinOps(
      @TestParameter({"+", "-", "*", "/", "&", "|", "^", "%"}) String op,
      @TestParameter({"0y34", "0yf3"}) String first,
      @TestParameter({"0y12", "0ye3"}) String second)
      throws Exception {
    assertThatCompiling(String.format(
        "      a=%s b=%s c=a %s b println c "
            + "d=b %s a println d "
            + "e=a %s a println e "
            + "f=b %s b println f",
        first, second, op, op, op, op)).executedEqualsInterpreted();
  }

  @Test
  public void byteMul() throws Exception {
    assertThatCompiling("f:proc {a=0y3e b=0y2 c=a * b print c} f()").executedEqualsInterpreted();
    assertThatCompiling("f:proc(a:byte, b:byte) {c=a * b print c} f(0y3e, 0y2)")
        .executedEqualsInterpreted();
    assertThatCompiling("b=0y2 f:proc(a:byte) {c=a * b print c} f(0y3e)")
        .executedEqualsInterpreted();
  }

  @Test
  public void byteDiv() throws Exception {
    assertThatCompiling("f:proc {a=0y3e b=0y0a c=a / b print c} f()").executedEqualsInterpreted();
    assertThatCompiling("f:proc(a:byte, b:byte) {c=a / b print c} f(0y3e, 0yf2)")
        .executedEqualsInterpreted();
    assertThatCompiling("b=0yf2 f:proc(a:byte) {c=a / b print c} f(0y3e)")
        .executedEqualsInterpreted();
  }

  @Test
  public void byteCompOps(
      @TestParameter({"<", "<=", "==", "!=", ">=", ">"}) String op,
      @TestParameter({"0y34", "0yf7"}) String first,
      @TestParameter({"0y34", "0yf7"}) String second)
      throws Exception {
    assertThatCompiling(String.format(
        "      a=%s b=%s " //
            + "c=a %s b print c " //
            + "d=b %s a print d",
        first, second, op, op)).executedEqualsInterpreted();
  }

  @Test
  public void shiftOps(@TestParameter({"<<", ">>"}) String op) throws Exception {
    assertThatCompiling(
        String.format("a=0y23 b=0y4 c=a %s b print c a=0yF4 d=b %s a print d", op, op))
        .executedEqualsInterpreted();
  }

  @Test
  public void rounding() throws Exception {
    assertThatCompiling("f=0y6 k=0y4/(0y5+(0y4-0y5*f)) print k").executedEqualsInterpreted();
  }

  @Test
  public void incDec() throws Exception {
    assertThatCompiling("a=0y42 a++ print a a=0y41 a-- print a").executedEqualsInterpreted();
  }

  @Test
  public void bug32() throws Exception {
    assertThatCompiling("p:proc() {\r\n"
        + "  a:byte\r\n"
        + "  a=0y3\r\n"
        + "  a=-0y3\r\n"
        + "  a=-0y3\r\n"
        + "  a=-+-0y3\r\n"
        + "  a=+0y3+-0y3\r\n"
        + "  a=+0y3\r\n"
        + "  b=a // 3\r\n"
        + "  a=(0y3+a)*-b // (3+3)*-3 = 6*-3=-18, ruh roh.\r\n"
        + "  b=+a\r\n"
        + "  b=-a\r\n"
        + "\r\n"
        + "  println a\r\n"
        + "  println 0y3+a*-b // 3+(-18*18)\r\n"
        + "  println (0y3+a)*-b\r\n"
        + "  println 0y4%0y6\r\n"
        + "  println 0y7f"
        + "}\r\n"
        + "p()\r\n").executedEqualsInterpreted();
  }

  @Test
  public void divisionByZeroLocal() throws Exception {
    String sourceCode = "f:proc:byte {a=0y0 b=0y1/a return b} f()";
    assertThatCompiling(sourceCode).withRuntimeError("Division by 0").executes();
    assertThatCompiling(sourceCode).withOptimize(false).withRuntimeError("Division by 0")
        .executes();
  }
}
