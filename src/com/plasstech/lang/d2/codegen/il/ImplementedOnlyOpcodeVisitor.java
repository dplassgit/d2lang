package com.plasstech.lang.d2.codegen.il;

/**
 * If any method is called but is NOT overridden in a subclass, the method will throw an exception.
 */
public abstract class ImplementedOnlyOpcodeVisitor extends IdentityOpcodeVisitor {
  protected ImplementedOnlyOpcodeVisitor() {
    super((op) -> {
      throw new UnsupportedOperationException("Cannot execute " + op);
    });
  }
}
