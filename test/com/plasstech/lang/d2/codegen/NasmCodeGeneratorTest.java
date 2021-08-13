package com.plasstech.lang.d2.codegen;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.google.common.base.Joiner;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.testing.TestUtils;

public class NasmCodeGeneratorTest {

  @Test
  public void printString() {
    String sourceCode = "print 'printString'";
    State state = TestUtils.compile(sourceCode);
    state = state.addFilename("printString");
    state = new NasmCodeGenerator().execute(state);
    System.err.println(Joiner.on('\n').join(state.asmCode()));
    assertThat(state.asmCode()).isNotEmpty();
  }

  @Test
  public void print3() {
    String sourceCode = "print 3";
    State state = TestUtils.compile(sourceCode);
    state = state.addFilename("print3");
    state = new NasmCodeGenerator().execute(state);
    System.err.println(Joiner.on('\n').join(state.asmCode()));
    assertThat(state.asmCode()).isNotEmpty();
  }

  //  @Test
  //  public void ifPrint() {
  //    String sourceCode = "a=3 while true { if a == 3 {continue} print a }";
  //    State state = TestUtils.compile(sourceCode);
  //    state = new NasmCodeGenerator().execute(state);
  //    System.err.println(Joiner.on('\n').join(state.asmCode()));
  //    assertThat(state.asmCode()).isNotEmpty();
  //  }
  //
  @Test
  public void assign() {
    String sourceCode = "assign=3 bassign=assign";
    State state = TestUtils.compile(sourceCode);
    state = state.addFilename("assign");
    state = new NasmCodeGenerator().execute(state);
    System.err.println(Joiner.on('\n').join(state.asmCode()));
    assertThat(state.asmCode()).isNotEmpty();
  }

  @Test
  public void exit() {
    String sourceCode = "exit";
    State state = TestUtils.compile(sourceCode);
    state = state.addFilename("exit");
    state = new NasmCodeGenerator().execute(state);
    System.err.println(Joiner.on('\n').join(state.asmCode()));
    assertThat(state.asmCode()).isNotEmpty();
  }

  @Test
  public void exitErrorGlobal() {
    String sourceCode = "a='exitErrorGlobal' exit a";
    State state = TestUtils.compile(sourceCode);
    state = state.addFilename("exitErrorGlobal");
    state = new NasmCodeGenerator().execute(state);
    System.err.println(Joiner.on('\n').join(state.asmCode()));
    assertThat(state.asmCode()).isNotEmpty();
  }

  @Test
  public void exitError() {
    String sourceCode = "exit 'exit'";
    State state = TestUtils.compile(sourceCode);
    state = state.addFilename("exitError");
    state = new NasmCodeGenerator().execute(state);
    System.err.println(Joiner.on('\n').join(state.asmCode()));
    assertThat(state.asmCode()).isNotEmpty();
  }

  @Test
  public void exitMain() {
    String sourceCode = "main {exit 'exitMain'}";
    State state = TestUtils.compile(sourceCode);
    state = state.addFilename("exitMain");
    state = new NasmCodeGenerator().execute(state);
    System.err.println(Joiner.on('\n').join(state.asmCode()));
    assertThat(state.asmCode()).isNotEmpty();
  }

  @Test
  public void incDec() {
    String sourceCode = "a=42 a=a+1 a=a-1";
    State state = TestUtils.compile(sourceCode);
    state = state.addFilename("incDec");
    state = new NasmCodeGenerator().execute(state);
    System.err.println(Joiner.on('\n').join(state.asmCode()));
    assertThat(state.asmCode()).isNotEmpty();
  }
}
