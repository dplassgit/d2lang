package com.plasstech.lang.d2.optimize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.il.Call;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.Goto;
import com.plasstech.lang.d2.codegen.il.IfOp;
import com.plasstech.lang.d2.codegen.il.Label;
import com.plasstech.lang.d2.codegen.il.Nop;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.ProcEntry;
import com.plasstech.lang.d2.codegen.il.ProcExit;
import com.plasstech.lang.d2.codegen.il.Return;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.type.SymbolTable;

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
    this.symbolTable = symbolTable;
    code = new ArrayList<>(input);
    for (ip = 0; ip < input.size(); ++ip) {
      changed = false;
      input.get(ip).accept(this);
      if (changed) {
        // We changed. Things can get out of sync, so stop.
        break;
      }
    }
    // TODO: NOP out the whole proc

    return ImmutableList.copyOf(code);
  }

  @Override
  public boolean isChanged() {
    return changed;
  }

  @Override
  public void visit(ProcEntry op) {
    if (op.formalNames().size() < 3) {
      // Find the length of the procedure.
      ArrayList<Op> opcodes = new ArrayList<>();
      boolean foundEnd = false;
      int returnCount = 0;
      for (int otherIp = ip + 1; otherIp < code.size() && !foundEnd; otherIp++) {
        Op otherOp = code.get(otherIp);
        if (otherOp instanceof Nop) {
          continue;
        }
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

  @Override
  public void visit(Call op) {
    List<Op> replacement = inlineableCode.get(op.procSym().name());

    if (replacement != null) {
      ProcEntry entry = procsByName.get(op.procSym().name());
      InlineRemapper inlineRemapper = new InlineRemapper(replacement, symbolTable);
      List<Op> remapped = inlineRemapper.remap();
      logger.at(loggingLevel).log(
          "Can inline '%s' from:\n %s\n to:\n%s",
          op.procSym(), Joiner.on('\n').join(replacement), Joiner.on('\n').join(remapped));

      // Nop the call and mark the end. Since we're repeatedly adding at "ip", the opcodes
      // get pushed up, so we start from the bottom up.
      code.set(ip, new Nop(op));
      code.add(ip, new Nop("(inline end)"));

      Return returnOp = null;
      if (Iterables.getLast(remapped) instanceof Return) {
        // Always return it, but don't always *use* it.
        returnOp = (Return) remapped.remove(remapped.size() - 1);
      }
      if (op.destination().isPresent() && returnOp != null) {
        // if op is assigned to a return value, copy that
        // from the "return" statement
        code.add(
            ip,
            new Transfer(
                op.destination().get(), returnOp.returnValueLocation().get(), op.position()));
      }
      // Insert the inlined code, then finally copy actuals to (remapped) formals.
      code.addAll(ip, remapped);
      for (int i = 0; i < op.actuals().size(); ++i) {
        Operand actual = op.actuals().get(i);
        code.add(
            ip,
            new Transfer(
                inlineRemapper.remapFormal(entry.formalNames().get(i), actual.type()),
                actual,
                op.position()));
      }
      code.add(ip, new Nop("(inline start)"));
      changed = true;
    }
  }
}
