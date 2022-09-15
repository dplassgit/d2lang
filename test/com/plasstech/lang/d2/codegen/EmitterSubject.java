package com.plasstech.lang.d2.codegen;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertAbout;

import com.google.common.collect.ImmutableList;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;

class EmitterSubject extends Subject {

  private final Emitter actual;

  private EmitterSubject(FailureMetadata metadata, Emitter actual) {
    super(metadata, actual);
    this.actual = actual;
  }

  public void containsExactly(String line) {
    check("containsExactly").that(trim(actual.all())).containsExactly(line);
  }

  private ImmutableList<String> trim(ImmutableList<String> all) {
    return all.stream()
        .map(
            (String s) -> {
              int semi = s.indexOf(';');
              if (semi != -1) {
                s = s.substring(0, semi - 1);
              }
              return s.trim();
            })
        .collect(toImmutableList());
  }

  public static EmitterSubject assertThat(Emitter actual) {
    return assertAbout(EmitterSubject::new).that(actual);
  }
}

