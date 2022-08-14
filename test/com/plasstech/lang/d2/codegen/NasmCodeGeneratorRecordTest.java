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
  public void setArrayOfDoubleField() throws Exception {
    execute(
        "  rt: record {\r\n"
            + "   id:int\r\n"
            + "   name:string\r\n"
            + "   abbrev:string    // first letter of planet\r\n"
            + "   x:int y:int    // location (0-50)\r\n"
            + "   population:double   // in millions\r\n"
            + "   status:int     // 1=occupied, 2=empire, 0=independent\r\n"
            + "   status_changed:bool  // 1 if status just changed, 0 if not. WHY?!\r\n"
            + "   assets: double[5]  // amount of each type on hand: food, fuel, parts, troops, money\r\n"
            + "   prod_ratio:int[5]   // ratio of each type of asset production\r\n"
            + "   civ_level:int    // primitive, limited, advanced, etc.\r\n"
            + "   troops:int    // # of troops on surface, or # of occupation troops\r\n"
            + "   fighters:int     // # of fighters in orbit, or # of occupation fighters\r\n"
            + "   sats_orbit:int    // # of satellites in orbit\r\n"
            + "   sats_enroute:int  // # of satellites en route\r\n"
            + "   sats_arrive:int[3]  // arrival date (in DAYS) of each satellite\r\n"
            + "   prices:int[2]    // food, fuel (note can only buy if status=empire)\r\n"
            + "   occupied_on:int    // date (years) planet was occupied\r\n"
            + " }\r\n"
            + "f:proc {"
            + "  p=new rt "
            + "  name='hello' p.name=name p.abbrev=name[0]"
            + "  p.x=5 p.y=6 "
            + "  p.population = 12.0 "
            + "  p.civ_level = 2 "
            + "  p.status=1"
            + "  pr = p.prod_ratio "
            + "  pr[0] = 23 "
            + "  pr[1] = 23 "
            + "  pr[2] = 23 "
            + "  pr[3] = 23 "
            + "  pr[4] = 23 "
            + "  p.fighters=56 p.troops=17 "
            + "  pc = p.prices "
            + "  pc[0] = 5 "
            + "  pc[1] = 7 "
            + "  ar=p.assets "
            + "  ar[0]=4.0 "
            + "  ar[1]=41.0 "
            + "  ar[2]=42.0 "
            + "  ar[3]=43.0 "
            + "  ar[4]=44.0 "
            + "  print ar"
            + "}"
            + " f()",
        "setArrayOfDoubleField");
  }
}
