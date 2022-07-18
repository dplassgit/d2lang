package com.plasstech.lang.d2.type;

/** A forward (or backward) reference to a record type. */
public class RecordReferenceType implements VarType {
  private final String name;

  public RecordReferenceType(String recordSymbolName) {
    this.name = recordSymbolName;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public int size() {
    return 8;
  }

  @Override
  public boolean isRecord() {
    return true;
  }

  @Override
  public String toString() {
    return String.format("%s: RECORD", name);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof RecordReferenceType)) {
      return false;
    }
    RecordReferenceType that = (RecordReferenceType) obj;
    return this.name.equals(that.name);
  }

  @Override
  public boolean compatibleWith(VarType that) {
    return that.equals(this) || that.isNull();
  }

  @Override
  public int hashCode() {
    return 17 + 37 * name.hashCode();
  }
}
