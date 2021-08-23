package com.plasstech.lang.d2.codegen;

import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Preconditions;

public class Registers {
  public static final Register RAX = Register.R0;
  public static final Register RCX = Register.R1;
  public static final Register RDX = Register.R2;
  public static final Register RBX = Register.R3;
  private static final int MAX_REGISTERS = Register.values().length;

  // these are the USED registers
  private final Set<Register> used = new HashSet<>();

  public void reserve(Register r) {
    used.add(r);
  }

  public Register allocate() {
    // find one to return
    for (Register r : Register.values()) {
      if (!used.contains(r)) {
        used.add(r);
        return r;
      }
    }
    return null;
  }

  public boolean isAllocated(Register r) {
    return used.contains(r);
  }

  public int numLeft() {
    return MAX_REGISTERS - used.size();
  }

  public void deallocate(Register r) {
    Preconditions.checkState(
        used.contains(r), String.format("Register %s not allocated in register bank", r.name()));
    used.remove(r);
  }
}
