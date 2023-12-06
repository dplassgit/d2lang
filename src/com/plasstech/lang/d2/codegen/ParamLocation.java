package com.plasstech.lang.d2.codegen;

import java.util.Objects;

import com.plasstech.lang.d2.type.ParamSymbol;

public class ParamLocation extends VariableLocation {
  private final int index;
  private final int offset;

  public ParamLocation(ParamSymbol param) {
    super(param);
    this.index = param.index();
    this.offset = param.offset();
  }

  public int index() {
    return index;
  }

  public int offset() {
    return offset;
  }

  @Override
  public boolean equals(Object that) {
    if (that == null || !(that instanceof ParamLocation)) {
      return false;
    }
    return this.hashCode() == that.hashCode();
  }

  @Override
  public int hashCode() {
    return Objects.hash(getClass().getName(), name(), type(), storage(), index, offset);
  }

  @Override
  public String toString() {
    if (offset == 0) {
      return String.format("%s: %s (#%d)", name(), type(), index);
    } else {
      return String.format("%s: %s (#%d, %d)", name(), type(), index, offset);
    }
  }
}