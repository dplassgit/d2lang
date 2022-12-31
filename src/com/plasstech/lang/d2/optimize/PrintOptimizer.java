package com.plasstech.lang.d2.optimize;

import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.SysCall.Call;
import com.plasstech.lang.d2.type.VarType;

/** Optimizes consecutive print statements, if they're constant strings. */
public class PrintOptimizer extends LineOptimizer {

  PrintOptimizer(int debugLevel) {
    super(debugLevel);
  }

  @Override
  public void visit(SysCall op) {
    if (isPrintStringConstant(op)) {
      Op next = getOpAt(ip() + 1);
      if (!isPrintStringConstant(next)) {
        return;
      }
      // Both constant strings!
      SysCall nextPrint = (SysCall) next;
      String combined = getConstValue(op.arg());
      if (op.call() == Call.PRINTLN) {
        // retain our println'ness.
        combined += "\n";
      }
      combined += getConstValue(nextPrint.arg());
      // if the next one was println, keep it.
      SysCall newOp = new SysCall(nextPrint.call(), ConstantOperand.of(combined));
      replaceCurrent(newOp);
      deleteAt(ip() + 1);
    }
  }

  private static boolean isPrintStringConstant(Op op) {
    if (op instanceof SysCall) {
      SysCall sysCall = (SysCall) op;
      if (sysCall.call() != Call.PRINT && sysCall.call() != Call.PRINTLN) {
        return false;
      }
      Operand operand = sysCall.arg();
      return operand.isConstant() && operand.type() == VarType.STRING;
    }
    return false;
  }

  private static String getConstValue(Operand operand) {
    ConstantOperand<String> constOp = (ConstantOperand<String>) operand;
    return constOp.value();
  }
}
