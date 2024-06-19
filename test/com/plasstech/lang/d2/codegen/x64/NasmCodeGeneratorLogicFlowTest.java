package com.plasstech.lang.d2.codegen.x64;

import static com.plasstech.lang.d2.codegen.x64.testing.ExecutionSubject.assertThatCompiling;

import org.junit.Test;

public class NasmCodeGeneratorLogicFlowTest {
  @Test
  public void ifPrint() throws Exception {
    assertThatCompiling("a=3 if a > 1 {print a}").executedEqualsInterpreted();
  }

  @Test
  public void fib() throws Exception {
    assertThatCompiling(
        "      n1 = 0 "
            + "n2 = 1 "
            + "nth = 0 "
            + "i=1 while i <= 10 do i = i + 1 {"
            + "  nth = n1 + n2"
            + "  n1 = n2"
            + "  n2 = nth"
            + "}"
            + "print nth")
        .executedEqualsInterpreted();
  }

  @Test
  public void fibRecursive() throws Exception {
    assertThatCompiling(
        "      recursive_fib: proc(n: int) : int {\n"
            + "  if n <= 1 {\n"
            + "    return n\n"
            + "  } else {\n"
            + "    return recursive_fib(n - 1) + recursive_fib(n - 2)\n"
            + "  }\n"
            + "}\n"
            + "println recursive_fib(10)")
        .executedEqualsInterpreted();
  }

  @Test
  public void fib0() throws Exception {
    assertThatCompiling("n=10\r\n"
        + "n1 = 0\r\n"
        + "n2 = 1\r\n"
        + "nth = 0\r\n"
        + "i=1 while i <= n do i = i+1 {\r\n"
        + "  nth = n1 + n2\r\n"
        + "  n1 = n2\r\n"
        + "  n2 = nth\r\n"
        + "  print i\r\n"
        + "  print \"th fib: \"\r\n"
        + "  println nth\r\n"
        + "}\r\n"
        + "println '' // NOTE: cannot just do PRINT (no expression)...\r\n"
        + "print \"Final fib: \"\r\n"
        + "println nth").executedEqualsInterpreted();
  }

  @Test
  public void fact() throws Exception {
    assertThatCompiling(
        "      fact = 1 "
            + "i=1 while i <= 10 do i = i + 1 {"
            + "  fact = fact * i"
            + "}"
            + "println fact")
        .executedEqualsInterpreted();
  }

  @Test
  public void nullCompare() throws Exception {
    assertThatCompiling("      s=''\r\n"
        + "if null != null { println 'This should never happen'}\r\n"
        + "if null == null { println 'null'}"
        + "if null != s { println 'null'}\r\n"
        + "if null == s { println 'This should never happen'}"
        + "if s != null { println 'null'}\r\n"
        + "if s == null { println 'This should never happen'}").executedEqualsInterpreted();
  }

  @Test
  public void commonSubexpressionElimination() throws Exception {
    assertThatCompiling(
        "      f:proc(a:int, b:int):int {\n"
            + "    c=a+b\n"
            + "    d=a+b+c\n"
            + "    println c\n"
            + "    return d\n"
            + "}\n"
            + "e=f(1, 2)\n"
            + "println e+1")
        .executedEqualsInterpreted();
  }

  @Test
  public void dumbsort() throws Exception {
    assertThatCompiling(
        "      f:proc(a:int, b:int):int {\n"
            + "    c=a+b\n"
            + "    d=a+b+c\n"
            + "    println c\n"
            + "    return d\n"
            + "}\n"
            + "e=f(1, 2)\n"
            + "println e+1")
        .executedEqualsInterpreted();
  }
}
