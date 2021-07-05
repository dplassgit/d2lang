package com.plasstech.lang.d2.codegen;

import java.util.logging.Level;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.Op;

public interface Optimizer {
  ImmutableList<Op> optimize(ImmutableList<Op> program);

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
