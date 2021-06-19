package com.plasstech.lang.d2.codegen;

import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.il.Goto;
import com.plasstech.lang.d2.codegen.il.IfOp;
import com.plasstech.lang.d2.codegen.il.Label;
import com.plasstech.lang.d2.codegen.il.Nop;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Transfer;

public class DeadCodeOptimizer extends LineOptimizer {
  private final FluentLogger logger = FluentLogger.forEnclosingClass();

  @Override
  public void visit(IfOp op) {
    if (op.condition().equals(ConstantOperand.TRUE) || op.condition().equals(ConstantOperand.ONE)) {
      replaceCurrent(new Goto(op.destination()));
    }
    if (op.condition().equals(ConstantOperand.FALSE)
        || op.condition().equals(ConstantOperand.ZERO)) {
      replaceCurrent(Nop.INSTANCE);
    }
  }

  @Override
  public void visit(Transfer op) {
    Operand source = op.source();
    Location dest = op.destination();

    if (dest.equals(source)) {
      // a=a is dead. unfortunately this almost never happens...
      replaceCurrent(Nop.INSTANCE);
      return;
    }
  }

  @Override
  public void visit(Goto op) {
    // if there only nops or labels between here and dest, we can skip this.
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
      replaceCurrent(Nop.INSTANCE);
    }
  }
}
