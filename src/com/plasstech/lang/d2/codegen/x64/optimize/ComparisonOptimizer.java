package com.plasstech.lang.d2.codegen.x64.optimize;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;

/**
 * Optimizes the clunky comparison code:
 *
 * <pre>
 *  cmp DWORD [_NUM_PLANETS], 0  ; direct comparison
 *  setg BL
 *  cmp BYTE BL, 0
 *  je __loop_end_75
 * </pre>
 *
 * becomes:
 *
 * <pre>
 *  cmp DWORD [_NUM_PLANETS], 0  ; direct comparison
 *  jle __loop_end_75
 * </pre>
 */
class ComparisonOptimizer extends Optimizer {
  protected static final ImmutableBiMap<String, String> OPPOSITES =
      ImmutableBiMap.of(
          "z", "nz",
          "g", "le",
          "ge", "l");

  protected static String toOpposite(String flag) {
    String result = OPPOSITES.get(flag);
    if (result != null) {
      return result;
    }
    return OPPOSITES.inverse().get(flag);
  }

  private static final String REG = "(AL|BL|CL|DL|SIL|DIL|R[89]b|R1[0-5]b)";
  private static final ImmutableList<Pattern> PATTERNS =
      ImmutableList.of(
          Pattern.compile("^  cmp (.*)$"),
          Pattern.compile("^  set([a-z]{1,2}) " + REG + "$"),
          Pattern.compile("^  cmp BYTE " + REG + ", 0$"),
          Pattern.compile("^  j(n)?e (.*)$"));

  @Override
  ImmutableList<String> optimize(ImmutableList<String> input) {
    List<String> code = new ArrayList<>(input);
    int offset = 0;
    String setx = "";
    String reg = "";
    for (int i = 0; i < code.size(); ++i) {
      String line = code.get(i);
      Pattern pattern = PATTERNS.get(offset);
      Matcher matcher = pattern.matcher(line);
      if (matcher.matches()) {
        switch (offset) {
          case 0:
            setx = "";
            reg = "";
            offset++;
            break;

          case 1:
            setx = matcher.group(1);
            reg = matcher.group(2);
            offset++;
            break;

          case 2:
            String otherReg = matcher.group(1);
            if (!otherReg.equals(reg)) {
              // not a match, reset everything.
              setx = "";
              reg = "";
              offset = 0;
            } else {
              offset++;
            }
            break;

          case 3:
            // we got this far, we can replace everything!
            String destination = matcher.group(2);
            if ("n".equals(matcher.group(1))) {
              code.set(i, String.format("  j%s %s", setx, destination));
            } else {
              code.set(i, String.format("  j%s %s", toOpposite(setx), destination));
            }
            code.set(i - 1, ";" + code.get(i - 1));
            code.set(i - 2, ";" + code.get(i - 2));
            setChanged(true);
            setx = "";
            reg = "";
            offset = 0;
            break;
        }
      } else {
        // no match of current offset; reset everything
        offset = 0;
        setx = "";
        reg = "";
      }
    }
    return ImmutableList.copyOf(code);
  }
}
