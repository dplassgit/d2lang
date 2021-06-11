package com.plasstech.lang.d2.codegen.il;

import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;

/** Represents transfer between two locations - could be memory/memory or memory/reg, etc. */
public class Transfer extends Op {
  private final Operand source;
  private final Location destination;

  public Transfer(Location destination, Operand source) {
    this.destination = destination;
    this.source = source;
  }

  public Location destination() {
    return destination;
  }

  public Operand source() {
    return source;
  }

  @Override
  public String toString() {
    return String.format("%s = %s;", destination.toString(), source.toString());
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }
}
