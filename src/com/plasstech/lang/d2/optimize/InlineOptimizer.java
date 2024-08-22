package com.plasstech.lang.d2.optimize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.il.Call;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.Goto;
import com.plasstech.lang.d2.codegen.il.IfOp;
import com.plasstech.lang.d2.codegen.il.Label;
import com.plasstech.lang.d2.codegen.il.Nop;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.OpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.ProcEntry;
import com.plasstech.lang.d2.codegen.il.ProcExit;
import com.plasstech.lang.d2.codegen.il.Return;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.type.SymbolTable;

/**
 * Replaces calls to small functions (up to 10 opcodes with no loops) with the equivalent code
 * in-line.
 */
class InlineOptimizer extends DefaultOpcodeVisitor implements Optimizer {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final Level loggingLevel;
  private boolean changed;
  private List<Op> code;
  // Maps from proc name to its code (before remapping)
  private Map<String, List<Op>> inlineableCode = new HashMap<>();
  // The proc entry for each proc; needed for formal remapping.
  private Map<String, ProcEntry> procsByName = new HashMap<>();
  // How many times each proc is called
  private Map<String, Integer> usageCounts = new HashMap<>();

  private int ip;
  private SymbolTable symbolTable;

  InlineOptimizer(int debugLevel) {
    loggingLevel = toLoggingLevel(debugLevel);
  }

  @Override
  public ImmutableList<Op> optimize(ImmutableList<Op> input, SymbolTable symbolTable) {
    inlineableCode.clear();
    procsByName.clear();
    usageCounts.clear();

    // Remove nops because reasons.
    input = new NopOptimizer().optimize(input, symbolTable);
    this.symbolTable = symbolTable;
    code = new ArrayList<>(input);

    OpcodeVisitor visitor = new ProcFinder();
    for (ip = 0; ip < input.size(); ++ip) {
      input.get(ip).accept(visitor);
    }
    visitor = new CallFinder();
    for (ip = 0; ip < input.size(); ++ip) {
      input.get(ip).accept(visitor);
    }

    for (ip = 0; ip < input.size(); ++ip) {
      changed = false;
      input.get(ip).accept(this);
      if (changed) {
        // We changed. Things can get out of sync, so stop.
        break;
      }
    }

    return ImmutableList.copyOf(code);
  }

  @Override
  public boolean isChanged() {
    return changed;
  }

  private class CallFinder extends DefaultOpcodeVisitor {
    @Override
    public void visit(Call op) {
      String name = op.procSym().name();
      Integer count = usageCounts.get(name);
      if (count == null) {
        count = 1;
      } else {
        count++;
      }
      usageCounts.put(name, count);
    }
  }

  private class ProcFinder extends DefaultOpcodeVisitor {
    @Override
    public void visit(ProcEntry op) {
      if (op.formalNames().size() < 3) {
        // Find the length of the procedure.
        List<Op> opcodes = new ArrayList<>();
        boolean foundEnd = false;
        int returnCount = 0;
        for (int otherIp = ip + 1; otherIp < code.size() && !foundEnd; otherIp++) {
          Op otherOp = code.get(otherIp);
          if (otherOp instanceof ProcExit) {
            foundEnd = true;
            break;
          }
          if (otherOp instanceof Call
              || otherOp instanceof IfOp
              || otherOp instanceof Goto
              || otherOp instanceof Label) {
            logger.at(loggingLevel).log(
                "NOT inlining '%s' because it has '%s'",
                op.name(), otherOp.getClass().getSimpleName());
            return;
          }
          if (otherOp instanceof Return) {
            returnCount++;
            if (returnCount > 1) {
              // only 0 or 1 returns are allowed
              logger.at(loggingLevel).log(
                  "NOT inlining '%s' because it has too many RETURNs",
                  op.name());
              return;
            }
          }
          opcodes.add(otherOp);
        }
        // Only consider procedures with size < 10 and that don't allow certain opcodes, like calls,
        // gotos/ifs that go outside the block or labels referenced outside the block.
        boolean candidate = foundEnd && opcodes.size() < 10;
        logger.at(loggingLevel).log("'%s' is %sa candidate", op.name(), candidate ? "" : "not ");
        if (candidate) {
          inlineableCode.put(op.name(), opcodes);
          procsByName.put(op.name(), op);
        }
      }
    }
  }

  @Override
  public void visit(Call callOp) {
    String procName = callOp.procSym().name();
    List<Op> codeToRemap = inlineableCode.get(procName);

    if (codeToRemap != null) {
      if (tooExpensive(procName)) {
        inlineableCode.remove(procName);
        procsByName.remove(procName);
        logger.at(loggingLevel).log("Not inlining '%s'; too expensive", procName);
        return;
      }
      InlineRemapper inlineRemapper = new InlineRemapper(codeToRemap, symbolTable, loggingLevel);
      List<Op> remapped = inlineRemapper.remap();

      // Nop the call and mark the end. Since we're repeatedly adding at "ip", the opcodes
      // get pushed up, so we start from the bottom up.
      code.set(ip, new Nop(callOp));
      code.add(ip, new Nop("(inline end)"));

      Return returnOp = null;
      int returnOpIndex = -1;
      for (int i = 0; i < remapped.size(); ++i) {
        Op op = remapped.get(i);
        if (op instanceof Return) {
          returnOp = (Return) op;
          returnOpIndex = i;
          break;
        }
      }
      if (returnOpIndex != -1) {
        // There's a return slot
        if (callOp.destination().isPresent()) {
          // if op is assigned to a return value, copy that
          // from the "return" statement
          remapped.set(returnOpIndex,
              new Transfer(
                  callOp.destination().get(), returnOp.returnValueLocation().get(),
                  callOp.position()));
        } else {
          // No destination. remove the op
          remapped.set(returnOpIndex, new Nop(returnOp));
        }
      }
      logger.at(loggingLevel).log(
          "Can inline '%s' from:\n %s\n to:\n%s",
          callOp.procSym(), Joiner.on('\n').join(codeToRemap), Joiner.on('\n').join(remapped));

      // Insert the inlined code, then finally copy actuals to (remapped) formals.
      code.addAll(ip, remapped);
      ProcEntry entry = procsByName.get(procName);
      for (int i = 0; i < callOp.actuals().size(); ++i) {
        Operand actual = callOp.actuals().get(i);
        code.add(
            ip,
            new Transfer(
                inlineRemapper.remapFormal(entry.formalNames().get(i), actual.type()),
                actual,
                callOp.position()));
      }
      code.add(ip, new Nop("(inline start)"));
      changed = true;
    }
  }

  private boolean tooExpensive(String procName) {
    int usageCount = usageCounts.get(procName);
    List<Op> codeToRemap = inlineableCode.get(procName);
    int inlinedCodeSize = codeToRemap.size();
    if (inlinedCodeSize < 3 || usageCount < 3) {
      logger.at(loggingLevel).log(
          "Not skipping inlining %s: small inlined size (%d) and/or usage count (%d)",
          procName, inlinedCodeSize, usageCount);
      return false;
    }

    ProcEntry entry = procsByName.get(procName);

    boolean hasReturn =
        codeToRemap.stream().filter(op -> op instanceof Return).count() > 0;
    int numParams = entry.formalNames().size();

    int locAdded = usageCount * (inlinedCodeSize + numParams + (hasReturn ? 1 : 0));
    // adjust for future optimizations
    int adjustedLocAdded = (3 * locAdded) / 4;
    // we remove the actual code, plus 5 more (for labels), plus the calls
    int locRemoved = inlinedCodeSize + 5 + usageCount;
    int netLoc = adjustedLocAdded - locRemoved;
    int preOptLoc = code.size();
    int postOptLoc = preOptLoc + netLoc;
    logger.at(loggingLevel).log(
        "%s: LOC added: %d adjusted: %d removed: %d net: %d pre-opt: %d post-opt: %d",
        procName, locAdded, adjustedLocAdded, locRemoved, netLoc, preOptLoc, postOptLoc);

    // Skip this inlining if the delta size is more than 50% of the code.
    return netLoc > code.size() / 2;
  }
}
