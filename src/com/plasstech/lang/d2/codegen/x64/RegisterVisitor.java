package com.plasstech.lang.d2.codegen.x64;

interface RegisterVisitor {
  void visit(IntRegister r);

  void visit(XmmRegister r);
}
