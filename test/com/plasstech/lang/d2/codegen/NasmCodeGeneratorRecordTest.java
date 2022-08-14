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
        "  PlanetType: record {\r\n"
            + "    id:int\r\n"
            + "    name:string\r\n"
            + "    abbrev:string    // first letter of planet\r\n"
            + "    x:int y:int    // location (0-50)\r\n"
            + "    population:double   // in millions\r\n"
            + "    status:int     // 1=occupied, 2=empire, 0=independent\r\n"
            + "    status_changed:bool  // 1 if status just changed, 0 if not. WHY?!\r\n"
            + "    assets: double[5]  // amount of each type on hand: food, fuel, parts, troops, money\r\n"
            + "    prod_ratio:int[5]   // ratio of each type of asset production\r\n"
            + "    civ_level:int    // primitive, limited, advanced, etc.\r\n"
            + "    troops:int    // # of troops on surface, or # of occupation troops\r\n"
            + "    fighters:int     // # of fighters in orbit, or # of occupation fighters\r\n"
            + "    sats_orbit:int    // # of satellites in orbit\r\n"
            + "    sats_enroute:int  // # of satellites en route\r\n"
            + "    sats_arrive:int[3]  // arrival date (in DAYS) of each satellite\r\n"
            + "    prices:int[2]    // food, fuel (note can only buy if status=empire)\r\n"
            + "    occupied_on:int    // date (years) planet was occupied\r\n"
            + " }\r\n"
            + "f:proc {"
            + "    p = new PlanetType\r\n"
            + "    name = 'planet'\r\n"
            + "    p.name = name\r\n"
            + "    p.abbrev = name[0]  // will this work? no.\r\n"
            + "    p.x = 5 \r\n"
            + "    p.y = 7 \r\n"
            + "    p.population = 55.5\r\n"
            + "    p.civ_level = 2\r\n"
            + "    p.status = 0\r\n"
            + "      prod_ratio = p.prod_ratio\r\n"
            + "      if p.civ_level == 0 {\r\n"
            + "                                prod_ratio[0] = 34\r\n"
            + "                                prod_ratio[1] = 33\r\n"
            + "                                prod_ratio[2] = 33\r\n"
            + "      } elif p.civ_level == 1 {\r\n"
            + "                                // if limited base is 25 (min 12)\r\n"
            + "                                prod_ratio[0] = 25\r\n"
            + "                                prod_ratio[1] = 25\r\n"
            + "                                prod_ratio[2] = 25\r\n"
            + "                                prod_ratio[3] = 25\r\n"
            + "      } else { // advanced, superior\r\n"
            + "                                // else base is 20       (min 10)\r\n"
            + "                                prod_ratio[0] = 20\r\n"
            + "                                prod_ratio[1] = 20\r\n"
            + "                                prod_ratio[2] = 20\r\n"
            + "                                prod_ratio[3] = 20\r\n"
            + "                                prod_ratio[4] = 20\r\n"
            + "\r\n"
            + "        p.fighters = 100\r\n"
            + "      }\r\n"
            + "\r\n"
            + "      // 7. set troops, fighters based on level & civ_type (no fighters if <ADVANCED)\r\n"
            + "      p.troops = 200\r\n"
            + "    // 8. set food price, fuel price based on level\r\n"
            + "    prices = p.prices\r\n"
            + "    prices[0] = 7\r\n"
            + "    prices[1] = 5\r\n"
            + "\r\n"
            + "    assets = p.assets\r\n"
            + "    assets[0] = 123.4 // abc(3.3973, -75.85, 432.41, leveld)  // npe\r\n"
            + "\r\n"
            + "    assets[1] = 234.5 // abc(27.268, -635.19, 3799.0, leveld)\r\n"
            + "\r\n"
            + "    // if < advanced, no parts\r\n"
            + "    if (p.civ_level >= 1) {\r\n"
            + "      assets[2] = 12.3 // abc(4.4802, -97.611, 541.88, leveld)\r\n"
            + "    }\r\n"
            + "\r\n"
            + "    assets[3] = 400.0 // abc(1.2727, -26.198, 143.58, leveld)\r\n"
            + "\r\n"
            + "    // if primitive, no fuel\r\n"
            + "    if (p.civ_level > 0) {\r\n"
            + "      assets[4] = 23.4 // abc(1.7437, -40.969, 247.21, leveld)\r\n"
            + "    }\r\n"
            + "}\r\n"
            + " f()",
        "setArrayOfDoubleField");
  }
}
