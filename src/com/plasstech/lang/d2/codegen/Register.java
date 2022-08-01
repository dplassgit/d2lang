package com.plasstech.lang.d2.codegen;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.type.VarType;

public interface Register {

  String name8();

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
          MmxRegister.XMM0,
          MmxRegister.XMM1,
          MmxRegister.XMM2,
          MmxRegister.XMM3,
          MmxRegister.XMM4,
          MmxRegister.XMM5);

  public static final ImmutableList<Register> NONVOLATILE_REGISTERS =
      ImmutableList.of(
          IntRegister.RBX,
          IntRegister.R12,
          IntRegister.R13,
          IntRegister.R14,
          IntRegister.R15,
          IntRegister.RDI,
          IntRegister.RSI,
          MmxRegister.XMM6,
          MmxRegister.XMM7,
          MmxRegister.XMM8,
          MmxRegister.XMM9,
          MmxRegister.XMM10,
          MmxRegister.XMM11,
          MmxRegister.XMM12,
          MmxRegister.XMM13,
          MmxRegister.XMM14,
          MmxRegister.XMM15);

  public static final ImmutableList<Register> INT_PARAM_REGISTERS =
      ImmutableList.of(IntRegister.RCX, IntRegister.RDX, IntRegister.R8, IntRegister.R9);

  public static final ImmutableList<Register> MMX_PARAM_REGISTERS =
      ImmutableList.of(MmxRegister.XMM0, MmxRegister.XMM1, MmxRegister.XMM2, MmxRegister.XMM3);

  static Register paramRegister(VarType type, int index) {
    if (index > 3) {
      return null;
    }
    if (type == VarType.DOUBLE) {
      return MMX_PARAM_REGISTERS.get(index);
    }
    return INT_PARAM_REGISTERS.get(index);
  }
}
