package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertAbout;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import com.google.common.truth.Truth;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Transfer;

/** Assertions for optimizer tests. */
public class OpcodeSubject extends Subject {

  private final Op actual;

  private OpcodeSubject(FailureMetadata metadata, Op actual) {
    super(metadata, actual);
    this.actual = actual;
  }

  public void isTransferredFrom(Operand source) {
    Truth.assertThat(actual).isInstanceOf(Transfer.class);
    Transfer transfer = (Transfer) actual;
    check("isTransferredFrom").that(transfer.source()).isEqualTo(source);
  }

  public static OpcodeSubject assertThat(Op actual) {
    return assertAbout(OpcodeSubject::new).that(actual);
  }
}
