package com.plasstech.lang.d2;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.DeallocateTemp;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.common.CompilationConfiguration;
import com.plasstech.lang.d2.phase.PhaseName;
import com.plasstech.lang.d2.phase.State;

public class YetAnotherCompilerTest {
  private YetAnotherCompiler yac = new YetAnotherCompiler();

  @Test
  public void lexerError() {
    CompilationConfiguration config = CompilationConfiguration.builder()
        .setParseDebugLevel(2)
        .setLastPhase(PhaseName.PARSE)
        .setSourceCode("_hi=3")
        .build();
    State result = yac.compile(config);
    assertThat(result.error()).isTrue();
    assertThat(result.errorMessage()).contains("Illegal variable name _hi");
  }

  @Test
  public void parseOnly() {
    CompilationConfiguration config = CompilationConfiguration.builder()
        .setParseDebugLevel(2)
        .setLastPhase(PhaseName.PARSE)
        .setSourceCode("hi=3")
        .build();
    State result = yac.compile(config);
    assertThat(result.error()).isFalse();
    assertThat(result.lastIlCode()).isNull();
  }

  @Test
  public void typeCheckOnly() {
    CompilationConfiguration config = CompilationConfiguration.builder()
        .setTypeDebugLevel(2)
        .setLastPhase(PhaseName.TYPE_CHECK)
        .setSourceCode("hi=3")
        .build();
    State result = yac.compile(config);
    assertThat(result.error()).isFalse();
    assertThat(result.lastIlCode()).isNull();
  }

  @Test
  public void ilCodeGenOnly() {
    CompilationConfiguration config = CompilationConfiguration.builder()
        .setCodeGenDebugLevel(2)
        .setLastPhase(PhaseName.IL_CODEGEN)
        .setSourceCode("a='' println a[0]")
        .build();
    State result = yac.compile(config);
    assertThat(result.error()).isFalse();
    ImmutableList<Op> code = result.lastIlCode();
    assertThat(code).isNotNull();
    long deallocCount = code.stream()
        .filter(op -> (op instanceof DeallocateTemp))
        .count();
    assertThat(deallocCount).isGreaterThan(0);
  }

  @Test
  public void all() {
    CompilationConfiguration config = CompilationConfiguration.builder()
        .setOptDebugLevel(2)
        .setOptimize(true)
        .setLastPhase(PhaseName.IL_OPTIMIZE)
        .setSourceCode("f:proc(a:int, b:int) {c=a+b d=(a+b)*(a+b)+c println d} f(1, 2)")
        .build();
    State result = yac.compile(config);
    assertThat(result.error()).isFalse();
    assertThat(result.lastIlCode()).isNotNull();
    assertThat(result.optimizedIlCode()).isNotNull();
  }
}
