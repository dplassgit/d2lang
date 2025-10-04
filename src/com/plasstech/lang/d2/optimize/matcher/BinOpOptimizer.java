package com.plasstech.lang.d2.optimize.matcher;

import java.util.List;
import java.util.function.Function;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.common.TokenType;

/**
 * A matcher that only works on BinOps.
 */
public record BinOpOptimizer(Matcher leftMatcher, List<TokenType> operators,
    Matcher rightMatcher, Function<Op, Op> transformer) implements OpcodeOptimizer {

  public BinOpOptimizer(Matcher leftMatcher, TokenType operator, Matcher rightMatcher,
      Function<Op, Op> transformer) {
    this(leftMatcher, ImmutableList.of(operator), rightMatcher, transformer);
  }

  @Override
  public Op optimize(Op target) {
    Preconditions.checkState(matches(target), "Should not call transform if it does not match");
    return transformer.apply(target);
  }

  @Override
  public boolean matches(Op op) {
    if (op instanceof BinOp binOp) {
      return (operators.contains(binOp.operator()) || operators.isEmpty()) &&
          leftMatcher.matches(binOp.left()) && rightMatcher.matches(binOp.right());
    }
    return false;
  }
}
