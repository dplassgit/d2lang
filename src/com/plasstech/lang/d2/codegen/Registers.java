package com.plasstech.lang.d2.codegen;

import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

public class Registers {
  private static final int MAX_REGISTERS = Register.values().length;

  private static final Set<Register> VOLATILE_REGISTERS =
      ImmutableSet.of(
          Register.RAX,
          Register.RCX,
          Register.RDX,
          Register.R8,
          Register.R9,
          Register.R10,
          Register.R11);

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

  public static boolean isVolatile(Register r) {
    return VOLATILE_REGISTERS.contains(r);
  }
}
