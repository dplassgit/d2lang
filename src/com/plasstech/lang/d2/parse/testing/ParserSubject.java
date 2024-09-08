package com.plasstech.lang.d2.parse.testing;

import static com.google.common.truth.Truth.assertAbout;

import com.google.common.truth.FailureMetadata;
import com.plasstech.lang.d2.parse.node.ProgramNode;
import com.plasstech.lang.d2.phase.PhaseName;
import com.plasstech.lang.d2.phase.testing.PhaseSubject;

public class ParserSubject extends PhaseSubject {
  public static ParserSubject assertThatParsing(String code) {
    return assertAbout(ParserSubject::new).that(code);
  }

  private ParserSubject(FailureMetadata metadata, String code) {
    super(metadata, code, PhaseName.PARSE);
  }

  public ProgramNode succeeds() {
    return compiles().programNode();
  }
}
