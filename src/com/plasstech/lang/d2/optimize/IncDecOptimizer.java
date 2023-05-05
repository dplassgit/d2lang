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
   * i=i+1: (also, i=i-1, i=i+2, i=i-2)
   *
   * temp1=i // first 
   * temp2=temp1+1 // secondOp 
   * i=temp2 // thirdOp
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
    if (plus && left.isConstant()) {
      // swap them
      left = second.right();
      right = second.left();
    }
    // Make sure it matches the pattern
    if (!right.isConstant()) {
      return;
    }
    ConstantOperand<?> delta = (ConstantOperand<?>) right;
    Object value = delta.value();
    if ((value.equals(1) || value.equals(2) || value.equals((byte) 1))
        && first.destination().equals(left)
        && second.destination().equals(third.source())
        && first.source().equals(third.destination())) {

      // WE HAVE ONE!
      logger.at(loggingLevel).log("Found Inc/Dec pattern at ip %d", ip());

      deleteCurrent();

      Inc increment = new Inc(third.destination(), third.position());
      Dec decrement = new Dec(third.destination(), third.position());
      if (value.equals(2)) {
        // +/- 2
        replaceAt(ip() + 1, plus ? increment : decrement);
      } else {
        deleteAt(ip() + 1);
      }
      replaceAt(ip() + 2, plus ? increment : decrement);
    }
  }

  /**
   * <pre>
   * temp1=i+1 // (also for minus, or 2) 
   * i=temp1 // secondOp
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
    ConstantOperand<?> delta = (ConstantOperand<?>) right;
    Object value = delta.value();
    if ((value.equals(1) || value.equals(2) || value.equals((byte) 1))
        && first.destination().equals(second.source())
        && left.equals(second.destination())) {

      // WE HAVE ONE!
      logger.at(loggingLevel).log("Found shorter Inc/Dec pattern at ip %d", ip());

      deleteCurrent();

      Inc increment = new Inc(second.destination(), first.position());
      Dec decrement = new Dec(second.destination(), first.position());
      if (value.equals(2)) {
        // +/- 2
        replaceAt(ip(), plus ? increment : decrement);
      }
      replaceAt(ip() + 1, plus ? increment : decrement);
    }
  }
}
