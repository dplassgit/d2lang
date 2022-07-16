package com.plasstech.lang.d2.codegen.il;

import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import com.plasstech.lang.d2.common.Position;

// An opcode.
public abstract class Op {

  private final Position position;

  Op() {
    this(null);
  }

  Op(Position position) {
    this.position = position;
  }

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

  public Position position() {
    return position;
  }
}
