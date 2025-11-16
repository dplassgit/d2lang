package com.plasstech.lang.d2.optimize.matcher;

import com.plasstech.lang.d2.codegen.Operand;
import java.util.function.Function;

/** Operand matcher. */
public interface Matcher extends Function<Operand, Boolean> {
  default boolean matches(Operand op) {
    return apply(op);
  }
}
