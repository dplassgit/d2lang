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
import com.plasstech.lang.d2.codegen.il.DeallocateTemp;
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
  // Maps from proc name to its code.
  private Map<String, List<Op>> inlineableCode = new HashMap<>();
  private Map<String, ProcEntry> procsByName = new HashMap<>();

  private int ip;
  private SymbolTable symbolTable;

  InlineOptimizer(int debugLevel) {
    loggingLevel = toLoggingLevel(debugLevel);
  }

  @Override
  public ImmutableList<Op> optimize(ImmutableList<Op> input, SymbolTable symbolTable) {
    // Remove nops because reasons.
    input = new NopOptimizer().optimize(input, symbolTable);
    this.symbolTable = symbolTable;
    code = new ArrayList<>(input);

    OpcodeVisitor finder = new ProcFinder();
    for (ip = 0; ip < input.size(); ++ip) {
      input.get(ip).accept(finder);
    }

    for (ip = 0; ip < input.size(); ++ip) {
      changed = false;
      input.get(ip).accept(this);
      if (changed) {
        // We changed. Things can get out of sync, so stop.
        break;
      }
    }

    code = Optimizer.removeMatchingOps(code, DeallocateTemp.class);
    code = Optimizer.removeMatchingOps(code, Nop.class);
    LongTempDeallocator deallocator = new LongTempDeallocator();
    code = deallocator.optimize(ImmutableList.copyOf(code), null);

    return ImmutableList.copyOf(code);
  }

  @Override
  public boolean isChanged() {
    return changed;
  }

  private class ProcFinder extends DefaultOpcodeVisitor {
    @Override
    public void visit(ProcEntry op) {
      if (op.formalNames().size() < 3) {
        // Find the length of the procedure.
        ArrayList<Op> opcodes = new ArrayList<>();
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
          }
          opcodes.add(otherOp);
        }
        // Only consider procedures with size < 10 and that don't allow certain opcodes, like calls,
        // gotos/ifs that go outside the block or labels referenced outside the block.
        boolean candidate = foundEnd && opcodes.size() < 10 && returnCount < 2;
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
    List<Op> source = inlineableCode.get(callOp.procSym().name());

    if (source != null) {
      ProcEntry entry = procsByName.get(callOp.procSym().name());
      InlineRemapper inlineRemapper = new InlineRemapper(source, symbolTable, loggingLevel);
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
      if (callOp.destination().isPresent()) {
        // if returnOpIndex is -1 it will (correctly) throw an exception
        // TODO: be smarter about this.
        // if op is assigned to a return value, copy that
        // from the "return" statement
        remapped.set(returnOpIndex,
            new Transfer(
                callOp.destination().get(), returnOp.returnValueLocation().get(),
                callOp.position()));
      } else {
        // No destination. remove the op
        if (returnOpIndex != -1) {
          remapped.set(returnOpIndex, new Nop(returnOp));
        }
      }
      logger.at(loggingLevel).log(
          "Can inline '%s' from:\n %s\n to:\n%s",
          callOp.procSym(), Joiner.on('\n').join(source), Joiner.on('\n').join(remapped));

      // Insert the inlined code, then finally copy actuals to (remapped) formals.
      code.addAll(ip, remapped);
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
}
