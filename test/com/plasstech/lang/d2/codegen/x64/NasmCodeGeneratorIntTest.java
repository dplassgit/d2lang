package com.plasstech.lang.d2.codegen.x64;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;

@RunWith(TestParameterInjector.class)
public class NasmCodeGeneratorIntTest extends NasmCodeGeneratorTestBase {
  @Test
  public void intUnaryOps(@TestParameter({"-", "!"}) String op) throws Exception {
    execute(String.format("a=3 b=%sa println b", op), "intUnaryOps");
  }

  @Test
  public void intParamUnaryOps(@TestParameter({"-", "!"}) String op) throws Exception {
    execute(String.format("f:proc(a:int, b:int) {b=%sa println b} f(2,3)", op), "intUnaryOps");
  }

  @Test
  public void intBinOps(
      @TestParameter({"+", "-", "*", "/", "&", "|", "^", "%"}) String op,
      @TestParameter({"1234", "-23456"}) int first,
      @TestParameter({"2345", "-34567"}) int second)
      throws Exception {
    execute(
        String.format(
            "a=%d b=%d "
                + "c=a %s b println c "
                + "d=b %s a println d "
                + "e=a %s a println e "
                + "f=b %s b println f",
            first, second, op, op, op, op),
        "intBinOps");
  }

  @Test
  public void intCompOps(
      @TestParameter({"<=", "!=", ">"}) String op,
      @TestParameter({"1234", "-24567"}) int first,
      @TestParameter({"2345", "-34567"}) int second)
      throws Exception {
    execute(
        String.format(
            "      a=%d b=%d " //
                + "c=a %s b println c " //
                + "d=b %s a println d " //
                + "e=a %s %d println e " //
                + "f=%d %s b println f",
            first, second, op, op, op, second, first, op),
        "intCompOps");
  }

  @Test
  public void intCompOpsArgs(
      @TestParameter({"<", "==", ">="}) String op,
      @TestParameter({"1234", "-24567"}) int first,
      @TestParameter({"2345", "-34567"}) int second)
      throws Exception {
    String program =
        String.format(
            "    bb:int g:bool f:proc(a:int, b:int) { " //
                + "aa=a " //
                + "c=a %s b println c " //
                + "d=b %s aa println d " //
                + "e=aa %s %d println e " //
                + "g=%d %s aa println g " // global g
                + "bb=b "
                + "h=bb < 3 println h"
                + "} "
                + "f(%d,%d)",
            op, op, op, first, second, op, first, second);
    execute(program, "intCompOpsArgs");
  }

  @Test
  public void shiftOpsProc(@TestParameter({"<<", ">>"}) String op) throws Exception {
    String program =
        String.format(
            "f:proc(a:int) {b=4 b=b %s a println a a=a%sb println a c=a<<2 println c} f(2)", op,
            op);
    execute(program, "shiftOpsProc");
  }

  @Test
  public void shiftSelf() throws Exception {
    execute("f:proc(a:int) {a=a << a println a} f(2)", "shiftOpsProc");
  }

  @Test
  public void increment() throws Exception {
    execute("a=3 a++ if a!=4 {exit('increment is broken')} println a", "increment");
  }

  @Test
  public void decrement() throws Exception {
    execute("a=3 a-- if a!=2 {exit('decrement is broken')} println a", "decrement");
  }

  @Test
  public void divLoop() throws Exception {
    execute("a=10000 while a > 0 {println a a = a / 10 }", "divLoop");
  }

  @Test
  public void divisionByZeroGlobal() throws Exception {
    String sourceCode = "a=0 b=1/a";
    //    assertGenerateError(sourceCode, "Division by 0");
    assertRuntimeError(sourceCode, "divisionByZeroLocal", "Division by 0");
  }

  @Test
  public void divisionByZeroLocal() throws Exception {
    String sourceCode = "f:proc:int {a=0 b=1/a return b} f()";
    assertGenerateError(sourceCode, "Division by 0");
    //    assertRuntimeError(sourceCode, "divisionByZeroLocal", "Division by 0");
  }

  @Test
  public void opEquals(
      @TestParameter({"+=", "-=", "*=", "/="}) String op)
      throws Exception {
    execute(
        String.format("f:proc(a: int, b:int) { a %s b c=b c %s a println a println c} f(100, 10)",
            op, op),
        "opEquals");
  }
}
