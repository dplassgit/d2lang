package com.plasstech.lang.d2.type;

/** Simple (primitive) type: int, bool, string, void. */
public class SimpleType implements VarType {
  private final String name;
  private final int size;

  public SimpleType(String name) {
    this(name, 0);
  }

  public SimpleType(String name, int size) {
    this.name = name;
    this.size = size;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public int size() {
    return size;
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
