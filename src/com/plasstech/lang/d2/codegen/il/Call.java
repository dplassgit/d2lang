package com.plasstech.lang.d2.codegen.il;

import java.util.Optional;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.Location;

public class Call extends Op {

  private final String functionToCall;
  private final ImmutableList<Location> actualLocations;
  private final Optional<Location> destination;

  public Call(Location destination, String functionToCall, ImmutableList<Location> actuals) {
    this.destination = Optional.ofNullable(destination);
    this.functionToCall = functionToCall;
    this.actualLocations = actuals;
  }

  public Call(String functionToCall, ImmutableList<Location> actuals) {
    this(null, functionToCall, actuals);
  }

  public String functionToCall() {
    return functionToCall;
  }

  public ImmutableList<Location> actualLocations() {
    return actualLocations;
  }

  public Optional<Location> destination() {
    return destination;
  }

  @Override
  public String toString() {
    if (destination().isPresent()) {
      return String.format("%s = %s(%s);", destination().get().name(), functionToCall,
              Joiner.on(",").join(actualLocations));
    } else {
      return String.format("%s(%s);", functionToCall, Joiner.on(",").join(actualLocations));
    }
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }
}
