package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;

import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Transfer;

public class OptimizerAsserts {
  static void assertTransferFrom(Op op, Operand expectedSource) {
    assertThat(((Transfer) op).source()).isEqualTo(expectedSource);
  }
}
