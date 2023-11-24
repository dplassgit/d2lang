package com.plasstech.lang.d2.codegen.il;

import java.util.function.Consumer;

/**
 * An OpcodeVisitor that runs the same code for all methods.
 */
public abstract class IdentityOpcodeVisitor implements OpcodeVisitor {
  private final Consumer<Op> fn;

  protected IdentityOpcodeVisitor(Consumer<Op> fn) {
    this.fn = fn;
  }

  @Override
  public void visit(Label op) {
    fn.accept(op);
  }

  @Override
  public void visit(IfOp op) {
    fn.accept(op);
  }

  @Override
  public void visit(Transfer op) {
    fn.accept(op);
  }

  @Override
  public void visit(BinOp op) {
    fn.accept(op);
  }

  @Override
  public void visit(Return op) {
    fn.accept(op);
  }

  @Override
  public void visit(Stop op) {
    fn.accept(op);
  }

  @Override
  public void visit(SysCall op) {
    fn.accept(op);
  }

  @Override
  public void visit(UnaryOp op) {
    fn.accept(op);
  }

  @Override
  public void visit(Goto op) {
    fn.accept(op);
  }

  @Override
  public void visit(Call op) {
    fn.accept(op);
  }

  @Override
  public void visit(ProcExit op) {
    fn.accept(op);
  }

  @Override
  public void visit(ProcEntry op) {
    fn.accept(op);
  }

  @Override
  public void visit(Dec op) {
    fn.accept(op);
  }

  @Override
  public void visit(Inc op) {
    fn.accept(op);
  }

  @Override
  public void visit(AllocateOp op) {
    fn.accept(op);
  }

  @Override
  public void visit(ArrayAlloc op) {
    fn.accept(op);
  }

  @Override
  public void visit(ArraySet op) {
    fn.accept(op);
  }

  @Override
  public void visit(FieldSetOp op) {
    fn.accept(op);
  }

  @Override
  public void visit(DeallocateTemp op) {
    fn.accept(op);
  }
}
