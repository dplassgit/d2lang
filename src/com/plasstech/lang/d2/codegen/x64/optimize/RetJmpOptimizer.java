package com.plasstech.lang.d2.codegen.x64.optimize;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;

/** Remove a jmp after a ret */
class RetJmpOptimizer extends Optimizer {
  @Override
  protected ImmutableList<String> optimize(ImmutableList<String> input) {
    List<String> code = new ArrayList<>(input);
    for (int i = 0; i < code.size() - 1; ++i) {
      String op = code.get(i);
      if (!op.trim().equals("ret")) {
        continue;
      }
      String next = code.get(i + 1).trim();
      if (next.startsWith("jmp ")) {
        // kill the jmp
        code.set(i + 1, "; " + next);
        setChanged(true);
      }
    }
    return ImmutableList.copyOf(code);
  }
}