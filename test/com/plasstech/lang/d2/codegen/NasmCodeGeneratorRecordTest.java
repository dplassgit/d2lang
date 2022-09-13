package com.plasstech.lang.d2.codegen;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameterInjector;

@RunWith(TestParameterInjector.class)
public class NasmCodeGeneratorRecordTest extends NasmCodeGeneratorTestBase {

  @Test
  public void alloc() throws Exception {
    execute("r: record{i:int s:string} x=new r", "alloc");
  }

  @Test
  public void allocEmpty() throws Exception {
    execute("r: record{} x=new r", "allocEmpty");
  }

  @Test
  public void allocRecursive() throws Exception {
    execute("rt: record{i:int r:rt} x=new rt", "allocRecursive");
  }

  @Test
  public void setField() throws Exception {
    execute("rt: record{i:int s:string} x=new rt x.i=3 x.i=x.i+1", "setField");
  }

  @Test
  public void setDoubleFieldConstant() throws Exception {
    execute("rt: record{d:double s:string} x=new rt x.d=3.0", "setDoubleFieldConstant");
  }

  @Test
  public void setDoubleField() throws Exception {
    execute("rt: record{d:double s:string} dd=3.0 x=new rt x.d=dd", "setDoubleField");
  }

  @Test
  public void setDoubleFieldParam() throws Exception {
    execute(
        "      rt: record{d:double s:string} "
            + "f: proc(dd:double): rt {"
            + "  x=new rt x.d=dd return x"
            + "} "
            + "f(3.0)",
        "setDoubleFieldParam");
  }

  @Test
  public void setFieldInProc() throws Exception {
    execute(
        "rt: record{s:string i:int} f:proc:int {i=3 x=new rt x.i=i return i} print f()",
        "setFieldInProc");
  }

  @Test
  public void setFieldRecordRefIsParam() throws Exception {
    execute(
        "      rt: record{s:string i:int} "
            + "f:proc(x:rt):int {i=3 x.i=i return i} "
            + "y=new rt "
            + "print f(y)",
        "setFieldRecordRefIsParam");
  }

  @Test
  public void setFieldRecordIsParam() throws Exception {
    execute(
        "      rt: record{s:string i:int} "
            + "f:proc(x:rt):int {i=3 x.i=i return i} "
            + "print f(new rt)",
        "setFieldRecordIsParam");
  }

  @Test
  public void getField() throws Exception {
    execute("rt: record{i:int s:string} x=new rt x.i=3 print x.i", "getField");
  }

  @Test
  public void getFieldDouble() throws Exception {
    execute("rt: record{d:double s:string} x=new rt x.d=3.0 print x.d", "getFieldDouble");
  }

  @Test
  public void getFieldInProc() throws Exception {
    execute(
        "rt: record{s:string i:int} f:proc:int {i=3 x=new rt x.i=i return x.i} print f()",
        "getFieldInProc");
  }

  @Test
  public void getFieldDoubleInProc() throws Exception {
    execute(
        "      rt: record{s:string d:double} "
            + "f:proc(dd:double):double {"
            + "  x=new rt "
            + "  x.d=dd "
            + "  return x.d"
            + "}"
            + "print f(3.0)",
        "getFieldInProc");
  }

  @Test
  public void getFieldDoubleInProcToLocal() throws Exception {
    execute(
        "      rt: record{s:string d:double} "
            + "f:proc(dd:double):double {"
            + "  x=new rt "
            + "  x.d=dd "
            + "  loc=x.d"
            + "  return loc"
            + "}"
            + "print f(3.0)",
        "getFieldInProc");
  }

  @Test
  public void getFieldDoubleInProcToArg() throws Exception {
    execute(
        "      rt: record{s:string d:double} "
            + "f:proc(dd:double):double {"
            + "  x=new rt "
            + "  x.d=dd "
            + "  dd=x.d"
            + "  return dd"
            + "}"
            + "print f(3.0)",
        "getFieldInProc");
  }

  @Test
  public void getFieldRecordRefIsParam() throws Exception {
    execute(
        "      rt: record{s:string i:int} "
            + "f:proc(x:rt):int {return x.i} "
            + "y=new rt "
            + "y.i=3 print f(y)",
        "getFieldRecordRefIsParam");
  }

  @Test
  public void crashOnSet() throws Exception {
    execute(
        "      Token: record { "
            + "  type: int "
            + "  start: Token "
            + "  end: Token "
            + "  value: String "
            + "} "
            + "makeToken: proc(type: int, start: Token, end: Token, text: String): Token { "
            + "  token = new Token "
            + "  token.type = type "
            + "  token.start = start "
            + "  token.end = end "
            + "  token.value = text "
            + "  print 'Made a token of value: ' "
            + "  println token.value "
            + "  print 'Made a token of type: ' "
            + "  println token.type "
            + "  return token "
            + "} "
            + "t = makeToken(1, null, null, 'keyword1') ",
        "crashOnSet");
  }

  @Test
  public void nullCheck() throws Exception {
    assertRuntimeError(
        "      rt: record {s:string i:int} \r\n"
            + " a:rt \r\n "
            + "a=null \r\n"
            + "println a.s",
        "nulLCheck",
        "Null pointer error");
  }

  @Test
  public void compare() throws Exception {
    execute(
        "      rt: record {s:string i:int} "
            + "a=new rt a.s='hi' a.i=3 "
            + "b=new rt b.s='hi' b.i=3 "
            + "print 'a==b Should be true: ' "
            + "println a==b "
            + "print 'a!=b Should be false: ' "
            + "println a!=b "
            + "print 'a==a Should be true: ' "
            + "println a==a "
            + "print 'b==b Should be true: ' "
            + "println b==b "
            + "c=a "
            + "print 'c==a Should be true: ' "
            + "println c==a "
            + "print 'c==b Should be true: ' "
            + "println c==b "
            + "print 'c!=b Should be false: ' "
            + "println c!=b "
            + "d=new rt d.s='hi ' d.i=4 "
            + "print 'a==d Should be false: ' "
            + "println a==d "
            + "print 'a!=d Should be true: ' "
            + "println a!=d ",
        "compare");
  }

  @Test
  public void setArrayFieldLiteral() throws Exception {
    execute(
        "rt: record{d:double ar:int[3]} x=new rt x.ar=[1,2,3] print x.ar", "setArrayFieldLiteral");
  }

  @Test
  public void setArrayField() throws Exception {
    execute("rt: record{d:double ar:int[3]} x=new rt ar=x.ar ar[1]=3 print x.ar", "setArrayField");
  }

  @Test
  public void setArrayOfDoubleField_bug159() throws Exception {
    execute(
        "      PlanetType: record {\r\n"
            + "  status:int \r\n"
            + "  assets:double[5] \r\n"
            + "}\r\n"
            + "EMPIRE=2 "
            + "f:proc { \r\n"
            + "    p = new PlanetType \r\n"
            + "    p.status = EMPIRE"
            + "    assets = p.assets \r\n"
            + "    assets[0] = 123.4 // npe\r\n"
            + "}\r\n"
            + "f()",
        "setArrayOfDoubleField_bug159");
  }

  @Test
  public void advancedRValue_bug158() throws Exception {
    execute(
        "      r1:record{bar:r2} r2:record{baz:r3[2]} r3:record{qux:string}"
            + " foo:r1[8]"
            + " foo7 = new r1"
            + " foo[7] = foo7"
            + " ar2 = new r2"
            + " foo7.bar = ar2"
            + " x=ar2.baz"
            + " ar3=new r3"
            + " ar3.qux='hi'"
            + " x[1]=ar3"
            + " a=4"
            + " f:proc:int{return 1}"
            + " bam = foo[3+a].bar.baz[f()].qux"
            + " if bam != 'hi' { exit 'fail, actual ' + bam} ",
        "advancedRValue_bug158");
  }
}
