package com.plasstech.lang.d2.codegen.x64;

import static com.plasstech.lang.d2.codegen.x64.testing.ExecutionSubject.assertThatCompiling;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(TestParameterInjector.class)
public class NasmCodeGeneratorBoolTest {
  @Test
  public void not(@TestParameter boolean bool) throws Exception {
    assertThatCompiling(String.format("a=%s c=not a println a println c", bool))
        .executedEqualsInterpreted();
  }

  @Test
  public void notProc(@TestParameter boolean bool) throws Exception {
    assertThatCompiling(String.format("f:proc{a=%s b=not a println a println b} f()", bool))
        .executedEqualsInterpreted();
  }

  @Test
  public void boolBinOp(
      @TestParameter({"and", "or", "xor", "<=", "!=", ">"}) String op,
      @TestParameter boolean boola,
      @TestParameter boolean boolb)
      throws Exception {
    assertThatCompiling(
            String.format("a=%s b=%s c=a %s b println c d=b %s a println d", boola, boolb, op, op))
        .executedEqualsInterpreted();
  }

  @Test
  public void boolBinOpProc(
      @TestParameter({"and", "or", "xor", "<", "==", ">="}) String op,
      @TestParameter boolean boola,
      @TestParameter boolean boolb)
      throws Exception {
    String program =
        String.format(
            "f:proc{a=%s b=%s c=a %s b println c d=b %s a println d} f()", boola, boolb, op, op);
    assertThatCompiling(program).withOptimize(true).executedEqualsInterpreted();
  }

  @Test
  public void boolBinOpProcParam(
      @TestParameter({"<=", "==", ">"}) String op,
      @TestParameter boolean boola,
      @TestParameter boolean boolb)
      throws Exception {
    assertThatCompiling(
            String.format(
                "c:bool f:proc(a:bool, b:bool) {c=a %s b println c d=c %s a println d} f(%s, %s )",
                op, op, boola, boolb))
        .executedEqualsInterpreted();
  }

  @Test
  public void shortCircuitAnd() throws Exception {
    assertThatCompiling(
            """
            f:proc(s:string) {
              if s != null and length(s) > 1 {
                 print 'length is: 'println s
              }
              println 'done'
            }
            f('a')
            f('hi')
            f(null)
            """)
        .executedEqualsInterpreted();
  }

  @Test
  public void shortCircuitOr() throws Exception {
    assertThatCompiling(
            """
            f:proc(s:string) {
              if s == null or length(s) > 1 {
                println 'null or big'
              }
              println 'done'
            }
            f('a')
            f('hi')
            f(null)
            """)
        .executedEqualsInterpreted();
  }
}
