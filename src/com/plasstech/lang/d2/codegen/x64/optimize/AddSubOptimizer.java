package com.plasstech.lang.d2.codegen.x64.optimize;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;

/** When add RSP, 0x20 followed by sub RSP, 0x20 is seen, both are removed. */
class AddSubOptimizer extends Optimizer {
  @Override
  protected ImmutableList<String> doOptimize(ImmutableList<String> input) {
    List<String> code = new ArrayList<String>(input);
    for (int i = 0; i < code.size() - 1; ++i) {
      String op = code.get(i).trim();
      if (!op.startsWith("add RSP, 0x")) {
        continue;
      }
      String next = code.get(i + 1).trim();
      if (!next.startsWith("sub RSP, 0x")) {
        continue;
      }
      if (next.endsWith(op.substring(8))) {
        // kill both
        code.set(i, "  ; " + op);
        code.set(i + 1, "  ; " + next);
        setChanged(true);
      }
    }
    return ImmutableList.copyOf(code);
  }
}