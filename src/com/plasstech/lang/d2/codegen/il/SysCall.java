package com.plasstech.lang.d2.codegen.il;

import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import com.plasstech.lang.d2.codegen.Operand;

public class SysCall extends Op {
  private static final Escaper ESCAPER =
      Escapers.builder()
          .addEscape('\n', "\\n")
          .addEscape('\r', "\\r")
          .addEscape('\t', "\\t")
          .addEscape('\"', "\\\"")
          .build();

  public enum Call {
    MESSAGE,
    PRINT,
    INPUT
  }

  private final Call call;
  private final Operand arg;

  public SysCall(Call call, Operand arg) {
    this.call = call;
    this.arg = arg;
  }

  public Call call() {
    return call;
  }

  public Operand arg() {
    return arg;
  }

  @Override
  public String toString() {
    switch (call) {
      case PRINT:
        return String.format("printf(\"%%s\", %s);", ESCAPER.escape(arg.toString()));
      case MESSAGE:
        return String.format("printf(\"ERROR: %%s\", %s);", ESCAPER.escape(arg.toString()));
      case INPUT:
        return String.format("%s=scanf();", arg);
      default:
        return String.format("call(%s, %s);", call.name(), arg);
    }
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }
}
