package com.plasstech.lang.d2.codegen.x64;

import com.plasstech.lang.d2.type.VarType;

public interface RegistersInterface {

  /** Allocate and return a register of the given type. */
  Register allocate(VarType varType);

  /** Deallocate the given register. */
  void deallocate(Register register);

  boolean isAllocated(Register r);

  Register reserve(Register r);

  /**
   * Return the least recently used register.
   */
  Register lru();

  /** Touches this register, making it the most recently "used". */
  void touch(Register r);
}
