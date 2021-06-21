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
    ConstantPropagationOptimizer cpo = new ConstantPropagationOptimizer();
    DeadCodeOptimizer dco = new DeadCodeOptimizer();
    DeadLabelOptimizer dlo = new DeadLabelOptimizer();
    do {
      changed = false;

      program = arithmetic.optimize(program);
      if (arithmetic.isChanged()) {
        iterations++;
        System.out.println("\nARITHMETIC OPTIMIZED:");
        System.out.println(Joiner.on("\n").join(program));
        changed = true;
      }

      program = cpo.optimize(program);
      if (cpo.isChanged()) {
        iterations++;
        System.out.println("\nCONSTANT OPTIMIZED:");
        System.out.println(Joiner.on("\n").join(program));
        changed = true;
      }

      program = dco.optimize(program);
      if (dco.isChanged()) {
        iterations++;
        System.out.println("\nDEAD CODE OPTIMIZED:");
        System.out.println(Joiner.on("\n").join(program));
        changed = true;
      }

      program = dlo.optimize(program);
      if (dlo.isChanged()) {
        iterations++;
        System.out.println("\nDEAD LABEL OPTIMIZED:");
        System.out.println(Joiner.on("\n").join(program));
        changed = true;
      }
    } while (changed);
    System.err.printf("\nITERATIONS: %d\n\n", iterations);

    return program;
  }
}
