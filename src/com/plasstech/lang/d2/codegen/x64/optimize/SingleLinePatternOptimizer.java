package com.plasstech.lang.d2.codegen.x64.optimize;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

/**
 * Replace ADD ..., 1 with INC ... (same for sub and dec) and mov (register), 0 with xor register,
 * register, using the list of patterns.
 */
class SingleLinePatternOptimizer extends Optimizer {
  // Yeah these are a little weird, (e.g., RAL doesn't exist)
  private static final String MODIFIER = "(BYTE|[DQ]?WORD)";
  private static final String MAYBE_MODIFIER = MODIFIER + "? ?";
  private static final ImmutableMap<String, String> PATTERNS =
      ImmutableMap.of(
          "^  ((add|sub|mov|cmp).*) *;.*$",
          "  $1", // removes trailing comments, very conservatively
          "^  add " + MAYBE_MODIFIER + "(" + Registers.REGISTER + "), 1 *$",
          "  inc $2",
          "^  sub " + MAYBE_MODIFIER + "(" + Registers.REGISTER + "), 1 *$",
          "  dec $2",
          "^  inc " + MODIFIER + " +" + "(" + Registers.REGISTER + ")*$",
          "  inc $2",
          "^  dec " + MODIFIER + " +" + "(" + Registers.REGISTER + ")*$",
          "  dec $2",
          "^  add (.*), 1 *$",
          "  inc $1",
          "^  sub (.*), 1 *$",
          "  dec $1",
          "^  mov " + MAYBE_MODIFIER + "(" + Registers.REGISTER + "), 0 *$",
          "  xor $2, $2");

  @Override
  protected ImmutableList<String> doOptimize(ImmutableList<String> input) {
    return input.stream()
        .map(
            line -> {
              for (Map.Entry<String, String> entry : PATTERNS.entrySet()) {
                if (line.matches(entry.getKey())) {
                  setChanged(true);
                  return line.replaceAll(entry.getKey(), entry.getValue());
                }
              }
              return line;
            })
        .collect(ImmutableList.toImmutableList());
  }
}
