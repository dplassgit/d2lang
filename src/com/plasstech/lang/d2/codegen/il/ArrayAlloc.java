package com.plasstech.lang.d2.codegen.il;

import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.ArrayType;

/** Represents an array allocation operation, e.g.: foo:int[3] */
public class ArrayAlloc extends Op {

  private final ArrayType arrayType;
  private final Operand sizeLocation;
  private final Location destination;

  public ArrayAlloc(Location destination, ArrayType arrayType, Operand sizeLocation,
      Position position) {
    super(position);
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

  public Operand sizeLocation() {
    return sizeLocation;
  }

  @Override
  public String toString() {
    return String.format("%s = new %s[size: %s]", destination, arrayType.baseType(), sizeLocation);
  }
}
