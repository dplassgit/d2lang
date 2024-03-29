package com.plasstech.lang.d2.codegen.il;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.type.ParamSymbol;

public class ProcEntry extends Op {

  private final String name;
  private final ImmutableList<ParamSymbol> formals;
  private final int localBytes;

  public ProcEntry(String name, ImmutableList<ParamSymbol> formals, int localBytes) {
    this.name = name;
    this.formals = formals;
    this.localBytes = localBytes;
  }

  public String name() {
    return name;
  }

  public ImmutableList<ParamSymbol> formals() {
    return formals;
  }

  public ImmutableList<String> formalNames() {
    return formals.stream().map(ParamSymbol::name).collect(toImmutableList());
  }

  @Override
  public String toString() {
    return String.format("%s(%s) {", name(), Joiner.on(',').join(formals));
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }

  /** Number of bytes needed for storage for locals */
  public int localBytes() {
    return localBytes;
  }
}
