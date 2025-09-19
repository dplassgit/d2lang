package com.plasstech.lang.d2.codegen.il;

import com.plasstech.lang.d2.codegen.Location;

/**
 * Given an old and new "destination" and an input opcode, returns a new opcode that stores into the
 * new destination instead of the old one.
 */
final class SetDestination {
  private Location newDest;
  private Op newOp;

  Op setDestination(Op input, Location newDest) {
    this.newOp = input;
    this.newDest = newDest;
    input.accept(new Visitor());
    // TODO: maybe double check that we have the right dest?
    return newOp;
  }

  private class Visitor implements OpcodeVisitor {
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

    @Override
    public void visit(Label op) {}

    @Override
    public void visit(IfOp op) {}

    @Override
    public void visit(Return op) {}

    @Override
    public void visit(Stop op) {}

    @Override
    public void visit(SysCall op) {}

    @Override
    public void visit(Goto op) {}

    @Override
    public void visit(ProcExit op) {}

    @Override
    public void visit(ProcEntry op) {}

    @Override
    public void visit(Dec op) {}

    @Override
    public void visit(Inc op) {}

    @Override
    public void visit(DeallocateTemp op) {}
  }
}