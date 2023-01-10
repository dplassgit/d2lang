package com.plasstech.lang.d2.codegen.x64;

import static org.junit.Assume.assumeFalse;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;

@RunWith(TestParameterInjector.class)
public class NasmCodeGeneratorBuiltinsTest extends NasmCodeGeneratorTestBase {
  @Test
  public void printDuplicateStrings() throws Exception {
    execute("print 'hello' print 'world' print 'hello world'", "printDuplicateStrings");
  }

  @Test
  public void printLn() throws Exception {
    execute("println 'hello world'", "println");
    execute("println 'println with cr\\r' println 'println with newline\\n'", "printlnWithCrLf");
    execute("print 'print\"ln' println '\"mixed'", "printlnMixed");
  }

  @Test
  public void printInt() throws Exception {
    execute("print 3 print -3 ", "printInt" /*heh*/);
  }

  @Test
  public void printBool(@TestParameter boolean bool) throws Exception {
    execute("print " + bool, "print" + bool);
  }

  @Test
  public void printIntVariable() throws Exception {
    execute("a=3 print a", "printIntVariable");
  }

  @Test
  public void evilVariableName() throws Exception {
    execute("rax=3 print rax", "evilVariableName");
  }

  @Test
  public void printStringVariable() throws Exception {
    execute("a='hello' print a", "printStringVariable");
  }

  @Test
  public void exit() throws Exception {
    execute("exit", "exit");
  }

  @Test
  public void exitErrorConst() throws Exception {
    assertCompiledEqualsInterpreted("exit 'exitErrorConst'", "exitErrorConst", -1);
  }

  @Test
  public void exitErrorVariable() throws Exception {
    assertCompiledEqualsInterpreted("a='exitErrorVariable' exit a", "exitErrorVariable", -1);
  }

  @Test
  public void exitMain() throws Exception {
    assertCompiledEqualsInterpreted("main {exit 'exitMain'}", "exitMain", -1);
  }

  @Test
  public void asc(@TestParameter({"s", "he"}) String value) throws Exception {
    execute(String.format("a='%s' b=asc(a) print b", value), "asc");
    execute(String.format("b=asc('%s') print b", value), "ascConst");
    execute(String.format("a='%s' b=a c=asc(b) print c", value), "asc2");
  }

  @Test
  public void constantAsc() throws Exception {
    assumeFalse(optimize);
    execute("println asc('hi')", "constantAsc");
  }

  @Test
  public void ascInProc() throws Exception {
    execute("f:proc(a:string) { b=asc(a) println b} f('hi')", "ascInProc");
  }

  @Test
  public void ascLocal() throws Exception {
    execute("f:proc(a:string) { c=a b=asc(c) println b} f('hi')", "ascInProc");
  }

  @Test
  public void printParse() throws Exception {
    execute(
        "print 123 print ', '\n" //
            + " print 'should be b:'\n" //
            + " Println 'abcde'[1]",
        "printParse");
  }

  @Test
  public void chr(@TestParameter({"65", "96"}) int value) throws Exception {
    execute(String.format("a=%d b=chr(a) print b", value), "chr");
    execute(String.format("a=chr(%d) print a", value), "chrConst");
  }
}
