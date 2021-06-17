package com.plasstech.lang.d2.codegen;

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.Op;

public class ILOptimizer implements Optimizer {
  private ImmutableList<Op> program;

  public ILOptimizer(List<Op> program) {
    this.program = ImmutableList.copyOf(program);
  }

  public List<Op> optimize(/* TODO: options */ ) {
    int iterations = 0;

    boolean changed = false;
    ArithmeticOptimizer arithmetic = new ArithmeticOptimizer();
    ConstantPropagationOptimizer cpOptimizer = new ConstantPropagationOptimizer();
    do {
      changed = false;
      // It optimizes in-place.
      // TODO: change optimize method to return status with changed & list of ops.
      program = arithmetic.optimize(program);
      if (arithmetic.isChanged()) {
        System.out.println("\nARITHMETIC OPTIMIZED:");
        System.out.println(Joiner.on("\n").join(program));
        changed = true;
      }
      program = cpOptimizer.optimize(program);
      if (cpOptimizer.isChanged()) {
        System.out.println("\nCONSTANT OPTIMIZED:");
        System.out.println(Joiner.on("\n").join(program));
        changed = true;
      }
      iterations++;
    } while (changed);
    System.err.println("Iterations: " + iterations);
    return program;
  }
}
