package com.plasstech.lang.d2.type;

import java.util.List;

/**
 * A record type. It can be recursive, either via a field with this type, or via a field with
 * RecordReferenceType, with this type's name.
 */
public class RecordType implements VarType {
  public static class Field {
    private final String name;
    private final VarType type;

    public Field(String name, VarType type) {
      this.name = name;
      this.type = type;
    }

    public String name() {
      return name;
    }

    public VarType type() {
      return type;
    }
  }

  private final String name;
  private final List<Field> fields;

  public RecordType(String name, List<Field> fields) {
    this.name = name;
    this.fields = fields;
  }

  @Override
  public String name() {
    return name;
  }

  public List<Field> fields() {
    return fields;
  }
}
