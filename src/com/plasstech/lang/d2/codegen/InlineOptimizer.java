package com.plasstech.lang.d2.codegen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.il.Call;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.Nop;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.ProcEntry;
import com.plasstech.lang.d2.codegen.il.ProcExit;

public class InlineOptimizer extends DefaultOpcodeVisitor implements Optimizer {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final Level loggingLevel;
  private boolean changed;
  private List<Op> code;
  // Maps from proc name to its code.
  private Map<String, List<Op>> inlineableCode = new HashMap<>();
  private Map<String, ProcEntry> procsByName = new HashMap<>();

  private int ip;

  InlineOptimizer(int debugLevel) {
    loggingLevel = toLoggingLevel(debugLevel);
  }

  @Override
  public ImmutableList<Op> optimize(ImmutableList<Op> input) {
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
      logger.at(loggingLevel).log("%s has few formals", op.name());
      // Find the length of the procedure.
      ArrayList<Op> opcodes = new ArrayList<>();
      boolean foundEnd = false;
      for (int otherIp = ip + 1; otherIp < code.size() && !foundEnd; otherIp++) {
        Op otherOp = code.get(otherIp);
        if (otherOp instanceof Nop) {
          continue;
        }
        if (otherOp instanceof ProcExit) {
          foundEnd = true;
          break;
        }
        opcodes.add(otherOp);
      }
      // Only consider procedures with size < 10 and that don't allow certain opcodes, like calls,
      // gotos/ifs that go outside the block or labels referenced outside the block. Also don't
      // allow any local variables (?)
      boolean candidate = foundEnd && opcodes.size() < 10;
      logger.at(loggingLevel).log("%s is %sa candidate", op.name(), candidate ? "" : "not ");
      if (candidate) {
        inlineableCode.put(op.name(), opcodes);
        procsByName.put(op.name(), op);
      }
    }
  }

  @Override
  public void visit(Call op) {
    List<Op> replacement = inlineableCode.get(op.functionToCall());
    if (replacement != null) {
      ProcEntry entry = procsByName.get(op.functionToCall());
      // woo!
      // now, for each actual/formal pair, substitute it
      // in the inlineable code
      // also, replace each temp with a new temp name

      logger.at(loggingLevel).log("Inlining!\nfrom %s", replacement);
      List<Op> remapped = new InlineRemapper(replacement, entry.formalNames()).remap();
      logger.at(loggingLevel).log("Inlining!\nto   %s", remapped);

      if (op.destination().isPresent()) {
        // if op is assigned to a return value, copy that
        // from the "return" statement
      } else {
        // Void proc
        if (entry.formalNames().size() == 0) {
          // No parameters!
          // NOP the current op, and insert the remapped code.
          code.set(ip, new Nop(op));
          code.addAll(ip, remapped);
          changed = true;
        } else {
          // copy actual to formal, then insert the rempaped code
          // code.set(ip, new Nop(op));
          // code.addAll(ip, remapped);
          // This is wrong/bad. the formal name needs to be remapped too.
          // code.add(ip, new Transfer(new StackLocation(entry.formalNames().get(0)),
          //    op.actuals().get(0)));
          // changed = true;
        }
      }
    }
  }
}
