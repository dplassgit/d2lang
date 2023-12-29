package com.plasstech.lang.d2.codegen;

import com.google.common.base.Objects;
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
    return name();
  }

  public Location baseLocation() {
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof Location && obj != null && obj.hashCode() == this.hashCode();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name(), type());
  }
}
