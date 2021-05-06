package com.plasstech.lang.d2.codegen;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;

public class Registers {
  // these are the USED registers
  private final List<Boolean> used;

  public Registers() {
    this(32);
  }

  public Registers(int size) {
    used = new ArrayList<>(size);
    for (int i = 0; i < size; ++i) {
      used.add(false);
    }
    // r0 is always considered the result of the last calculation.
    used.set(0, true);
  }

  public int allocate() {
    // find one to return
    for (int i = 0; i < used.size(); ++i) {
      if (!used.get(i)) {
        used.set(i, true);
        return i;
      }
    }
    throw new IllegalStateException("Too many registers used; used array = " + used);
  }

  public void deallocate(int id) {
    Preconditions.checkState(used.get(id),
            String.format("Register %d not allocated in register bank", id));
    used.set(id, false);
  }
}
