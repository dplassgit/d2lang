package com.plasstech.lang.d2.codegen.x64.optimize;

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.phase.Phase;
import com.plasstech.lang.d2.phase.State;

/**
 * Optimizes the asm code using a few simple transformations.
 */
public class NasmOptimizer implements Phase {
  private final int debugLevel;

  public NasmOptimizer() {
    this(0);
  }

  public NasmOptimizer(int debugLevel) {
    this.debugLevel = debugLevel;
  }

  private static final List<Optimizer> OPTIMIZERS = ImmutableList.of(
      new NopOptimizer(),
      new JmpOptimizer(),
      new RetJmpOptimizer(),
      new SingleLinePatternOptimizer(),
      new AddSubOptimizer(),
      new ComparisonOptimizer());

  @Override
  public State execute(State input) {
    ImmutableList<String> code = input.asmCode();
    if (debugLevel > 0) {
      System.out.printf("\nPRE-ASM-OPTIMIZED:\n");
      System.out.println(Joiner.on("\n").join(code));
      System.out.println();
    }

    boolean changed = false;
    do {
      changed = false;
      for (Optimizer child : OPTIMIZERS) {
        code = child.optimize(code);
        if (child.isChanged()) {
          if (debugLevel == 2) {
            System.out.printf("\n%s OPTIMIZED:\n", child.getClass().getSimpleName());
            System.out.println(Joiner.on("\n").join(code));
          }
        }
        changed |= child.isChanged();
      }
    } while (changed);

    if (debugLevel > 0) {
      System.out.println("------------------------------");
      System.out.println("\nOPTIMIZED ASM CODE:");
      System.out.println(Joiner.on("\n").join(code));
      System.out.println("------------------------------");
    }

    return input.addAsmCode(code);
  }
}
