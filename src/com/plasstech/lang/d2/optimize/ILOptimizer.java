package com.plasstech.lang.d2.optimize;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.common.D2RuntimeException;
import com.plasstech.lang.d2.phase.Phase;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.type.SymTab;

public class ILOptimizer extends DefaultOptimizer implements Phase {
  private int debugLevel;
  private final ImmutableList<Optimizer> children;

  public ILOptimizer() {
    this(0);
  }

  public ILOptimizer(int debugLevel) {
    this(
        ImmutableList.of(
            new ConstantPropagationOptimizer(debugLevel),
            new ArithmeticOptimizer(debugLevel),
            new AdjacentArithmeticOptimizer(debugLevel),
            new PrintOptimizer(debugLevel),
            new DeadCodeOptimizer(debugLevel),
            new DeadLabelOptimizer(debugLevel),
            new DeadAssignmentOptimizer(debugLevel),
            new IncDecOptimizer(debugLevel),
            // This doesn't play well with temps that are reused
            // new InlineOptimizer(debugLevel),
            // This doesn't work with field set or array set
            // new LoopInvariantOptimizer(debugLevel),
            new NopOptimizer() // ,
            ));
    setDebugLevel(debugLevel);
  }

  public ILOptimizer(ImmutableList<Optimizer> children) {
    this.children = children;
  }

  public ILOptimizer(Optimizer child) {
    this(ImmutableList.of(child));
  }

  public ILOptimizer setDebugLevel(int debugLevel) {
    this.debugLevel = debugLevel;
    return this;
  }

  @Override
  public State execute(State input) {
    try {
      ImmutableList<Op> optimized = optimize(input.ilCode(), input.symbolTable());
      return input.addOptimizedCode(optimized);
    } catch (D2RuntimeException e) {
      return input.addException(e);
    }
  }

  @Override
  public ImmutableList<Op> optimize(ImmutableList<Op> input, SymTab symbolTable) {
    setChanged(false);

    ImmutableList<Op> program = ImmutableList.copyOf(input);
    int iterations = 0;

    boolean changed = false;
    do {
      changed = false;

      for (Optimizer child : children) {
        program = child.optimize(program, symbolTable);
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
