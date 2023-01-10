package com.plasstech.lang.d2.codegen.x64;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameterInjector;

@RunWith(TestParameterInjector.class)
public class NasmCodeGeneratorLogicFlowTest extends NasmCodeGeneratorTestBase {
  @Test
  public void ifPrint() throws Exception {
    execute("a=3 if a > 1 {print a}", "ifPrint");
  }

  @Test
  public void fib() throws Exception {
    // DANG THIS WORKS!
    execute(
        "      n1 = 0 "
            + "n2 = 1 "
            + "i=1 while i <= 10 do i = i + 1 {"
            + "  nth = n1 + n2"
            + "  n1 = n2"
            + "  n2 = nth"
            + "}"
            + "print nth",
        "fib");
  }

  @Test
  public void fib0() throws Exception {
    execute(
        "n=10\r\n"
            + "n1 = 0\r\n"
            + "n2 = 1\r\n"
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
            + "println nth",
        "fib0");
  }

  @Test
  public void fact() throws Exception {
    execute(
        "      fact = 1 "
            + "i=1 while i <= 10 do i = i + 1 {"
            + "  fact = fact * i"
            + "}"
            + "print fact",
        "fact");
  }

  @Test
  public void nullCompare() throws Exception {
    execute(
        "      s=''\r\n"
            + "if null != null { println 'This should never happen'}\r\n"
            + "if null == null { println 'null'}"
            + "if null != s { println 'null'}\r\n"
            + "if null == s { println 'This should never happen'}"
            + "if s != null { println 'null'}\r\n"
            + "if s == null { println 'This should never happen'}",
        "nullCompare");
  }
}
