package com.plasstech.lang.d2.codegen.il;

import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.common.Position;

public class Inc extends Op {
  private final Location target;

  public Inc(Location target) {
    this(target, null);
  }

  public Inc(Location target, Position position) {
    super(position);
    this.target = target;
  }

  public Location target() {
    return target;
  }

  @Override
  public String toString() {
    return String.format("%s++", target.toString());
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }
}
