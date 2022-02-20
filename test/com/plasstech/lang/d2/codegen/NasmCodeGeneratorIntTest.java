package com.plasstech.lang.d2.codegen;

import static org.junit.Assume.assumeFalse;

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
                + "c=a %s b print c " //
                + "d=b %s a print d",
            first, second, op, op),
        "intCompOps");
  }

  @Test
  public void shiftOps(@TestParameter({"<<", ">>"}) String op) throws Exception {
    execute(String.format("a=123 b=4 c=a%sb print c a=-234 d=b%sa print d", op, op), "shiftOps");
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
    assumeFalse(optimize);
    execute(
        "      a=1 b=2 c=3 d=4 e=5 f=6 g=3\n"
            + "fun:proc():int { \n"
            + "   g=a+a*b+(b+c)*d-(c+d)/e+(d-e)*f  \n"
            + "   return g \n"
            + "} \n"
            + "print fun()",
        "allOpsLocals");
  }

  @Test
  public void addToItself() throws Exception {
    execute("a=3 a=a+10 print a", "addToItself");
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
    execute("a=42 a=a+1 print a a=41 a=a-1 print a", "incDec");
  }
}
