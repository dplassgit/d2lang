package com.plasstech.lang.d2.type;

import java.util.List;
import java.util.Objects;

import com.google.common.collect.ImmutableList;

/** A forward (or backward) reference to a record type. */
public class RecordReferenceType extends PointerType {
  private final ImmutableList<VarType> actualTypes;

  public RecordReferenceType(String recordSymbolName) {
    this(recordSymbolName, ImmutableList.of());
  }

  /** Generic */
  public RecordReferenceType(String recordSymbolName, List<VarType> actualTypes) {
    super(recordSymbolName);
    this.actualTypes = ImmutableList.copyOf(actualTypes);
  }

  @Override
  final public boolean isRecord() {
    return true;
  }

  @Override
  final public boolean compatibleWith(VarType that) {
    return that.equals(this) || that.isNull();
  }

  @Override
  public String toString() {
    return String.format("%s: RECORD", name());
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof RecordReferenceType)) {
      return false;
    }
    return this.hashCode() == obj.hashCode();
  }

  @Override
  public int hashCode() {
    return Objects.hash(name(), size()) + 7;
  }

  public ImmutableList<VarType> actualTypes() {
    return actualTypes;
  }

  public boolean isGeneric() {
    return !actualTypes.isEmpty();
  }
}
