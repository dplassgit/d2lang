package com.plasstech.lang.d2.optimize;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.il.Call;
import com.plasstech.lang.d2.codegen.il.ProcEntry;
import com.plasstech.lang.d2.codegen.il.ProcExit;

public class DeadProcOptimizer extends LineOptimizer {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private Set<String> calledProcs = new HashSet<>();
  private Map<String, Integer> procEntries = new HashMap<>();
  private Map<String, Integer> procExits = new HashMap<>();

  DeadProcOptimizer(int debugLevel) {
    super(debugLevel);
  }

  @Override
  protected void preProcess() {
    calledProcs.clear();
    procEntries.clear();
    procExits.clear();
  }

  @Override
  protected void postProcess() {
    // If any proc is not called, remove it from entry to exit.
    Set<String> uncalled = procEntries.keySet();
    uncalled.removeAll(calledProcs);
    // Now, uncalled contains all the proc names who weren't in the "called" set.
    for (String procName : uncalled) {
      setChanged(true);
      logger.at(loggingLevel).log("Deleting uncalled proc %s", procName);
      int entry = procEntries.get(procName);
      int exit = procExits.get(procName);
      for (int i = entry; i <= exit; ++i) {
        deleteAt(i);
      }
    }
  }

  @Override
  public void visit(Call op) {
    calledProcs.add(op.procSym().name());
  }

  @Override
  public void visit(ProcEntry op) {
    procEntries.put(op.name(), ip());
  }

  @Override
  public void visit(ProcExit op) {
    procExits.put(op.procName(), ip());
  }
}
