package com.plasstech.lang.d2.optimize;

import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.common.TokenType;

/**
 * Converts a = b + (-1) to a = b - 1 and a =b - (-1) to a = b + 1
 */
public class NormalizeNegativesOptimizer extends LineOptimizer {
  NormalizeNegativesOptimizer(int debugLevel) {
    super(debugLevel);
  }

  @Override
  public void visit(BinOp op) {
    if (!(op.operator() == TokenType.PLUS || op.operator() == TokenType.MINUS)) {
      return;
    }
    if (!(op.right().isConstant() && op.right().type().isIntegral())) {
      return;
    }
    Number value = ConstantOperand.valueFromConstOperand(op.right());
    long valueLong = value.longValue();
    if (valueLong >= 0) {
      return;
    }
    replaceCurrent(new BinOp(op.destination(), op.left(),
        op.operator() == TokenType.PLUS ? TokenType.MINUS : TokenType.PLUS,
        ConstantOperand.fromValue(-valueLong, op.right().type()), op.position()));
  }
}
