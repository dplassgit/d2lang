package com.plasstech.lang.d2.codegen;

import com.plasstech.lang.d2.codegen.il.Goto;
import com.plasstech.lang.d2.codegen.il.IfOp;
import com.plasstech.lang.d2.codegen.il.Nop;
import com.plasstech.lang.d2.codegen.il.Transfer;

public class DeadCodeOptimizer extends LineOptimizer {

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
      // a=a is dead
      replaceCurrent(Nop.INSTANCE);
      return;
    }
  }
}
