package com.plasstech.lang.d2.codegen.x64;

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
  public void index(@TestParameter({"1", "4"}) int index) throws Exception {
    String value = "value";
    execute(
        String.format("i=%d a='%s' b=a[i] print b c=a[%d] print c", index, value, index), "index");
  }

  @Test
  public void oneCharStringIndex0() throws Exception {
    execute("a='x' b=a[0] println b", "oneCharStringIndex0");
    execute("f:proc(a:string) {b=a[0] println b} f('a')", "oneCharStringIndexProc0");
  }

  @Test
  public void twoCharStringIndex1() throws Exception {
    execute("a='xy' b=a[1] println b", "twoCharStringIndex1");
    execute("f:proc(a:string) {b=a[1] println b} f('xy')", "twoCharStringIndexProc1");
  }

  @Test
  public void negativeIndexCompileTime() throws Exception {
    assertGenerateError(
        "s='hello' print s[-2]", "Index of ARRAY variable 's' must be non-negative; was -2");
    assertGenerateError(
        "f:proc() {s='hello' print s[-3]} f()",
        "Index of ARRAY variable 's' must be non-negative; was -3");
  }

  @Test
  public void oobeIndex() throws Exception {
    String sourceCode = "f:proc() {s='hello' print s[10]} f()";
    if (optimize) {
      assertGenerateError(sourceCode, "STRING index out of bounds.*was 10");
    } else {
      assertRuntimeError(sourceCode, "oobeIndex", "STRING index out of bounds (length 5); was 10");
    }
  }

  @Test
  public void oobeIndexVariable() throws Exception {
    String sourceCode = "f:proc(i:int) {s='hello' print s[i]} f(10)";
    assertRuntimeError(sourceCode, "oobeIndex", "STRING index out of bounds (length 5); was 10");
  }

  @Test
  public void negativeIndexRunTimeLocal() throws Exception {
    String sourceCode = "f:proc() {i=-2 s='hello' print s[i]} f()";
    if (optimize) {
      assertGenerateError(sourceCode, "STRING index must be non-negative; was -2");
    } else {
      assertRuntimeError(
          sourceCode, "negativeIndexRunTime", "STRING index must be non-negative; was -2");
    }
  }

  @Test
  public void negativeIndexRunTimeGlobal() throws Exception {
    String sourceCode = "i=-2 s='hello' print s[i]";
    if (optimize) {
      assertGenerateError(sourceCode, "STRING index must be non-negative; was -2");
    } else {
      assertRuntimeError(
          sourceCode, "negativeIndexRunTimeGlobal", "STRING index must be non-negative; was -2");
    }
  }

  @Test
  public void procIndex() throws Exception {
    execute(
        "       b: string "
            + "foo: proc(a: string, i:int) {"
            + "   b=a[i] // b = o\r\n"
            + "   print b"
            + "   c=a[4] // c = d\r\n"
            + "   print c"
            + "} "
            + "foo('world', 1)",
        "procIndex");
  }

  @Test
  public void constantStringIndex(@TestParameter({"1", "4"}) int index) throws Exception {
    String value = "value";
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
                + "c=a %s b println c " //
                + "d=b %s a println d",
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
  public void compOpsNull(@TestParameter({"<", "<=", ">=", ">"}) String op) throws Exception {
    execute(String.format("a='abc' c=a %s null", op), "compOpsNullLiteral");
    execute(String.format("a='abc' b=null c=a %s b", op), "compOpsNullGlobal");
    execute(String.format("a='abc' b:string b=null c=b %s a", op), "compOpsNullGlobalAsString");
  }

  @Test
  public void equalityOpsNull(@TestParameter({"==", "!="}) String op) throws Exception {
    execute(String.format("a='abc' c=a %s null println c", op), "equalityOpsNullLiteral");
    execute(String.format("a='abc' b=null c=a %s b println c", op), "equalityOpsNullGlobal");
    execute(
        String.format("a='abc' b:string b=null c=b %s a println c", op), "equalityOpsNullAsString");
  }

  @Test
  public void equalityOpsNullProc(@TestParameter({"==", "!="}) String op) throws Exception {
    execute(
        String.format("f:proc:bool { a='abc' b=null c=a %s b return c} println f()", op),
        "equalityOpsNullProc");
  }

  @Test
  public void equalityOpsNull2(@TestParameter({"==", "!="}) String op) throws Exception {
    execute(String.format("a='abc' c=a %s null", op), "equalityOpsNull");
    execute(String.format("a='abc' a=null b=null c=a %s b", op), "equalityOpsNull");
    execute(String.format("a='abc' b:string b=null c=b %s a", op), "equalityOpsNull");
  }

  @Test
  public void lengthNullLocal() throws Exception {
    String program = "f:proc {a='hello' a=null println length(a)} f()";
    if (optimize) {
      assertGenerateError(program, ".*NULL expression.*");
    } else {
      assertRuntimeError(program, "length", "Null pointer error");
    }
  }

  @Test
  public void lengthNullGlobal() throws Exception {
    String program = "a='hello' a=null println length(a)";
    if (optimize) {
      assertGenerateError(program, ".*NULL expression.*");
    } else {
      assertRuntimeError(program, "length", "Null pointer error");
    }
  }

  @Test
  public void constStringLength(
      @TestParameter({"s", "hello", "hello this is a very long string"}) String value)
      throws Exception {
    execute(String.format("b=length('hello' + '%s') print b", value), "constStringLength");
  }

  @Test
  public void stringLength(
      @TestParameter({"", "s", "hello", "hello this is a very long string"}) String value)
      throws Exception {
    execute(String.format("a='%s' c='lo' b=length(c)+length(a) print b", value), "stringLength");
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
  public void bug97ComparingParams() throws Exception {
    execute(
        "      bug97ComparingParams:proc(a:string, b:string) { "
            + "  println a == chr(10) "
            + "  println chr(10) == a "
            + "  println b == chr(10) "
            + "  println chr(10) == b "
            + "} "
            + "bug97ComparingParams('abc', 'def')",
        "bug97ComparingParams");
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

  @Test
  public void concatEmpty() throws Exception {
    execute(
        "      tester: proc(left:string, right:string) {"
            + "   t=left+right println t "
            + "} "
            + "tester('', 'hi') "
            + "tester('hi', '') ",
        "concatInProc");
  }

  @Test
  public void concatNull() throws Exception {
    assertRuntimeError(
        "      tester: proc(left:string, right:string) {"
            + "   t=left+right println t "
            + "} "
            + "tester(null, '') "
            + "tester('', null) ",
        "concatNull",
        "Null pointer error");
  }

  @Test
  public void indexOfTemp() throws Exception {
    execute(
        "      h='hello '\r\n"
            + "w='world'\r\n"
            + "len = length(h+w)\r\n"
            + "println 'Should be hello world:'\r\n"
            + "i = 0 while i < len do i = i + 1 {\r\n"
            + "  print ((h+w)[i])[0]\r\n"
            + "}\r\n",
        "indexOfTemp");
  }

  @Test
  public void bug83() throws Exception {
    execute(
        "      prepend: proc(s:string) {\n"
            + "   println s + ' there'\n"
            + "}\n"
            + "return_prepend: proc(s:string):string {\n"
            + "   return s + ' there'\n"
            + "}\n"
            + "postpend: proc(s:string) {\n"
            + "   println 'there ' + s\n"
            + "}\n"
            + "return_postpend: proc(s:string):string {\n"
            + "   return 'there ' + s\n"
            + "}\n"
            + "\n"
            + "  println 'Should print hello there'\n"
            + "  prepend('hello')\n"
            + "  println return_prepend('hello')\n"
            + "  println 'Should print there hello'\n"
            + "  postpend('hello')\n"
            + "  println return_postpend('hello')\n",
        "bug83");
  }

  @Test
  public void compareToNull() throws Exception {
    execute(
        "f:proc(s:string) { if s == null {println 'isnull'} else {println 'nope'}} f('hi')",
        "compareToNull");
  }

  @Test
  public void addToItself() throws Exception {
    execute(
        "f:proc(s:string):string {\n "
            + "  sb = ''\n"
            // This only fails in a loop because otherwise it is optimized to "return 'X'"
            + "  i = 0 while i < 2 do i++ {\n"
            + "    sb = sb + 'X'\n"
            + "  }\n"
            + "  sb = sb + 'Y'\n"
            + "  nb = sb + 'Z'\n"
            + "  return nb\n"
            + "}\n"
            + "print f('abcde')\n",
        "addChr");
  }
}
