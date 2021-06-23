package com.plasstech.lang.d2.codegen;

import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.il.Goto;
import com.plasstech.lang.d2.codegen.il.IfOp;
import com.plasstech.lang.d2.codegen.il.Label;
import com.plasstech.lang.d2.codegen.il.Nop;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.ProcEntry;
import com.plasstech.lang.d2.codegen.il.ProcExit;
import com.plasstech.lang.d2.codegen.il.Return;
import com.plasstech.lang.d2.codegen.il.Transfer;

public class DeadCodeOptimizer extends LineOptimizer {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  @Override
  public void visit(IfOp op) {
    if (op.condition().equals(ConstantOperand.TRUE) || op.condition().equals(ConstantOperand.ONE)) {
      replaceCurrent(new Goto(op.destination()));
    }
    if (op.condition().equals(ConstantOperand.FALSE)
        || op.condition().equals(ConstantOperand.ZERO)) {
      deleteCurrent();
    }
    if (ip < code.size()) {
      Op next = code.get(ip + 1);
      if (next instanceof Goto) {
        Goto nextGoto = (Goto) next;
        if (op.destination().equals(nextGoto.label())) {
          logger.atInfo().log("Nopping 'if' followed by 'goto' to same place");
          // both the "if" and the "goto" goto the same place, so one is redundant.
          deleteCurrent();
        }
      }
    }
  }

  @Override
  public void visit(Transfer op) {
    Operand source = op.source();
    Location dest = op.destination();

    if (dest.equals(source)) {
      // a=a is dead. unfortunately this almost never happens...
      deleteCurrent();
      return;
    }
  }

  @Override
  public void visit(Goto op) {
    // 1. if there only nops or labels between here and dest, we don't have to goto.
    boolean onlyLabels = true;
    for (int testip = ip + 1; testip < code.size(); ++testip) {
      Op testop = code.get(testip);
      if (testop instanceof Nop) {
        continue;
      }
      if (testop instanceof Label) {
        Label label = (Label) testop;
        if (label.label().equals(op.label())) {
          // Found the label!
          logger.atInfo().log("Found the label for 'goto %s' with nothing in between", op.label());
          break;
        }
      } else {
        onlyLabels = false;
        break;
      }
    }
    if (onlyLabels) {
      deleteCurrent();
      return;
    }
    // 2. any code between a goto and a label is dead.
    for (int testip = ip + 1; testip < code.size(); ++testip) {
      Op testop = code.get(testip);
      if (testop instanceof Nop) {
        continue;
      }
      if (testop instanceof Label || testop instanceof ProcEntry || testop instanceof ProcExit) {
        break;
      } else {
        logger.atInfo().log("Nopping statement between goto and next label");
        replaceAt(testip, new Nop(testop));
      }
    }
  }

  @Override
  public void visit(Return op) {
    // 2. any code between a return and a label is dead.
    for (int testip = ip + 1; testip < code.size(); ++testip) {
      Op testop = code.get(testip);
      if (testop instanceof Nop) {
        continue;
      }
      if (testop instanceof Label || testop instanceof ProcEntry || testop instanceof ProcExit) {
        break;
      } else {
        logger.atInfo().log("Nopping statement after return");
        replaceAt(testip, new Nop(testop));
      }
    }
  }
}
