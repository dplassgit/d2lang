package com.plasstech.lang.d2.type;

/** Simple (primitive) type: int, bool, string, void. */
class SimpleType implements VarType {
  private final String name;

  public SimpleType(String name) {
    this.name = name;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public String toString() {
    return name();
  }

  @Override
  public boolean equals(Object thatObject) {
    if (thatObject == null || !(thatObject instanceof SimpleType)) {
      return false;
    }
    SimpleType that = (SimpleType) thatObject;
    return this.name().equals(that.name());
  }

  @Override
  public int hashCode() {
    return 31 * name().hashCode() + 7;
  }
}
