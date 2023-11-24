package com.plasstech.lang.d2.type;

public enum SymbolStorage {
  GLOBAL, // known memory location
  LOCAL, // usually stack
  PARAM, // also usually stack
  REGISTER, // ?
  TEMP, // temporary, usually a register. It is deallocated when it's read.
  IMMEDIATE, // not really a storage location, but it is in a way.
  LONG_TEMP; // a temp that is long-lived. It is not auto-deallocated when it's read.
}
