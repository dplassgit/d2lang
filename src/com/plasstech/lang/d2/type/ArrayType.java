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

  final public int dimensions() {
    return dimensions;
  }

  @Override
  final public boolean isArray() {
    return true;
  }

  final public VarType baseType() {
    return baseType;
  }

  @Override
  final public boolean compatibleWith(VarType thatType) {
    return this.equals(thatType);
  }

  final public ArrayType setKnownLength(int length) {
    this.knownLength = Optional.of(length);
    return this;
  }

  final public Optional<Integer> knownLength() {
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
