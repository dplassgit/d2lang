package com.plasstech.lang.d2.codegen.il;

import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.type.ArrayType;

/** Represents an array allocation operation, e.g.: foo:int[3] */
public class ArrayAlloc extends Op {

  private final ArrayType arrayType;
  private final Location sizeLocation;
  private final Location destination;

  public ArrayAlloc(Location destination, ArrayType arrayType, Location sizeLocation) {
    this.destination = destination;
    this.arrayType = arrayType;
    this.sizeLocation = sizeLocation;
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }

  public Location destination() {
    return destination;
  }
  public ArrayType arrayType() {
    return arrayType;
  }

  public Location sizeLocation() {
    return sizeLocation;
  }
}
