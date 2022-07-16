package com.plasstech.lang.d2.codegen.il;

import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.RecordSymbol;

/** Sets the value of a field, e.g., record.field = source */
public class FieldSetOp extends Op {
  private final Location record;
  private final String field;
  private final Operand source;
  private final RecordSymbol recordSymbol;

  public FieldSetOp(
      Location record, RecordSymbol recordSymbol, String field, Operand source, Position position) {
    super(position);
    this.record = record;
    this.recordSymbol = recordSymbol;
    this.field = field;
    this.source = source;
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }

  public Operand source() {
    return source;
  }

  @Override
  public String toString() {
    return String.format("%s.%s = %s; // type: %s", record, field, source, recordSymbol);
  }

  public Location recordLocation() {
    return record;
  }

  public String field() {
    return field;
  }

  public RecordSymbol recordSymbol() {
    return recordSymbol;
  }
}
