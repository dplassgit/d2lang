package com.plasstech.lang.d2.codegen.t100;

import com.google.auto.value.AutoValue;
import com.plasstech.lang.d2.type.VarType;

@AutoValue
public abstract class PseudoReg {
  public abstract String name();

  public abstract VarType type();

  public static PseudoReg create(String name, VarType type) {
    return new AutoValue_PseudoReg(name, type);
  }
}
