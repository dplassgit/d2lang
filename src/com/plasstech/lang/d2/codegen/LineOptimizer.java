package com.plasstech.lang.d2.codegen;

import com.google.common.flogger.FluentLogger;
import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.Op;
import java.util.List;
import java.util.ArrayList;

abstract class LineOptimizer extends DefaultOpcodeVisitor {
  private final FluentLogger logger = FluentLogger.forEnclosingClass();

  private List<Op> code;
  private boolean changed;
  private int ip;

  public final ImmutableList<Op> optimize(ImmutableList<Op> input) {
    this.code = new ArrayList<>(input);
    changed = false;
    for (ip = 0; ip < code.size(); ++ip) {
      Op op = code.get(ip);
      op.accept(this);
    }
    return ImmutableList.copyOf(this.code);
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
