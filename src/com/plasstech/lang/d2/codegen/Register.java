package com.plasstech.lang.d2.codegen;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.type.VarType;

public interface Register {

  String name8();

  String name16();

  String name32();

  String name64();

  String name();

  String sizeByType(VarType type);

  void accept(RegisterVisitor visitor);

  public static final ImmutableList<Register> VOLATILE_REGISTERS =
      ImmutableList.of(
          IntRegister.RCX,
          IntRegister.RDX,
          IntRegister.R8,
          IntRegister.R9,
          IntRegister.R10,
          IntRegister.R11,
          XmmRegister.XMM0,
          XmmRegister.XMM1,
          XmmRegister.XMM2,
          XmmRegister.XMM3,
          XmmRegister.XMM4,
          XmmRegister.XMM5);

  public static final ImmutableList<Register> NONVOLATILE_REGISTERS =
      ImmutableList.of(
          XmmRegister.XMM6,
          XmmRegister.XMM7,
          XmmRegister.XMM8,
          XmmRegister.XMM9,
          XmmRegister.XMM10,
          XmmRegister.XMM11,
          XmmRegister.XMM12,
          XmmRegister.XMM13,
          XmmRegister.XMM14,
          XmmRegister.XMM15,
          IntRegister.RBX,
          IntRegister.R12,
          IntRegister.R13,
          IntRegister.R14,
          IntRegister.R15,
          IntRegister.RDI,
          IntRegister.RSI);

  public static final ImmutableList<Register> INT_PARAM_REGISTERS =
      ImmutableList.of(IntRegister.RCX, IntRegister.RDX, IntRegister.R8, IntRegister.R9);

  public static final ImmutableList<Register> XMM_PARAM_REGISTERS =
      ImmutableList.of(XmmRegister.XMM0, XmmRegister.XMM1, XmmRegister.XMM2, XmmRegister.XMM3);

  static Register paramRegister(VarType type, int index) {
    if (index > 3) {
      return null;
    }
    if (type == VarType.DOUBLE) {
      return XMM_PARAM_REGISTERS.get(index);
    }
    return INT_PARAM_REGISTERS.get(index);
  }
}
