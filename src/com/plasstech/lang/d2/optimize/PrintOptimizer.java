package com.plasstech.lang.d2.optimize;

import java.math.BigDecimal;
import java.math.MathContext;

import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.SysCall.Call;
import com.plasstech.lang.d2.type.VarType;

/** Optimizes printing constants, and consecutive print statements, if they're constant strings. */
public class PrintOptimizer extends LineOptimizer {

  PrintOptimizer(int debugLevel) {
    super(debugLevel);
  }

  @Override
  public void visit(SysCall op) {
    if (optimizePrintConstant(op)) {
      return;
    }

    // Optimize two printed strings in a row into a single string.
    if (!isPrintStringConstant(op)) {
      return;
    }
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
    // Use the next one's call (PRINT or PRINTLN)
    SysCall newOp = new SysCall(nextPrint.call(), ConstantOperand.of(combined));
    replaceCurrent(newOp);
    deleteAt(ip() + 1);
  }

  /**
   * Optimize printing a constant non-string, to print it as a string.
   * 
   * @return true if optimized this op.
   */
  private boolean optimizePrintConstant(SysCall op) {
    if (op.call() != SysCall.Call.PRINT && op.call() != SysCall.Call.PRINTLN) {
      return false;
    }
    Operand arg = op.arg();
    if (!arg.isConstant()) {
      return false;
    }
    if (arg.type() == VarType.STRING) {
      return false;
    }
    ConstantOperand<?> operand = (ConstantOperand<?>) arg;
    if (operand.type().isNull() || operand.value() == null) {
      // print a "null"
      replaceCurrent(new SysCall(op.call(), ConstantOperand.of("null")));
      return true;
    }

    String asString = operand.value().toString();
    if (operand.type() == VarType.DOUBLE) {
      double value = (double) operand.value();
      BigDecimal bd = BigDecimal.valueOf(value).round(MathContext.DECIMAL64).stripTrailingZeros();
      asString = bd.toPlainString();
    }
    replaceCurrent(new SysCall(op.call(), ConstantOperand.of(asString)));
    return true;
  }

  private static boolean isPrintStringConstant(Op op) {
    if (!(op instanceof SysCall)) {
      return false;
    }
    SysCall sysCall = (SysCall) op;
    if (!(sysCall.call() == Call.PRINT || sysCall.call() == Call.PRINTLN)) {
      // not print or println: done.
      return false;
    }
    Operand operand = sysCall.arg();
    return operand.isConstant() && operand.type() == VarType.STRING;
  }

  private static String getConstValue(Operand operand) {
    ConstantOperand<String> constOp = (ConstantOperand<String>) operand;
    return constOp.value();
  }
}
