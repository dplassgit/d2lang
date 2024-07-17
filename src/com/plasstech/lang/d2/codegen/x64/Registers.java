package com.plasstech.lang.d2.codegen.x64;

import static com.plasstech.lang.d2.codegen.x64.IntRegister.RAX;
import static com.plasstech.lang.d2.codegen.x64.XmmRegister.XMM0;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.plasstech.lang.d2.common.D2RuntimeException;
import com.plasstech.lang.d2.type.VarType;

class Registers implements RegistersInterface {
  // these are the USED registers
  private final Set<Register> used = new HashSet<>();
  // These are the registers in order of allocation.
  private final List<Register> registersAllocated = new ArrayList<>();

  @Override
  public Register reserve(Register r) {
    // do NOT add to registersAllocated
    used.add(r);
    return r;
  }

  /** Return the least recently used register compatible with the requested type. */
  @Override
  public Register lru(VarType varType) {
    boolean isDouble = varType == VarType.DOUBLE;
    for (Register r : registersAllocated) {
      if (isDouble == (r.varType() == VarType.DOUBLE)) {
        return r;
      }
    }
    throw new IllegalStateException("No lru registers of type " + varType);
  }

  /** Effectively moves to the top of the LRU list. */
  @Override
  public void touch(Register r) {
    registersAllocated.remove(r);
    registersAllocated.add(r);
  }

  @Override
  public Register allocate(VarType varType) {
    if (varType == VarType.DOUBLE) {
      // find one to return
      for (Register r : XmmRegister.values()) {
        if (!used.contains(r)) {
          if (r == XMM0) {
            continue;
          }
          used.add(r);
          registersAllocated.add(r);
          return r;
        }
      }
      return null;
    }
    // find one to return
    for (Register r : IntRegister.values()) {
      if (!used.contains(r)) {
        if (r == RAX) {
          continue;
        }
        used.add(r);
        registersAllocated.add(r);
        return r;
      }
    }
    return null;
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
    if (!used.contains(r)) {
      throw new D2RuntimeException(
          String.format("Register %s not allocated in register bank", r.name()), null, "CodeGen");
    }
    used.remove(r);
    registersAllocated.remove(r);
  }

  public static Register returnRegister(VarType type) {
    if (type == VarType.DOUBLE) {
      return XMM0;
    } else {
      // note, it may be actually EAX or AL
      return RAX;
    }
  }
}
