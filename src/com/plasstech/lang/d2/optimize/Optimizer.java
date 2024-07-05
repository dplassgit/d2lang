package com.plasstech.lang.d2.optimize;

import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.type.SymbolTable;

public interface Optimizer {
  ImmutableList<Op> optimize(ImmutableList<Op> program, SymbolTable symtab);

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

  static List<Op> removeMatchingOps(List<Op> program,
      Class<? extends Op> clazz) {
    return program
        .stream()
        .filter(op -> !op.getClass().equals(clazz))
        .collect(Collectors.toList());
  }
}
