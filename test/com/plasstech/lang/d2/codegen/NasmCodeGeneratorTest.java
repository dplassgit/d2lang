package com.plasstech.lang.d2.codegen;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.google.common.base.Joiner;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.testing.TestUtils;

public class NasmCodeGeneratorTest {

  @Test
  public void print() {
    String sourceCode = "print 3";
    State state = TestUtils.compile(sourceCode);
    state = new NasmCodeGenerator().execute(state);
    System.err.println(Joiner.on('\n').join(state.asmCode()));
    assertThat(state.asmCode()).isNotEmpty();
  }

  @Test
  public void ifPrint() {
    String sourceCode = "a=3 while true { if a == 3 {continue} print a }";
    State state = TestUtils.compile(sourceCode);
    state = new NasmCodeGenerator().execute(state);
    System.err.println(Joiner.on('\n').join(state.asmCode()));
    assertThat(state.asmCode()).isNotEmpty();
  }
  //
  //  @Test
  //  public void assign() {
  //    String sourceCode = "a=3 print a";
  //    State state = TestUtils.compile(sourceCode);
  //    state = new NasmCodeGenerator().execute(state);
  //    System.err.println(Joiner.on('\n').join(state.asmCode()));
  //    assertThat(state.asmCode()).isNotEmpty();
  //  }

  @Test
  public void stop() {
    String sourceCode = "main {print 3 exit 'hi'}";
    State state = TestUtils.compile(sourceCode);
    state = new NasmCodeGenerator().execute(state);
    System.err.println(Joiner.on('\n').join(state.asmCode()));
    assertThat(state.asmCode()).isNotEmpty();
  }
}
