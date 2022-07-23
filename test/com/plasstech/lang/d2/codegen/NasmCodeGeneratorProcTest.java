package com.plasstech.lang.d2.codegen;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.plasstech.lang.d2.parse.node.ProcedureNode;
import com.plasstech.lang.d2.parse.node.ProcedureNode.Parameter;
import com.plasstech.lang.d2.parse.node.ProgramNode;
import com.plasstech.lang.d2.phase.State;

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

    State state = compileToNasm(FOUR_PARAM_PROC);
    ProgramNode root = state.programNode();
    ProcedureNode proc = (ProcedureNode) (root.statements().statements().get(0));

    // RCX, RDX, R8, and R9
    Parameter param1 = proc.parameters().get(0);
    assertThat(param1.location()).isInstanceOf(RegisterLocation.class);
    RegisterLocation register1 = (RegisterLocation) param1.location();
    assertThat(register1.register()).isEqualTo(Register.RCX);

    Parameter param2 = proc.parameters().get(1);
    assertThat(param2.location()).isInstanceOf(RegisterLocation.class);
    RegisterLocation register2 = (RegisterLocation) param2.location();
    assertThat(register2.register()).isEqualTo(Register.RDX);

    Parameter param3 = proc.parameters().get(2);
    assertThat(param3.location()).isInstanceOf(RegisterLocation.class);
    RegisterLocation register3 = (RegisterLocation) param3.location();
    assertThat(register3.register()).isEqualTo(Register.R8);

    Parameter param4 = proc.parameters().get(3);
    assertThat(param4.location()).isInstanceOf(RegisterLocation.class);
    RegisterLocation register4 = (RegisterLocation) param4.location();
    assertThat(register4.register()).isEqualTo(Register.R9);
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
  public void recursion() throws Exception {
    execute(
        "      reverseRecursive: proc(s: string): string {"
            + "  return reverse2(s, length(s), 0, false)"
            + "} "
            + "reverse2: proc(s: string, start: int, ignored: int, ignored2: bool): string {"
            + "  if start == 0 {"
            + "    return '' "
            + "  } else {"
            + "    return s[start - 1] + reverse2(s, start - 1, start + 1, start==1) "
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
            + "main {"
            + "  setup()"
            + "}",
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
}
