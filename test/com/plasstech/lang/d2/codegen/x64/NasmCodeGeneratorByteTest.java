package com.plasstech.lang.d2.codegen.x64;

import static com.google.common.truth.TruthJUnit.assume;
import static com.plasstech.lang.d2.codegen.x64.testing.ExecutionSubject.assertThatCompiling;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;

@RunWith(TestParameterInjector.class)
public class NasmCodeGeneratorByteTest {
  @TestParameter
  boolean optimize;

  @Test
  public void byteUnaryOps(@TestParameter({"-", "!"}) String op) throws Exception {
    assertThatCompiling(String.format("a=0y3 b=%sa print b", op)).withOptimize(optimize)
        .executedEqualsInterpreted();
  }

  @Test
  public void byteBinOps(
      @TestParameter({"+", "-", "*", "&", "|", "^", "%"}) String op,
      @TestParameter({"0y34", "0yf3"}) String first,
      @TestParameter({"0y12", "0ye3"}) String second)
      throws Exception {
    assertThatCompiling(String.format(
        "      a=%s b=%s c=a %s b println c "
            + "d=b %s a println d "
            + "e=a %s a println e "
            + "f=b %s b println f",
        first, second, op, op, op, op)).withOptimize(optimize).withOptimize(optimize)
        .executedEqualsInterpreted();
  }

  @Test
  public void byteMul() throws Exception {
    assertThatCompiling("f1:proc {a=0y3e b=0y2 c=a * b * 0y03 print c} f1()").withOptimize(optimize)
        .executedEqualsInterpreted();
    assertThatCompiling("f2:proc(a:byte, b:byte) {c=a * b print c} f2(0y3e, 0y2)")
        .withOptimize(optimize).executedEqualsInterpreted();
    assertThatCompiling("b=0y2 f3:proc(a:byte) {c=a * b print c} f3(0y3e)")
        .withOptimize(optimize).executedEqualsInterpreted();
  }

  @Test
  public void byteMulAdjacentInMemory() throws Exception {
    assertThatCompiling("a=0y3e b=0y2 f1:proc {c=a * b * 0y03 print c} f1()").withOptimize(optimize)
        .executedEqualsInterpreted();
  }

  @Test
  public void byteDiv() throws Exception {
    assertThatCompiling("f:proc {a=0y3e b=0y0a c=a / b print c} f()").withOptimize(optimize)
        .executedEqualsInterpreted();
    assertThatCompiling("f:proc(a:byte, b:byte) {c=a / b print c} f(0y3e, 0yf2)")
        .withOptimize(optimize).executedEqualsInterpreted();
    assertThatCompiling("b=0yf2 f:proc(a:byte) {c=a / b print c} f(0y3e)")
        .withOptimize(optimize).executedEqualsInterpreted();
    assertThatCompiling("f=0y6 k=0y4/(0y5+(0y4-0y5*f)) print k").withOptimize(optimize)
        .executedEqualsInterpreted();
  }

  @Test
  public void byteCompOps(
      @TestParameter({"<", "==", "!=", ">="}) String op,
      @TestParameter({"0y34", "0yf7"}) String first,
      @TestParameter({"0y34", "0yf7"}) String second)
      throws Exception {
    assertThatCompiling(String.format(
        "      a=%s b=%s " //
            + "c=a %s b print c " //
            + "d=b %s a print d",
        first, second, op, op)).withOptimize(optimize).executedEqualsInterpreted();
  }

  @Test
  public void shiftOps(@TestParameter({"<<", ">>"}) String op) throws Exception {
    assertThatCompiling(
        String.format("a=0y23 b=0y4 c=a %s b print c a=0yF4 d=b %s a print d", op, op))
        .withOptimize(optimize).executedEqualsInterpreted();
  }

  @Test
  public void incDec() throws Exception {
    assertThatCompiling("a=0y42 a++ println a a=0y41 a-- println a").withOptimize(optimize)
        .executedEqualsInterpreted();
  }

  @Test
  public void divisionByZeroLocal() throws Exception {
    assume().that(optimize).isFalse();
    String sourceCode = "f:proc:byte {a=0y0 b=0y1/a return b} f()";
    assertThatCompiling(sourceCode).withOptimize(true).hasCompileTimeError("Division by 0");
    assertThatCompiling(sourceCode).withOptimize(false).withRuntimeError("Division by 0")
        .executes();
  }
}
