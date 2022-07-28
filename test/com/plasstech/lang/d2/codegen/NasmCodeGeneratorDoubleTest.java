package com.plasstech.lang.d2.codegen;

import static org.junit.Assume.assumeTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;

@RunWith(TestParameterInjector.class)
public class NasmCodeGeneratorDoubleTest extends NasmCodeGeneratorTestBase {

  @Test
  public void assignDouble() throws Exception {
    execute("a=3.4", "assignDouble");
  }

  @Test
  public void printDoubleConstant() throws Exception {
    assumeTrue(optimize);
    execute("print 3.4", "printDoubleConstant");
  }

  @Test
  public void doubleUnary() throws Exception {
    assertGenerateError("a=3.0 b=-a print b", "Cannot.*DOUBLEs.*$");
  }

  @Test
  public void doubleBinOps(
      @TestParameter({"+", "-", "*", "/"}) String op,
      @TestParameter({"1234.5", "-234567.8"}) double first,
      @TestParameter({"-1234.5", "234567.8"}) double second)
      throws Exception {
    assertGenerateError(
        String.format(
            "a=%f b=%f c=a %s b print c d=b %s a print d e=a %s a print e f=b %s b print f",
            first, second, op, op, op, op),
        "Cannot do .*DOUBLEs.*$");
  }

  @Test
  public void doubleCompOps(
      @TestParameter({"<", "<=", "==", "!=", ">=", ">"}) String op,
      @TestParameter({"0.0", "1234.5", "-34567.8"}) double first,
      @TestParameter({"0.0", "-1234.5", "34567.8"}) double second)
      throws Exception {
    assertGenerateError(
        String.format(
            "      a=%f b=%f " //
                + "c=a %s b print c " //
                + "d=b %s a print d",
            first, second, op, op),
        "Cannot do .*DOUBLEs.*$");
  }

  /*  @Test
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
  */

  @Test
  public void addToItself() throws Exception {
    assertGenerateError("a=3.1 a=a+10.1 print a", "Cannot do .*DOUBLEs.*$");
  }

  @Test
  public void printDouble() throws Exception {
    assertGenerateError("a=3.4 b=a print b print a", "Cannot.*DOUBLEs.*$");
  }
}
