package com.plasstech.lang.d2.optimize;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.il.Nop;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.SymbolTable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * If there are any assignments to a long temp without a subsequent read, remove the assignment. Hm,
 * maybe we can use this elsewhere?
 */
class DeadLongTempAssignmentOptimizer implements Optimizer {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private final Level loggingLevel;

  public DeadLongTempAssignmentOptimizer(int debugLevel) {
    this.loggingLevel = toLoggingLevel(debugLevel);
  }

  private boolean isChanged;

  @Override
  public ImmutableList<Op> optimize(ImmutableList<Op> unOptimized, SymbolTable symtab) {
    isChanged = false;
    List<Op> code = new ArrayList<>(unOptimized);

    Set<Operand> longTempsRead = new HashSet<>();
    // Go from end to start, because we want to find the *last* entry
    for (int ip = code.size() - 1; ip >= 0; ip--) {
      Op opcode = code.get(ip);
      List<Operand> sources = opcode.getSources();
      for (Operand operand : sources) {
        if (operand.storage() == SymbolStorage.LONG_TEMP) {
          longTempsRead.add(operand);
        }
      }
      Operand dest = opcode.getDestination();
      if (dest != null
          && dest.storage() == SymbolStorage.LONG_TEMP
          && !longTempsRead.contains(dest)) {
        // 3. We are writing to this long temp but have never read from it. Delete it.
        isChanged = true;
        // TODO: fix calls
        replaceAt(code, ip, opcode, new Nop(opcode));
      }
    }
    return ImmutableList.copyOf(code);
  }

  private void replaceAt(List<Op> code, int ip, Op opcode, Op newOp) {
    logger.at(loggingLevel).log("REPLACING ip %d: %s with %s", ip, opcode, newOp);
    code.set(ip, newOp);
  }

  @Override
  public boolean isChanged() {
    return isChanged;
  }
}
