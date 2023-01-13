package com.plasstech.lang.d2.codegen.x64;

import com.plasstech.lang.d2.type.VarType;

interface RegistersInterface {

  /** Allocate and return a register of the given type. */
  Register allocate(VarType varType);

  /** Deallocate the given register. */
  void deallocate(Register register);

  boolean isAllocated(Register r);

  void reserve(Register r);
}
