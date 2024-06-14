package com.plasstech.lang.d2.codegen.x64.optimize;

import com.google.common.collect.ImmutableList;

abstract class Optimizer {
  private boolean changed;

  final boolean isChanged() {
    return changed;
  }

  final void setChanged(boolean changed) {
    this.changed = changed;
  }

  final ImmutableList<String> optimize(ImmutableList<String> program) {
    setChanged(false);
    return doOptimize(program);
  }

  abstract ImmutableList<String> doOptimize(ImmutableList<String> program);
}
