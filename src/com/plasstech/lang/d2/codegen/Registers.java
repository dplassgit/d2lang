package com.plasstech.lang.d2.codegen;

import static com.plasstech.lang.d2.codegen.IntRegister.RAX;
import static com.plasstech.lang.d2.codegen.MmxRegister.XMM0;

import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.plasstech.lang.d2.common.D2RuntimeException;
import com.plasstech.lang.d2.type.VarType;

public class Registers implements RegistersInterface {
  // these are the USED registers
  private final Set<Register> used = new HashSet<>();

  @Override
  public Register reserve(Register r) {
    used.add(r);
    return r;
  }

  @Override
  public Register allocate(VarType varType) {
    if (varType == VarType.DOUBLE) {
      // find one to return
      for (Register r : MmxRegister.values()) {
        if (!used.contains(r)) {
          used.add(r);
          return r;
        }
      }
      throw new D2RuntimeException("IllegalStateException", null, "No MMX registers left");
    }
    // find one to return
    for (Register r : IntRegister.values()) {
      if (!used.contains(r)) {
        used.add(r);
        return r;
      }
    }
    throw new D2RuntimeException("IllegalStateException", null, "No registers left");
  }

  @Override
  public boolean isAllocated(Register r) {
    return used.contains(r);
  }

  @Override
  public void deallocate(Register r) {
    if (r == null) {
      return;
    }
    Preconditions.checkState(
        used.contains(r), String.format("Register %s not allocated in register bank", r.name()));
    used.remove(r);
  }

  public static Register returnRegister(VarType type) {
    if (type == VarType.DOUBLE) {
      return XMM0;
    } else {
      return RAX;
    }
  }
}
