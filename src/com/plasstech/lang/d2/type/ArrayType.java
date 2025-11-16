package com.plasstech.lang.d2.type;

import java.util.Objects;
import java.util.Optional;

public class ArrayType extends PointerType {
  private final VarType baseType;
  private final int dimensions;
  private Optional<Integer> knownLength = Optional.empty();

  public ArrayType(VarType baseType, int dimensions) {
    super("ARRAY of " + baseType.name());
    // TODO: check the base type
    this.baseType = baseType;
    // TODO: check the dimensions
    this.dimensions = dimensions;
  }

  public final int dimensions() {
    return dimensions;
  }

  @Override
  public final boolean isArray() {
    return true;
  }

  public final VarType baseType() {
    return baseType;
  }

  @Override
  public final boolean compatibleWith(VarType thatType) {
    // This allows arrays of different sizes to be "compatible" - which should be OK, since
    // the runtime checks will make sure indexing is never out of bounds
    return this.equals(thatType) || thatType.isNull();
  }

  public final ArrayType setKnownLength(int length) {
    this.knownLength = Optional.of(length);
    return this;
  }

  public final Optional<Integer> knownLength() {
    return knownLength;
  }

  @Override
  public boolean equals(Object that) {
    if (that == null || !(that instanceof ArrayType)) {
      return false;
    }
    return this.hashCode() == that.hashCode();
  }

  @Override
  public int hashCode() {
    return Objects.hash(name(), dimensions(), baseType());
  }

  @Override
  public String toString() {
    return String.format("%d-d %s", dimensions(), name());
  }
}
