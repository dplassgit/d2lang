package com.plasstech.lang.d2.codegen;

interface RegisterVisitor {
  void visit(IntRegister r);

  void visit(XmmRegister r);
}
