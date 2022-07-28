package com.plasstech.lang.d2.codegen;

import com.plasstech.lang.d2.type.VarType;

interface RegistersInterface {

  /** Allocate and return a register of the given type. */
  Register allocate(VarType varType);

  /** Deallocate the given register. */
  void deallocate(Register register);

  boolean isAllocated(Register r);

  Register reserve(Register r);
}
