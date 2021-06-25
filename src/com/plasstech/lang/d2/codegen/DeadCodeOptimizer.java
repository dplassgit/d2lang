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
import com.plasstech.lang.d2.codegen.il.Stop;
import com.plasstech.lang.d2.codegen.il.Transfer;

public class DeadCodeOptimizer extends LineOptimizer {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  DeadCodeOptimizer(int debugLevel) {
    super(debugLevel);
  }

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
          logger.at(loggingLevel).log("Nopping 'if' followed by 'goto' to same place");
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
    for (int testip = ip + 1; testip < code.size(); ++testip) {
      Op testop = code.get(testip);
      if (testop instanceof Label) {
        Label label = (Label) testop;
        if (label.label().equals(op.label())) {
          // Found the label!
          logger.at(loggingLevel).log(
              "Found the label for GOTO '%s' with nothing in between", op.label());
          deleteCurrent();
          return;
        }
      } else if (testop instanceof Nop) {
        continue;
      } else {
        break;
      }
    }

    // 2. any code between a goto and a label is dead.
    killUntilLabel("GOTO");

    // 3. Optimize double-jumps: find the label. if the next active statement is a goto, just go there.
    for (int testip = ip + 1; testip < code.size(); ++testip) {
      Op testop = code.get(testip);
      if (testop instanceof Label) {
        Label label = (Label) testop;
        if (label.label().equals(op.label())) {
          // Found the label! Now see if it's just a goto somewhere else.
          for (int ip2 = testip + 1; ip2 < code.size(); ++ip2) {
            Op testop2 = code.get(ip2);
            if (testop2 instanceof Nop || testop2 instanceof Label) {
              continue;
            } else if (testop2 instanceof Goto) {
              Goto otherGoto = (Goto) testop2;
              logger.at(loggingLevel).log("Replacing double hop from %s to %s", op.label(), otherGoto.label());
              replaceCurrent(new Goto(otherGoto.label()));
              break;
            } else {
              // another op, stop.
              break;
            }
          }
          // We found the label and either did or didn't do something to it.
          break;
        }
      }
    }
  }

  @Override
  public void visit(Return op) {
    // any code between a return and a label is dead.
    killUntilLabel("RETURN");
  }

  @Override
  public void visit(Stop op) {
    // any code between a stop and a label is dead.
    killUntilLabel("STOP");
  }

  /** Nop lines between current IP and the next label, proc entry or proc exit. */
  private void killUntilLabel(String source) {
    for (int testip = ip + 1; testip < code.size(); ++testip) {
      Op testop = code.get(testip);
      if (testop instanceof Nop) {
        continue;
      }
      if (testop instanceof Label || testop instanceof ProcEntry || testop instanceof ProcExit) {
        break;
      } else {
        logger.at(loggingLevel).log("Nopping dead statement after %s", source);
        replaceAt(testip, new Nop(testop));
      }
    }
  }
}
