package com.plasstech.lang.d2.codegen;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;

@RunWith(TestParameterInjector.class)
public class NasmCodeGeneratorStringTest extends NasmCodeGeneratorTestBase {
  @Test
  public void assignString(
      @TestParameter({"s", "hello", "hello this is a very long string"}) String value)
      throws Exception {
    execute(String.format("a='%s' b=a print b", value), "assignString");
  }

  @Test
  public void stringIndex(
      @TestParameter({"world", "hello this is a very long string"}) String value,
      @TestParameter({"1", "4"}) int index)
      throws Exception {
    execute(
        String.format("i=%d a='%s' b=a[i] print b c=a[%d] print c", index, value, index),
        "stringIndex");
  }

  @Test
  @Ignore
  public void constantStringIndex(
      @TestParameter({"world", "hello this is a very long string"}) String value,
      @TestParameter({"1", "4"}) int index)
      throws Exception {
    execute(
        String.format("i=%d b='%s'[i] print b c='%s'[%d] print c", index, value, value, index),
        "constantStringIndex");
  }

  @Test
  public void stringAddSimple() throws Exception {
    execute("a='a' c=a+'b' print c", "stringAddSimple");
  }

  @Test
  public void stringAddComplex() throws Exception {
    execute(
        "a='abc' b='def' c=a+b print c d=c+'xyz' print d e='ijk'+d+chr(32) print e",
        "stringAddComplex");
  }

  @Test
  public void stringCompOps(
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
        "stringCompOps");
  }
}