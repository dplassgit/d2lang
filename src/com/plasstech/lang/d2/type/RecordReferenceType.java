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
}
