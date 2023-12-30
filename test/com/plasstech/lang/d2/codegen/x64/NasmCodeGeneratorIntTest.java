package com.plasstech.lang.d2.codegen.x64;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;

@RunWith(TestParameterInjector.class)
public class NasmCodeGeneratorIntTest extends NasmCodeGeneratorTestBase {
  @Test
  public void intUnaryOps(@TestParameter({"-", "!"}) String op) throws Exception {
    execute(String.format("a=3 b=%sa print b", op), "intUnaryOps");
  }

  @Test
  public void intBinOps(
      @TestParameter({"+", "-", "*", "/", "&", "|", "^", "%"}) String op,
      @TestParameter({"1234", "-234567"}) int first,
      @TestParameter({"1234", "-234567"}) int second)
      throws Exception {
    execute(
        String.format(
            "a=%d b=%d c=a %s b print c d=b %s a print d e=a %s a print e f=b %s b print f",
            first, second, op, op, op, op),
        "intBinOps");
  }

  @Test
  public void intCompOps(
      @TestParameter({"<", "<=", "==", "!=", ">=", ">"}) String op,
      @TestParameter({"0", "1234", "-34567"}) int first,
      @TestParameter({"0", "1234", "-34567"}) int second)
      throws Exception {
    execute(
        String.format(
            "      a=%d b=%d " //
                + "c=a %s b println c " //
                + "d=b %s a println d " //
                + "e=a %s %d println e " //
                + "f=%d %s b println f",
            first, second, op, op, op, second, first, op),
        "intCompOps");
  }

  @Test
  public void intCompOpsArgs(
      @TestParameter({"<", "<=", "==", "!=", ">=", ">"}) String op,
      @TestParameter({"1234", "-34567"}) int first,
      @TestParameter({"0", "34567"}) int second)
      throws Exception {
    String program =
        String.format(
            "    bb:int g:bool f:proc(a:int, b:int) { " //
                + "aa=a " //
                + "c=a %s b println c " //
                + "d=b %s aa println d " //
                + "e=aa %s %d println e " //
                + "g=%d %s aa println g " // global g
                + "bb=b "
                + "h=bb < 3 println h"
                + "} "
                + "f(%d,%d)",
            op, op, op, first, second, op, first, second);
    System.err.println(program);
    execute(program, "intCompOpsArgs");
  }

  @Test
  public void shiftOps(@TestParameter({"<<", ">>"}) String op) throws Exception {
    execute(String.format("a=123 b=4 c=a%sb print c a=-234 d=b%sa print d", op, op), "shiftOps");
  }

  @Test
  public void shiftConstant(@TestParameter({"<<", ">>"}) String op) throws Exception {
    execute(String.format("a=123 c=a%s4 print c a=-234 d=a%s4 print d", op, op), "shiftConstant");
  }

  @Test
  public void shiftOpsProc(@TestParameter({"<<", ">>"}) String op) throws Exception {
    String program =
        String.format(
            "f:proc(a:int) {b=4 b=b %s a println a a=a%sb println a c=a<<2 print c} f(2)", op, op);
    execute(program, "shiftOpsProc");
  }

  @Test
  public void shiftSelf() throws Exception {
    execute("f:proc(a:int) {a=a<<a println a} f(2)", "shiftOpsProc");
  }

  @Test
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
  public void rounding() throws Exception {
    execute("f=6 k=4/(5+(4-5*f)) print k", "rounding");
  }

  @Test
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
    execute("a=3 a=a+10 print a", "addToItself");
  }

  @Test
  public void increment() throws Exception {
    execute("a=3 a++ if a!=4 {exit('increment is broken')} print a", "increment");
  }

  @Test
  public void decrement() throws Exception {
    execute("a=3 a-- print a", "decrement");
  }

  @Test
  public void divLoop() throws Exception {
    execute("a=10000 while a > 0 {print a a = a / 10 }", "divLoop");
  }

  @Test
  public void divLoopByte() throws Exception {
    execute("a=100 while a > 0 {print a a = a / 3 }", "divLoopByte");
  }

  @Test
  public void assignInt() throws Exception {
    execute("a=3 b=a a=4 print b print a", "assignInt");
  }

  @Test
  public void incDec() throws Exception {
    execute("a=42 a=a+1 print a a=a-1 print a", "incDec");
    execute("f:proc(a:int) {a=a+1 print a a=a-1 print a} f(42)", "incDec");
  }

  @Test
  public void bug32() throws Exception {
    execute(
        "p:proc() {\r\n"
            + "  a:int\r\n"
            + "  a=3\r\n"
            + "  a=-3\r\n"
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
            + "p()\r\n",
        "bug32");
  }

  @Test
  public void divisionByZeroGlobal() throws Exception {
    String sourceCode = "a=0 b=1/a";
    assertGenerateError(sourceCode, "Division by 0");
    assertRuntimeError(sourceCode, "divisionByZeroLocal", "Division by 0");
  }

  @Test
  public void divisionByZeroLocal() throws Exception {
    String sourceCode = "f:proc:int {a=0 b=1/a return b} f()";
    assertGenerateError(sourceCode, "Division by 0");
    assertRuntimeError(sourceCode, "divisionByZeroLocal", "Division by 0");
  }

  @Test
  public void simpleParamBinop() throws Exception {
    execute("f:proc(a:int):int { a=a+3 print a return a} f(1)", "simpleParamBinop");
  }

  @Test
  public void simpleLocalBinop() throws Exception {
    execute("f:proc(a:int):int { b=a+3 print b return b} f(1)", "simpleLocalBinop");
  }

  @Test
  public void opEquals(
      @TestParameter({"+=", "-=", "*=", "/="}) String op)
      throws Exception {
    execute(
        String.format("f:proc(a: int, b:int) { a %s b c=b c %s a println a println c} f(100, 10)",
            op, op),
        "opEquals");
  }
}
