package com.plasstech.lang.d2.codegen.x64;

import static com.plasstech.lang.d2.codegen.x64.testing.ExecutionSubject.assertThatCompiling;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;

@RunWith(TestParameterInjector.class)
public class NasmCodeGeneratorProcTest {
  @Test
  public void voidProcNoArgs() throws Exception {
    assertThatCompiling("      a=1 \n"
        + "procTest:proc() { \n" //
        + "   if a==1 {println a return } else {println 4 return} \n" //
        + "} \n" //
        + "procTest()").executedEqualsInterpreted();
  }

  @Test
  public void procReturnsInt() throws Exception {
    assertThatCompiling("      fun:proc():int { \n" //
        + "   return 3 \n" //
        + "} \n" //
        + "x=fun() println x").executedEqualsInterpreted();
  }

  @Test
  public void procReturnsBool() throws Exception {
    assertThatCompiling("      fun:proc():bool { \n" //
        + "   return true \n" //
        + "} \n" //
        + "x=fun() println x").executedEqualsInterpreted();
  }

  @Test
  public void procReturnsString() throws Exception {
    assertThatCompiling("      fun:proc():string { \n" //
        + "   return 'hi' \n" //
        + "} \n" //
        + "x=fun() println x").executedEqualsInterpreted();
  }

  @Test
  public void procIntParam() throws Exception {
    assertThatCompiling("      procIntParam:proc(n:int):int { "
        + "   return n+1"
        + "}"
        + "x=procIntParam(4) "
        + "println x").executedEqualsInterpreted();
  }

  @Test
  public void procLocals() throws Exception {
    assertThatCompiling("      procLocals:proc(n:int):int { "
        + "  // a is a local \n"
        + "  a=n+1 b=a"
        + "  return b"
        + "} "
        + "x=procLocals(4) "
        + "println x").withOptimize(false).executedEqualsInterpreted();
  }

  @Test
  public void bug102ParamMunge() throws Exception {
    assertThatCompiling("      f2: proc(c2:bool, b2:string, d2:int, a2:int) {"
        + "  println 'c=' println c2 println 'b=' println b2 println 'a=' println a2 "
        + "}"
        + "f1: proc(a1:int, c1:bool, b1:string, d1:int) {"
        + "   f2(c1,b1,a1,d1) "
        + "}"
        + "f1(2,true, 'hi', 4)").executedEqualsInterpreted();
  }

  @Test
  public void bug102Redux() throws Exception {
    assertThatCompiling("makeSymbol: proc(it: int, start: int): int {\r"
        + "  return makeToken(3, start, start)\r"
        + "} \r"
        + "makeToken:proc(type:int, start:int, end:int): int {\r"
        + "  return start\r"
        + "} \r"
        + "println makeSymbol(1,2)\r").executedEqualsInterpreted();
  }

  private static final String FOUR_PARAM_PROC =
      "  procParamFirst4Locations:proc(p3:bool, p4: string, p1: int, p2: int) { "
          + "  println p2 println p1 println p2 println p1 println p3 println p4 \n"
          + "  p1 = p2 + 1 "
          + "  p3 = p1 == p2"
          + "  println p1 "
          + "  println p3"
          + "} "
          + "glob='theglob' "
          + "println glob "
          + "procParamFirst4Locations(true,'thep4',1,3) "
          + "procParamFirst4Locations(false,'thep4',-1,-2) ";

  @Test
  public void procParamFirst4Locations() throws Exception {
    assertThatCompiling(FOUR_PARAM_PROC).executedEqualsInterpreted();
  }

  @Test
  public void recursion() throws Exception {
    assertThatCompiling("      reverseRecursive: proc(s: string): string {"
        + "  return reverse2(s, length(s))"
        + "} "
        + "reverse2: proc(s: string, start: int): string {"
        + "  if start == 0 {"
        + "    return '' "
        + "  } else {"
        + "    return s[start - 1] + reverse2(s, start - 1) "
        + "  } "
        + "} "
        + "  println 'Recursive reverse ' + reverseRecursive('Reverse')")
        .executedEqualsInterpreted();
  }

  @Test
  public void bug39() throws Exception {
    assertThatCompiling("      a:string a='bye'"
        + "setup: proc {"
        + "  a = 'hi'"
        + "  b = 'bee'"
        + "}"
        + "setup()").executedEqualsInterpreted();
  }

  @Test
  public void forwardRef() throws Exception {
    assertThatCompiling("      p2()\r\n"
        + "p2: proc {\r\n"
        + "  println \"Should println 1\"\r\n"
        + "  // forward reference\r\n"
        + "  val = p1()\r\n"
        + "  println val\r\n"
        + "}\r\n"
        + "p1: proc: int {\r\n"
        + "  return 1\r\n"
        + "}\r\n").executedEqualsInterpreted();
  }

  @Test
  public void ignoreReturn() throws Exception {
    assertThatCompiling("      a:proc(): string {" //
        + "  return 'aproc'"
        + "}" //
        + "a()").executedEqualsInterpreted();
  }

  @Test
  public void commonSubexpression(@TestParameter boolean optimize) throws Exception {
    assertThatCompiling(
        "      fun:proc(a:int, b:int) {\r"
            + "  c=((a+b)*((a-b)*((a+b)*((a-b)*((a+b)*((a-b)*((a+b)*((a-b)*((a+b)*((a-b)*((a+b)*((a-b)*((a+b)*(a-b))))))))))))))\r"
            + "  println c\r"
            + "}\r"
            + "fun(1,2)\r")
        .withOptimize(optimize)
        .executedEqualsInterpreted();
  }

  @Test
  public void commonSubexpression_double(@TestParameter boolean optimize) throws Exception {
    assertThatCompiling(
        "      fun:proc(a:double, b:double) {\r"
            + "  c=((a+b)*((a-b)*((a+b)*((a-b)*((a+b)*((a-b)*((a+b)*((a-b)*((a+b)*((a-b)*((a+b)*((a-b)*((a+b)*(a-b))))))))))))))\r"
            + "  println c\r"
            + "}\r"
            + "fun(1.0,2.0)\r")
        .withOptimize(optimize)
        .executedEqualsInterpreted();
  }

  @Test
  public void spillover_int(@TestParameter boolean optimize) throws Exception {
    assertThatCompiling(
        "      fun:proc(a:int, b:int, c:int, d:int) {\r"
            + "  e=((a+b)*((a-b)*((a*b)*((a/b)*((a+c)*((a-c)*((a*c)*((a+d)*((a-d)*((a*d)*((b+c)*((b*c)*((b/c)*(b+d))))))))))))))\r"
            + "  println e\r"
            + "}\r"
            + "fun(1,2,3,4)\r")
        .withOptimize(optimize)
        .executedEqualsInterpreted();
  }

  @Test
  public void spillover_int_calls(@TestParameter boolean optimize) throws Exception {
    assertThatCompiling(
        "     maybedouble:proc(i:int):int {"
            + "       if (i%4)==0 { return i+1}"
            + "       return i*2"
            + "   } "
            + "   fun:proc(a:int, b:int, c:int, d:int) {\r"
            + "  e=((a+b)*(maybedouble(a-b)*((a*b)*((a/b)*((a+c)*(maybedouble(a-c)*((a*c)*((a+d)*(maybedouble(a-d)*((a*d)*(maybedouble(b+c)*(maybedouble(b*c)*"
            + "  ((b/c)*maybedouble(b+d))))))))))))))\r"
            + "  println e\r"
            + "}\r"
            + "fun(1,2,3,4)\r")
        .withOptimize(optimize)
        .executedEqualsInterpreted();
  }

  @Test
  public void spillover_long(@TestParameter boolean optimize) throws Exception {
    assertThatCompiling(
        "      fun:proc(a:long, b:long, c:long, d:long) {\r"
            + "  e=((a+b)*((a-b)*((a*b)*((a/b)*((a+c)*((a-c)*((a*c)*((a+d)*((a-d)*((a*d)*((b+c)*((b*c)*((b/c)*(b+d))))))))))))))\r"
            + "  println e\r"
            + "}\r"
            + "fun(1L,2L,3L,4L)\r")
        .withOptimize(optimize)
        .executedEqualsInterpreted();
  }

  @Test
  public void spillover_double(@TestParameter boolean optimize) throws Exception {
    assertThatCompiling(
        "      fun:proc(a:double, b:double, c:double, d:double) {\r"
            + "  e=((a+b)*((a-b)*((a*b)*((a+b)*((a+c)*((a-c)*((a*c)*((a+d)*((a-d)*((a*d)*((b+c)*((b*c)*((b+c)*(b+d))))))))))))))\r"
            + "  println e\r"
            + "}\r"
            + "fun(1.0,2.0, 3.0, 4.0)\r")
        .withOptimize(optimize)
        .executedEqualsInterpreted();
  }

  @Test
  public void spillover_string(@TestParameter boolean optimize) throws Exception {
    assertThatCompiling(
        "      fun:proc(a:string, b:string, i:int, j:int) {\r"
            + "  e = (a+(b+(a[i]+(a[j]+(b[i]+(b[j]+(a[i:j]+(b[i:j]+(a+(b+(a[i]+(a[j]+(b[i]+(b[j]+(a[i:j]+(b[i:j]))))))))))))))))\r"
            + "  println e\r"
            + "}\r"
            + "fun('first', 'second', 0, 2)\r")
        .withOptimize(optimize)
        .executedEqualsInterpreted();
  }

  @Test
  public void spillover_string_globals(@TestParameter boolean optimize) throws Exception {
    assertThatCompiling(
        "      a='first' b='second' fun:proc(i:int, j:int) {\r"
            + "  e = (a+(b+(a[i]+(a[j]+(b[i]+(b[j]+(a[i:j]+(b[i:j]+(a+(b+(a[i]+(a[j]+(b[i]+(b[j]+(a[i:j]+(b[i:j]))))))))))))))))\r"
            + "  println e\r"
            + "}\r"
            + "fun(0, 2)\r")
        .withOptimize(optimize)
        .executedEqualsInterpreted();
  }

  @Test
  public void moreThan4Params() throws Exception {
    assertThatCompiling("      six:proc(a:bool,b:int,c:double,d:byte,e:long,f:bool):bool {"
        + "  println e"
        + "  println f"
        + "  return a and b==1 and c>0.5 and d<0y7f and e!=4L and f"
        + "}"
        + " println six(true, 1, 2.0, 0y03, 4L, false)").executedEqualsInterpreted();
  }

  @Test
  public void manyManyParams() throws Exception {
    assertThatCompiling(
        "      add12:proc(a:int,b:int,c:int,d:int,e:int,f:int,"
            + "          a2:int,b2:int,c2:int,d2:int,e2:int,f2:int):int {"
            + "  println e"
            + "  println f"
            + "  println a2"
            + "  println b2"
            + "  println c2"
            + "  println d2"
            + "  println e2"
            + "  println f2"
            + "  return a+b+c+d+e+f-a2-b2-c2-d2-e2-f2"
            + "}"
            + " add122:proc(a:int,b:int,c:int,d:int,e:int,f:int,"
            + "           a2:int,b2:int,c2:int,d2:int,e2:int,f2:int):int {"
            + "  println add12(f,e,d,c,b,a,a2,b2,c2,d2,e2,f2) "
            + "  return add12(f,e,d,c,b,a,a2,b2,c2,d2,e2,f2) "
            + "}"
            + " println add122(1,2,3,4,5,6,7,8,9,10,11,12) ")
        .executedEqualsInterpreted();
  }

  @Test
  public void moreThan4StringParams() throws Exception {
    assertThatCompiling(
        "      add6:proc(a:string,b:string,c:string,d:string,e:string,f:string):string {"
            + "  println e"
            + "  println f"
            + "  return a+b+c+d+e+f"
            + "}"
            + " println add6('a','b','c','d','e','f') ")
        .executedEqualsInterpreted();
  }

  @Test
  public void moreThan4IntLocalParams() throws Exception {
    assertThatCompiling("      add6:proc(a:int,b:int,c:int,d:int,e:int,f:int):int {"
        + "  println e"
        + "  println f"
        + "  return a+b+c+d+e+f"
        + "}"
        + "doit:proc(a:int,b:int,c:int,d:int,e:int,f:int):int {"
        + "  aa = -a bb = -b dd=d*2 cc=c+5 ff = -f ee = -e"
        + "  return add6(ff,ee,dd,cc,bb,aa)"
        + "}"
        + " println doit(1,2,3,4,5,6) ").executedEqualsInterpreted();
  }

  @Test
  public void incDecOfLocal() throws Exception {
    assertThatCompiling("      g:proc(arg: int): int {\n"
        + "  local = arg\n"
        + "  local++\n"
        + "  return local\n"
        + "}\n"
        + "result = g(3)"
        + "if result != 4 { exit 'Should have been 4'}").executedEqualsInterpreted();
  }
}
