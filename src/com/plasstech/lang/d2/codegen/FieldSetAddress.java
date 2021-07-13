package com.plasstech.lang.d2.codegen;

import com.plasstech.lang.d2.type.SymbolStorage;

public class FieldSetAddress extends Location {
  private final String field;
  private final Location recordLocation;

  public FieldSetAddress(String variable, String field, SymbolStorage storage) {
    super(variable);
    this.recordLocation = Location.allocate(storage, variable);
    this.field = field;
  }

  public String record() {
    return super.name();
  }

  public Location recordLocation() {
    return recordLocation;
  }

  public String field() {
    return field;
  }

  @Override
  public String name() {
    throw new IllegalStateException("Cannot take name of field set");
  }

  @Override
  public String toString() {
    return String.format("%s.%s", record(), field());
  }

  @Override
  public boolean equals(Object thatO) {
    if (thatO == null || !(thatO instanceof FieldSetAddress)) {
      return false;
    }
    FieldSetAddress that = (FieldSetAddress) thatO;
    return this.field.equals(that.field) && this.record().equals(that.record());
  }

  @Override
  public int hashCode() {
    return 53 + 11 * this.record().hashCode() ^ this.field.hashCode();
  }

  @Override
  public SymbolStorage storage() {
    return SymbolStorage.GLOBAL;
  }
}
