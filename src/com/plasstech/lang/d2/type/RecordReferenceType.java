package com.plasstech.lang.d2.type;

/** A forward (or backward) reference to a record type. */
public class RecordReferenceType implements VarType {

  private final String recordTypeName;

  public RecordReferenceType(String recordTypeName) {
    this.recordTypeName = recordTypeName;
  }

  @Override
  public String name() {
    return recordTypeName;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof RecordReferenceType)) {
      return false;
    }
    RecordReferenceType that = (RecordReferenceType) obj;
    return this.recordTypeName.equals(that.recordTypeName);
  }

  @Override
  public int hashCode() {
    return 17 + 37 * recordTypeName.hashCode();
  }
}
