package com.plasstech.lang.d2.optimize.testing;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.optimize.DefaultOptimizer;
import com.plasstech.lang.d2.optimize.NopOptimizer;
import com.plasstech.lang.d2.optimize.Optimizer;
import com.plasstech.lang.d2.type.SymbolTable;

// Runs the backing optimizer once, then the NopOptimizer. (In contrast to ILOptimizer, which
// runs all given optimizers multiple times.)
public class OptimizerWithNop extends DefaultOptimizer {
  private final Optimizer backing;

  public OptimizerWithNop(Optimizer backing) {
    this.backing = backing;
  }

  @Override
  public ImmutableList<Op> optimize(ImmutableList<Op> program, SymbolTable symtab) {
    ImmutableList<Op> optimized = backing.optimize(program, symtab);
    setChanged(backing.isChanged());
    return new NopOptimizer().optimize(optimized, null);
  }
}
