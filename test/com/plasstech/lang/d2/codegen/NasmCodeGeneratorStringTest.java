package com.plasstech.lang.d2.codegen;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;

@RunWith(TestParameterInjector.class)
public class NasmCodeGeneratorStringTest extends NasmCodeGeneratorTestBase {
  @Test
  public void assign(
      @TestParameter({"s", "hello", "hello this is a very long string"}) String value)
      throws Exception {
    execute(String.format("a='%s' b=a print b", value), "assign");
  }

  @Test
  public void index(
      @TestParameter({"world", "hello this is a very long string"}) String value,
      @TestParameter({"1", "4"}) int index)
      throws Exception {
    execute(
        String.format("i=%d a='%s' b=a[i] print b c=a[%d] print c", index, value, index), "index");
  }

  @Test
  public void procIndex() throws Exception {
    execute(
        "       b: string "
            + "foo: proc(a: string, i:int) {"
            + "   b=a[i]"
            + "   print b"
            + "   c=a[4]"
            + "   print c"
            + "} "
            + "foo('world', 1)",
        "procIndex");
  }

  public void constantStringIndex(
      @TestParameter({"world", "hello this is a very long string"}) String value,
      @TestParameter({"1", "4"}) int index)
      throws Exception {
    execute(
        String.format("i=%d b='%s'[i] print b c='%s'[%d] print c", index, value, value, index),
        "constantStringIndex");
  }

  @Test
  public void addSimple() throws Exception {
    execute("a='a' c=a+'b' print c", "addSimple");
  }

  @Test
  public void addComplex() throws Exception {
    execute(
        "a='abc' b='def' c=a+b print c d=c+'xyz' print d e='ijk'+d+chr(32) print e", "addComplex");
  }

  @Test
  public void compOpsGlobals(
      @TestParameter({"<", "<=", "==", "!=", ">=", ">"}) String op,
      @TestParameter({"abc", "def", ""}) String first,
      @TestParameter({"abc", "def", ""}) String second)
      throws Exception {
    execute(
        String.format(
            "      a='%s' b='%s' " //
                + "c=a %s b print c " //
                + "d=b %s a print d",
            first, second, op, op),
        "compOpsGlobals");
  }

  @Test
  public void compOpsParams(@TestParameter({"<", "<=", "==", "!=", ">=", ">"}) String op)
      throws Exception {
    execute(
        String.format(
            "      doit:proc(a:string,b:string) { "
                + "  c=a %s b "
                + "  print c "
                + "  d=b %s a "
                + "  print d "
                + "} "
                + "doit('abc', 'def') ",
            op, op),
        "compOpsParams");
  }

  @Test
  public void compOpsThreeParams(@TestParameter({"<", "<=", "==", "!=", ">=", ">"}) String op)
      throws Exception {
    execute(
        String.format(
            "      compOpsThreeParams:proc(x:int, a:string,b:string) { "
                + "  print x "
                + "  print a %s b "
                + "  print b %s a "
                + "} "
                + "compOpsThreeParams(123, 'abc', 'def') ",
            op, op),
        "compOpsThreeParams");
  }

  @Test
  public void compOpsLocals(@TestParameter({"<", "<=", "==", "!=", ">=", ">"}) String op)
      throws Exception {
    execute(
        String.format(
            "      compOpsLocals:proc() { "
                + "  a='abc' "
                + "  b='def' "
                + "  print b "
                + "  print a "
                + "  c = a %s b "
                + "  print c "
                + "} "
                + "compOpsLocals() ",
            op),
        "compOpsLocals");
  }

  @Test
  public void concat_bug83() throws Exception {
    execute(
        "      x='hi' "
            + "x='hi' "
            + "z='' "
            + "z=' ' "
            + "x=x+z "
            + "x=x+' there' "
            + "println x + x[0]",
        "concat");
  }
  
  @Test
  public void concatInProc() throws Exception {
    execute(
        "      tester: proc(s:string) {" //
            + "   println 'the first letter of \"' + s + '\" is ' + s[0]"
            + "}"
            + "  tester('h ')",
        "concatInProc");
  }
}
