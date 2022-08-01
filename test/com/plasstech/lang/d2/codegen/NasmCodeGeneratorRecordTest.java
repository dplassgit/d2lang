package com.plasstech.lang.d2.codegen;

import org.junit.Ignore;
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
  public void getFieldInProc() throws Exception {
    execute(
        "rt: record{s:string i:int} f:proc:int {i=3 x=new rt x.i=i return x.i} print f()",
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
  @Ignore("Interpreter cannot compare records (!)")
  public void compare() throws Exception {
    execute(
        "      rt: record {s:string i:int} "
            + "a=new rt "
            + "b=new rt "
            + "println 'Should be false' "
            + "println a==b "
            + "println 'Should be true' "
            + "println a!=b "
            + "println 'Should be true' "
            + "println a==a "
            + "println 'Should be true' "
            + "println b==b "
            + "c=a "
            + "println 'Should be true' "
            + "println c==a "
            + "println 'Should be false' "
            + "println c==b "
            + "println 'Should be true' "
            + "println c!=b ",
        "compare");
  }
}
