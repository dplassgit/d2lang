package com.plasstech.lang.d2.codegen.x64;

import com.plasstech.lang.d2.type.VarType;

enum XmmRegister implements Register {
  XMM4,
  XMM5,
  XMM6,
  XMM7,
  XMM8,
  XMM9,
  XMM10,
  XMM11,
  XMM12,
  XMM13,
  XMM14,
  XMM15,
  // XMM0-3 are args (and XMM0 is the return register) so pick them last
  XMM3,
  XMM2,
  XMM1,
  XMM0;

  @Override
  public String nameByType(VarType type) {
    return name();
  }

  @Override
  public String nameBySize(int bits) {
    return name();
  }

  @Override
  public void accept(RegisterVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public VarType varType() {
    return VarType.DOUBLE;
  }
}
