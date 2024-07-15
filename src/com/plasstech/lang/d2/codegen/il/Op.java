package com.plasstech.lang.d2.codegen.il;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.common.Position;

// An opcode.
public abstract class Op {

  private final Position position;

  Op() {
    this(null);
  }

  Op(Position position) {
    this.position = position;
  }

  protected static final Escaper ESCAPER =
      Escapers.builder()
          .addEscape('\n', "\\n")
          .addEscape('\r', "\\r")
          .addEscape('\t', "\\t")
          .addEscape('\"', "\\\"")
          .build();

  public void accept(OpcodeVisitor visitor) {
    // does nothing by default.
  }

  public Position position() {
    return position;
  }

  /** Get the destination of this opcode. It may return null */
  public Location getDestination() {
    return new GetDestinationVisitor().getDestination(this);
  }

  /** Get the sources of this opcode. It may return an empty list. */
  public ImmutableList<Operand> getSources() {
    return new GetSourcesVisitor().getSources(this);
  }

  /**
   * Create a new Op object based on `this` with the new destination. If there is no "destination"
   * for the Op (e.g., ProcEntry), it returns `this`.
   */
  public Op setDestination(Location newDestination) {
    return new SetDestinationVisitor().setDest(this, newDestination);
  }

  /**
   * Create a new Op object based on `this` with the given old source replaced with the given new
   * source. If there is more than one, all are updated. If there is no "source" for the Op (e.g.,
   * ProcEntry), it returns `this`.
   */
  public Op setSource(Operand oldSource, Operand newSource) {
    return new SetSourcesVisitor().setSource(this, oldSource, newSource);
  }

  public static List<Op> removeMatchingOps(List<Op> program,
      Class<? extends Op> clazz) {
    return program
        .stream()
        .filter(op -> !op.getClass().equals(clazz))
        .collect(Collectors.toList());
  }
}
