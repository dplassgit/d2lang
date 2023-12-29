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
    if (!isVarPlusEquals(first) || !first.destination().type().isIntegral()) {
      return;
    }
    Op second = getOpAt(ip() + 1);
    ConstantOperand<? extends Number> nextConst = getCompatibleConst(first.destination(), second);
    if (nextConst == null) {
      return;
    }
    nextConst = addConsts(nextConst, (ConstantOperand<? extends Number>) first.right());
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
    nextConst = addConsts(nextConst, ConstantOperand.fromValue(1L, first.target().type()));
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
    nextConst = addConsts(nextConst, ConstantOperand.fromValue(-1L, first.target().type()));
    logger.at(loggingLevel).log("Found pair: %s and %s", first, second);
    replaceCurrent(
        new BinOp(first.target(), first.target(), TokenType.PLUS, nextConst, first.position()));
    deleteAt(ip() + 1);
  }

  private ConstantOperand<? extends Number> addConsts(
      ConstantOperand<? extends Number> leftConst,
      ConstantOperand<? extends Number> rightConst) {
    Number left = leftConst.value();
    Number right = rightConst.value();
    return ConstantOperand.fromValue(left.longValue() + right.longValue(), leftConst.type());
  }

  private ConstantOperand<? extends Number> getCompatibleConst(Location target, Op op) {
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

  private boolean isVarPlusEquals(BinOp binOp) {
    return binOp.left().equals(binOp.destination())
        && binOp.operator() == TokenType.PLUS
        && binOp.right().isConstant()
        && binOp.right().type().isIntegral();
  }

  private boolean isVarMinusEquals(BinOp binOp) {
    return binOp.left().equals(binOp.destination())
        && binOp.operator() == TokenType.MINUS
        && binOp.right().isConstant()
        && binOp.right().type().isIntegral();
  }
}
