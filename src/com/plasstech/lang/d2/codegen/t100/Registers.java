package com.plasstech.lang.d2.codegen.t100;

import java.util.HashSet;
import java.util.Set;

import com.plasstech.lang.d2.common.D2RuntimeException;
import com.plasstech.lang.d2.type.VarType;

class Registers {
  // these are the USED registers
  private final Set<Register> used = new HashSet<>();

  /** Allocate and return a register of the given type. */
  Register allocate(VarType varType) {
    return null;
  }

  /** Deallocate the given register. */
  void deallocate(Register register) {
    used.remove(register);
    used.remove(register.left());
    used.remove(register.right());
  }

  /** Returns true if the register (or part of the register pair) is allocated. */
  boolean isAllocated(Register r) {
    return used.contains(r) || used.contains(r.left()) || used.contains(r.right());
  }

  void reserve(Register r) {
    if (isAllocated(r)) {
      throw new D2RuntimeException("Register already reserved", null, null);
    }
    used.add(r);
  }
}
