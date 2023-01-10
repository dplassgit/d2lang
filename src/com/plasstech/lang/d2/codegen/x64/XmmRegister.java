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
  public String name8() {
    throw new UnsupportedOperationException("Cannot get name8 of xmm register");
  }

  @Override
  public String name16() {
    throw new UnsupportedOperationException("Cannot get name16 of xmm register");
  }

  @Override
  public String name32() {
    throw new UnsupportedOperationException("Cannot get name32 of xmm register");
  }

  @Override
  public String name64() {
    return name();
  }

  @Override
  public String sizeByType(VarType type) {
    return name();
  }

  @Override
  public void accept(RegisterVisitor visitor) {
    visitor.visit(this);
  }
}
