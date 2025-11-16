package com.plasstech.lang.d2.codegen.x64;

import static com.plasstech.lang.d2.codegen.x64.testing.ExecutionSubject.assertThatCompiling;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(TestParameterInjector.class)
public class NasmCodeGeneratorBuiltinsTest {
  @Test
  public void printDuplicateStrings() throws Exception {
    assertThatCompiling("print 'hello' print 'world' print 'hello world'")
        .executedEqualsInterpreted();
  }

  @Test
  public void printLn() throws Exception {
    assertThatCompiling("println 'hello world'").executedEqualsInterpreted();
    assertThatCompiling("println 'println with cr\\r' println 'println with newline\\n'")
        .executedEqualsInterpreted();
    assertThatCompiling("print 'print\"ln' println '\"mixed'").executedEqualsInterpreted();
  }

  @Test
  public void printInt() throws Exception {
    assertThatCompiling("print 3 print -3 ").executedEqualsInterpreted();
  }

  @Test
  public void printBool(@TestParameter boolean bool) throws Exception {
    assertThatCompiling("print " + bool).executedEqualsInterpreted();
  }

  @Test
  public void printIntVariable() throws Exception {
    assertThatCompiling("a=3 print a").executedEqualsInterpreted();
  }

  @Test
  public void evilVariableName() throws Exception {
    assertThatCompiling("rax=3 print rax").executedEqualsInterpreted();
  }

  @Test
  public void printStringVariable() throws Exception {
    assertThatCompiling("a='hello' print a").executedEqualsInterpreted();
  }

  @Test
  public void exit() throws Exception {
    assertThatCompiling("exit").executedEqualsInterpreted();
  }

  @Test
  public void exitErrorConst() throws Exception {
    assertThatCompiling("exit 'exitErrorConst'").withRuntimeError("exitErrorConst").executes();
  }

  @Test
  public void exitErrorVariable() throws Exception {
    assertThatCompiling("a='exitErrorVariable' exit a")
        .withRuntimeError("exitErrorVariable")
        .executes();
  }

  @Test
  public void asc(@TestParameter({"s", "he"}) String value) throws Exception {
    assertThatCompiling(String.format("a='%s' b=asc(a) print b", value))
        .executedEqualsInterpreted();
    assertThatCompiling(String.format("b=asc('%s') print b", value)).executedEqualsInterpreted();
    assertThatCompiling(String.format("a='%s' b=a c=asc(b) print c", value))
        .executedEqualsInterpreted();
  }

  @Test
  public void constantAsc() throws Exception {
    assertThatCompiling("println asc('hi')").executedEqualsInterpreted();
  }

  @Test
  public void ascInProc() throws Exception {
    assertThatCompiling("f:proc(a:string) { b=asc(a) println b} f('hi')")
        .executedEqualsInterpreted();
  }

  @Test
  public void ascLocal() throws Exception {
    assertThatCompiling("f:proc(a:string) { c=a b=asc(c) println b} f('hi')")
        .executedEqualsInterpreted();
  }

  @Test
  public void printParse() throws Exception {
    assertThatCompiling(
            "print 123 print ', '\n" //
                + " print 'should be b:'\n" //
                + " Println 'abcde'[1]")
        .executedEqualsInterpreted();
  }

  @Test
  public void chr(@TestParameter({"65", "96"}) int value) throws Exception {
    assertThatCompiling(String.format("a=%d b=chr(a) print b", value)).executedEqualsInterpreted();
    assertThatCompiling(String.format("a=chr(%d) print a", value)).executedEqualsInterpreted();
  }
}
