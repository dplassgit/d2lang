package com.plasstech.lang.d2.optimize;

import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Dec;
import com.plasstech.lang.d2.codegen.il.Inc;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.common.TokenType;

/**
 * Optimizes:
 * 
 * <pre>
 * i++
 * i++
 * </pre>
 * 
 * into
 * 
 * <pre>
 * i = i + 2
 * </pre>
 * 
 * And vice/versa.
 */
public class AdjacentIncDecOptimizer extends LineOptimizer {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  AdjacentIncDecOptimizer(int debugLevel) {
    super(debugLevel);
  }

  @Override
  public void visit(BinOp first) {
    if (!isVarPlusEquals(first) || !first.destination().type().isNumeric()) {
      return;
    }
    Op second = getOpAt(ip() + 1);
    ConstantOperand<? extends Number> nextConst = getCompatibleConst(first.destination(), second);
    if (nextConst == null) {
      return;
    }
    nextConst =
        addConsts(nextConst, ConstantOperand.valueFromConstOperand(first.right()).longValue());
    logger.at(loggingLevel).log("Found pair: %s and %s", first, second);
    replaceCurrent(
        new BinOp(first.destination(), first.destination(), TokenType.PLUS, nextConst,
            first.position()));
    deleteAt(ip() + 1);
  }

  @Override
  public void visit(Inc first) {
    Op second = getOpAt(ip() + 1);
    ConstantOperand<? extends Number> nextConst = getCompatibleConst(first.target(), second);
    if (nextConst == null) {
      return;
    }
    nextConst = addConsts(nextConst, 1L);
    logger.at(loggingLevel).log("Found pair: %s and %s", first, second);
    replaceCurrent(
        new BinOp(first.target(), first.target(), TokenType.PLUS, nextConst, first.position()));
    deleteAt(ip() + 1);
  }

  @Override
  public void visit(Dec first) {
    Op second = getOpAt(ip() + 1);
    ConstantOperand<? extends Number> nextConst = getCompatibleConst(first.target(), second);
    if (nextConst == null) {
      return;
    }
    nextConst = addConsts(nextConst, -1L);
    logger.at(loggingLevel).log("Found pair: %s and %s", first, second);
    replaceCurrent(
        new BinOp(first.target(), first.target(), TokenType.PLUS, nextConst, first.position()));
    deleteAt(ip() + 1);
  }

  private static ConstantOperand<? extends Number> addConsts(
      ConstantOperand<? extends Number> leftConst, long right) {
    return ConstantOperand.fromValue(leftConst.value().longValue() + right, leftConst.type());
  }

  private static ConstantOperand<? extends Number> getCompatibleConst(Location target, Op op) {
    if (op instanceof BinOp) {
      BinOp binOp = (BinOp) op;
      if (binOp.destination().equals(target) && isVarPlusEquals(binOp)) {
        return (ConstantOperand<? extends Number>) binOp.right();
      }
      if (binOp.destination().equals(target) && isVarMinusEquals(binOp)) {
        Number value = ConstantOperand.valueFromConstOperand(binOp.right());
        long valueLong = value.longValue();
        return ConstantOperand.fromValue(-valueLong, binOp.destination().type());
      }
    }
    if (op instanceof Inc) {
      Inc nextInc = (Inc) op;
      if (nextInc.target().equals(target)) {
        return ConstantOperand.fromValue(1L, nextInc.target().type());
      }
    }
    if (op instanceof Dec) {
      Dec nextDec = (Dec) op;
      if (nextDec.target().equals(target)) {
        return ConstantOperand.fromValue(-1L, nextDec.target().type());
      }
    }
    return null;
  }

  /**
   * @return true if binop is a=a + integral constant
   */
  private static boolean isVarPlusEquals(BinOp binOp) {
    return isVarPlusMinus(binOp, TokenType.PLUS);
  }

  /**
   * @return true if binop is a=a - integral constant
   */
  private static boolean isVarMinusEquals(BinOp binOp) {
    return isVarPlusMinus(binOp, TokenType.MINUS);
  }

  /**
   * @return true if binop is a=a (op) integral constant
   */
  private static boolean isVarPlusMinus(BinOp binOp, TokenType tokenType) {
    return binOp.left().equals(binOp.destination())
        && binOp.operator() == tokenType
        && binOp.right().isConstant()
        && binOp.right().type().isNumeric();
  }
}
