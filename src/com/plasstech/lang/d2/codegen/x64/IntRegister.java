package com.plasstech.lang.d2.codegen.x64;

import com.plasstech.lang.d2.type.VarType;

enum IntRegister implements Register {
  RBX("RBX", "EBX", "BX", "BL"),
  // R4/RSP - not used as GP register
  // R5/RBP - not used as GP register
  RSI("RSI", "ESI", "SI", "SIL"),
  RDI("RDI", "EDI", "DI", "DIL"),
  R12("R12"),
  R13("R13"),
  R14("R14"),
  R15("R15"),
  // RBX through R15 are always saved/restored by procedures so they should be used first.
  R10("R10"),
  R11("R11"),
  R9("R9"),
  R8("R8"),
  // These are at the bottom so that they're less frequently used, since division uses EDX:EAX
  // and RCX, RDX, R8, R9 are the first 4 params to both system and regular calls.
  RDX("RDX", "EDX", "DX", "DL"),
  RCX("RCX", "ECX", "CX", "CL"),
  RAX("RAX", "EAX", "AX", "AL");

  private final String name64;
  private final String name32;
  private final String name16;
  private final String name8;

  IntRegister(String name) {
    this(name, name + "d", name + "w", name + "b");
  }

  IntRegister(String name64, String name32, String name16, String name8) {
    this.name64 = name64;
    this.name32 = name32;
    this.name16 = name16;
    this.name8 = name8;
  }

  @Override
  public String toString() {
    return name64;
  }

  // TODO: implement equals by comparing the input string to any of the names
  @Override
  public String sizeByType(VarType type) {
    if (type == VarType.INT) {
      return name32;
    } else if (type == VarType.BYTE || type == VarType.BOOL) {
      return name8;
    }
    return name64;
  }

  @Override
  public String name8() {
    return name8;
  }

  @Override
  public String name16() {
    return name16;
  }

  @Override
  public String name32() {
    return name32;
  }

  @Override
  public String name64() {
    return name64;
  }

  @Override
  public void accept(RegisterVisitor visitor) {
    visitor.visit(this);
  }
}
