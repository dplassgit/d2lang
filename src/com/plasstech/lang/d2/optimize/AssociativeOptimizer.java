package com.plasstech.lang.d2.optimize;

import java.util.Set;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.VarType;

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
public class AssociativeOptimizer extends LineOptimizer {
  private static final Set<TokenType> ASSOCIATIVE_OPERATORS =
      ImmutableSet.of(
          TokenType.AND,
          TokenType.BIT_AND,
          TokenType.BIT_OR,
          TokenType.BIT_XOR,
          TokenType.EQEQ,
          TokenType.MULT,
          TokenType.NEQ,
          TokenType.OR,
          TokenType.PLUS,
          TokenType.XOR);

  private static final ImmutableBiMap<TokenType, TokenType> COMPARATOR_OPPOSITES =
      ImmutableBiMap.<TokenType, TokenType>builder()
          .put(TokenType.LT, TokenType.GT)
          .put(TokenType.GT, TokenType.LT)
          .put(TokenType.LEQ, TokenType.GEQ)
          .put(TokenType.GEQ, TokenType.LEQ)
          .build();

  AssociativeOptimizer(int debugLevel) {
    super(debugLevel);
  }

  @Override
  public void visit(BinOp op) {
    Operand left = op.left();
    Operand right = op.right();
    TokenType operator = op.operator();

    // constant (op) non constant: swap it so the constant is on the right.
    boolean swapit = left.isConstant()
        && !right.isConstant()
        && left.type() != VarType.STRING
        && ASSOCIATIVE_OPERATORS.contains(operator);

    if (!swapit) {
      // maybe string
      swapit = left.isConstant()
          && !right.isConstant()
          && left.type() == VarType.STRING
          && (operator == TokenType.EQEQ || operator == TokenType.NEQ);
    }

    if (!swapit) {
      if (!left.isConstant()) {
        return;
      }
      operator = COMPARATOR_OPPOSITES.get(operator);
      if (operator == null) {
        // can't swap
        return;
      }
    }
    replaceCurrent(
        new BinOp(
            op.destination(), right, operator, left, op.position()));
  }
}
