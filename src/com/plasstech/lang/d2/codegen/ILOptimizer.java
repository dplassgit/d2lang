package com.plasstech.lang.d2.codegen;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
import com.plasstech.lang.d2.codegen.il.Op;

public class ILOptimizer implements Optimizer {
  @Override
  public List<Op> optimize(List<Op> program) {
    program = new ArrayList<>(program);
    int iterations = 0;

    boolean changed = false;
    ArithmeticOptimizer arithmetic = new ArithmeticOptimizer();
    ConstantPropagationOptimizer cpOptimizer = new ConstantPropagationOptimizer();
    do {
      changed = false;
      // It optimizes in-place.
      // TODO: change optimize method to return status with changed & list of ops.
      List<Op> maybeOptimized = arithmetic.optimize(program);
      if (!maybeOptimized.equals(program)) {
        iterations++;
        program = maybeOptimized;
        System.out.println("\nARITHMETIC OPTIMIZED:");
        System.out.println(Joiner.on("\n").join(program));
        changed = true;
      }
      maybeOptimized = cpOptimizer.optimize(program);
      if (!maybeOptimized.equals(program)) {
        iterations++;
        program = maybeOptimized;
        System.out.println("\nCONSTANT OPTIMIZED:");
        System.out.println(Joiner.on("\n").join(program));
        changed = true;
      }
    } while (changed);
    System.err.println("Iterations: " + iterations);

    return program;
  }
}
