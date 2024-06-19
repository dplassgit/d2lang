package com.plasstech.lang.d2.codegen.il;

import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.common.Position;

/**
 * Indicates the code generator should "deallocate" this (long) temp, meaning it is no longer used.
 */
public class DeallocateTemp extends Op {

  private final Location temp;

  public DeallocateTemp(Location temp, Position position) {
    super(position);
    this.temp = temp;
  }

  public Location temp() {
    return temp;
  }

  @Override
  public String toString() {
    return String.format("dealloc(%s)", temp.toString());
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }
}
