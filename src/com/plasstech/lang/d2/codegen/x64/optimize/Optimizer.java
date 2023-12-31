package com.plasstech.lang.d2.codegen.x64.optimize;

import com.google.common.collect.ImmutableList;

abstract class Optimizer {
  private boolean changed;

  final ImmutableList<String> optimize(ImmutableList<String> input) {
    ImmutableList<String> output = doOptimize(input);
    // I don't love this.
    changed = !output.equals(input);
    return output;
  }

  abstract protected ImmutableList<String> doOptimize(ImmutableList<String> program);

  boolean isChanged() {
    return changed;
  }
}
