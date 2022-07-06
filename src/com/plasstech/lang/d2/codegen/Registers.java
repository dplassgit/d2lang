package com.plasstech.lang.d2.codegen;

import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.plasstech.lang.d2.common.D2RuntimeException;

public class Registers {
  private static final int MAX_REGISTERS = Register.values().length;

  // these are the USED registers
  private final Set<Register> used = new HashSet<>();

  public Register reserve(Register r) {
    used.add(r);
    return r;
  }

  public Register allocate() {
    // find one to return
    for (Register r : Register.values()) {
      if (!used.contains(r)) {
        used.add(r);
        return r;
      }
    }
    throw new D2RuntimeException("IllegalStateException", null, "No registers left");
  }

  public boolean isAllocated(Register r) {
    return used.contains(r);
  }

  public int numLeft() {
    return MAX_REGISTERS - used.size();
  }

  public void deallocate(Register r) {
    if (r == null) {
      return;
    }
    Preconditions.checkState(
        used.contains(r), String.format("Register %s not allocated in register bank", r.name()));
    used.remove(r);
  }
}
