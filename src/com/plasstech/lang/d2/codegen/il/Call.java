package com.plasstech.lang.d2.codegen.il;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.ProcSymbol;
import java.util.Optional;

/** Represents a procedure call - either local or extern */
public class Call extends Op {

  private final ImmutableList<Operand> actuals;
  private final Optional<Location> destination;
  private final ImmutableList<Location> formals;
  private final ProcSymbol procSym;

  public Call(
      Location destination,
      ProcSymbol procSym,
      ImmutableList<Operand> actuals,
      ImmutableList<Location> formals,
      Position position) {
    this(Optional.of(destination), procSym, actuals, formals, position);
  }

  public Call(
      Optional<Location> destination,
      ProcSymbol procSym,
      ImmutableList<Operand> actuals,
      ImmutableList<Location> formals,
      Position position) {
    super(position);
    this.destination = destination;
    this.procSym = procSym;
    this.actuals = actuals;
    this.formals = formals;
  }

  public Call(
      ProcSymbol procSym,
      ImmutableList<Operand> actuals,
      ImmutableList<Location> formals,
      Position position) {
    this(Optional.empty(), procSym, actuals, formals, position);
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
          "%s = %s(%s)", destination().get(), procSym.name(), Joiner.on(", ").join(actuals));
    } else {
      return String.format("%s(%s)", procSym.name(), Joiner.on(", ").join(actuals));
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Call)) {
      return false;
    }
    return this.hashCode() == obj.hashCode();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(procSym().name(), actuals, formals);
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }
}
