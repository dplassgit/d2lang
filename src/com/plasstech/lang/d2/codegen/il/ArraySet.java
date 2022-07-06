package com.plasstech.lang.d2.codegen.il;

import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.type.ArrayType;

public class ArraySet extends Op {
  private final Location destination;
  private final Operand index;
  private final Operand rhs;
  private final ArrayType arrayType;

  public ArraySet(
      ArrayType arrayType, Location destination, Operand index, Operand rhs) {
    this.arrayType = arrayType;
    this.destination = destination;
    this.index = index;
    this.rhs = rhs;
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }

  public Location destination() {
    return destination;
  }

  public Operand index() {
    return index;
  }

  public Operand source() {
    return rhs;
  }

  @Override
  public String toString() {
    return String.format("%s[%s] = %s;", destination, index, rhs);
  }

  public ArrayType arrayType() {
    return arrayType;
  }
}
