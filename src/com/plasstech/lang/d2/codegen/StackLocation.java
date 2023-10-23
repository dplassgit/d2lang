package com.plasstech.lang.d2.codegen;

import java.util.Objects;

import com.plasstech.lang.d2.type.LocalSymbol;

public class StackLocation extends VariableLocation {

  private final int offset;

  public StackLocation(LocalSymbol local) {
    super(local);
    this.offset = local.offset();
  }

  /** this is ALWAYS positive */
  public int offset() {
    return offset;
  }

  @Override
  public boolean equals(Object that) {
    if (that == null || !(that instanceof StackLocation)) {
      return false;
    }
    return this.hashCode() == that.hashCode();
  }

  @Override
  public int hashCode() {
    return Objects.hash(getClass().getName(), name(), type(), offset(), storage());
  }

  @Override
  public String toString() {
    return String.format("%s: %s (%d)", name(), type(), offset());
  }
}
