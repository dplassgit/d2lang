package com.plasstech.lang.d2.codegen.x64;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.plasstech.lang.d2.phase.PhaseName;

@RunWith(TestParameterInjector.class)
public class NasmCodeGeneratorStringTest extends NasmCodeGeneratorTestBase {
  @Test
  public void assign() throws Exception {
    execute("a='string' b=a print b", "assign");
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
  }

  @Test
  public void twoCharStringIndex1() throws Exception {
    execute("f:proc(a:string) {b=a[1] println b} f('xy')", "twoCharStringIndexProc1");
  }

  @Test
  public void negativeIndex() throws Exception {
    assertGenerateError(
        "s='hello' print s[-2]", "Index of ARRAY variable 's' must be non-negative; was -2", false,
        PhaseName.TYPE_CHECK);
    assertGenerateError(
        "f:proc() {s='hello' print s[-3]} f()",
        "Index of ARRAY variable 's' must be non-negative; was -3", false,
        PhaseName.TYPE_CHECK);
  }

  @Test
  public void oobeIndex() throws Exception {
    String sourceCode = "f:proc() {s='hello' print s[10]} f()";
    assertGenerateError(sourceCode, "out of bounds.*was 10");
    assertRuntimeError(sourceCode, "oobeIndex", "out of bounds (length 5); was 10");
  }

  @Test
  public void oobeIndexVariable() throws Exception {
    String sourceCode = "f:proc(i:int) {s='hello' print s[i]} f(10)";
    assertRuntimeError(sourceCode, "oobeIndex", "STRING index out of bounds (length 5); was 10");
  }

  @Test
  public void negativeIndexLocal() throws Exception {
    String sourceCode = "f:proc() {i=-2 s='hello' print s[i]} f()";
    assertGenerateError(sourceCode, "must be non-negative; was -2");
    // Skipping runtime test
  }

  @Test
  public void negativeIndexGlobal() throws Exception {
    String sourceCode = "i=-2 s='hello' print s[i]";
    // skipping generate test
    assertRuntimeError(
        sourceCode, "negativeIndexRunTimeGlobal", "must be non-negative; was -2");
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
        "a='abc' b='def' c=a+b println c d=c+'xyz' println d e='ijk'+d+chr(32) println e",
        "addComplex");
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
  public void compOpsParams(@TestParameter({"<", "==", ">="}) String op)
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
    execute(String.format("a='abc' c=null %s a println c", op), "equalityOpsNullGlobal");
    execute(String.format("a='abc' b=null c=a %s b println c", op), "equalityOpsNullGlobal");
    execute(
        String.format("a='abc' b:string b=null c=b %s a println c", op), "equalityOpsNullAsString");
    execute(
        String.format("f:proc:bool { a='abc' b=null c=a %s b return c} println f()", op),
        "equalityOpsNullProc");
  }

  @Test
  public void lengthNullLocal() throws Exception {
    String program = "f:proc {a='hello' a=null println length(a)} f()";
    assertGenerateError(program, ".*NULL expression.*", true, PhaseName.ASM_CODEGEN);
    assertRuntimeError(program, "length", "Null pointer error");
  }

  @Test
  public void lengthNullGlobal() throws Exception {
    String program = "a='hello' a=null println length(a)";
    assertGenerateError(program, ".*NULL expression.*", true, PhaseName.ASM_CODEGEN);
    assertRuntimeError(program, "length", "Null pointer error");
  }

  @Test
  public void constStringLength(
      @TestParameter({"s", "hello this is a very long string"}) String value)
      throws Exception {
    execute(String.format("b=length('hello' + '%s') print b", value), "constStringLength");
  }

  @Test
  public void stringLength(
      @TestParameter({"", "s", "hello this is a very long string"}) String value)
      throws Exception {
    execute(String.format("a='%s' c='lo' b=length(c)+length(a) print b", value), "stringLength");
  }

  @Test
  public void compOpsThreeParams(@TestParameter({"<=", "!=", ">"}) String op)
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
  public void compOpsLocals(@TestParameter({"<", "==", ">="}) String op)
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
  public void concatEmpty() throws Exception {
    execute(
        " tester: proc(left:string, right:string) {"
            + "   t=left+right println t "
            + "} "
            + "tester('', 'hi') "
            + "tester('hi', '') ",
        "concatInProc");
  }

  @Test
  public void concatNull() throws Exception {
    assertRuntimeError(
        " tester: proc(left:string, right:string) {"
            + "   t=left+right println t "
            + "} "
            + "tester(null, '') "
            + "tester('', null) ",
        "concatNull", "Null pointer error");
  }

  @Test
  public void indexOfTemp() throws Exception {
    execute(
        "      h='hello '\r\n"
            + "w='world'\r\n"
            + "len = length(h+w)\r\n"
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

  @Test
  public void bug289() throws Exception {
    execute(""
        + "DSet: record {}"
        + "addToSet: proc(set: DSet, value: string): bool {\n"
        + "  return false\n"
        + "}\n"
        + "\n"
        + "s = new DSet\n"
        + "i = 0\n"
        + "addToSet(s, 'A')\n"
        + "e = chr(i+asc('A'))+'B'\n"
        + "print 'e: ' println e", "bug289");
  }
}
