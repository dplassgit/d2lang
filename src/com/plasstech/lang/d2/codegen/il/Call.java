package com.plasstech.lang.d2.codegen.il;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

public class Call extends Op {

  private final String functionToCall;
  private final ImmutableList<Location> actualLocations;
  private final Location destination;

  public Call(Location destination, String functionToCall, ImmutableList<Location> actuals) {
    this.destination = destination;
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

  @Override
  public String toString() {
    if (destination != null) {
      return String.format("\t%s = %s(%s);", destination.name(), functionToCall,
              Joiner.on(",").join(actualLocations));
    } else {
      return String.format("\t%s(%s);", functionToCall, Joiner.on(",").join(actualLocations));
    }
  }
}
