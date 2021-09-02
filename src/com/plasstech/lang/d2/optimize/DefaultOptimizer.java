package com.plasstech.lang.d2.optimize;

abstract class DefaultOptimizer implements Optimizer {

  private boolean changed;

  void setChanged(boolean changed) {
    this.changed = changed;
  }

  @Override
  public boolean isChanged() {
    return changed;
  }
}
