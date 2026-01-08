package com.plasstech.lang.d2.codegen.testing;

import static com.google.common.truth.Truth.assertAbout;

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.truth.FailureMetadata;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.phase.PhaseName;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.phase.testing.PhaseSubject;

public class ILCodeGeneratorSubject extends PhaseSubject {
  public static ILCodeGeneratorSubject assertThatGenerating(String code) {
    return assertAbout(ILCodeGeneratorSubject::new).that(code);
  }

  private ILCodeGeneratorSubject(FailureMetadata metadata, String code) {
    super(metadata, code, PhaseName.RANGE_CHECKS);
  }

  public List<Op> succeeds() {
    State state = compiles();

    ImmutableList<Op> ilCode = state.ilCode();
    System.err.println("\nIL CODE:\n--------");
    System.err.println(Joiner.on("\n").join(ilCode));
    return ilCode;
  }
}
