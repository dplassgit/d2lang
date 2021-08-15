package com.plasstech.lang.d2.codegen;

import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VarType;

public class FieldSetAddress extends Location {
  private final String field;
  private final Location recordLocation;
  private final SymbolStorage storage;

  public FieldSetAddress(String variable, String field, SymbolStorage storage, VarType varType) {
    super(variable, varType);
    this.storage = storage;
    // we should already know the location, because the variable should already exist
    this.recordLocation = Location.allocate(storage, variable, varType);
    this.field = field;
  }

  public String record() {
    return super.name();
  }

  @Override
  public Location baseLocation() {
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
    return storage;
  }
}
