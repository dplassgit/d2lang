package com.plasstech.lang.d2.optimize;

import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.il.Dec;
import com.plasstech.lang.d2.codegen.il.Goto;
import com.plasstech.lang.d2.codegen.il.IfOp;
import com.plasstech.lang.d2.codegen.il.Inc;
import com.plasstech.lang.d2.codegen.il.Label;
import com.plasstech.lang.d2.codegen.il.Nop;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.ProcEntry;
import com.plasstech.lang.d2.codegen.il.ProcExit;
import com.plasstech.lang.d2.codegen.il.Return;
import com.plasstech.lang.d2.codegen.il.Stop;
import com.plasstech.lang.d2.codegen.il.Transfer;

class DeadCodeOptimizer extends LineOptimizer {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  DeadCodeOptimizer(int debugLevel) {
    super(debugLevel);
  }

  @Override
  public void visit(Inc inc) {
    int nextIp = getNextMatch(Dec.class);
    if (nextIp != -1) {
      Dec dec = (Dec) getOpAt(nextIp);
      if (dec.target().equals(inc.target())) {
        logger.at(loggingLevel).log("Deleting inc+dec %s at ip %d", inc.target(), ip());
        deleteAt(nextIp);
        deleteCurrent();
      }
    }
  }

  @Override
  public void visit(Dec dec) {
    int nextIp = getNextMatch(Inc.class);
    if (nextIp != -1) {
      Inc inc = (Inc) getOpAt(nextIp);
      if (inc.target().equals(dec.target())) {
        logger.at(loggingLevel).log("Deleting dec+inc %s at ip %d", dec.target(), ip());
        deleteAt(nextIp);
        deleteCurrent();
      }
    }
  }

  private int getNextMatch(Class<?> clazz) {
    for (int testIp = ip() + 1; testIp < code.size(); ++testIp) {
      Op testOp = code.get(testIp);
      if (testOp instanceof Nop) {
        continue;
      } else if (testOp.getClass() == clazz) {
        return testIp;
      } else {
        break;
      }
    }
    return -1;
  }

  @Override
  public void visit(IfOp op) {
    if (op.condition().equals(ConstantOperand.TRUE) || op.condition().equals(ConstantOperand.ONE)) {
      replaceCurrent(new Goto(op.destination()));
    }
    if (op.condition().equals(ConstantOperand.FALSE)
        || op.condition().equals(ConstantOperand.ZERO)) {
      deleteCurrent();
      return;
    }
    int nextIp = getNextMatch(Goto.class);
    if (nextIp != -1) {
      Op next = getOpAt(nextIp);
      Goto nextGoto = (Goto) next;
      if (op.destination().equals(nextGoto.label())) {
        logger.at(loggingLevel).log("Nopping 'if' followed by 'goto' to same place");
        // both the "if" and the "goto" goto the same place, so one is redundant.
        deleteCurrent();
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
    int nextIp = getNextMatch(Label.class);
    if (nextIp != -1) {
      Op testOp = code.get(nextIp);
      Label label = (Label) testOp;
      if (label.label().equals(op.label())) {
        // Found the label!
        logger.at(loggingLevel).log(
            "Found the label for GOTO '%s' with nothing in between", op.label());
        deleteCurrent();
        return;
      }
    }

    // 2. any code between a goto and a label is dead.
    killUntilLabel("GOTO");

    // 3. Optimize double-jumps: find the label. if the next active statement is a goto, just go
    // there.
    for (int testIp = ip() + 1; testIp < code.size(); ++testIp) {
      Op testOp = code.get(testIp);
      if (testOp instanceof Label) {
        Label label = (Label) testOp;
        if (label.label().equals(op.label())) {
          // Found the label! Now see if it's just a goto somewhere else.
          for (int testIp2 = testIp + 1; testIp2 < code.size(); ++testIp2) {
            Op testOp2 = code.get(testIp2);
            if (testOp2 instanceof Nop || testOp2 instanceof Label) {
              continue;
            } else if (testOp2 instanceof Goto) {
              Goto otherGoto = (Goto) testOp2;
              logger.at(loggingLevel).log(
                  "Replacing double hop from %s to %s", op.label(), otherGoto.label());
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
    for (int testIp = ip() + 1; testIp < code.size(); ++testIp) {
      Op testOp = code.get(testIp);
      if (testOp instanceof Nop) {
        continue;
      }
      if (testOp instanceof Label || testOp instanceof ProcEntry || testOp instanceof ProcExit) {
        break;
      } else {
        logger.at(loggingLevel).log("Nopping dead statement after %s", source);
        replaceAt(testIp, new Nop(testOp));
      }
    }
  }
}
