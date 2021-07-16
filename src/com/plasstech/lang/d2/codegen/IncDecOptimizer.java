package com.plasstech.lang.d2.codegen;

import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Dec;
import com.plasstech.lang.d2.codegen.il.Inc;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.lex.Token;

/** Optimizes i=i+1 or i=i-1 into i++ or i-- */
public class IncDecOptimizer extends LineOptimizer {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  IncDecOptimizer(int debugLevel) {
    super(debugLevel);
  }

  /*
   * i=i+1: (also, i=i-1, i=i+2, i=i+2)
   *
   * temp1=i // first
   * temp2=temp1+1 // secondOp
   * i=temp2 // thirdOp
   */
  @Override
  public void visit(Transfer first) {
    Op secondOp = getOpAt(ip + 1);
    if (!(secondOp instanceof BinOp)) {
      return;
    }
    // See if it matches the pattern
    BinOp second = (BinOp) secondOp;
    if (!second.right().isConstant()) {
      return;
    }
    boolean plus = second.operator() == Token.Type.PLUS;
    boolean minus = second.operator() == Token.Type.MINUS;
    if (!(plus || minus)) {
      return;
    }
    Op thirdOp = getOpAt(ip + 2);
    if (!(thirdOp instanceof Transfer)) {
      return;
    }
    Transfer third = (Transfer) thirdOp;
    ConstantOperand<?> right = (ConstantOperand<?>) second.right();
    Object value = right.value();
    if ((value.equals(1) || value.equals(2))
        && first.destination().equals(second.left())
        && second.destination().equals(third.source())
        && first.source().equals(third.destination())) {

      // WE HAVE ONE!
      logger.at(loggingLevel).log("Found Inc/Dec pattern at ip %d", ip);

      deleteCurrent();

      Inc increment = new Inc(third.destination());
      Dec decrement = new Dec(third.destination());
      if (value.equals(1)) {
        deleteAt(ip + 1);
      } else {
        // +/- 2
        replaceAt(ip + 1, plus ? increment : decrement);
      }
      replaceAt(ip + 2, plus ? increment : decrement);
    }
  }

  /*
   * temp1=i+1 // (also for minus, or 2)
   * i=temp1 // secondOp
   */
  @Override
  public void visit(BinOp first) {
    boolean plus = first.operator() == Token.Type.PLUS;
    boolean minus = first.operator() == Token.Type.MINUS;
    if (!(plus || minus)) {
      return;
    }
    if (!first.right().isConstant()) {
      return;
    }
    Op secondOp = getOpAt(ip + 1);
    if (!(secondOp instanceof Transfer)) {
      return;
    }
    Transfer second = (Transfer) secondOp;

    // Make sure it matches the pattern
    ConstantOperand<?> delta = (ConstantOperand<?>) first.right();
    Object value = delta.value();
    if ((value.equals(1) || value.equals(2))
        && first.destination().equals(second.source())
        && first.left().equals(second.destination())) {

      // WE HAVE ONE!
      logger.at(loggingLevel).log("Found shorter Inc/Dec pattern at ip %d", ip);

      deleteCurrent();

      Inc increment = new Inc(second.destination());
      Dec decrement = new Dec(second.destination());
      if (value.equals(1)) {
        deleteAt(ip);
      } else {
        // +/- 2
        replaceAt(ip, plus ? increment : decrement);
      }
      replaceAt(ip + 1, plus ? increment : decrement);
    }
  }
}
