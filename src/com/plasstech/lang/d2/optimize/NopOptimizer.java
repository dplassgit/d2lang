package com.plasstech.lang.d2.optimize;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.Nop;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.type.SymbolTable;

public class NopOptimizer implements Optimizer {

  private boolean changed;

  @Override
  public ImmutableList<Op> optimize(ImmutableList<Op> program, SymbolTable symtab) {
    ImmutableList<Op> noNops = program
        .stream()
        .filter(op -> !(op instanceof Nop))
        .collect(ImmutableList.toImmutableList());
    changed = noNops.size() < program.size();
    return noNops;
  }

  @Override
  public boolean isChanged() {
    return changed;
  }
}
