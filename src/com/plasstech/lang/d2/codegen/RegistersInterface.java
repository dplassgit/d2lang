package com.plasstech.lang.d2.codegen;

interface RegistersInterface {

  /** Allocate and return a register. */
  Register allocate();

  /** Deallocate the given register. */
  void deallocate(Register register);

  boolean isAllocated(Register r);

  Register reserve(Register r);
}
