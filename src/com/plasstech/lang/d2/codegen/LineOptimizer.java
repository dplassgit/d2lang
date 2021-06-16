package com.plasstech.lang.d2.codegen;

import java.util.ArrayList;
import java.util.List;

import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.il.Op;

abstract class LineOptimizer implements Optimizer {
  protected List<Op> code;
  protected int ip;
  private final FluentLogger logger = FluentLogger.forEnclosingClass();

  @Override
  public final List<Op> optimize(List<Op> code) {
    this.code = new ArrayList<>(code);
    for (ip = 0; ip < code.size(); ++ip) {
      Op op = code.get(ip);
      doOptimize(op);
    }
    return this.code;
  }

  protected abstract void doOptimize(Op op);

  protected void replaceCurrent(Op newOp) {
    logger.atInfo().log("Replacing ip %d: %s with %s", ip, code.get(ip), newOp);
    code.set(ip, newOp);
  }
 }
