package com.plasstech.lang.d2.codegen.testing;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertAbout;
import static java.util.Arrays.asList;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Ordered;
import com.google.common.truth.Subject;
import com.plasstech.lang.d2.codegen.Emitter;
import com.plasstech.lang.d2.codegen.Trimmers;

public class EmitterSubject extends Subject {
  public static EmitterSubject assertThat(Emitter actual) {
    return assertAbout(EmitterSubject::new).that(actual);
  }

  public static EmitterSubject assertWithoutTrimmingThat(Emitter actual) {
    return assertAbout(EmitterSubject::createWithoutTrimming).that(actual);
  }

  private final Emitter actual;
  private boolean trimIt;

  private EmitterSubject(FailureMetadata metadata, Emitter actual) {
    super(metadata, actual);
    this.actual = actual;
    this.trimIt = true;
  }

  private static EmitterSubject createWithoutTrimming(FailureMetadata metadata, Emitter actual) {
    return new EmitterSubject(metadata, actual).withoutTrimming();
  }

  private EmitterSubject withoutTrimming() {
    this.trimIt = false;
    return this;
  }

  private List<String> allEmitterOutputTrimmed() {
    return Stream.of(actual.externs(), actual.data(), actual.all())
        .flatMap(Collection::stream)
        .map(EmitterSubject::trim)
        .filter(s -> !s.isEmpty())
        .collect(toImmutableList());
  }

  public void containsExactly(String... lines) {
    List<String> expected = asList(lines);
    check("containsExactly")
        .that(trimAll(actual.all()))
        .containsExactlyElementsIn(trimAll(expected))
        .inOrder();
  }

  public void contains(String line) {
    containsAtLeast(line);
  }

  public void doesNotContain(String line) {
    check("contains")
        .that(trimAll(actual.all()))
        .doesNotContain(optionallyTrim(line));
  }

  public Ordered containsAtLeast(String... lines) {
    List<String> expected = asList(lines);
    return check("contains")
        .that(allEmitterOutputTrimmed())
        .containsAtLeastElementsIn(trim(expected));
  }

  public void isEmpty() {
    check("contains").that(trimAll(actual.all())).isEmpty();
  }

  private ImmutableList<String> trimAll(List<String> all) {
    if (trimIt) {
      return Trimmers.trim(all);
    } else {
      return all.stream()
          .filter(s -> !s.isEmpty())
          .collect(toImmutableList());
    }
  }

  private ImmutableList<String> trim(List<String> all) {
    return all.stream()
        .map(s -> optionallyTrim(s))
        .filter(s -> !s.isEmpty())
        .collect(toImmutableList());
  }

  private String optionallyTrim(String s) {
    if (trimIt) {
      return Trimmers.trim(s);
    } else {
      return s;
    }
  }
}
