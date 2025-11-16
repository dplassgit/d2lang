package com.plasstech.lang.d2.optimize.matcher;

import com.google.common.base.Preconditions;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.common.TokenType;
import java.util.function.Function;

/** Optimizes a single UnOp opcode. */
public record UnaryOpOptimizer(
    Matcher operandMatcher, TokenType operator, Function<Op, Op> transformer)
    implements OpcodeOptimizer {

  @Override
  public Op optimize(Op target) {
    Preconditions.checkState(matches(target), "Should not call transform if it does not match");
    return transformer.apply(target);
  }

  @Override
  public boolean matches(Op op) {
    if (op instanceof UnaryOp unaryOp) {
      return operator == unaryOp.operator() && operandMatcher.matches(unaryOp.operand());
    }
    return false;
  }
}
