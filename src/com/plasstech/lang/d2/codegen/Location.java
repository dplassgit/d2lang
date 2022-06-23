package com.plasstech.lang.d2.codegen;

import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VarType;

public abstract class Location implements Operand {
  private final String name;
  private final VarType varType;

  public Location(String name, VarType varType) {
    this.name = name;
    this.varType = varType;
  }

  public String name() {
    return name;
  }

  @Override
  public VarType type() {
    return varType;
  }

  @Override
  public String toString() {
    //    return String.format("%s /* (%s) */", name(), this.getClass().getSimpleName());
    return name();
  }
  
  public Location baseLocation() {
    return this;
  }

  public static Location allocate(SymbolStorage storage, String name, VarType varType) {
    switch (storage) {
      case GLOBAL:
      case HEAP:
        return new MemoryAddress(name, varType);
      case LOCAL:
        return new StackLocation(name, varType);
//      case PARAM:
//        return new StackLocation(name, SymbolStorage.PARAM, varType);
      case TEMP:
        return new TempLocation(name, varType);
      default:
        throw new IllegalArgumentException("Cannot allocate storage for " + storage.name());
    }
  }
}
