package com.plasstech.lang.d2.optimize;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.common.TokenType;

/**
 * Optimizes comparisons against constants so that the constant is always second.
 *
 * <pre>
 * _temp = 1 < a
 * </pre>
 *
 * into
 *
 * <pre>
 * _temp = a > 1
 * </pre>
 */
public class ComparisonOptimizer extends LineOptimizer {
  private static final BiMap<TokenType, TokenType> OPPOSITE_OPS =
      ImmutableBiMap.of(
          TokenType.EQEQ, TokenType.EQEQ, //
          TokenType.NEQ, TokenType.NEQ, //
          TokenType.LT, TokenType.GT, //
          TokenType.GEQ, TokenType.LEQ);
  private static final BiMap<TokenType, TokenType> INVERSED_OPPOSITE_OPS = OPPOSITE_OPS.inverse();

  ComparisonOptimizer(int debugLevel) {
    super(debugLevel);
  }

  @Override
  public void visit(BinOp opcode) {
    if (!opcode.left().isConstant()) {
      return;
    }
    TokenType opposite = OPPOSITE_OPS.get(opcode.operator());
    if (opposite == null) {
      opposite = INVERSED_OPPOSITE_OPS.get(opcode.operator());
    }
    if (opposite == null) {
      return;
    }
    // fix it!
    replaceCurrent(
        new BinOp(
            opcode.destination(), opcode.right(), opposite, opcode.left(), opcode.position()));
  }
}
