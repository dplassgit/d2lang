package com.plasstech.lang.d2.codegen;

import org.junit.Ignore;
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
      //          @TestParameter({"+", "-", "*", "/", "&", "|", "^", "%"}) String op,
      @TestParameter({"+", "-", "*", "&", "|", "^"}) String op,
      @TestParameter({"0y34", "0yf3"}) String first,
      @TestParameter({"0y12", "0ye3"}) String second)
      throws Exception {
    execute(
        String.format(
            "a=%s b=%s c=a %s b print c d=b %s a print d e=a %s a print e f=b %s b print f",
            first, second, op, op, op, op),
        "byteBinOps");
  }

  @Test
  public void byteMul() throws Exception {
    execute("a=0y3e b=0y2 c=a * b print c", "byteMul1");
    execute("f:proc {a=0y3e b=0y2 c=a * b print c} f()", "byteMul2");
    execute("f:proc(a:byte, b:byte) {c=a * b print c} f(0y3e, 0y2)", "byteMul3");
    execute("b=0y2 f:proc(a:byte) {c=a * b print c} f(0y3e)", "byteMul4");
  }

  @Test
  @Ignore
  public void byteDiv() throws Exception {
    execute("a=0y3e b=0y2 c=a / b print c", "byteDiv1");
    //    execute("f:proc {a=0y3e b=0y2 c=a / b print c} f()", "byteDiv2");
    //    execute("f:proc(a:byte, b:byte) {c=a / b print c} f(0y3e, 0y2)", "byteDiv3");
    //    execute("b=0y2 f:proc(a:byte) {c=a / b print c} f(0y3e)", "byteDiv4");
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
  @Ignore
  public void tree() throws Exception {
    execute(
        "      a=2 "
            + "b=3 "
            + "c=-5 "
            + "d=7 "
            + "e=11 "
            + "f=13 "
            + "g = (((a+b+c+2)*(b+c+d+1))*((c-d-e-1)/(d-e-f-2))*((e+f+a-3)*"
            + "      (f-a-b+4)))*((a+c+e-9)/(b-d-f+11)) "
            + "print g",
        "tree");
  }

  @Test
  @Ignore
  public void allOpsGlobals() throws Exception {
    execute(
        "      a=2 "
            + "b=3 "
            + "c=-5 "
            + "d=7 "
            + "e=11 "
            + "f=13 "
            + "z=0"
            + " g=a+a*(b+(b+c*(d-(c+d/(e+(d-e*f)+a)*b)/c)-d)) print g"
            + " k=z+4/(5+(4-5*f)) print k"
            + " k=0+d/(5+(4-5*f)) print k"
            + " g=a+a*(b+(b+c*(d-(c+d/(e+(d-e*f)))))) print g"
            + " h=0+a+(4+3*(4-(3+4/(4+(5-e*6))))) print h"
            + " j=a+a*(b+(b+c*(d-(c+d/(e+(d-e*f+0)))))) print j"
            + " aa=2+a*(3+(3+5*(7-(5+7/11)+(7-11*13))*2)/b) print aa"
            + "",
        "allOpsGlobal");
  }

  @Test
  @Ignore
  public void rounding() throws Exception {
    execute("f=6 k=4/(5+(4-5*f)) print k", "rounding");
  }

  @Test
  @Ignore
  public void allOpsLocals() throws Exception {
    execute(
        "fun:proc(a:int, b:int):int { \n"
            + "b=3 "
            + "c=-5 "
            + "d=7 "
            + "e=11 "
            + "f=13 "
            + "z=0"
            + " g=a+a*(b+(b+c*(d-(c+d/(e+(d-e*f)+a)*b)/c)-d)) print g"
            + " k=z+4/(5+(4-5*f)) print k"
            + " k=0+d/(5+(4-5*f)) print k"
            + " g=a+a*(b+(b+c*(d-(c+d/(e+(d-e*f)))))) print g"
            + " h=0+a+(4+3*(4-(3+4/(4+(5-e*6))))) print h"
            + " j=a+a*(b+(b+c*(d-(c+d/(e+(d-e*f+0)))))) print j"
            + " aa=2+a*(3+(3+5*(7-(5+7/11)+(7-11*13))*2)/b) print aa"
            + "   return aa\n"
            + "} \n"
            + "print fun(2, 3)",
        "allOpsLocals");
  }

  @Test
  public void addToItself() throws Exception {
    execute("a=0y3 a=a+0y10 print a", "addToItself");
  }

  @Test
  @Ignore
  public void divLoop() throws Exception {
    execute("a=10000 while a > 0 {print a a = a / 10 }", "divLoop");
  }

  @Test
  @Ignore
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
  @Ignore
  public void bug32() throws Exception {
    execute(
        "p:proc() {\r\n"
            + "  a:int\r\n"
            + "  a=3\r\n"
            + "  a=-3\r\n"
            + "  a=--3\r\n"
            + "  a=-+-3\r\n"
            + "  a=+3+-3\r\n"
            + "  a=+3\r\n"
            + "  b=a // 3\r\n"
            + "  a=(3+a)*-b // (3+3)*-3 = 6*-3=-18, ruh roh.\r\n"
            + "  b=+a\r\n"
            + "  b=-a\r\n"
            + "\r\n"
            + "  println a\r\n"
            + "  println 3+a*-b // 3+(-18*18)\r\n"
            + "  println (3+a)*-b\r\n"
            + "  println 4%6\r\n"
            + "}\r\n"
            + "main {\r\n"
            + "  p()\r\n"
            + "}\r\n",
        "bug32");
  }

  @Test
  @Ignore
  public void divisionByZeroGlobal() throws Exception {
    assertRuntimeError("a=0y0 b=0y1/a", "divByZero", "Division by 0");
  }

  @Test
  @Ignore
  public void divisionByZeroLocal() throws Exception {
    String sourceCode = "f:proc:int {a=0y0 b=0y1/a return b} f()";
    if (optimize) {
      assertGenerateError(sourceCode, "Division by 0");
    } else {
      assertRuntimeError(sourceCode, "divisionByZeroLocal", "Division by 0");
    }
  }
}
