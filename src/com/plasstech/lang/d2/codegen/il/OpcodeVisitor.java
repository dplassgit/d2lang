package com.plasstech.lang.d2.codegen.il;

public interface OpcodeVisitor {

  void visit(Label op);

  void visit(IfOp op);

  void visit(Assignment op);

  void visit(BinOp op);

  void visit(Load op);

  void visit(Return op);

  void visit(Stop op);

  void visit(Store op);

  void visit(SysCall op);

  void visit(UnaryOp op);

  void visit(Goto op);
}

