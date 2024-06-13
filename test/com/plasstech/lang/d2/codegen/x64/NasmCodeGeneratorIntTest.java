package com.plasstech.lang.d2.codegen.x64;

import static com.plasstech.lang.d2.codegen.x64.testing.ExecutionSubject.assertThatCompiling;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;

@RunWith(TestParameterInjector.class)
public class NasmCodeGeneratorIntTest {
  @Test
  public void intUnaryOps(@TestParameter({"-", "!"}) String op) throws Exception {
    assertThatCompiling(String.format("a=3 b=%sa println b", op)).executedEqualsInterpreted();
  }

  @Test
  public void intParamUnaryOps(@TestParameter({"-", "!"}) String op) throws Exception {
    assertThatCompiling(String.format("f:proc(a:int, b:int) {b=%sa println b} f(2,3)", op))
        .executedEqualsInterpreted();
  }

  @Test
  public void intBinOps(
      @TestParameter({"+", "-", "*", "/", "&", "|", "^", "%"}) String op,
      @TestParameter({"1234", "-23456"}) int first,
      @TestParameter({"2345", "-34567"}) int second)
      throws Exception {
    assertThatCompiling(String.format(
        "a=%d b=%d "
            + "c=a %s b println c "
            + "d=b %s a println d "
            + "e=a %s a println e "
            + "f=b %s b println f",
        first, second, op, op, op, op)).executedEqualsInterpreted();
  }

  @Test
  public void intCompOps(
      @TestParameter({"<=", "!=", ">"}) String op,
      @TestParameter({"1234", "0"}) int first,
      @TestParameter({"0", "-34567"}) int second)
      throws Exception {
    assertThatCompiling(String.format(
        "      a=%d b=%d "
            + "  f:proc {} " // suppresses constant propagation optimizer 
            + "  if a %s b { println true } "
            + "  if b %s a { println true } "
            + "  if a %s %d { println true } "
            + "  if %d %s b { println true }"
            + "f()", // without the call, the declaration will be optimized out
        first, second, op, op, op, second, first, op))
        .executedEqualsInterpreted();
  }

  @Test
  public void intCompOpsArgs(
      @TestParameter({"<", "==", ">="}) String op,
      @TestParameter({"1234", "-24567"}) int first,
      @TestParameter({"2345", "-34567"}) int second)
      throws Exception {
    String program =
        String.format(
            "    bb:int "
                + "f:proc(a:int, b:int) { "
                + "  aa=a "
                + "  if a %s b { println true } "
                + "  if b %s aa { println true }  "
                + "  if aa %s %d { println true } "
                + "  if %d %s aa { println true } "
                + "  bb=b "
                + "  if bb < 3 {println true}"
                + "} "
                + "f(%d,%d)",
            op, op, op, first, second, op, first, second);
    assertThatCompiling(program).executedEqualsInterpreted();
  }

  @Test
  public void shiftOpsProc(@TestParameter({"<<", ">>"}) String op) throws Exception {
    String program =
        String.format(
            "f:proc(a:int) {b=4 b=b %s a println a a=a%sb println a c=a<<2 println c} f(2)", op,
            op);
    assertThatCompiling(program).executedEqualsInterpreted();
  }

  @Test
  public void shiftSelf() throws Exception {
    assertThatCompiling("f:proc(a:int) {a=a << a println a} f(2)").executedEqualsInterpreted();
  }

  @Test
  public void increment() throws Exception {
    assertThatCompiling("a=3 a++ if a!=4 {exit('increment is broken')} println a")
        .executedEqualsInterpreted();
  }

  @Test
  public void decrement() throws Exception {
    assertThatCompiling("a=3 a-- if a!=2 {exit('decrement is broken')} println a")
        .executedEqualsInterpreted();
  }

  @Test
  public void divLoop() throws Exception {
    assertThatCompiling("a=10000 while a > 0 {println a a = a / 10 }").executedEqualsInterpreted();
  }

  @Test
  public void divisionByZeroGlobal() throws Exception {
    String sourceCode = "a=0 b=1/a";
    assertThatCompiling(sourceCode).hasCompileTimeError("Division by 0");
  }

  @Test
  public void divisionByZeroLocal() throws Exception {
    String sourceCode = "f:proc:int {a=0 b=1/a return b} f()";
    assertThatCompiling(sourceCode).withRuntimeError("Division by 0").executes();
    assertThatCompiling(sourceCode).withOptimize(false).withRuntimeError("Division by 0")
        .executes();
  }

  @Test
  public void simpleParamBinop() throws Exception {
    assertThatCompiling("f:proc(a:int):int { a=a+3 print a return a} f(1)")
        .executedEqualsInterpreted();
  }

  @Test
  public void simpleLocalBinop() throws Exception {
    assertThatCompiling("f:proc(a:int):int { b=a+3 print b return b} f(1)")
        .executedEqualsInterpreted();
  }

  @Test
  public void opEquals(
      @TestParameter({"+=", "-=", "*=", "/="}) String op)
      throws Exception {
    assertThatCompiling(
        String.format("f:proc(a: int, b:int) { a %s b c=b c %s a println a println c} f(100, 10)",
            op, op))
        .executedEqualsInterpreted();
  }
}
