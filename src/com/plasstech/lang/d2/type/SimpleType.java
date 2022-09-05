package com.plasstech.lang.d2.type;

import java.util.Objects;

/** Simple (primitive) type: int, bool, string, void. */
class SimpleType implements VarType {
  private final String name;
  private final int size;

  SimpleType(String name) {
    this(name, 0);
  }

  SimpleType(String name, int size) {
    this.name = name;
    this.size = size;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public String toString() {
    return name();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null || !(that instanceof SimpleType)) {
      return false;
    }
    return this.hashCode() == that.hashCode();
  }

  @Override
  public int hashCode() {
    return Objects.hash(getClass().getName(), name(), size);
  }
}
