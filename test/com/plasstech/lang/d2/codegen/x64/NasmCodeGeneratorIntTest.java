package com.plasstech.lang.d2.codegen.x64;

import static com.plasstech.lang.d2.codegen.x64.testing.ExecutionSubject.assertThatCompiling;
import static org.junit.Assume.assumeFalse;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;

@RunWith(TestParameterInjector.class)
public class NasmCodeGeneratorIntTest {
  @TestParameter
  boolean optimize;

  @Test
  public void intUnaryOps(@TestParameter({"-", "!"}) String op) throws Exception {
    assertThatCompiling(String.format("a=3 b=%sa println b", op)).withOptimize(optimize)
        .executedEqualsInterpreted();
  }

  @Test
  public void intParamUnaryOps(@TestParameter({"-", "!"}) String op) throws Exception {
    assertThatCompiling(String.format("f:proc(a:int, b:int) {b=%sa println b} f(2,3)", op))
        .withOptimize(optimize)
        .executedEqualsInterpreted();
  }

  @Test
  public void intBinOps(
      @TestParameter({"+", "-", "*", "&", "|", "^"}) String op,
      @TestParameter({"1024", "-23456"}) int first,
      @TestParameter({"2048", "-34567"}) int second)
      throws Exception {
    assertThatCompiling(String.format(
        "a=%d b=%d "
            + "c=a %s b println c "
            + "d=b %s a println d "
            + "e=a %s a println e "
            + "f=b %s b println f",
        first, second, op, op, op, op)).withOptimize(optimize).executedEqualsInterpreted();
  }

  @Test
  public void divMod(
      @TestParameter({"/", "%"}) String op,
      @TestParameter({"1234", "-25"}) int first,
      @TestParameter({"-2345", "37"}) int second,
      @TestParameter boolean optimize)
      throws Exception {
    assertThatCompiling(String.format(
        "      a=%d b=%d "
            + "c=a %s b println c "
            + "dp:proc(ad:int, bd:int) { d=ad %s bd println d} "
            + "ep:proc() { ae=%d be=%d e=be %s ae println e} "
            + "dp(a,b) "
            + "ep()",
        first, second, op, op, first, second, op)).withOptimize(optimize)
        .executedEqualsInterpreted();
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
        .withOptimize(optimize)
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
    assertThatCompiling(program).withOptimize(optimize).executedEqualsInterpreted();
  }

  @Test
  public void shiftOpsProc(@TestParameter({"<<", ">>"}) String op,
      @TestParameter boolean optimize) throws Exception {
    String program =
        String.format(
            "f:proc(a:int) {b=4 b=b %s a println a a=a %s b println a c=a<<2 println c} f(2)", op,
            op);
    assertThatCompiling(program).withOptimize(optimize).executedEqualsInterpreted();
  }

  @Test
  public void shiftOpsGlobal(@TestParameter({"<<", ">>"}) String op,
      @TestParameter boolean optimize) throws Exception {
    String program =
        String.format(
            "a=2 b=4 b=b %s a println a a=a %s b println a c=a<<2 println c", op,
            op);
    assertThatCompiling(program).withOptimize(optimize).executedEqualsInterpreted();
  }

  @Test
  public void shiftParamWhenRcxIsUsed(@TestParameter boolean optimize) throws Exception {
    assertThatCompiling(
        "f:proc(donottouch: int, a:int) {a=a << a println a println donottouch} f(1, 2)")
        .withOptimize(optimize)
        .executedEqualsInterpreted();
  }

  @Test
  public void shiftParamInExpression(@TestParameter boolean optimize) throws Exception {
    assertThatCompiling(
        "f:proc(b: int, a:int) {a=(b>>2) + a << (a+b) println a} f(256, 2)")
        .withOptimize(optimize)
        .executedEqualsInterpreted();
  }

  @Test
  public void shiftSelfParam(@TestParameter boolean optimize) throws Exception {
    assertThatCompiling("f:proc(a:int) {a=a << a println a} f(2)").withOptimize(optimize)
        .executedEqualsInterpreted();
  }

  @Test
  public void shiftSelfLocal(@TestParameter boolean optimize) throws Exception {
    assertThatCompiling("f:proc(a:int) {b=a+1 b=b << b println b} f(2)").withOptimize(optimize)
        .executedEqualsInterpreted();
  }

  @Test
  public void shiftSelf() throws Exception {
    assertThatCompiling("f:proc(a:int) {a=a << a println a} f(2)").withOptimize(optimize)
        .executedEqualsInterpreted();
  }

  @Test
  public void increment() throws Exception {
    assertThatCompiling("a=3 a++ if a!=4 {exit('increment is broken')} println a")
        .withOptimize(optimize)
        .executedEqualsInterpreted();
  }

  @Test
  public void decrement() throws Exception {
    assertThatCompiling("a=3 a-- if a!=2 {exit('decrement is broken')} println a")
        .withOptimize(optimize)
        .executedEqualsInterpreted();
  }

  @Test
  public void divLoop() throws Exception {
    assertThatCompiling("a=10000 while a > 0 {println a a = a / 10 }").withOptimize(optimize)
        .executedEqualsInterpreted();
  }

  @Test
  public void divisionByZeroGlobal() throws Exception {
    assumeFalse(optimize);
    String sourceCode = "a=0 b=1/a";
    assertThatCompiling(sourceCode).withOptimize(true).hasCompileTimeError("Division by 0");
    assertThatCompiling(sourceCode).withOptimize(false).withRuntimeError("Division by 0")
        .executes();
  }

  @Test
  public void divisionByZeroLocal() throws Exception {
    assumeFalse(optimize);
    String sourceCode = "f:proc:int {a=0 b=1/a return b} f()";
    assertThatCompiling(sourceCode).withOptimize(true).hasCompileTimeError("Division by 0");
    assertThatCompiling(sourceCode).withOptimize(false).withRuntimeError("Division by 0")
        .executes();
  }

  @Test
  public void simpleParamBinop() throws Exception {
    assertThatCompiling("f:proc(a:int):int { a=a+3 print a return a} f(1)")
        .withOptimize(optimize).executedEqualsInterpreted();
  }

  @Test
  public void simpleLocalBinop() throws Exception {
    assertThatCompiling("f:proc(a:int):int { b=a+3 print b return b} f(1)")
        .withOptimize(optimize).executedEqualsInterpreted();
  }

  @Test
  public void opEquals(
      @TestParameter({"+=", "-=", "*=", "/="}) String op)
      throws Exception {
    assertThatCompiling(
        String.format("f:proc(a: int, b:int) { a %s b c=b c %s a println a println c} f(100, 10)",
            op, op))
        .withOptimize(optimize).executedEqualsInterpreted();
  }

  @Test
  public void bug360() {
    String program = ""
        + "buffering=0\n"
        + "setBuffering: proc(newval:int):int {\n"
        + "  oldbuffering = buffering\n"
        + "  buffering = newval\n"
        + "  return oldbuffering\n"
        + "}\n"
        + "doit: proc(i:int) {\n"
        + "  if i < 2 {\n"
        + "    old = setBuffering(1)\n"
        + "    println \"Setting buffering to 1\"\n"
        + "    setBuffering(old)\n"
        + "    print \"buffering now = \" println buffering\n"
        + "  }\n"
        + "}\n"
        + "doit(0)\n";
    assertThatCompiling(program).withOptimize(optimize).executedEqualsInterpreted();
  }
}
