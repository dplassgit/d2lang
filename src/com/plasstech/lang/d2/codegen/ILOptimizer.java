package com.plasstech.lang.d2.codegen;

import java.util.ArrayList;
import java.util.List;

import com.plasstech.lang.d2.codegen.il.Op;

public class ILOptimizer implements Optimizer {
  private final List<Op> program;

  public ILOptimizer(List<Op> program) {
    this.program = new ArrayList<>(program);
  }

  public List<Op> optimize(/* TODO: options */) {
    int iterations = 0;
    ArithmeticOptimizer arithmetic = new ArithmeticOptimizer(program);
    while (arithmetic.optimize()) {
      iterations++;
    }
    System.err.println("Iterations: " + iterations);
    return program;
  }
}
