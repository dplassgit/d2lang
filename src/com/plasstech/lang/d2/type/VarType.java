package com.plasstech.lang.d2.type;

/**
 * The type of an expression or variable.
 */
public enum VarType {
  INT, STRING, BOOLEAN, MAP, LAMBDA, OTHER, UNKNOWN;

  public boolean isUnknown() {
    return this == UNKNOWN;
  }
}
