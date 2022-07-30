package com.plasstech.lang.d2.codegen;

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
  public void doubleCompOps_unimplemented(
      @TestParameter({"<", "<=", "==", "!=", ">=", ">"}) String op,
      @TestParameter({"0.0", "1234.5", "-34567.8"}) double first,
      @TestParameter({"0.0", "-1234.5", "34567.8"}) double second)
      throws Exception {
    assertGenerateError(
        String.format(
            "      a=%f b=%f " //
                + "c=a %s b println c " //
                + "d=b %s a println d",
            first, second, op, op),
        "Cannot do .*DOUBLEs.*$");
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
            + "g = (((a+b+c+2.0)*(b+c+d+1.0))*((c-d-e-1.0)/(d-e-f-2.0))*((e+f+a-3.0)*"
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
            + " g=a+a*(b+(b+c*(d-(c+d/(e+(d-e*f)+a)*b)/c)-d)) print g"
            + " k=z+4.0/(5.0+(4.0-5.0*f)) print k"
            + " k=0.0+d/(5.0+(4.0-5.0*f)) print k"
            + " g=a+a*(b+(b+c*(d-(c+d/(e+(d-e*f)))))) print g"
            + " h=0.0+a+(4.0+3.0*(4.0-(3.0+4.0/(4.0+(5.0-e*6.0))))) print h"
            + " j=a+a*(b+(b+c*(d-(c+d/(e+(d-e*f+0.0)))))) print j"
            + " aa=2.0+a*(3.0+(3.0+5.0*(7.0-(5.0+7.0/11.0)+(7.0-11.0*13.0))*2.0)/b) print aa"
            + "",
        "allOpsGlobal");
  }

  @Test
  public void rounding() throws Exception {
    execute("f=6.0 k=4.0/(5.0+(4.0-5.0*f)) print k", "rounding");
  }

  @Test
  @Ignore("Convert me")
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
}
