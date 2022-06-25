package com.plasstech.lang.d2.optimize;

import java.util.logging.Level;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.type.SymTab;

public interface Optimizer {
  ImmutableList<Op> optimize(ImmutableList<Op> program, SymTab symtab);

  boolean isChanged();

  default Level toLoggingLevel(int debugLevel) {
    switch (debugLevel) {
      case 1:
        return Level.CONFIG;
      case 2:
        return Level.INFO;
      default:
      case 0:
        return Level.FINE;
    }
  }
}
