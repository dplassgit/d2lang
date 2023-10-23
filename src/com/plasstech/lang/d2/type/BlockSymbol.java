package com.plasstech.lang.d2.type;

import com.plasstech.lang.d2.parse.node.BlockNode;

public class BlockSymbol extends AbstractSymbol {

  private final SymbolTable locals;

  public BlockSymbol(BlockNode node, SymbolTable locals) {
    super(node.name());
    this.locals = locals;
  }

  @Override
  public String toString() {
    return String.format("BlockSym %s: %s", locals.storage().name(), locals);
  }

  @Override
  public SymbolStorage storage() {
    return locals.storage();
  }

  public SymbolTable symTab() {
    return locals;
  }
}
