package com.plasstech.lang.d2.codegen;

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
  public void byteDivByByte() throws Exception {
    execute("a=0yf3 e= a / a println e ", "byteDivByByte");
  }

  @Test
  public void byteMul() throws Exception {
    execute("a=0y3e b=0y2 c=a * b print c", "byteMul1");
    execute("f:proc {a=0y3e b=0y2 c=a * b print c} f()", "byteMul2");
    execute("f:proc(a:byte, b:byte) {c=a * b print c} f(0y3e, 0y2)", "byteMul3");
    execute("b=0y2 f:proc(a:byte) {c=a * b print c} f(0y3e)", "byteMul4");
  }

  @Test
  public void byteDivGlobals() throws Exception {
    execute("a=0y3e b=0y2 c=a / b print c", "byteDiv1");
  }

  @Test
  public void byteDivLocals() throws Exception {
    execute("f:proc {a=0y3e b=0y0a c=a / b print c} f()", "byteDiv2");
  }

  @Test
  public void byteDivLocalsNeg() throws Exception {
    execute("f:proc {a=0yfe b=0y0a c=a / b print c} f()", "byteDiv2");
  }

  @Test
  public void byteDivParams() throws Exception {
    execute("f:proc(a:byte, b:byte) {c=a / b print c} f(0y3e, 0yf2)", "byteDiv3");
  }

  @Test
  public void byteDivGlobalAndParam() throws Exception {
    execute("b=0yf2 f:proc(a:byte) {c=a / b print c} f(0y3e)", "byteDiv4");
  }

  @Test
  public void byteCompOps(
      @TestParameter({"<", "<=", "==", "!=", ">=", ">"}) String op,
      @TestParameter({"0y0", "0y34", "0yf7"}) String first,
      @TestParameter({"0y0", "0y34", "0yf7"}) String second)
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
  public void shiftConstant(@TestParameter({"<<", ">>"}) String op) throws Exception {
    execute(
        String.format("a=0y23 c=a %s 0y4 print c a=0yF4 d=a %s 0y4 print d", op, op),
        "byteShiftConstant");
  }

  @Test
  public void shiftOpsProc(@TestParameter({"<<", ">>"}) String op) throws Exception {
    execute(
        String.format(
            "f:proc(a:byte) {b=0y4 a=b %s a println a a=a %s b println a} f(0y2)", op, op),
        "byteShiftOpsProc");
  }

  @Test
  public void tree() throws Exception {
    execute(
        "      a=0y2 "
            + "b=0y3 "
            + "c=0yf5 "
            + "d=0y7 "
            + "e=0y11 "
            + "f=0y13 "
            + "g = (((a+b+c+0y2)*(b+c+d+0y1))*((c-d-e-0y1)/(d-e-f-0y2))*((e+f+a-0y3)*"
            + "      (f-a-b+0y4)))*((a+c+e-0y9)/(b-d-f+0y11)) "
            + "print g",
        "tree");
  }

  @Test
  public void allOpsGlobals() throws Exception {
    execute(
        "      a=0y2 "
            + "b=0y3 "
            + "c=0yf5 "
            + "d=0y7 "
            + "e=0y11 "
            + "f=0y13 "
            + "z=0y0"
            + " g=a+a*(b+(b+c*(d-(c+d/(e+(d-e*f)+a)*b)/c)-d)) print g"
            + " k=z+0y4/(0y5+(0y4-0y5*f)) print k"
            + " k=0y0+d/(0y5+(0y4-0y5*f)) print k"
            + " g=a+a*(b+(b+c*(d-(c+d/(e+(d-e*f)))))) print g"
            + " h=0y0+a+(0y4+0y3*(0y4-(0y3+0y4/(0y4+(0y5-e*0y6))))) print h"
            + " j=a+a*(b+(b+c*(d-(c+d/(e+(d-e*f+0y0)))))) print j"
            + " aa=0y2+a*(0y3+(0y3+0y5*(0y7-(0y5+0y7/0y11)+(0y7-0y11*0y13))*0y2)/b) print aa"
            + "",
        "allOpsGlobal");
  }

  @Test
  public void rounding() throws Exception {
    execute("f=0y6 k=0y4/(0y5+(0y4-0y5*f)) print k", "rounding");
  }

  @Test
  public void allOpsLocals() throws Exception {
    execute(
        "fun:proc(a:byte, b:byte):byte { \n"
            + "b=0y3 "
            + "c=0yf5 "
            + "d=0y7 "
            + "e=0y11 "
            + "f=0y13 "
            + "z=0y0"
            + " g=a+a*(b+(b+c*(d-(c+d/(e+(d-e*f)+a)*b)/c)-d)) print g"
            + " k=z+0y4/(0y5+(0y4-0y5*f)) print k"
            + " k=0y0+d/(0y5+(0y4-0y5*f)) print k"
            + " g=a+a*(b+(b+c*(d-(c+d/(e+(d-e*f)))))) print g"
            + " h=0y0+a+(0y4+0y3*(0y4-(0y3+0y4/(0y4+(0y5-e*0y6))))) print h"
            + " j=a+a*(b+(b+c*(d-(c+d/(e+(d-e*f+0y0)))))) print j"
            + " aa=0y2+a*(0y3+(0y3+0y5*(0y7-(0y5+0y7/0y11)+(0y7-0y11*0y13))*0y2)/b) print aa"
            + "   return aa\n"
            + "} \n"
            + "print fun(0y2, 0y3)",
        "allOpsLocals");
  }

  @Test
  public void addToItself() throws Exception {
    execute("a=0y3 a=a+0y10 print a", "addToItself");
  }

  @Test
  public void divLoopByte() throws Exception {
    execute("a=100 while a > 0 {print a a = a / 3 }", "divLoopByte");
  }

  @Test
  public void assign() throws Exception {
    execute("a=0y3 b=a a=0y4 print b print a", "assign");
  }

  @Test
  public void incDec() throws Exception {
    execute("a=0y42 a=a+0y1 print a a=0y41 a=a-0y1 print a", "incDec");
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
            + "}\r\n"
            + "main {\r\n"
            + "  p()\r\n"
            + "}\r\n",
        "bug32");
  }

  @Test
  public void divisionByZeroGlobal() throws Exception {
    assertRuntimeError("a=0y0 b=0y1/a", "divByZero", "Division by 0");
  }

  @Test
  public void divisionByZeroLocal() throws Exception {
    String sourceCode = "f:proc:byte {a=0y0 b=0y1/a return b} f()";
    if (optimize) {
      assertGenerateError(sourceCode, "Division by 0");
    } else {
      assertRuntimeError(sourceCode, "divisionByZeroLocal", "Division by 0");
    }
  }
}
