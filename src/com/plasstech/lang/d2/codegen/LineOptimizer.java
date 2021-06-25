package com.plasstech.lang.d2.codegen;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.Nop;
import com.plasstech.lang.d2.codegen.il.Op;

abstract class LineOptimizer extends DefaultOpcodeVisitor {
  private final FluentLogger logger = FluentLogger.forEnclosingClass();

  protected final Level loggingLevel;
  private boolean changed;
  protected List<Op> code;
  protected int ip;
  protected Op currentOp;

  LineOptimizer(int debugLevel) {
    switch (debugLevel) {
      case 1:
        loggingLevel = Level.CONFIG;
        break;
      case 2:
        loggingLevel = Level.INFO;
        break;
      default:
      case 0:
        loggingLevel = Level.FINE;
        break;
    }
  }

  public ImmutableList<Op> optimize(ImmutableList<Op> input) {
    this.code = new ArrayList<>(input);
    setChanged(false);
    for (ip = 0; ip < code.size(); ++ip) {
      currentOp = code.get(ip);
      currentOp.accept(this);
    }
    return ImmutableList.copyOf(this.code);
  }

  public boolean isChanged() {
    return changed;
  }

  public void setChanged(boolean changed) {
    this.changed = changed;
  }

  /** Replace the given op with the given nop. */
  protected void replaceAt(int theIp, Op newOp) {
    setChanged(true);
    logger.at(loggingLevel).log("Replacing ip %d: %s with %s", theIp, code.get(theIp), newOp);
    code.set(theIp, newOp);
  }

  /** Replace the current op with the given nop. */
  protected void replaceCurrent(Op newOp) {
    replaceAt(ip, newOp);
  }

  /** Replace the given op with a nop. */
  protected void deleteAt(int theIp) {
    Op theOp = code.get(theIp);
    Op newOp = new Nop(theOp);
    replaceAt(theIp, newOp);
  }

  /** Replace the current op with a nop. */
  protected void deleteCurrent() {
    deleteAt(ip);
  }
}
