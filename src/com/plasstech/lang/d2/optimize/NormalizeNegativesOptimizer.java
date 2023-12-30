package com.plasstech.lang.d2.optimize;

import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.common.TokenType;

/**
 * Converts a = b + (-1) to a = b - 1 and a = b - (-1) to a = b + 1
 */
public class NormalizeNegativesOptimizer extends LineOptimizer {
  NormalizeNegativesOptimizer(int debugLevel) {
    super(debugLevel);
  }

  @Override
  public void visit(BinOp op) {
    if (!op.right().isConstant()) {
      // not a constant RHS
      return;
    }
    if (!op.right().type().isNumeric()) {
      // not a numeric RHS
      return;
    }

    boolean plus = op.operator() == TokenType.PLUS;
    boolean minus = op.operator() == TokenType.MINUS;
    if (!(plus || minus)) {
      return;
    }

    Number rightValue = ConstantOperand.valueFromConstOperand(op.right());
    if (op.right().type().isIntegral()) {
      long value = rightValue.longValue();
      if (value >= 0) {
        return;
      }
      replaceCurrent(new BinOp(op.destination(), op.left(),
          plus ? TokenType.MINUS : TokenType.PLUS,
          ConstantOperand.fromValue(-value, op.right().type()), op.position()));
      return;
    }
    double value = rightValue.doubleValue();
    if (value >= 0) {
      return;
    }
    replaceCurrent(new BinOp(op.destination(), op.left(),
        plus ? TokenType.MINUS : TokenType.PLUS,
        ConstantOperand.of(-value), op.position()));
  }
}
