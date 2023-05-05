package com.plasstech.lang.d2.codegen.x64;

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
      @TestParameter({"+", "-", "*", "&", "|", "^", "/" /* , "%" */ }) String op,
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
      @TestParameter({"12345678901234", "-12345"}) String first,
      @TestParameter({"123456", "-3456778901234"}) String second)
      throws Exception {
    String program =
        String.format(
            "    bb:long g:bool f:proc(a:long, b:long) { " //
                + "aa=a " // to test locals vs args.
                //                + "c=a %s b println c " //
                //                + "d=b %s aa println d " //
                //                + "e=aa %s %sL println e " //
                + "g=%sL %s aa println g " // global g
                //                + "bb=b "
                //                + "h=bb < 3L println h"
                + "} "
                + "f(%sL, %sL)",
            //            op, op, op, first, second, op, first, second);
            second, op, first, second);
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
  public void allOpsGlobals() throws Exception {
    execute(
        "      a=2L "
            + "b=3L "
            + "c=-5L "
            + "d=7L "
            + "e=11L "
            + "f=13L "
            + "z=0L "
            + " g=a+a*(b+(b+c*(d-(c+d/(e+(d-e*f)+a)*b)/c)-d)) print g"
            + " k=z+4L/(5L+(4L-5L*f)) print k"
            + " k=0L+d/(5L+(4L-5L*f)) print k"
            + " g=a+a*(b+(b+c*(d-(c+d/(e+(d-e*f)))))) print g"
            + " h=0L+a+(4L+3L*(4L-(3L+4L/(4L+(5L-e*6L))))) print h"
            + " j=a+a*(b+(b+c*(d-(c+d/(e+(d-e*f+0L)))))) print j"
            + " aa=2L+a*(3L+(3L+5L*(7L-(5L+7L/11L)+(7L-k*13L))*2L)/b) print aa"
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
        "fun:proc(a:long, b:long):long { \n"
            + "c=-5L "
            + "d=7L "
            + "e=11L "
            + "f=13L "
            + "z=0L "
            + " g=a+a*(b+(b+c*(d-(c+d/(e+(d-e*f)+a)*b)/c)-d)) print g"
            + " k=z+4L/(5L+(4L-5L*f)) print k"
            + " k=0L+d/(5L+(4L-5L*f)) print k"
            + " g=a+a*(b+(b+c*(d-(c+d/(e+(d-e*f)))))) print g"
            + " h=0L+a+(4L+3L*(4L-(3L+4L/(4L+(5L-e*6L))))) print h"
            + " j=a+a*(b+(b+c*(d-(c+d/(e+(d-e*f+0L)))))) print j"
            + " aa=2L+a*(3L+(3L+5L*(7L-(5L+7L/11L)+(7L-k*13L))*2L)/b) print aa"
            + "   return aa\n"
            + "} \n"
            + "print fun(2L, 3L)",
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
  public void incDec() throws Exception {
    execute("a=3L a-- println a a++ println a", "incdec");
  }

  @Test
  public void bug32() throws Exception {
    execute(
        "p:proc() {\r\n"
            + "  a:long\r\n"
            + "  a=3L\r\n"
            + "  a=-3L\r\n"
            + "  a=-+-3L\r\n"
            + "  a=+3L+-3L\r\n"
            + "  a=+3L\r\n"
            + "  b=a // 3\r\n"
            + "  a=(3L+a)*-b // (3+3)*-3 = 6*-3=-18, ruh roh.\r\n"
            + "  b=+a\r\n"
            + "  b=-a\r\n"
            + "\r\n"
            + "  println a\r\n"
            + "  println 3L+a*-b // 3+(-18*18)\r\n"
            + "  println (3L+a)*-b\r\n"
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
