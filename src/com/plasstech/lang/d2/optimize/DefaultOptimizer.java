package com.plasstech.lang.d2.optimize;

abstract class DefaultOptimizer implements Optimizer {
  private boolean changed;

  final void setChanged(boolean changed) {
    this.changed = changed;
  }

  @Override
  final public boolean isChanged() {
    return changed;
  }
}
