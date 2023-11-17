package com.plasstech.lang.d2.type;

public enum SymbolStorage {
  GLOBAL, // known memory location
  LOCAL, // usually stack
  PARAM, // also usually stack
  REGISTER, // ?
  TEMP, // temporary, may be a register or stack
  IMMEDIATE; // not really a storage location, but it is in a way.
}
