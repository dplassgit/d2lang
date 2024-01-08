package com.plasstech.lang.d2.type;

import java.util.Objects;

/** Simple (primitive) type: int, bool, string, void. */
class SimpleType extends DefaultVarType {
  private final int size;

  SimpleType(String name) {
    this(name, 0);
  }

  // size in bytes
  SimpleType(String name, int size) {
    super(name);
    this.size = size;
  }

  @Override
  final public int size() {
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
