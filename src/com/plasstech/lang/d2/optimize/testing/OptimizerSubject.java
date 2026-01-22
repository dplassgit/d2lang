package com.plasstech.lang.d2.optimize.testing;

import static com.google.common.truth.Truth.assertAbout;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import com.google.common.truth.Truth;
import com.plasstech.lang.d2.optimize.Optimizer;

public class OptimizerSubject extends Subject {
  public static OptimizerSubject assertThat(Optimizer optimizer) {
    return assertAbout(OptimizerSubject::new).that(optimizer);
  }

  private final Optimizer optimizer;

  private OptimizerSubject(FailureMetadata metadata, Optimizer optimizer) {
    super(metadata, optimizer);
    this.optimizer = optimizer;
  }

  public void isChanged() {
    Truth.assertThat(optimizer.isChanged()).isTrue();
  }

  public void isNotChanged() {
    Truth.assertThat(optimizer.isChanged()).isFalse();
  }
}
