package com.plasstech.lang.d2.codegen.il;

public class DefaultOpcodeVisitor implements OpcodeVisitor {

  @Override
  public void visit(Label op) {}

  @Override
  public void visit(IfOp op) {}

  @Override
  public void visit(BinOp op) {}

  @Override
  public void visit(Return op) {}

  @Override
  public void visit(Stop op) {}

  @Override
  public void visit(SysCall op) {}

  @Override
  public void visit(UnaryOp op) {}

  @Override
  public void visit(Goto op) {}

  @Override
  public void visit(Transfer op) {}

  @Override
  public void visit(Call op) {}

  @Override
  public void visit(ProcExit op) {}

  @Override
  public void visit(ProcEntry op) {}

  @Override
  public void visit(Dec op) {}

  @Override
  public void visit(Inc op) {}

  @Override
  public void visit(AllocateOp op) {}

  @Override
  public void visit(ArrayAlloc op) {}
}
