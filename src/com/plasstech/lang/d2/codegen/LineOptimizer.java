package com.plasstech.lang.d2.codegen;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.Nop;
import com.plasstech.lang.d2.codegen.il.Op;

abstract class LineOptimizer extends DefaultOpcodeVisitor {
  private final FluentLogger logger = FluentLogger.forEnclosingClass();

  private boolean changed;
  protected List<Op> code;
  protected int ip;
  protected Op currentOp;

  public final ImmutableList<Op> optimize(ImmutableList<Op> input) {
    this.code = new ArrayList<>(input);
    changed = false;
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

  protected void deleteCurrent() {
    // Replace the current op with a nop.
    Op newOp = new Nop(currentOp);
    setChanged(true);
    logger.atInfo().log("Replacing ip %d: %s with %s", ip, currentOp, newOp);
    code.set(ip, newOp);
  }

  protected void replaceCurrent(Op newOp) {
    setChanged(true);
    logger.atInfo().log("Replacing ip %d: %s with %s", ip, currentOp, newOp);
    code.set(ip, newOp);
  }

  protected void replaceAt(int theIp, Op newOp) {
    setChanged(true);
    logger.atInfo().log("Replacing ip %d: %s with %s", theIp, code.get(theIp), newOp);
    code.set(theIp, newOp);
  }
}
