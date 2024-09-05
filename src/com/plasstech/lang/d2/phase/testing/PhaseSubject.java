package com.plasstech.lang.d2.phase.testing;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assert.fail;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import com.plasstech.lang.d2.YetAnotherCompiler;
import com.plasstech.lang.d2.common.CompilationConfiguration;
import com.plasstech.lang.d2.phase.PhaseName;
import com.plasstech.lang.d2.phase.State;

public abstract class PhaseSubject extends Subject {
  private CompilationConfiguration config;

  protected PhaseSubject(FailureMetadata metadata, String code, PhaseName phase) {
    super(metadata, code);
    this.config =
        CompilationConfiguration.builder()
            .setLastPhase(phase)
            .setSourceCode(code)
            .build();
  }

  public void hasError(String expectedError) {
    YetAnotherCompiler compiler = new YetAnotherCompiler();
    State state = compiler.compile(this.config);
    assertWithMessage("Should have an error").that(state.error()).isTrue();
    String expectedRegexp = expectedError;
    if (expectedRegexp != null && expectedRegexp.length() > 0) {
      if (!expectedRegexp.startsWith(".*")) {
        expectedRegexp = ".*" + expectedRegexp;
      }
      if (!expectedRegexp.endsWith(".*")) {
        expectedRegexp = expectedRegexp + ".*";
      }
      expectedRegexp = "(?s)" + expectedRegexp;
    }
    assertThat(state.errorMessage()).matches(expectedRegexp);
  }

  protected final State compiles() {
    YetAnotherCompiler compiler = new YetAnotherCompiler();
    State state = compiler.compile(this.config);
    if (state.error()) {
      if (state.exception() != null) {
        state.exception().printStackTrace(System.err);
      }
      fail(state.errorMessage());
    }
    return state;
  }
}
