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

  public List<Op> optimize(/* TODO: options */) {
    int iterations = 0;

    // It optimizes in-place
    ArithmeticOptimizer arithmetic = new ArithmeticOptimizer(program);
    // TODO: Do a big loop
    while (arithmetic.optimize()) {
      iterations++;
      System.out.println("\nARITHMETIC OPTIMIZED:");
      System.out.println(Joiner.on("\n").join(program));
    }
    ConstantPropagationOptimizer cpOptimizer = new ConstantPropagationOptimizer(program);
    while (cpOptimizer.optimize()) {
      iterations++;
      System.out.println("\nCONSTANT OPTIMIZED:");
      System.out.println(Joiner.on("\n").join(program));
    }
    System.err.println("Iterations: " + iterations);
    return program;
  }
}
