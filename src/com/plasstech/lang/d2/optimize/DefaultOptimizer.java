package com.plasstech.lang.d2.optimize;

public abstract class DefaultOptimizer implements Optimizer {
  private boolean changed;

  final protected void setChanged(boolean changed) {
    this.changed = changed;
  }

  @Override
  final public boolean isChanged() {
    return changed;
  }
}
