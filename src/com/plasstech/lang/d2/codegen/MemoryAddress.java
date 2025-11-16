package com.plasstech.lang.d2.codegen;

import com.plasstech.lang.d2.type.VariableSymbol;
import java.util.Objects;

public class MemoryAddress extends VariableLocation {
  public MemoryAddress(VariableSymbol variable) {
    super(variable);
  }

  @Override
  public boolean equals(Object that) {
    if (that == null || !(that instanceof MemoryAddress)) {
      return false;
    }
    return this.hashCode() == that.hashCode();
  }

  @Override
  public int hashCode() {
    return Objects.hash(getClass().getName(), name(), type(), storage());
  }
}
