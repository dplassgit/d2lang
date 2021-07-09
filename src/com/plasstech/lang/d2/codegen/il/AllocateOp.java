package com.plasstech.lang.d2.codegen.il;

import com.plasstech.lang.d2.codegen.MemoryAddress;
import com.plasstech.lang.d2.type.RecordSymbol;

public class AllocateOp extends Op {
  private final RecordSymbol record;
  private final MemoryAddress destination;

  public AllocateOp(MemoryAddress destination, RecordSymbol record) {
    this.destination = destination;
    this.record = record;
  }

  public MemoryAddress destination() {
    return destination;
  }

  public RecordSymbol record() {
    return record;
  }

  @Override
  public String toString() {
    return String.format("%s = NEW RECORD %s;", destination, record.name());
  }
}
