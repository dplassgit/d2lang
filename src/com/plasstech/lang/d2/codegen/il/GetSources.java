package com.plasstech.lang.d2.codegen.il;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.Operand;
import java.util.ArrayList;
import java.util.List;

/** Gets all sources of an opcode. */
final class GetSources {
  private final List<Operand> sources = new ArrayList<>();

  ImmutableList<Operand> execute(Op opcode) {
    sources.clear();
    opcode.accept(new Visitor());
    return ImmutableList.copyOf(sources);
  }

  private class Visitor implements OpcodeVisitor {
    @Override
    public void visit(IfOp op) {
      sources.add(op.condition());
    }

    @Override
    public void visit(Transfer op) {
      sources.add(op.source());
    }

    @Override
    public void visit(BinOp op) {
      sources.add(op.left());
      sources.add(op.right());
    }

    @Override
    public void visit(Return op) {
      op.returnValueLocation()
          .ifPresent(
              source -> {
                sources.add(source);
              });
    }

    @Override
    public void visit(SysCall op) {
      sources.addAll(op.operands());
    }

    @Override
    public void visit(UnaryOp op) {
      sources.add(op.operand());
    }

    @Override
    public void visit(Call op) {
      sources.addAll(op.actuals());
    }

    @Override
    public void visit(ArrayAlloc op) {
      sources.add(op.sizeLocation());
    }

    @Override
    public void visit(ArraySet op) {
      sources.add(op.index());
      sources.add(op.source());
      // technically we're reading the array whose index we're setting
      sources.add(op.getDestination());
    }

    @Override
    public void visit(FieldSetOp op) {
      sources.add(op.source());
      // Technically we're reading the record whose field we're setting
      sources.add(op.recordLocation());
    }

    @Override
    public void visit(Dec op) {
      sources.add(op.target());
    }

    @Override
    public void visit(Inc op) {
      sources.add(op.target());
    }

    @Override
    public void visit(DeallocateTemp op) {
      sources.add(op.temp());
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
    public void visit(AllocateOp op) {}
  }
}
