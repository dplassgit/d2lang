package com.plasstech.lang.d2.type;

import java.util.Objects;

/** Represents the one and only UNKNOWN type. */
final class UnknownType extends DefaultVarType {
  UnknownType() {
    super("UNKNOWN");
  }

  @Override
  public final int size() {
    throw new IllegalStateException("Should not try to get size of UNKNOWN");
  }

  @Override
  public final String toString() {
    return name();
  }

  @Override
  public final boolean equals(Object obj) {
    // Yes this is intentionally strict.
    return this == obj;
  }

  @Override
  public final int hashCode() {
    return Objects.hash(name(), 16);
  }
}
