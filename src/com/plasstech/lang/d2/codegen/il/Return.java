package com.plasstech.lang.d2.codegen.il;

import java.util.Optional;

import com.plasstech.lang.d2.codegen.Location;

public class Return extends Op {

  private final Optional<Location> returnValueLocation;

  public Return() {
    this.returnValueLocation = Optional.empty();
  }

  public Return(Location location) {
    this.returnValueLocation = Optional.of(location);
  }

  public Optional<Location> returnValueLocation() {
    return returnValueLocation;
  }

  @Override
  public String toString() {
    return String.format("return %s;",
            returnValueLocation.isPresent() ? returnValueLocation.get().toString() : "");
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }
}
