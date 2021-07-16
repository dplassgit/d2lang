package com.plasstech.lang.d2.codegen;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.Op;

public class ILOptimizer implements Optimizer {
  private int debugLevel;
  private final ImmutableList<Optimizer> children;
  private boolean changed;

  public ILOptimizer() {
    this(0);
  }

  public ILOptimizer(int debugLevel) {
    this(
        ImmutableList.of(
            new ArithmeticOptimizer(debugLevel),
            new ConstantPropagationOptimizer(debugLevel),
            new DeadCodeOptimizer(debugLevel),
            new DeadLabelOptimizer(debugLevel),
            new DeadAssignmentOptimizer(debugLevel),
            new IncDecOptimizer(debugLevel),
            new InlineOptimizer(debugLevel),
            new LoopInvariantOptimizer(debugLevel) // , //
            ));
    setDebugLevel(debugLevel);
  }

  public ILOptimizer(ImmutableList<Optimizer> children) {
    this.children = children;
  }

  public ILOptimizer setDebugLevel(int debugLevel) {
    this.debugLevel = debugLevel;
    return this;
  }

  @Override
  public boolean isChanged() {
    return changed;
  }

  public void setChanged(boolean changed) {
    this.changed = changed;
  }

  @Override
  public ImmutableList<Op> optimize(ImmutableList<Op> input) {
    setChanged(false);

    ImmutableList<Op> program = ImmutableList.copyOf(input);
    int iterations = 0;

    boolean changed = false;
    do {
      changed = false;

      for (Optimizer child : children) {
        program = child.optimize(program);
        if (child.isChanged()) {
          iterations++;
          if (debugLevel == 2) {
            System.out.printf("\n%s OPTIMIZED:\n", child.getClass().getSimpleName());
            System.out.println(Joiner.on("\n").join(program));
          }
          changed = true;
          setChanged(true);
        }
      }
    } while (changed);

    if (debugLevel > 0) {
      System.err.printf("\nITERATIONS: %d\n\n", iterations);
    }

    if (debugLevel > 0) {
      System.out.println("\nOPTIMIZED:");
      System.out.println(Joiner.on("\n").join(program));
    }
    return program;
  }
}
