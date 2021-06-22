package com.plasstech.lang.d2.codegen;

import com.plasstech.lang.d2.type.SymbolStorage;

public class StackLocation extends Location {

  // TODO: capture "offset" into stack frame
  public StackLocation(String name) {
    super(name);
  }

  @Override
  public SymbolStorage storage() {
    // TODO: might be a parameter (but still on the stack, shrug)
    return SymbolStorage.LOCAL;
  }

  @Override
  public boolean equals(Object that) {
    if (that == null || !(that instanceof StackLocation)) {
      return false;
    }
    return this.name().equals(((StackLocation) that).name());
  }

  @Override
  public int hashCode() {
    return 37 + 11 * this.name().hashCode();
  }
}
