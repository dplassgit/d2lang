package com.plasstech.lang.d2.codegen;

import com.plasstech.lang.d2.common.D2RuntimeException;
import com.plasstech.lang.d2.type.VarType;

enum Size {
  _1BYTE("BYTE"),
  _32BITS("DWORD"),
  _64BITS("QWORD");

  public final String asmName;

  Size(String asmName) {
    this.asmName = asmName;
  }

  @Override
  public String toString() {
    return asmName;
  }

  static Size of(VarType type) {
    // TODO: Just use type.size()!
    if (type == VarType.INT) {
      return Size._32BITS;
    } else if (type == VarType.BOOL) {
      return Size._1BYTE;
    } else if (type == VarType.STRING || type.isArray() || type.isRecord() || type.isNull()) {
      return Size._64BITS;
    }
    throw new D2RuntimeException("Cannot get type of " + type, null, "IllegalState");
  }
}
