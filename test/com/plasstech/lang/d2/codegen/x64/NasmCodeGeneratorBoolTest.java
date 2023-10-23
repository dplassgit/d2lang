package com.plasstech.lang.d2.codegen.x64;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;

@RunWith(TestParameterInjector.class)
public class NasmCodeGeneratorBoolTest extends NasmCodeGeneratorTestBase {
  @Test
  public void boolAssign(@TestParameter boolean bool) throws Exception {
    execute(String.format("a=%s b=a print a print b", bool), "boolAssign" + bool);
  }

  @Test
  public void not(@TestParameter boolean bool) throws Exception {
    execute(String.format("a=%s c=not a print a print c", bool), "not" + bool);
  }

  @Test
  public void boolBinOp(
      @TestParameter({"and", "or", "xor", "<", "<=", "==", "!=", ">", ">="}) String op,
      @TestParameter boolean boola, @TestParameter boolean boolb) throws Exception {
    execute(String.format("a=%s b=%s c=a %s b print c d=b %s a print d", boola, boolb, op, op),
        "boolBinOp" + boola + boolb);
  }

  @Test
  public void boolAssignProc(@TestParameter boolean bool) throws Exception {
    execute(String.format("f:proc {a=%s b=a print a print b} f()", bool), "boolAssignProc" + bool);
  }

  @Test
  public void notProc(@TestParameter boolean bool) throws Exception {
    execute(String.format("f:proc{a=%s b=not a print a print b} f()", bool), "notProc" + bool);
  }

  @Test
  public void boolBinOpProc(
      @TestParameter({"and", "or", "xor", "<", "<=", "==", "!=", ">", ">="}) String op,
      @TestParameter boolean boola, @TestParameter boolean boolb) throws Exception {
    String program = String.format("f:proc{a=%s b=%s c=a %s b println c d=b %s a println d} f()",
        boola,
        boolb,
        op,
        op);
    System.err.println(program);
    execute(program, "boolBinOpProc" + boola + boolb);
  }

  @Test
  public void boolBinOpProcParam(@TestParameter({"==", "!=", ">"}) String op,
      @TestParameter boolean boola, @TestParameter boolean boolb) throws Exception {
    execute(String.format(
        "c:bool f:proc(a:bool, b:bool) {c=a %s b print c d=c %s a print d} f(%s, %s )",
        op,
        op,
        boola,
        boolb), "boolBinOpProcParam" + boola + boolb);
  }

  @Test
  public void boolAnd() throws Exception {
    execute("f:proc(a:int, b:int):bool {if a+b > b-a or a<a-b { return true} return false} f(1,2)",
        "boolAnd");
  }

  @Test
  public void shortCircuitAnd() throws Exception {
    execute("      f:proc(s:string) { " +
        "  if s != null and length(s) > 1 { " +
        "     print 'length is: ' println s " +
        "  } " + "  println 'done'" +
        "} " +
        "f('a') f('hi') f(null)",
        "shortCircuitAnd");
  }

  @Test
  public void shortCircuitOr() throws Exception {
    execute(
        "f:proc(s:string) { if s== null or length(s) > 1 { println 'null or big' } println 'done'} "
            + "f('a') "
            + "f('hi') "
            + "f(null)",
        "shortCircuitOr");
  }
}
