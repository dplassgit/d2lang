package com.plasstech.lang.d2.codegen;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertAbout;
import static java.util.Arrays.asList;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;

class EmitterSubject extends Subject {

  private final Emitter actual;

  private EmitterSubject(FailureMetadata metadata, Emitter actual) {
    super(metadata, actual);
    this.actual = actual;
  }

  public void containsExactly(String... line) {
    List<String> expected = asList(line);
    check("containsExactly")
        .that(trim(actual.all()))
        .containsExactlyElementsIn(trim(expected))
        .inOrder();
  }

  public void contains(String line) {
    check("contains").that(trim(actual.all())).contains(trim(line));
  }

  public void isEmpty() {
    check("contains").that(trim(actual.all())).isEmpty();
  }

  private static String trim(String s) {
    int semi = s.indexOf(';');
    if (semi != -1) {
      s = s.substring(0, semi - 1);
    }
    return s.trim();
  }

  private static ImmutableList<String> trim(List<String> all) {
    return all.stream()
        .map(EmitterSubject::trim)
        .filter(s -> !s.isEmpty())
        .collect(toImmutableList());
  }

  public static EmitterSubject assertThat(Emitter actual) {
    return assertAbout(EmitterSubject::new).that(actual);
  }
}

