package com.plasstech.lang.d2.optimize;

import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Dec;
import com.plasstech.lang.d2.codegen.il.Inc;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.common.TokenType;

/** Optimizes i=i+1 or i=i-1 into i++ or i-- */
class IncDecOptimizer extends LineOptimizer {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  IncDecOptimizer(int debugLevel) {
    super(debugLevel);
  }

  /**
   * <pre>
   * temp1=i       // first 
   * temp2=temp1+1 // second 
   * i=temp2       // third
   * </pre>
   * 
   * becomes
   * 
   * <pre>
   * i=i+1: (also, i=i-1)
   * </pre>
   */
  @Override
  public void visit(Transfer first) {
    Op secondOp = getOpAt(ip() + 1);
    if (!(secondOp instanceof BinOp)) {
      return;
    }
    // See if it matches the pattern
    BinOp second = (BinOp) secondOp;
    Operand left = second.left();
    Operand right = second.right();
    if (!((left.isConstant() && !right.isConstant())
        || (!left.isConstant() && right.isConstant()))) {
      return;
    }
    if (!left.type().isIntegral() || !right.type().isIntegral()) {
      return;
    }
    boolean plus = second.operator() == TokenType.PLUS;
    boolean minus = second.operator() == TokenType.MINUS;
    if (!(plus || minus)) {
      return;
    }
    Op thirdOp = getOpAt(ip() + 2);
    if (!(thirdOp instanceof Transfer)) {
      return;
    }
    Transfer third = (Transfer) thirdOp;
    if (plus && left.isConstant()) { // why only plus?
      // swap them
      left = second.right();
      right = second.left();
    }
    // Make sure it matches the pattern
    if (!right.isConstant()) {
      return;
    }
    Number value = ConstantOperand.valueFromConstOperand(right);
    if ((value.equals(1) || value.equals(1L) || value.equals((byte) 1))
        && first.destination().equals(left)
        && second.destination().equals(third.source())
        && first.source().equals(third.destination())) {

      // WE HAVE ONE!
      logger.at(loggingLevel).log("Found Inc/Dec pattern at ip %d", ip());

      deleteCurrent();

      replaceAt(ip() + 2,
          plus ? new Inc(third.destination(), third.position())
              : new Dec(third.destination(), third.position()));
      deleteAt(ip() + 1);
    }
  }

  /**
   * <pre>
   * temp1=i+1 // (also for minus) 
   * i=temp1 // secondOp
   * </pre>
   * 
   * becomes
   * 
   * <pre>
   * i++
   * </pre>
   */
  @Override
  public void visit(BinOp first) {
    boolean plus = first.operator() == TokenType.PLUS;
    boolean minus = first.operator() == TokenType.MINUS;
    if (!(plus || minus)) {
      return;
    }
    Operand left = first.left();
    Operand right = first.right();
    if (!((left.isConstant() && !right.isConstant())
        || (!left.isConstant() && right.isConstant()))) {
      return;
    }
    if (!left.type().isIntegral() || !right.type().isIntegral()) {
      return;
    }
    if (trySimpleIncDec(first)) {
      return;
    }
    Op secondOp = getOpAt(ip() + 1);
    if (!(secondOp instanceof Transfer)) {
      return;
    }
    Transfer second = (Transfer) secondOp;

    if (plus && left.isConstant()) {
      // swap them
      left = first.right();
      right = first.left();
    }
    // Make sure it matches the pattern
    if (!right.isConstant()) {
      return;
    }
    Number value = ConstantOperand.valueFromConstOperand(right);
    if ((value.equals(1) || value.equals(1L) || value.equals((byte) 1))
        && first.destination().equals(second.source())
        && left.equals(second.destination())) {

      // WE HAVE ONE!
      logger.at(loggingLevel).log("Found shorter Inc/Dec pattern at ip %d", ip());

      deleteCurrent();
      replaceAt(ip() + 1,
          plus ? new Inc(second.destination(), first.position())
              : new Dec(second.destination(), first.position()));
    }
  }

  /**
   * Replaces i=i+1 with i++.
   */
  private boolean trySimpleIncDec(BinOp op) {
    if (!op.destination().equals(op.left())) {
      return false;
    }
    if (!op.right().isConstant()) {
      return false;
    }
    Operand right = op.right();
    if (!ConstantOperand.isAnyIntOne(right)) {
      return false;
    }
    // do it!
    logger.at(loggingLevel).log("Found shortest Inc/Dec pattern at ip %d", ip());
    replaceCurrent(
        (op.operator() == TokenType.PLUS)
            ? new Inc(op.destination(), op.position())
            : new Dec(op.destination(), op.position()));
    return true;
  }
}
