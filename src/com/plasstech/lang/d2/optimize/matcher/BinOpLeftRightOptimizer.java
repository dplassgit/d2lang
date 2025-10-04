package com.plasstech.lang.d2.optimize.matcher;

import java.util.List;
import java.util.function.Function;

import com.google.common.base.Preconditions;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.common.TokenType;

/** A, optimizer that applies if left equals right (and the extra matchers too) */
public record BinOpLeftRightOptimizer(List<TokenType> operators, Function<Op, Op> transformer,
    Matcher extraLeftMatcher, Matcher extraRightMatcher) implements OpcodeOptimizer {

  public BinOpLeftRightOptimizer(List<TokenType> operators, Function<Op, Op> transformer) {
    this(operators, transformer, Matchers.any(), Matchers.any());
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
          extraLeftMatcher.matches(binOp.left()) && extraRightMatcher.matches(binOp.right()) &&
          binOp.left().equals(binOp.right());
    }
    return false;
  }
}
