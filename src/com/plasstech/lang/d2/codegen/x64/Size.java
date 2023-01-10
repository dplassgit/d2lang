package com.plasstech.lang.d2.codegen.x64;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.plasstech.lang.d2.common.D2RuntimeException;
import com.plasstech.lang.d2.type.VarType;

/**
 * Represents the size of a datatype. See https://www.nasm.us/doc/nasmdoc3.html#section-3.2.1 for
 * reference.
 */
enum Size {
  _1BYTE("BYTE", "db"),
  _16BYTE("WORD", "dw"),
  _32BITS("DWORD", "dd"),
  _64BITS("QWORD", "dq");

  final String asmType;
  final String dataSizeName;

  Size(String asmName, String dataSizeName) {
    this.asmType = asmName;
    this.dataSizeName = dataSizeName;
  }

  @Override
  public String toString() {
    return asmType;
  }

  private static Map<Integer, Size> BYTES_TO_SIZE =
      ImmutableMap.of(1, _1BYTE, 2, _16BYTE, 4, _32BITS, 8, _64BITS);

  static Size of(VarType type) {
    int bytes = type.size();
    Size size = BYTES_TO_SIZE.get(bytes);
    if (size == null) {
      throw new D2RuntimeException("Cannot get type of " + type, null, "IllegalState");
    }
    return size;
  }
}
