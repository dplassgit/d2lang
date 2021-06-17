package com.plasstech.lang.d2.codegen;

import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.Op;
import java.util.List;

abstract class LineOptimizer extends DefaultOpcodeVisitor {
  protected final List<Op> code;
  private boolean changed;
  protected int ip;
  private final FluentLogger logger = FluentLogger.forEnclosingClass();

  public LineOptimizer(List<Op> code) {
    this.code = code;
  }

  public final boolean optimize() {
    changed = false;
    for (ip = 0; ip < code.size(); ++ip) {
      Op op = code.get(ip);
      op.accept(this);
    }
    return changed;
  }

  public boolean isChanged() {
    return changed;
  }

  public void setChanged(boolean changed) {
    this.changed = changed;
  }

  protected void replaceCurrent(Op newOp) {
    setChanged(true);
    logger.atInfo().log("Replacing ip %d: %s with %s", ip, code.get(ip), newOp);
    code.set(ip, newOp);
  }
}
