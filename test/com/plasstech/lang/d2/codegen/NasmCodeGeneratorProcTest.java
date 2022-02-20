package com.plasstech.lang.d2.codegen;

import static org.junit.Assume.assumeFalse;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameterInjector;

@RunWith(TestParameterInjector.class)
public class NasmCodeGeneratorProcTest extends NasmCodeGeneratorTestBase {
  @Test
  public void proc() throws Exception {
    execute(
        "      a=1 \n"
            + "fun:proc() { \n" //
            + "   if a==1 {println a return } else {println 4 return} \n" //
            + "} \n" //
            + "fun()",
        "proc");
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
  @Ignore
  public void procIntParam() throws Exception {
    execute(
        "      fun:proc(n:int):int { \n" //
            + "   return n+1 \n" //
            + "} \n" //
            + "x=fun(4) print x",
        "procReturnsInt");
  }
}