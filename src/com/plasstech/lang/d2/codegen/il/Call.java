package com.plasstech.lang.d2.codegen.il;

import java.util.Optional;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;

public class Call extends Op {

  private final String functionToCall;
  private final ImmutableList<Operand> actualLocations;
  private final Optional<Location> destination;

  public Call(
      Optional<Location> destination, String functionToCall, ImmutableList<Operand> actuals) {
    this.destination = destination;
    this.functionToCall = functionToCall;
    this.actualLocations = actuals;
  }

  public Call(Location destination, String functionToCall, ImmutableList<Operand> actuals) {
    this(Optional.of(destination), functionToCall, actuals);
  }

  public Call(String functionToCall, ImmutableList<Operand> actuals) {
    this(Optional.empty(), functionToCall, actuals);
  }

  public String functionToCall() {
    return functionToCall;
  }

  public ImmutableList<Operand> actuals() {
    return actualLocations;
  }

  public Optional<Location> destination() {
    return destination;
  }

  @Override
  public String toString() {
    if (destination().isPresent()) {
      return String.format(
          "%s = %s(%s);",
          destination().get(), functionToCall, Joiner.on(", ").join(actualLocations));
    } else {
      return String.format("%s(%s);", functionToCall, Joiner.on(", ").join(actualLocations));
    }
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }
}
