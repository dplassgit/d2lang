package com.plasstech.lang.d2.codegen;

public class FieldSetAddress extends MemoryAddress {
  private final String field;

  public FieldSetAddress(String record, String field) {
    super(record);
    this.field = field;
  }

  public String record() {
    return super.name();
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
}
