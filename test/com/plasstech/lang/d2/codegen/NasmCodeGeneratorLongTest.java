package com.plasstech.lang.d2.codegen;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;

@RunWith(TestParameterInjector.class)
public class NasmCodeGeneratorLongTest extends NasmCodeGeneratorTestBase {
  @Test
  public void longUnaryOps(@TestParameter({"-", "!"}) String op) throws Exception {
    execute(String.format("a=3L b=%sa print b", op), "intUnaryOps");
  }

  @Test
  public void longBinOps(
      @TestParameter({"+", "-", "*", "&", "|", "^", "/" /*, "%"*/}) String op,
      @TestParameter({"1234", "-234567"}) String first,
      @TestParameter({"1234", "-234567"}) String second)
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
    execute(program, "longBinOps");
  }

  @Test
  public void longBinOpsWithLongConstants(
      @TestParameter({"+", "*", "&"}) String op,
      @TestParameter({"12345678901234", "-1234567"}) String first,
      @TestParameter({"12345", "-12345678901234"}) String second)
      throws Exception {
    String program = String.format("a=%sL c=a %s %sL println c ", first, op, second);
    execute(program, "longBinOpsWithLongConstants");
  }

  @Test
  public void longCompOps(
      @TestParameter({"<", "<=", "==", "!=", ">=", ">"}) String op,
      @TestParameter({"12345678901234", "-12345"}) String first,
      @TestParameter({"123456", "-3456778901234"}) String second)
      throws Exception {
    execute(
        String.format(
            "      a=%sL b=%sL " //
                + "c=a %s b println c " //
                + "d=b %s a println d " //
                + "e=a %s %sL println e " //
                + "f=%sL %s b println f",
            first, second, op, op, op, second, first, op),
        "longCompOps");
  }

  @Test
  public void longCompOpsArgs(
      @TestParameter({"<", "<=", "==", "!=", ">=", ">"}) String op,
      @TestParameter({"1234", "-34567"}) int first,
      @TestParameter({"0", "34567"}) int second)
      throws Exception {
    String program =
        String.format(
            "    bb:long g:bool f:proc(a:long, b:long) { " //
                + "aa=a " //
                + "c=a %s b println c " //
                + "d=b %s aa println d " //
                + "e=aa %s %dL println e " //
                + "g=%dL %s aa println g " // global g
                + "bb=b "
                + "h=bb < 3L println h"
                + "} "
                + "f(%dL, %dL)",
            op, op, op, first, second, op, first, second);
    System.err.println(program);
    execute(program, "longCompOpsArgs");
  }

  @Test
  public void shiftOps(@TestParameter({"<<", ">>"}) String op) throws Exception {
    execute(String.format("a=123L b=4L c=a%sb print c a=-234L d=b%sa print d", op, op), "shiftOps");
  }

  @Test
  public void shiftConstant(@TestParameter({"<<", ">>"}) String op) throws Exception {
    execute(
        String.format("a=123L c=a %s 4L print c a=-234L d=a %s 4L print d", op, op),
        "shiftConstant");
  }

  @Test
  public void shiftOpsProc(@TestParameter({"<<", ">>"}) String op) throws Exception {
    String program =
        String.format(
            "f:proc(a:long) {b=4L b=b %s a println a a=a%sb println a c=a<<2L print c} f(2L)",
            op, op);
    execute(program, "shiftOpsProc");
  }

  @Test
  public void shiftSelf() throws Exception {
    execute("f:proc(a:long) {a=a<<a println a} f(2L)", "shiftOpsProc");
  }

  @Test
  @Ignore("Don't know why this is failng")
  public void tree() throws Exception {
    execute(
        "      a=2L "
            + "b=3L "
            + "c=-5L "
            + "d=7L "
            + "e=11L "
            + "f=13L "
            + "g = (((a+b+c+2L)*(b+c+d+1L))*((c-d-e-1L)/(d-e-f-2L))*((e+f+a-3L)*"
            + "      (f-a-b+4L)))*((a+c+e-9L)/(b-d-f+11L)) "
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
  public void rounding() throws Exception {
    execute("f=6 k=4/(5+(4-5*f)) print k", "rounding");
  }

  @Test
  @Ignore
  public void allOpsLocals() throws Exception {
    execute(
        "fun:proc(a:long, b:long):long { \n"
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
    execute("a=3L a=a+10L print a", "addToItself");
  }

  @Test
  public void increment() throws Exception {
    execute("a=3L a++ if a!=4L {exit('increment is broken')} print a", "increment");
  }

  @Test
  public void decrement() throws Exception {
    execute("a=3L a-- print a", "decrement");
  }

  @Test
  public void divLoop() throws Exception {
    execute("a=10000L while a > 0L {print a a = a / 10L }", "divLoop");
  }

  @Test
  public void assignInt() throws Exception {
    execute("a=3L b=a a=4L print b print a", "assignInt");
  }

  @Test
  public void incDec() throws Exception {}

  @Test
  @Ignore
  public void bug32() throws Exception {
    execute(
        "p:proc() {\r\n"
            + "  a:long\r\n"
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
            + "main {\r\n"
            + "  p()\r\n"
            + "}\r\n",
        "bug32");
  }

  @Test
  public void divisionByZeroGlobal() throws Exception {
    String sourceCode = "a=0L b=1L/a";
    if (optimize) {
      assertGenerateError(sourceCode, "Division by 0");
    } else {
      assertRuntimeError(sourceCode, "divisionByZeroLocal", "Division by 0");
    }
  }

  @Test
  public void divisionByZeroLocal() throws Exception {
    String sourceCode = "f:proc:long {a=0L b=1L/a return b} f()";
    if (optimize) {
      assertGenerateError(sourceCode, "Division by 0");
    } else {
      assertRuntimeError(sourceCode, "divisionByZeroLocal", "Division by 0");
    }
  }
}
