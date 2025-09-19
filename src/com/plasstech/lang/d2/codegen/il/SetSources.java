package com.plasstech.lang.d2.codegen.il;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.Operand;

/**
 * Given an old and new "source" and an input opcode, returns a new opcode that changes the old
 * source into the new one.
 */
final class SetSources {
  private Operand newSource;
  private Operand oldSource;
  private Op newOp;

  Op setSource(Op input, Operand oldSource, Operand newSource) {
    this.newOp = input;
    this.oldSource = oldSource;
    this.newSource = newSource;
    input.accept(new Visitor());
    // TODO: maybe double check that we have the right source?
    return newOp;
  }

  private class Visitor implements OpcodeVisitor {
    @Override
    public void visit(IfOp op) {
      newOp = new IfOp(newSource, op.destination(), op.isNot(), op.position());
    }

    @Override
    public void visit(Transfer op) {
      newOp = new Transfer(op.destination(), newSource, op.position());
    }

    @Override
    public void visit(BinOp op) {
      Operand newLeft = op.left();
      if (op.left().equals(oldSource)) {
        newLeft = newSource;
      }
      Operand newRight = op.right();
      if (op.right().equals(oldSource)) {
        newRight = newSource;
      }

      newOp = new BinOp(op.destination(), newLeft, op.operator(), newRight, op.position());
    }

    @Override
    public void visit(Return op) {
      op.returnValueLocation().ifPresent(destination -> {
        newOp = new Return(op.procName(), newSource);
      });
    }

    @Override
    public void visit(SysCall op) {
      // I'm sure this can be done with a stream, but /shrug.
      List<Operand> operands = new ArrayList<>();
      for (Operand operand : op.operands()) {
        if (operand.equals(oldSource)) {
          operands.add(newSource);
        } else {
          operands.add(operand);
        }
      }
      newOp = new SysCall(op.call(), operands);
    }

    @Override
    public void visit(UnaryOp op) {
      newOp = new UnaryOp(op.destination(), op.operator(), newSource, op.position());
    }

    @Override
    public void visit(Call op) {
      // I'm sure this can be done with a stream, but /shrug.
      List<Operand> actuals = new ArrayList<>();
      for (Operand operand : op.actuals()) {
        if (operand.equals(oldSource)) {
          actuals.add(newSource);
        } else {
          actuals.add(operand);
        }
      }
      newOp = new Call(op.destination(), op.procSym(), ImmutableList.copyOf(actuals), op.formals(),
          op.position());
    }

    @Override
    public void visit(ArrayAlloc op) {
      newOp = new ArrayAlloc(op.destination(), op.arrayType(), newSource, op.position());
    }

    @Override
    public void visit(ArraySet op) {
      if (op.source().equals(oldSource)) {
        newOp = new ArraySet(op.array(), op.arrayType(), newSource, op.index(), op.isArrayLiteral(),
            op.position());
      } else {
        newOp =
            new ArraySet(op.array(), op.arrayType(), op.source(), newSource, op.isArrayLiteral(),
                op.position());
      }
    }

    @Override
    public void visit(FieldSetOp op) {
      newOp = new FieldSetOp(op.recordLocation(), op.recordSymbol(), op.field(), newSource,
          op.position());
    }

    @Override
    public void visit(Label op) {}

    @Override
    public void visit(Stop op) {}

    @Override
    public void visit(Goto op) {}

    @Override
    public void visit(ProcExit op) {}

    @Override
    public void visit(ProcEntry op) {}

    @Override
    public void visit(Dec op) {
      // TODO: should this be done?
    }

    @Override
    public void visit(Inc op) {}

    @Override
    public void visit(AllocateOp op) {}

    @Override
    public void visit(DeallocateTemp op) {}
  }
}