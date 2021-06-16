package com.plasstech.lang.d2.codegen;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
import com.plasstech.lang.d2.codegen.il.Op;

public class ILOptimizer implements Optimizer {
  private final List<Op> program;

  public ILOptimizer(List<Op> program) {
    this.program = new ArrayList<>(program);
  }

  public List<Op> optimize(/* TODO: options */ ) {
    int iterations = 0;

    boolean changed = false;
    do {
      changed = false;
      // It optimizes in-place.
      // TODO: change optimize method to return status with changed & list of ops.
      ArithmeticOptimizer arithmetic = new ArithmeticOptimizer(program);
      if (arithmetic.optimize()) {
        System.out.println("\nARITHMETIC OPTIMIZED:");
        System.out.println(Joiner.on("\n").join(program));
        changed = true;
      }
      ConstantPropagationOptimizer cpOptimizer = new ConstantPropagationOptimizer(program);
      if (cpOptimizer.optimize()) {
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
