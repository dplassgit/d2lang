package com.plasstech.lang.d2.codegen;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.type.VarType;

public interface Register {

  String name8();

  String name32();

  String name64();

  String name();

  String sizeByType(VarType type);

  // TODO: add MMX registers here
  public static final ImmutableList<Register> VOLATILE_REGISTERS =
      ImmutableList.of(
          IntRegister.RCX,
          IntRegister.RDX,
          IntRegister.R8,
          IntRegister.R9,
          IntRegister.R10,
          IntRegister.R11);

  public static final ImmutableList<Register> INT_PARAM_REGISTERS =
      ImmutableList.of(IntRegister.RCX, IntRegister.RDX, IntRegister.R8, IntRegister.R9);

  static Register paramRegister(int index) {
    return paramRegister(VarType.INT, index);
  }

  static Register paramRegister(VarType type, int index) {
    if (type == VarType.DOUBLE) {
      return null;
    }
    if (index > 3) {
      return null;
    }
    return INT_PARAM_REGISTERS.get(index);
  }
}
