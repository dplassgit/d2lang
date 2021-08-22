package com.plasstech.lang.d2.codegen;

public enum Register {
  R0, // RAX,
  R1, // RCX, NOTE NOT RBX
  R2, // RDX, NOTE NOT RCX
  R3, // RBX, NOTE NOT RDX
  // R4/RSP - not used as GP register
  // R5/RBP - not used as GP register
  R6, // RSI
  R7, // RDI
  R8,
  R9,
  R10,
  R11,
  R12,
  R13,
  R14,
  R15
}
