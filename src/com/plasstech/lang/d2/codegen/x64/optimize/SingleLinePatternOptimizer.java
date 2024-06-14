package com.plasstech.lang.d2.codegen.x64.optimize;

import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Replace ADD ..., 1 with INC ... (same for sub and dec) and mov (register), 0 with xor register,
 * register, using the list of patterns.
 */
class SingleLinePatternOptimizer extends Optimizer {
  // Yeah these are a little weird, (e.g., RAL doesn't exist)
  private static final String BIG4_REGS = "([RE]?[A-D][XLH])";
  private static final String INDEX_REGS = "([RE]?[SD]IL?)";
  private static final String EXT_REGS = "([Rr][0-9][0-5]?[bwd]?)";
  private static final String REGISTER =
      String.format("(%s|%s|%s)", BIG4_REGS, INDEX_REGS, EXT_REGS);
  private static final String MODIFIER = "(BYTE|[DQ]?WORD)";
  private static final String MAYBE_MODIFIER = MODIFIER + "? ?";
  private static final ImmutableMap<String, String> PATTERNS = ImmutableMap.of(
      "^  ((add|sub|mov|cmp).*) *;.*$", "  $1", // removes trailing comments, very conservatively
      "^  add " + MAYBE_MODIFIER + REGISTER + ", 1 *$", "  inc $2",
      "^  sub " + MAYBE_MODIFIER + REGISTER + ", 1 *$", "  dec $2",
      "^  inc " + MODIFIER + " +" + REGISTER + "*$", "  inc $2",
      "^  dec " + MODIFIER + " +" + REGISTER + "*$", "  dec $2",
      "^  add (.*), 1 *$", "  inc $1",
      "^  sub (.*), 1 *$", "  dec $1",
      "^  mov " + MAYBE_MODIFIER + REGISTER + ", 0 *$", "  xor $2, $2");

  @Override
  protected ImmutableList<String> doOptimize(ImmutableList<String> input) {
    return input.stream()
        .map(line -> {
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