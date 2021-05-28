package com.plasstech.lang.d2.codegen.il;

import java.util.Optional;

public class Return extends Op {

  private final Optional<Location> location;

  public Return() {
    this.location = Optional.empty();
  }

  public Return(Location location) {
    this.location = Optional.of(location);
  }

  public Optional<Location> location() {
    return location;
  }

  @Override
  public String toString() {
    return String.format("\treturn %s;", location.isPresent() ? location.toString() : "");
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }
}
