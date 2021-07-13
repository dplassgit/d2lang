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
    //    return String.format("%s /* (%s) */", name(), this.getClass().getSimpleName());
    return name();
  }

  public static Location allocate(SymbolStorage storage, String name) {
    switch (storage) {
      case GLOBAL:
      case HEAP:
        return new MemoryAddress(name);
      case LOCAL:
      case PARAM:
      case TEMP:
        return new StackLocation(name);
      default:
        throw new IllegalArgumentException("Cannot allocate storage for " + storage.name());
    }
  }
}
