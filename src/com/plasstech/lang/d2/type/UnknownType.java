package com.plasstech.lang.d2.type;

import java.util.Objects;

/** Represents the one and only UNKNOWN type. */
class UnknownType implements VarType {
  @Override
  public String name() {
    return "UNKNOWN";
  }

  @Override
  public int size() {
    throw new IllegalStateException("Should not try to get size of UNKNOWN");
  }

  @Override
  public String toString() {
    return name();
  }

  @Override
  public boolean equals(Object obj) {
    // Yes this is intentionally strict.
    return this == obj;
  }

  @Override
  public int hashCode() {
    return Objects.hash("UNKNOWN", 2);
  }
}
