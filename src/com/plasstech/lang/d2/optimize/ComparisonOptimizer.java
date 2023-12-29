package com.plasstech.lang.d2.optimize;

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
  private static final ImmutableBiMap<TokenType, TokenType> OPPOSITE_OPS =
      ImmutableBiMap.<TokenType, TokenType>builder()
          .put(TokenType.EQEQ, TokenType.EQEQ)
          .put(TokenType.NEQ, TokenType.NEQ)
          .put(TokenType.LT, TokenType.GT)
          .put(TokenType.GT, TokenType.LT)
          .put(TokenType.GEQ, TokenType.LEQ)
          .put(TokenType.LEQ, TokenType.GEQ)
          .build();

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
      return;
    }
    replaceCurrent(
        new BinOp(
            opcode.destination(), opcode.right(), opposite, opcode.left(), opcode.position()));
  }
}
