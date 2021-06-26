package com.plasstech.lang.d2.codegen;

import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Dec;
import com.plasstech.lang.d2.codegen.il.Inc;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.lex.Token;

public class IncDecOptimizer extends LineOptimizer {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  IncDecOptimizer(int debugLevel) {
    super(debugLevel);
  }

  @Override
  public void visit(Transfer first) {
    /*
     * temp1=i // first
     * temp2=temp1+1 // secondOp
     * i=temp2 // thirdOp
     */
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
    if (right.value().equals(1)
        && first.destination().equals(second.left())
        && second.destination().equals(third.source())
        && first.source().equals(third.destination())) {
      // WE HAVE ONE!
      logger.at(loggingLevel).log("Found Inc/Dec pattern at ip %d", ip);
      if (plus) {
        replaceCurrent(new Inc(third.destination()));
      } else {
        replaceCurrent(new Dec(third.destination()));
      }
      deleteAt(ip + 1);
      deleteAt(ip + 2);
    }
  }
}
