package com.plasstech.lang.d2.codegen.il;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

public class ProcEntry extends Op {

  private final String name;
  private final ImmutableList<String> formals;

  public ProcEntry(String name, ImmutableList<String> formals) {
    this.name = name;
    this.formals = formals;
  }

  public String name() {
    return name;
  }

  public ImmutableList<String> formalNames() {
    return formals;
  }

  @Override
  public String toString() {
    return String.format("%s(%s) {", name(), Joiner.on(',').join(formals));
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }
}
