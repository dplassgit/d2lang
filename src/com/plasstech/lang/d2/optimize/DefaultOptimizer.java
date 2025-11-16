package com.plasstech.lang.d2.optimize;

public abstract class DefaultOptimizer implements Optimizer {
  private boolean changed;

  protected final void setChanged(boolean changed) {
    this.changed = changed;
  }

  @Override
  public final boolean isChanged() {
    return changed;
  }
}
