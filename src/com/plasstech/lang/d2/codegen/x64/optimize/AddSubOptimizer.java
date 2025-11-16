package com.plasstech.lang.d2.codegen.x64.optimize;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * When we see
 *
 * <pre>
 * add/sub (register), constant
 * </pre>
 *
 * followed by
 *
 * <pre>
 * add/sub (register), constant
 * </pre>
 *
 * it's replaced by a single add/sub of the combined constant.
 */
class AddSubOptimizer extends Optimizer {
  private static final String ADD = "add";
  private static final String SUB = "sub";
  private static final String ADD_SUB =
      String.format("(%s|%s) (%s|RSP), 0x([0-9a-f][0-9a-f])", ADD, SUB, Registers.REGISTER);
  private static final Pattern ADD_SUB_PATTERN = Pattern.compile(ADD_SUB);

  @Override
  protected ImmutableList<String> doOptimize(ImmutableList<String> input) {
    setChanged(false);
    List<String> code = new ArrayList<String>(input);
    for (int i = 0; i < code.size() - 1; ++i) {
      String firstOp = code.get(i).trim();
      Matcher firstMatcher = ADD_SUB_PATTERN.matcher(firstOp);
      if (!firstMatcher.matches()) {
        // Not an add or sub
        continue;
      }

      String secondOp = code.get(i + 1).trim();
      Matcher secondMatcher = ADD_SUB_PATTERN.matcher(secondOp);
      if (!secondMatcher.matches()) {
        // Not an add or sub
        continue;
      }
      if (!firstMatcher.group(2).equals(secondMatcher.group(2))) {
        // Different registers
        continue;
      }

      // If we get here it means we have "add/sub (register), constant"
      // followed by "add/sub (register), constant" so we can proceed as such.
      boolean firstIsAdd = firstMatcher.group(1).equals(ADD);
      boolean secondIsAdd = secondMatcher.group(1).equals(ADD);
      int firstValue = Integer.parseInt(firstMatcher.group(3), 16) * (firstIsAdd ? 1 : -1);
      int secondValue = Integer.parseInt(secondMatcher.group(3), 16) * (secondIsAdd ? 1 : -1);
      int delta = firstValue + secondValue;

      code.set(i, "; " + firstOp);
      setChanged(true);
      if (delta == 0) {
        // kill both
        code.set(i + 1, "; " + secondOp);
      } else if (delta < 0) {
        // negative delta; replace the second one with the subtract
        code.set(i + 1, String.format("  sub %s, 0x%02x", firstMatcher.group(2), -delta));
      } else {
        // positive delta; replace the second one with the add
        code.set(i + 1, String.format("  add %s, 0x%02x", firstMatcher.group(2), delta));
      }
    }
    return ImmutableList.copyOf(code);
  }
}
