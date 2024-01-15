package com.plasstech.lang.d2;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.plasstech.lang.d2.common.CompilationConfiguration;
import com.plasstech.lang.d2.phase.PhaseName;
import com.plasstech.lang.d2.phase.State;

public class YetAnotherCompilerTest {
  private YetAnotherCompiler yac = new YetAnotherCompiler();

  @Test
  public void lexerError() {
    CompilationConfiguration config = CompilationConfiguration.builder()
        .setLastPhase(PhaseName.PARSE)
        .setSourceCode("_hi=3")
        .build();
    State result = yac.compile(config);
    assertThat(result.error()).isTrue();
    assertThat(result.errorMessage()).contains("Illegal variable name _hi");
  }
}
