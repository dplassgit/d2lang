package com.plasstech.lang.d2.codegen.il;

import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;

// An opcode.
public abstract class Op {
  protected static final Escaper ESCAPER =
      Escapers.builder()
          .addEscape('\n', "\\n")
          .addEscape('\r', "\\r")
          .addEscape('\t', "\\t")
          .addEscape('\"', "\\\"")
          .build();

  public void accept(OpcodeVisitor visitor) {
    // does nothing by default.
  }
}
