package com.plasstech.lang.d2.codegen;

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.Op;

public class ILOptimizer implements Optimizer {
  public ILOptimizer(/* TODO: options */ ) {}

  @Override
  public List<Op> optimize(List<Op> input) {
    ImmutableList<Op> program = ImmutableList.copyOf(input);
    int iterations = 0;

    boolean changed = false;
    ArithmeticOptimizer arithmetic = new ArithmeticOptimizer();
    ConstantPropagationOptimizer cpOptimizer = new ConstantPropagationOptimizer();
    do {
      changed = false;
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
    } while (changed);
    System.err.println("Iterations: " + iterations);

    return program;
  }
}
