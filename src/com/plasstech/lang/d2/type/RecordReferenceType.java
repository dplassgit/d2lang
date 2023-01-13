package com.plasstech.lang.d2.type;

import java.util.Objects;

/** A forward (or backward) reference to a record type. */
public class RecordReferenceType extends PointerType {
  public RecordReferenceType(String recordSymbolName) {
    super(recordSymbolName);
  }

  @Override
  public boolean isRecord() {
    return true;
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
  public boolean compatibleWith(VarType that) {
    return that.equals(this) || that.isNull();
  }

  @Override
  public int hashCode() {
    return Objects.hash(name(), size()) + 7;
  }
}
