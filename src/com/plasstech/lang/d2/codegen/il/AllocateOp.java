package com.plasstech.lang.d2.codegen.il;

import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.RecordSymbol;

/** Represents "new record" */
public class AllocateOp extends Op {
  private final RecordSymbol record;
  private final Location destination;

  public AllocateOp(Location destination, RecordSymbol record, Position position) {
    super(position);
    this.destination = destination;
    this.record = record;
  }

  public Location destination() {
    return destination;
  }

  public RecordSymbol recordSymbol() {
    return record;
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return String.format("%s = NEW %s", destination, record.name());
  }
}
