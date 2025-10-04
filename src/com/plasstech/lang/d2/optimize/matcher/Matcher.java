package com.plasstech.lang.d2.optimize.matcher;

import java.util.function.Function;

import com.plasstech.lang.d2.codegen.Operand;

/**
 * Operand matcher.
 */
public interface Matcher extends Function<Operand, Boolean> {
  default boolean matches(Operand op) {
    return apply(op);
  }
}