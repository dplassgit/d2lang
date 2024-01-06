package com.plasstech.lang.d2.codegen.x64;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;

@RunWith(TestParameterInjector.class)
public class NasmCodeGeneratorByteTest extends NasmCodeGeneratorTestBase {
  @Test
  public void byteUnaryOps(@TestParameter({"-", "!"}) String op) throws Exception {
    execute(String.format("a=0y3 b=%sa print b", op), "UnaryOps");
  }

  @Test
  public void byteBinOps(
      @TestParameter({"+", "-", "*", "/", "&", "|", "^", "%"}) String op,
      @TestParameter({"0y34", "0yf3"}) String first,
      @TestParameter({"0y12", "0ye3"}) String second)
      throws Exception {
    execute(
        String.format(
            "      a=%s b=%s c=a %s b println c "
                + "d=b %s a println d "
                + "e=a %s a println e "
                + "f=b %s b println f",
            first, second, op, op, op, op),
        "byteBinOps");
  }

  @Test
  public void byteMul() throws Exception {
    execute("f:proc {a=0y3e b=0y2 c=a * b print c} f()", "byteMul2");
    execute("f:proc(a:byte, b:byte) {c=a * b print c} f(0y3e, 0y2)", "byteMul3");
    execute("b=0y2 f:proc(a:byte) {c=a * b print c} f(0y3e)", "byteMul4");
  }

  @Test
  public void byteDiv() throws Exception {
    execute("f:proc {a=0y3e b=0y0a c=a / b print c} f()", "byteDiv2");
    execute("f:proc(a:byte, b:byte) {c=a / b print c} f(0y3e, 0yf2)", "byteDiv3");
    execute("b=0yf2 f:proc(a:byte) {c=a / b print c} f(0y3e)", "byteDiv4");
  }

  @Test
  public void byteCompOps(
      @TestParameter({"<", "<=", "==", "!=", ">=", ">"}) String op,
      @TestParameter({"0y34", "0yf7"}) String first,
      @TestParameter({"0y34", "0yf7"}) String second)
      throws Exception {
    execute(
        String.format(
            "      a=%s b=%s " //
                + "c=a %s b print c " //
                + "d=b %s a print d",
            first, second, op, op),
        "byteCompOps");
  }

  @Test
  public void shiftOps(@TestParameter({"<<", ">>"}) String op) throws Exception {
    execute(
        String.format("a=0y23 b=0y4 c=a %s b print c a=0yF4 d=b %s a print d", op, op),
        "byteShiftOps");
  }

  @Test
  public void rounding() throws Exception {
    execute("f=0y6 k=0y4/(0y5+(0y4-0y5*f)) print k", "rounding");
  }

  @Test
  public void incDec() throws Exception {
    execute("a=0y42 a++ print a a=0y41 a-- print a", "incDec");
  }

  @Test
  public void bug32() throws Exception {
    execute(
        "p:proc() {\r\n"
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
            + "p()\r\n",
        "bug32");
  }

  @Test
  public void divisionByZeroGlobal() throws Exception {
    String sourceCode = "a=0y0 b=0y1/a";
    assertGenerateError(sourceCode, "Division by 0");
    assertRuntimeError(sourceCode, "divisionByZeroLocal", "Division by 0");
  }

  @Test
  public void divisionByZeroLocal() throws Exception {
    String sourceCode = "f:proc:byte {a=0y0 b=0y1/a return b} f()";
    assertGenerateError(sourceCode, "Division by 0");
    assertRuntimeError(sourceCode, "divisionByZeroLocal", "Division by 0");
  }
}
