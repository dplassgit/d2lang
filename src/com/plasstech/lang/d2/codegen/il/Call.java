package com.plasstech.lang.d2.codegen.il;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

public class Call extends Op {

  private final String functionToCall;
  private final ImmutableList<Location> actualLocations;

  public Call(String functionToCall, ImmutableList<Location> actuals) {
    this.functionToCall = functionToCall;
    this.actualLocations = actuals;
  }

  public String functionToCall() {
    return functionToCall;
  }

  public ImmutableList<Location> actualLocations() {
    return actualLocations;
  }

  @Override
  public String toString() {
    return String.format("\t%s(%s);", functionToCall, Joiner.on(",").join(actualLocations));
  }
}
