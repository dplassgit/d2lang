package com.plasstech.lang.d2.optimize;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.google.common.flogger.LogSites;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.Nop;
import com.plasstech.lang.d2.codegen.il.Op;

abstract class LineOptimizer extends DefaultOpcodeVisitor implements Optimizer {
  private final FluentLogger logger = FluentLogger.forEnclosingClass();

  private boolean changed;
  private int ip;

  protected final Level loggingLevel;
  protected List<Op> code;

  LineOptimizer(int debugLevel) {
    this.loggingLevel = toLoggingLevel(debugLevel);
  }

  @Override
  public final ImmutableList<Op> optimize(ImmutableList<Op> input) {
    code = new ArrayList<>(input);
    preProcess();
    setChanged(false);
    for (ip = 0; ip < code.size(); ++ip) {
      code.get(ip).accept(this);
    }
    postProcess();
    return ImmutableList.copyOf(code);
  }

  protected void postProcess() {}

  protected void preProcess() {}

  @Override
  public boolean isChanged() {
    return changed;
  }

  public void setChanged(boolean changed) {
    this.changed = changed;
  }

  /** Return the opcode at the given IP, if it's in range. Otherwise, return null. */
  protected Op getOpAt(int theIp) {
    if (theIp < code.size()) {
      return code.get(theIp);
    }
    return null;
  }

  /** Replace the op at the given ip with the given op. */
  protected void replaceAt(int theIp, Op newOp) {
    setChanged(true);
    logger
        .at(loggingLevel)
        .withInjectedLogSite(LogSites.callerOf(LineOptimizer.class))
        .log("Replacing ip %d: %s with %s", theIp, code.get(theIp), newOp);
    code.set(theIp, newOp);
  }

  /** Replace the current op with the given op. */
  protected void replaceCurrent(Op newOp) {
    replaceAt(ip, newOp);
  }

  /** Replace the op at the given ip with a nop. */
  protected void deleteAt(int theIp) {
    Op theOp = code.get(theIp);
    Op newOp = new Nop(theOp);
    replaceAt(theIp, newOp);
  }

  /** Replace the current op with a nop. */
  protected void deleteCurrent() {
    deleteAt(ip);
  }

  protected int ip() {
    return ip;
  }
}
