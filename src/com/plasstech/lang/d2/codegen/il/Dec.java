package com.plasstech.lang.d2.codegen.il;

import com.plasstech.lang.d2.codegen.Location;

public class Dec extends Op {
  private final Location target;

  public Dec(Location target) {
    this.target = target;
  }

  public Location target() {
    return target;
  }

  @Override
  public String toString() {
    return String.format("%s--;", target.toString());
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }
}
