package com.plasstech.lang.d2.codegen.x64.optimize;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;

/** Remove jmp label followed by the same label */
class JmpOptimizer extends Optimizer {
  @Override
  protected ImmutableList<String> optimize(ImmutableList<String> input) {
    List<String> code = new ArrayList<String>(input);
    for (int i = 0; i < code.size() - 1; ++i) {
      String op = code.get(i);
      if (!op.trim().startsWith("jmp")) {
        continue;
      }
      String next = code.get(i + 1);
      if (!next.startsWith("_")) {
        continue;
      }
      // get the label
      String label = op.trim().substring(4) + ":";
      if (next.trim().equals(label)) {
        // kill the jmp
        code.set(i, "; " + op);
        setChanged(true);
      }
    }
    return ImmutableList.copyOf(code);
  }
}