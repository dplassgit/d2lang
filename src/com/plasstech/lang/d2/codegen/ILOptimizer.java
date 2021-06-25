package com.plasstech.lang.d2.codegen;

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.Op;

public class ILOptimizer implements Optimizer {
  private int debugLevel;

  public ILOptimizer(/* TODO: options */ ) {}

  public void setDebugLevel(int debugLevel) {
    this.debugLevel = debugLevel;
  }

  @Override
  public List<Op> optimize(List<Op> input) {
    ImmutableList<Op> program = ImmutableList.copyOf(input);
    int iterations = 0;

    boolean changed = false;
    ArithmeticOptimizer arithmetic = new ArithmeticOptimizer(debugLevel);
    ConstantPropagationOptimizer cpo = new ConstantPropagationOptimizer(debugLevel);
    DeadCodeOptimizer dco = new DeadCodeOptimizer(debugLevel);
    DeadLabelOptimizer dlo = new DeadLabelOptimizer(debugLevel);
    DeadAssignmentOptimizer dao = new DeadAssignmentOptimizer(debugLevel);
    do {
      changed = false;

      program = arithmetic.optimize(program);
      if (arithmetic.isChanged()) {
        iterations++;
        if (debugLevel > 1) {
          System.out.println("\nARITHMETIC OPTIMIZED:");
          System.out.println(Joiner.on("\n").join(program));
        }
        changed = true;
      }

      program = cpo.optimize(program);
      if (cpo.isChanged()) {
        iterations++;
        if (debugLevel > 1) {
          System.out.println("\nCONSTANT OPTIMIZED:");
          System.out.println(Joiner.on("\n").join(program));
        }
        changed = true;
      }

      program = dco.optimize(program);
      if (dco.isChanged()) {
        iterations++;
        if (debugLevel > 1) {
          System.out.println("\nDEAD CODE OPTIMIZED:");
          System.out.println(Joiner.on("\n").join(program));
        }
        changed = true;
      }

      program = dlo.optimize(program);
      if (dlo.isChanged()) {
        iterations++;
        if (debugLevel > 1) {
          System.out.println("\nDEAD LABEL OPTIMIZED:");
          System.out.println(Joiner.on("\n").join(program));
        }
        changed = true;
      }

      program = dao.optimize(program);
      if (dao.isChanged()) {
        iterations++;
        if (debugLevel > 1) {
          System.out.println("\nDEAD ASSIGNMENT OPTIMIZED:");
          System.out.println(Joiner.on("\n").join(program));
        }
        changed = true;
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
