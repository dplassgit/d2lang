package com.plasstech.lang.d2.type.testing;

import static com.google.common.truth.Truth.assertAbout;

import com.google.common.truth.FailureMetadata;
import com.plasstech.lang.d2.phase.PhaseName;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.phase.testing.PhaseSubject;

public class StaticCheckerSubject extends PhaseSubject {
  public static StaticCheckerSubject assertThatTypeChecking(String code) {
    return assertAbout(StaticCheckerSubject::new).that(code);
  }

  //  private CompilationConfiguration config;

  private StaticCheckerSubject(FailureMetadata metadata, String code) {
    super(metadata, code, PhaseName.TYPE_CHECK);
    //
    //    this.config =
    //        CompilationConfiguration.builder()
    //            .setSourceCode(code)
    //            .setLastPhase(PhaseName.TYPE_CHECK)
    //            .build();
  }

  //  public void hasError(String expectedError) {
  //    // get rid of this
  //    this.config = this.config.toBuilder().setExpectedErrorMessage(expectedError).build();
  //    YetAnotherCompiler compiler = new YetAnotherCompiler();
  //    State state = compiler.compile(this.config);
  //    assertWithMessage("Should have an error").that(state.error()).isTrue();
  //    // I have a feeling this will be needed...
  //    String expectedRegexp = expectedError;
  //    if (expectedRegexp != null && expectedRegexp.length() > 0) {
  //      if (!expectedRegexp.startsWith(".*")) {
  //        expectedRegexp = ".*" + expectedRegexp;
  //      }
  //      if (!expectedRegexp.endsWith(".*")) {
  //        expectedRegexp = expectedRegexp + ".*";
  //      }
  //      expectedRegexp = "(?s)" + expectedRegexp;
  //    }
  //    assertThat(state.errorMessage()).matches(expectedRegexp);
  //  }

  public State succeeds() {
    return compiles();
    // 1. compile & execute the original source
    //    YetAnotherCompiler compiler = new YetAnotherCompiler();
    //    State state = compiler.compile(this.config);
    //    assertWithMessage("Should not have an error").that(state.error()).isFalse();
    //    if (state.error()) {
    //      state.exception().printStackTrace(System.err);
    //      fail(state.errorMessage());
    //    }
    //    return state;
  }
}
