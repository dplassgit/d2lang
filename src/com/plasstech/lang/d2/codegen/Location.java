package com.plasstech.lang.d2.codegen;

import com.plasstech.lang.d2.type.SymbolStorage;

public abstract class Location implements Operand {
  private final String name;

  public Location(String name) {
    this.name = name;
  }

  public String name() {
    return name;
  }

  @Override
  public String toString() {
    return String.format("/* (%s) */ %s", this.getClass().getSimpleName(), name);
  }

  public abstract SymbolStorage storage();
}
