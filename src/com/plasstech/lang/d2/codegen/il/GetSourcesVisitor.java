package com.plasstech.lang.d2.codegen.il;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.Operand;

/**
 * Gets all sources of an opcode.
 */
final class GetSourcesVisitor extends DefaultOpcodeVisitor {
  private final List<Operand> sources = new ArrayList<>();

  ImmutableList<Operand> getSources(Op opcode) {
    sources.clear();
    opcode.accept(this);
    return ImmutableList.copyOf(sources);
  }

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
    op.returnValueLocation().ifPresent(source -> {
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
  }

  @Override
  public void visit(FieldSetOp op) {
    sources.add(op.source());
  }

  @Override
  public void visit(Dec op) {
    sources.add(op.target());
  }

  @Override
  public void visit(Inc op) {
    sources.add(op.target());
  }
}
