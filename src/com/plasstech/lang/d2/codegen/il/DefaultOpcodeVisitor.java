package com.plasstech.lang.d2.codegen.il;

/**
 * An implementation of the OpcodeVisitor interface that does nothing.
 */
public abstract class DefaultOpcodeVisitor extends IdentityOpcodeVisitor {

  protected DefaultOpcodeVisitor() {
    super((op) -> {});
  }
}
