package com.plasstech.lang.d2.codegen;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertAbout;
import static java.util.Arrays.asList;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Ordered;
import com.google.common.truth.Subject;

public class EmitterSubject extends Subject {

  private final Emitter actual;

  private EmitterSubject(FailureMetadata metadata, Emitter actual) {
    super(metadata, actual);
    this.actual = actual;
  }

  public void containsExactly(String... lines) {
    List<String> expected = asList(lines);
    check("containsExactly")
        .that(trim(actual.all()))
        .containsExactlyElementsIn(trim(expected))
        .inOrder();
  }

  public void contains(String line) {
    containsAtLeast(line);
  }

  public void doesNotContain(String line) {
    check("contains").that(trim(actual.all())).doesNotContain(trim(line));
  }

  public Ordered containsAtLeast(String... lines) {
    List<String> expected = asList(lines);
    return check("contains").that(trim(actual.all())).containsAtLeastElementsIn(trim(expected));
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
