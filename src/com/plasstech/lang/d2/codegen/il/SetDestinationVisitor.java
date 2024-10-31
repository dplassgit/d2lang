package com.plasstech.lang.d2.codegen.il;

import com.plasstech.lang.d2.codegen.Location;

/**
 * Given an old and new "destination" and an input opcode, returns a new opcode that stores into the
 * new destination instead of the old one.
 */
final class SetDestinationVisitor extends DefaultOpcodeVisitor {
  private Location newDest;
  private Op newOp;

  Op setDest(Op input, Location newDest) {
    this.newOp = input;
    this.newDest = newDest;
    input.accept(this);
    // TODO: maybe double check that we have the right dest?
    return newOp;
  }

  @Override
  public void visit(Transfer op) {
    newOp = new Transfer(newDest, op.source(), op.position());
  }

  @Override
  public void visit(BinOp op) {
    newOp = new BinOp(newDest, op.left(), op.operator(), op.right(), op.position());
  }

  @Override
  public void visit(UnaryOp op) {
    newOp = new UnaryOp(newDest, op.operator(), op.operand(), op.position());
  }

  @Override
  public void visit(Call op) {
    op.destination().ifPresent(ignored -> {
      newOp =
          new Call(newDest, op.procSym(), op.actuals(), op.formals(), op.position());
    });
  }

  @Override
  public void visit(ArrayAlloc op) {
    newOp = new ArrayAlloc(newDest, op.arrayType(), op.sizeLocation(), op.position());
  }

  @Override
  public void visit(FieldSetOp op) {
    newOp = new FieldSetOp(newDest, op.recordSymbol(), op.field(), op.source(),
        op.position());
  }

  @Override
  public void visit(AllocateOp op) {
    newOp = new AllocateOp(newDest, op.recordSymbol(), op.position());
  }

  @Override
  public void visit(ArraySet op) {
    newOp = new ArraySet(newDest, op.arrayType(), op.index(), op.source(), op.isArrayLiteral(),
        op.position());
  }
}