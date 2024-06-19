package com.plasstech.lang.d2.codegen.il;

import com.plasstech.lang.d2.codegen.Location;

/**
 * Gets the destination of an opcode.
 */
final class GetDestinationVisitor extends DefaultOpcodeVisitor {
  private Location dest = null;

  public Location getDestination(Op opcode) {
    dest = null;
    opcode.accept(this);
    return dest;
  }

  @Override
  public void visit(AllocateOp op) {
    dest = op.destination();
  }

  @Override
  public void visit(ArrayAlloc op) {
    dest = op.destination();
  }

  @Override
  public void visit(Transfer op) {
    dest = op.destination();
  }

  @Override
  public void visit(BinOp op) {
    dest = op.destination();
  }

  @Override
  public void visit(UnaryOp op) {
    dest = op.destination();
  }

  @Override
  public void visit(Call op) {
    op.destination().ifPresent(destination -> {
      dest = destination;
    });
  }

  @Override
  public void visit(Dec op) {
    dest = op.target();
  }

  @Override
  public void visit(Inc op) {
    dest = op.target();
  }

  @Override
  public void visit(ArraySet op) {
    dest = op.array();
  }

  @Override
  public void visit(FieldSetOp op) {
    dest = op.recordLocation();
  }
}