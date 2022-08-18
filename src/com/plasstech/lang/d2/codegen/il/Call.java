package com.plasstech.lang.d2.codegen.il;

import java.util.Optional;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.type.ProcSymbol;

public class Call extends Op {

  private final ImmutableList<Operand> actuals;
  private final Optional<Location> destination;
  private final ImmutableList<Location> formals;
  private final ProcSymbol procSym;

  public Call(
      Optional<Location> destination,
      ProcSymbol procSym,
      ImmutableList<Operand> actuals,
      ImmutableList<Location> formals) {
    this.destination = destination;
    this.procSym = procSym;
    this.actuals = actuals;
    this.formals = formals;
  }

  public Call(
      Location destination,
      ProcSymbol procSym,
      ImmutableList<Operand> actuals,
      ImmutableList<Location> formals) {
    this(Optional.of(destination), procSym, actuals, formals);
  }

  public Call(ProcSymbol procSym, ImmutableList<Operand> actuals, ImmutableList<Location> formals) {
    this(Optional.empty(), procSym, actuals, formals);
  }

  public ProcSymbol procSym() {
    return procSym;
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
          "%s = %s(%s);", destination().get(), procSym.name(), Joiner.on(", ").join(actuals));
    } else {
      return String.format("%s(%s);", procSym.name(), Joiner.on(", ").join(actuals));
    }
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }
}
