package com.plasstech.lang.d2.codegen.il;

import java.util.Optional;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;

public class Call extends Op {

  private final String procName;
  private final ImmutableList<Operand> actuals;
  private final Optional<Location> destination;
  private final ImmutableList<Location> formals;

  public Call(
      Optional<Location> destination,
      String procName,
      ImmutableList<Operand> actuals,
      ImmutableList<Location> formals) {
    this.destination = destination;
    this.procName = procName;
    this.actuals = actuals;
    this.formals = formals;
  }

  public Call(
      Location destination,
      String procName,
      ImmutableList<Operand> actuals,
      ImmutableList<Location> formals) {
    this(Optional.of(destination), procName, actuals, formals);
  }

  public Call(
      String functionToCall, ImmutableList<Operand> actuals, ImmutableList<Location> formals) {
    this(Optional.empty(), functionToCall, actuals, formals);
  }

  public String procName() {
    return procName;
  }

  public ImmutableList<Location> formals() {
    return formals;
  }

  public ImmutableList<Operand> actuals() {
    return actuals;
  }

  /** Optional location where the return value might be put */
  public Optional<Location> destination() {
    return destination;
  }

  @Override
  public String toString() {
    if (destination().isPresent()) {
      return String.format(
          "%s = %s(%s);", destination().get(), procName, Joiner.on(", ").join(actuals));
    } else {
      return String.format("%s(%s);", procName, Joiner.on(", ").join(actuals));
    }
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }
}
