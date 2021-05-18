package com.plasstech.lang.d2.type;

/**
 * The type of an expression or variable.
 */
public enum VarType {
  INT, STRING, BOOL, MAP, LAMBDA, ARRAY, RECORD, PROC, VOID, UNKNOWN;

  public boolean isUnknown() {
    return this == UNKNOWN;
  }
}
