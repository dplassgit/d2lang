package com.plasstech.lang.d2.codegen;

import com.plasstech.lang.d2.type.SymbolStorage;

public class StackLocation extends Location {

  private final SymbolStorage storage;

  public StackLocation(String name) {
    this(name, SymbolStorage.LOCAL);
  }

  // TODO: capture "offset" into stack frame
  public StackLocation(String name, SymbolStorage storage) {
    super(name);
    this.storage = storage;
  }

  @Override
  public SymbolStorage storage() {
    return storage;
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
