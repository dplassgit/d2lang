package com.plasstech.lang.d2.codegen.x64;

import static com.plasstech.lang.d2.codegen.x64.testing.ExecutionSubject.assertThatCompiling;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;

@RunWith(TestParameterInjector.class)
public class NasmCodeGeneratorStringTest {
  @Test
  public void assign() throws Exception {
    assertThatCompiling("a='string' b=a print b").executedEqualsInterpreted();
  }

  @Test
  public void index(@TestParameter({"1", "4"}) int index) throws Exception {
    String value = "value";
    assertThatCompiling(
        String.format("i=%d a='%s' b=a[i] print b c=a[%d] print c", index, value, index))
        .executedEqualsInterpreted();
  }

  @Test
  public void oneCharStringIndex0Global() throws Exception {
    assertThatCompiling("a='x' b=a[0] println b").executedEqualsInterpreted();
  }

  @Test
  public void twoCharStringIndex1() throws Exception {
    assertThatCompiling("f:proc(a:string) {b=a[1] println b} f('xy')").executedEqualsInterpreted();
  }

  @Test
  public void negativeIndex() throws Exception {
    assertThatCompiling("s='hello' print s[-2]").withOptimize(false)
        .hasCompileTimeError("Index of STRING variable 's' must be non-negative; was -2");
    assertThatCompiling("f:proc() {s='hello' print s[-3]} f()").withOptimize(false)
        .hasCompileTimeError("Index of STRING variable 's' must be non-negative; was -3");
  }

  @Test
  public void oobeIndex() throws Exception {
    String sourceCode = "f:proc() {s='hello' print s[10]} f()";
    assertThatCompiling(sourceCode)
        .hasCompileTimeError("STRING index out of bounds \\(length 5\\); was 10");
    assertThatCompiling(sourceCode).withOptimize(false)
        .withRuntimeError("STRING index out of bounds (length 5); was 10").executes();
  }

  @Test
  public void oobeIndexVariable() throws Exception {
    String sourceCode = "f:proc(i:int) {s='hello' print s[i]} f(10)";
    assertThatCompiling(sourceCode)
        .hasCompileTimeError("STRING index out of bounds.*length 5.*was 10");
    assertThatCompiling(sourceCode).withOptimize(false)
        .withRuntimeError("STRING index out of bounds (length 5); was 10").executes();
  }

  @Test
  public void negativeIndexLocal() throws Exception {
    String sourceCode = "f:proc() {i=-2 s='hello' print s[i]} f()";
    assertThatCompiling(sourceCode).hasCompileTimeError("must be non-negative; was -2");
    assertThatCompiling(sourceCode).withOptimize(false)
        .withRuntimeError("must be non-negative; was -2").executes();
  }

  @Test
  public void negativeIndexCalculated() throws Exception {
    String sourceCode = "f:proc(i:int) {s='hello' print s[i*2]} f(-1)";
    assertThatCompiling(sourceCode).hasCompileTimeError("must be non-negative; was -2");
    assertThatCompiling(sourceCode).withOptimize(false)
        .withRuntimeError("must be non-negative; was -2").executes();
  }

  @Test
  public void negativeIndexGlobal() throws Exception {
    String sourceCode = "i=-2 s='hello' print s[i]";
    assertThatCompiling(sourceCode).hasCompileTimeError("must be non-negative; was -2");

    assertThatCompiling(sourceCode).withOptimize(false)
        .withRuntimeError("must be non-negative; was -2").executes();
  }

  @Test
  public void procIndex() throws Exception {
    assertThatCompiling("       b: string "
        + "foo: proc(a: string, i:int) {"
        + "   b=a[i] // b = o\r\n"
        + "   print b"
        + "   c=a[4] // c = d\r\n"
        + "   print c"
        + "} "
        + "foo('world', 1)").executedEqualsInterpreted();
  }

  @Test
  public void constantStringIndex(@TestParameter({"1", "4"}) int index) throws Exception {
    String value = "value";
    assertThatCompiling(
        String.format("i=%d b='%s'[i] print b c='%s'[%d] print c", index, value, value, index))
        .executedEqualsInterpreted();
  }

  @Test
  public void addSimple() throws Exception {
    assertThatCompiling("a='a' c=a+'b' print c").executedEqualsInterpreted();
  }

  @Test
  public void addComplex() throws Exception {
    assertThatCompiling(
        "a='abc' b='def' c=a+b println c d=c+'xyz' println d e='ijk'+d+chr(32) println e")
        .executedEqualsInterpreted();
  }

  @Test
  public void compOpsGlobals(
      @TestParameter({"<", "!=", ">="}) String op,
      @TestParameter({"abc", "def", ""}) String first,
      @TestParameter({"abc", "def", ""}) String second)
      throws Exception {
    assertThatCompiling(String.format(
        "      a='%s' b='%s' " //
            + "c=a %s b println c " //
            + "d=b %s a println d",
        first, second, op, op)).executedEqualsInterpreted();
  }

  @Test
  public void compOpsParams(@TestParameter({"<", "!=", ">="}) String op)
      throws Exception {
    assertThatCompiling(String.format(
        "      doit:proc(a:string,b:string) { "
            + "  c=a %s b "
            + "  print c "
            + "  d=b %s a "
            + "  print d "
            + "} "
            + "doit('abc', 'def') ",
        op, op)).executedEqualsInterpreted();
  }

  @Test
  public void compOpsNull(@TestParameter({"<", ">="}) String op) throws Exception {
    assertThatCompiling(String.format("a='abc' c=a %s null", op)).executedEqualsInterpreted();
    assertThatCompiling(String.format("a='abc' b=null c=a %s b", op)).executedEqualsInterpreted();
    assertThatCompiling(String.format("a='abc' b:string b=null c=b %s a", op))
        .executedEqualsInterpreted();
  }

  @Test
  public void equalityOpsNull(@TestParameter({"==", "!="}) String op) throws Exception {
    assertThatCompiling(String.format("a='abc' c=a %s null println c", op))
        .executedEqualsInterpreted();
    assertThatCompiling(String.format("a='abc' c=null %s a println c", op))
        .executedEqualsInterpreted();
    assertThatCompiling(String.format("a='abc' b=null c=a %s b println c", op))
        .executedEqualsInterpreted();
    assertThatCompiling(String.format("a='abc' b:string b=null c=b %s a println c", op))
        .executedEqualsInterpreted();
    assertThatCompiling(
        String.format("f:proc:bool { a='abc' b=null c=a %s b return c} println f()", op))
        .executedEqualsInterpreted();
  }

  @Test
  public void lengthNullLocal() throws Exception {
    String program = "f:proc {a='hello' a=null println length(a)} f()";
    assertThatCompiling(program).hasCompileTimeError("Null pointer error");
    assertThatCompiling(program).withOptimize(false).withRuntimeError("Null pointer error")
        .executes();
  }

  @Test
  public void lengthNullGlobal() throws Exception {
    assertThatCompiling("a='hello' a=null println length(a)")
        .hasCompileTimeError("Null pointer error");
    assertThatCompiling("a='hello' a=null println length(a)").withOptimize(false)
        .withRuntimeError("Null pointer error").executes();
    assertThatCompiling("a:string a=null println length(a)")
        .hasCompileTimeError("Null pointer error");
    assertThatCompiling("a:string a=null println length(a)").withOptimize(false)
        .withRuntimeError("Null pointer error").executes();
  }

  @Test
  public void constStringLength() throws Exception {
    assertThatCompiling("b=length('hello') print b").executedEqualsInterpreted();
  }

  @Test
  public void stringLength(@TestParameter({"", "s", "hello"}) String value)
      throws Exception {
    assertThatCompiling(String.format("a='%s' c='lo' b=length(c)+length(a) print b", value))
        .executedEqualsInterpreted();
  }

  @Test
  public void compOpsThreeParams(@TestParameter({"<=", "!=", ">"}) String op)
      throws Exception {
    assertThatCompiling(String.format(
        "      compOpsThreeParams:proc(x:int, a:string,b:string) { "
            + "  print x "
            + "  print a %s b "
            + "  print b %s a "
            + "} "
            + "compOpsThreeParams(123, 'abc', 'def') ",
        op, op)).executedEqualsInterpreted();
  }

  @Test
  public void bug97ComparingParams() throws Exception {
    assertThatCompiling("      bug97ComparingParams:proc(a:string, b:string) { "
        + "  println a == chr(10) "
        + "  println chr(65) == a "
        + "  println b == chr(66) "
        + "  println chr(10) == b "
        + "} "
        + "bug97ComparingParams('A', 'B')").executedEqualsInterpreted();
  }

  @Test
  public void compOpsLocals(@TestParameter({"<", "==", ">="}) String op)
      throws Exception {
    assertThatCompiling(String.format(
        "      compOpsLocals:proc() { "
            + "  a='abc' "
            + "  b='def' "
            + "  print b "
            + "  print a "
            + "  c = a %s b "
            + "  print c "
            + "} "
            + "compOpsLocals() ",
        op)).executedEqualsInterpreted();
  }

  @Test
  public void concatEmpty() throws Exception {
    assertThatCompiling(" tester: proc(left:string, right:string) {"
        + "   t=left+right println t "
        + "} "
        + "tester('', 'hi') "
        + "tester('hi', '') ").executedEqualsInterpreted();
  }

  @Test
  public void concatNull(@TestParameter boolean optimize) throws Exception {
    String sourceCode = " tester: proc(left:string, right:string) {"
        + "   t=left+right println t "
        + "} "
        + "tester(null, '') "
        + "tester('', null) ";
    assertThatCompiling(sourceCode)
        .withOptimize(optimize)
        .withRuntimeError("Null pointer error")
        .executes();
  }

  @Test
  public void indexOfTemp() throws Exception {
    assertThatCompiling("      h='hello '\r\n"
        + "w='world'\r\n"
        + "len = length(h+w)\r\n"
        + "i = 0 while i < len do i = i + 1 {\r\n"
        + "  print ((h+w)[i])[0]\r\n"
        + "}\r\n").executedEqualsInterpreted();
  }

  @Test
  public void paramSlice() throws Exception {
    assertThatCompiling("f:proc(r:range) {s='first' print s[r]} f(0:2)")
        .executedEqualsInterpreted();
  }

  @Test
  public void stringParamThenSliced() throws Exception {
    assertThatCompiling(
        "      f:proc(s:string, i:int, j:int) {println s[0:2] println s[i:j]}\n"
            + "f('first', 0, 2)")
        .withOptimize(false)
        .executedEqualsInterpreted();
  }

  @Test
  public void stringParamSliceToEmpty() throws Exception {
    assertThatCompiling("f:proc(r:range) {s='123456' print s[r]} f(2:2)")
        .executedEqualsInterpreted();
  }

  @Test
  public void stringParamSliced(@TestParameter boolean optimize) throws Exception {
    assertThatCompiling(
        "      f:proc(s:string) {println s[0:2]}\n"
            + "f('first')")
        .withOptimize(optimize)
        .executedEqualsInterpreted();
  }

  @Test
  public void globalStringSliced() throws Exception {
    assertThatCompiling(
        "      s='first' println s[0:2]")
        .withOptimize(false)
        .executedEqualsInterpreted();
  }

  @Test
  public void stringParamFullSlice() throws Exception {
    assertThatCompiling("s='123456' f:proc(r:range) {print s[r]} f(0:length(s))")
        .executedEqualsInterpreted();
  }

  @Test
  public void stringLocalSlice() throws Exception {
    assertThatCompiling("f:proc(start:int, e:int) {r=start:e s='123456' print s[r]} f(2,5)")
        .executedEqualsInterpreted();
  }

  @Test
  public void stringLocalSliceToEmpty() throws Exception {
    assertThatCompiling("f:proc(start:int, e:int) {r=start:e s='123456' print s[r]} f(2,2)")
        .executedEqualsInterpreted();
  }

  @Test
  public void stringLocalFullSlice() throws Exception {
    assertThatCompiling(
        "s='123456' f:proc(start:int, e:int) {r=start:e s='123456' print s[r]} f(2,length(s))")
        .executedEqualsInterpreted();
  }

  @Test
  public void stringConstantSlice() throws Exception {
    assertThatCompiling("r=2:5 s='123456' print s[r]").executedEqualsInterpreted();
  }

  @Test
  public void stringConstantSliceToEmpty() throws Exception {
    assertThatCompiling("r=2:2 s='123456' print s[r]").executedEqualsInterpreted();
  }

  @Test
  public void stringConstantFullSlice() throws Exception {
    assertThatCompiling("s='123456' r=0:length(s) print s[r]").executedEqualsInterpreted();
  }

  @Test
  public void bug83() throws Exception {
    assertThatCompiling("      prepend: proc(s:string) {\n"
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
        + "  println return_postpend('hello')\n").executedEqualsInterpreted();
  }

  @Test
  public void addToItself() throws Exception {
    assertThatCompiling("f:proc(s:string):string {\n "
        + "  sb = ''\n"
        // This only fails in a loop because otherwise it is optimized to "return 'X'"
        + "  i = 0 while i < 2 do i++ {\n"
        + "    sb = sb + 'X'\n"
        + "  }\n"
        + "  sb = sb + 'Y'\n"
        + "  nb = sb + 'Z'\n"
        + "  return nb\n"
        + "}\n"
        + "print f('abcde')\n").executedEqualsInterpreted();
  }

  @Test
  public void bug289() throws Exception {
    assertThatCompiling(""
        + "DSet: record {}"
        + "addToSet: proc(set: DSet, value: string): bool {\n"
        + "  return false\n"
        + "}\n"
        + "\n"
        + "s = new DSet\n"
        + "i = 0\n"
        + "addToSet(s, 'A')\n"
        + "e = chr(i+asc('A'))+'B'\n"
        + "print 'e: ' println e").executedEqualsInterpreted();
  }
}
