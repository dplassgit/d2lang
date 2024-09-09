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

  private StaticCheckerSubject(FailureMetadata metadata, String code) {
    super(metadata, code, PhaseName.TYPE_CHECK);
  }

  public State succeeds() {
    return compiles();
  }
}
