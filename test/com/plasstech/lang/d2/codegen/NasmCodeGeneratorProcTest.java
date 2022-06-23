package com.plasstech.lang.d2.codegen;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assume.assumeFalse;

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
  public void procTest() throws Exception {
    execute(
        "      a=1 \n"
            + "procTest:proc() { \n" //
            + "   if a==1 {println a return } else {println 4 return} \n" //
            + "} \n" //
            + "procTest()",
        "procTest");
  }

  @Test
  public void procReturnsInt() throws Exception {
    assumeFalse(optimize);
    execute(
        "      fun:proc():int { \n" //
            + "   return 3 \n" //
            + "} \n" //
            + "x=fun() print x",
        "procReturnsInt");
  }

  @Test
  public void procReturnsBool() throws Exception {
    assumeFalse(optimize);
    execute(
        "      fun:proc():bool { \n" //
            + "   return true \n" //
            + "} \n" //
            + "x=fun() print x",
        "procReturnsBool");
  }

  @Test
  public void procReturnsString() throws Exception {
    assumeFalse(optimize);
    execute(
        "      fun:proc():string { \n" //
            + "   return 'hi' \n" //
            + "} \n" //
            + "x=fun() print x",
        "procReturnsString");
  }

  @Test
  public void procIntParam() throws Exception {
    // cannot run with optimize=true because the inline optimizer is creating a "local"
    // variable, which cannot be generated yet.
    assumeFalse(optimize);
    execute(
        "      procIntParam:proc(n:int):int { \n" //
            + "   return n+1 \n" //
            + "} \n" //
            + "x=procIntParam(4) print x",
        "procReturnsInt");
  }

  @Test
  public void procLocals() throws Exception {
    assumeFalse(optimize);
    execute(
        "      procLocals:proc(n:int):int { \n" //
            + "   a=n+1 return a\n" //
            + "} \n" //
            + "x=procLocals(4) print x",
        "procLocals");
  }

  @Test
  public void procParamFirst4Locations() {
    assumeFalse(optimize);
    // this is failing because:
    // 1. print p1 is losing the value of p1 (ECX) because it's being overwritten by the setup for
    // "print"

    State state =
        compileToNasm(
            "procParamFirst4Locations:proc(p1: int, p2: bool, p3: int, p4: string) { "
                + "  print p1 print p2 print p3 print p4"
                + "} "
                + "procParamFirst4Locations(1,true,3,'hi')");
    ProgramNode root = state.programNode();
    ProcedureNode proc = (ProcedureNode) (root.statements().statements().get(0));

    // RCX, RDX, R8, and R9

    // Should this be in parser? Or codegen?
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
}
