package com.plasstech.lang.d2.codegen;

public enum Register {
  RBX("RBX", "EBX", "BX", "BL"),
  // R4/RSP - not used as GP register
  // R5/RBP - not used as GP register
  RSI("RSI", "ESI", "SI", "SIL"),
  RDI("RDI", "EDI", "DI", "DIL"),
  R8("R8"),
  R9("R9"),
  R10("R10"),
  R11("R11"),
  R12("R12"),
  R13("R13"),
  R14("R14"),
  R15("R15"),
  // These are at the bottom so that they're less frequently used, since division uses EDX:EAX
  // and system calls use RCX and RDX.
  RCX("RCX", "ECX", "CX", "CL"),
  RDX("RDX", "EDX", "DX", "DL"),
  RAX("RAX", "EAX", "AX", "AL");

  public final String name64;
  public final String name32;
  public final String name16;
  public final String name8;

  Register(String name) {
    this(name, name + "d", name + "w", name + "b");
  }

  Register(String name64, String name32, String name16, String name8) {
    this.name64 = name64;
    this.name32 = name32;
    this.name16 = name16;
    this.name8 = name8;}
}
