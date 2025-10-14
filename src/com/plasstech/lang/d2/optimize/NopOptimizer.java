package com.plasstech.lang.d2.optimize;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.Nop;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.type.SymbolTable;

/**
 * Removes Nop operations from the IL program.
 */
public final class NopOptimizer extends DefaultOptimizer {
  @Override
  public ImmutableList<Op> optimize(ImmutableList<Op> program, SymbolTable symtab) {
    List<Op> noNops = Op.removeMatchingOps(program, Nop.class);
    setChanged(noNops.size() < program.size());
    return ImmutableList.copyOf(noNops);
  }
}
