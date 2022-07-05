package com.plasstech.lang.d2.codegen.il;

import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.type.ArrayType;

public class ArraySet extends Op {
  private final Location destLocation;
  private final Location indexLocation;
  private final Location rhsLocation;
  private final ArrayType arrayType;

  public ArraySet(ArrayType arrayType, Location destLocation, Location indexLocation, Location rhsLocation) {
    this.arrayType = arrayType;
    this.destLocation = destLocation;
    this.indexLocation = indexLocation;
    this.rhsLocation = rhsLocation;
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }

  public Location destination() {
    return destLocation;
  }

  public Location index() {
    return indexLocation;
  }

  public Location source() {
    return rhsLocation;
  }

  @Override
  public String toString() {
    return String.format("%s[%s] = %s;", destLocation, indexLocation, rhsLocation);
  }

  public ArrayType arrayType() {
    return arrayType;
  }
}
