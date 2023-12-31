package com.plasstech.lang.d2.codegen.x64;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameterInjector;

@RunWith(TestParameterInjector.class)
public class NasmCodeGeneratorProcTest extends NasmCodeGeneratorTestBase {
  @Test
  public void voidProcNoArgs() throws Exception {
    execute(
        "      a=1 \n"
            + "procTest:proc() { \n" //
            + "   if a==1 {println a return } else {println 4 return} \n" //
            + "} \n" //
            + "procTest()",
        "voidProcNoArgs");
  }

  @Test
  public void procReturnsInt() throws Exception {
    execute(
        "      fun:proc():int { \n" //
            + "   return 3 \n" //
            + "} \n" //
            + "x=fun() print x",
        "procReturnsInt");
  }

  @Test
  public void procReturnsBool() throws Exception {
    execute(
        "      fun:proc():bool { \n" //
            + "   return true \n" //
            + "} \n" //
            + "x=fun() print x",
        "procReturnsBool");
  }

  @Test
  public void procReturnsString() throws Exception {
    execute(
        "      fun:proc():string { \n" //
            + "   return 'hi' \n" //
            + "} \n" //
            + "x=fun() print x",
        "procReturnsString");
  }

  @Test
  public void procIntParam() throws Exception {
    execute(
        "      procIntParam:proc(n:int):int { "
            + "   return n+1"
            + "}"
            + "x=procIntParam(4) "
            + "print x",
        "procReturnsInt");
  }

  @Test
  public void procLocals() throws Exception {
    execute(
        "      procLocals:proc(n:int):int { "
            + "  // a is a local \n"
            + "  a=n+1"
            + "  return a"
            + "} "
            + "x=procLocals(4) "
            + "print x",
        "procLocals");
  }

  @Test
  public void bug102ParamMunge() throws Exception {
    execute(
        "      f2: proc(c2:bool, b2:string, d2:int, a2:int) {"
            + "  print 'c=' println c2 print 'b=' println b2 print 'a=' println a2 "
            + "}"
            + "f1: proc(a1:int, c1:bool, b1:string, d1:int) {"
            + "   f2(c1,b1,a1,d1) "
            + "}"
            + "f1(2,true, 'hi', 4)",
        "bug102ParamMunge");
  }

  @Test
  public void bug102Redux() throws Exception {
    execute(
        "makeSymbol: proc(it: int, start: int): int {\r"
            + "  return makeToken(3, start, start)\r"
            + "} \r"
            + "makeToken:proc(type:int, start:int, end:int): int {\r"
            + "  return start\r"
            + "} \r"
            + "println makeSymbol(1,2)\r",
        "bug102Redux");
  }

  private static final String FOUR_PARAM_PROC =
      "  procParamFirst4Locations:proc(p3:bool, p4: string, p1: int, p2: int) { "
          + "  print p2 println p1 print p2 println p1 print p3 print p4 \n"
          + "  p1 = p2 + 1 "
          + "  p3 = p1 == p2"
          + "  print p1 "
          + "  print p3"
          + "} "
          + "glob='theglob' "
          + "print glob "
          + "procParamFirst4Locations(true,'thep4',1,3) "
          + "procParamFirst4Locations(false,'thep4',-1,-2) ";

  @Test
  public void procParamFirst4Locations() throws Exception {
    execute(FOUR_PARAM_PROC, "procParamFirst4Locations");
  }

  @Test
  public void allOpsLocals() throws Exception {
    execute(
        "      allOpsLocals:proc():int { \n"
            + "   a=1 b=2 c=3 d=4 e=5 f=6 g=3\n"
            + "   g=a+a*b+(b+c)*d-(c+d)/e+(d-e)*f  \n"
            + "   b=a+a*b+(b+c)*d-(c+d)/e+(d-e)*f  \n"
            + "   return g+b \n"
            + "} \n"
            + "print allOpsLocals()",
        "allOpsLocals");
  }

  @Test
  public void allOpsLocalsMixed() throws Exception {
    execute(
        "      allOpsLocals:proc(a:int, b:double, c:int, d:double):double { \n"
            + "   e=5.0 f=6\n"
            + "   g=a+a*c+(a+c)*f-(c+a)/f+(c-a)*f  \n"
            + "   b=e+e*b+(b+d)*d-(e+d)/e+(e-d)*b  \n"
            + "   return b \n"
            + "} \n"
            + "print allOpsLocals(1, 2.0, 3, 4.0)",
        "allOpsLocals");
  }

  @Test
  public void recursion() throws Exception {
    execute(
        "      reverseRecursive: proc(s: string): string {"
            + "  return reverse2(s, length(s))"
            + "} "
            + "reverse2: proc(s: string, start: int): string {"
            + "  if start == 0 {"
            + "    return '' "
            + "  } else {"
            + "    return s[start - 1] + reverse2(s, start - 1) "
            + "  } "
            + "} "
            + "  println 'Recursive reverse ' + reverseRecursive('Reverse')",
        "recursion");
  }

  @Test
  public void bug39() throws Exception {
    execute(
        "      a:string a='bye'"
            + "setup: proc {"
            + "  a = 'hi'"
            + "  b = 'bee'"
            + "}"
            + "setup()",
        "bug39");
  }

  @Test
  public void forwardRef() throws Exception {
    execute(
        "      p2()\r\n"
            + "p2: proc {\r\n"
            + "  println \"Should print 1\"\r\n"
            + "  // forward reference\r\n"
            + "  val = p1()\r\n"
            + "  print val\r\n"
            + "}\r\n"
            + "p1: proc: int {\r\n"
            + "  return 1\r\n"
            + "}\r\n",
        "forwardRef");
  }

  @Test
  public void ignoreReturn() throws Exception {
    execute(
        "      a:proc(): string {" //
            + "  return 'aproc'"
            + "}" //
            + "a()",
        "ignoreReturn");
  }

  @Test
  @Ignore("Bug #188: Runs out of registers, even with optimization")
  public void outOfRegisters() throws Exception {
    execute(
        "      fun:proc(a:int, b:int, cc:int, dd:int) {\r"
            + "  c=((a+b)*((a+b)*((a+b)*((a+b)*((a+b)*((a+b)*((a+b)*((a+b)*((a+b)*((a+b)*((a+b)*((a+b)*((a+b)*(a+b))))))))))))))\r"
            + "  println c\r"
            + "}\r"
            + "fun(1,2,3,4)\r",
        "outOfRegs");
  }

  @Test
  public void fact() throws Exception {
    execute(
        "      dofact:proc(n:int): int {"
            + "  fact = 1 "
            + "  unchanged = 0"
            + "  i=1 while i <= 10 do i = i + 1 {"
            + "    unchanged = 0"
            + "    fact = fact * i"
            + "  }"
            + "  return fact + unchanged"
            + "}"
            + "print dofact(10)",
        "fact");
  }

  @Test
  public void moreThan4BoolParams() throws Exception {
    execute(
        "      and6:proc(a:bool,b:bool,c:bool,d:bool,e:bool,f:bool):bool {"
            + "  println e"
            + "  println f"
            + "  return a and b and c and d and e and f"
            + "}"
            + " println and6(true, false, true, false, true, false)",
        "params6");
  }

  @Test
  public void moreThan4IntParams() throws Exception {
    execute(
        "      add6:proc(a:int,b:int,c:int,d:int,e:int,f:int):int {"
            + "  println e"
            + "  println f"
            + "  return a+b+c+d+e+f"
            + "}"
            + " println add6(1,2,3,4,5,6) ",
        "params6");
  }

  @Test
  public void manyManyParams() throws Exception {
    execute(
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
            + " println add122(1,2,3,4,5,6,7,8,9,10,11,12) ",
        "params6");
  }

  @Test
  public void moreThan4StringParams() throws Exception {
    execute(
        "      add6:proc(a:string,b:string,c:string,d:string,e:string,f:string):string {"
            + "  println e"
            + "  println f"
            + "  return a+b+c+d+e+f"
            + "}"
            + " println add6('a','b','c','d','e','f') ",
        "params6");
  }

  @Test
  public void moreThan4DoubleParams() throws Exception {
    execute(
        "      add6:proc(a:double,b:double,c:double,d:double,e:double,f:double):double {"
            + "  println e"
            + "  println f"
            + "  return a+b+c+d+e+f"
            + "}"
            + " println add6(1.,2.,3.,4.,5.,6.) ",
        "params6");
  }

  @Test
  public void moreThan4IntLocalParams() throws Exception {
    execute(
        "      add6:proc(a:int,b:int,c:int,d:int,e:int,f:int):int {"
            + "  println e"
            + "  println f"
            + "  return a+b+c+d+e+f"
            + "}"
            + "doit:proc(a:int,b:int,c:int,d:int,e:int,f:int):int {"
            + "  aa = -a bb = -b ff = -f ee = -e"
            + "  return add6(ff,ee,d,c,bb,aa)"
            + "}"
            + " println doit(1,2,3,4,5,6) ",
        "params6");
  }

  @Test
  public void moreThan4StringLocalParams() throws Exception {
    execute(
        "      add6:proc(a:string,b:string,c:string,d:string,e:string,f:string):string {"
            + "  println e"
            + "  println f"
            + "  return a+b+c+d+e+f"
            + "}"
            + "doit:proc(a:string,b:string,c:string,d:string,e:string,f:string):string {"
            + "  aa = a[0] ee = e[0]"
            + "  return add6(f,ee,d,c,b,aa)"
            + "}"
            + " println doit('aa','bb','cc','dd','ee','ff') ",
        "params6");
  }

  @Test
  public void moreThan4DoubleLocalParams() throws Exception {
    execute(
        "      add6:proc(a:double,b:double,c:double,d:double,e:double,f:double):double {"
            + "  println e"
            + "  println f"
            + "  return a+b+c+d+e+f"
            + "}"
            + "doit:proc(a:double,b:double,c:double,d:double,e:double,f:double):double {"
            + "  aa = -a bb = -b ff = -f ee = -e"
            + "  return add6(ff,ee,d,c,bb,aa) "
            + "}"
            + " println doit(1.,2.,3.,4.,5.,6.) ",
        "params6");
  }

  @Test
  public void moreThan4BoolLocalParams() throws Exception {
    execute(
        "      add6:proc(a:bool,b:bool,c:bool,d:bool,e:bool,f:bool):bool {"
            + "  println e"
            + "  println f"
            + "  return a and b and c and d and e and f"
            + "}"
            + "doit:proc(a:bool,b:bool,c:bool,d:bool,e:bool,f:bool):bool {"
            + "  aa = not a bb = not b ee = not e ff = not f"
            + "  return add6(ff,ee,d,c,bb,aa)"
            + "}"
            + " println doit(true, false, true, false, true, false) ",
        "params6");
  }

  @Test
  public void incDecOfLocal() throws Exception {
    execute(
        "      g:proc(arg: int): int {\n"
            + "  local = arg\n"
            + "  local++\n"
            + "  return local\n"
            + "}\n"
            + "result = g(3)"
            + "if result != 4 { exit 'Should have been 4'}",
        "incDecOfLocal");
  }

  @Test
  public void incDecOfConst() throws Exception {
    execute(
        "      g:proc: int {\n"
            + "  local = 3\n"
            + "  local++\n"
            + "  return local\n"
            + "}\n"
            + "result = g()"
            + "if result != 4 { exit 'Should have been 4'}",
        "incDecOfConst");
  }
}
