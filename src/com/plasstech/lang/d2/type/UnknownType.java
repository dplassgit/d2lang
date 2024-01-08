package com.plasstech.lang.d2.type;

import java.util.Objects;

/** Represents the one and only UNKNOWN type. */
final class UnknownType extends DefaultVarType {
  UnknownType() {
    super("UNKNOWN");
  }

  @Override
  final public int size() {
    throw new IllegalStateException("Should not try to get size of UNKNOWN");
  }

  @Override
  final public String toString() {
    return name();
  }

  @Override
  final public boolean equals(Object obj) {
    // Yes this is intentionally strict.
    return this == obj;
  }

  @Override
  final public int hashCode() {
    return Objects.hash(name(), 16);
  }
}
