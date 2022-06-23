package com.plasstech.lang.d2.codegen;

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
}
