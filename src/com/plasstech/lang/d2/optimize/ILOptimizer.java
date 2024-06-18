package com.plasstech.lang.d2.optimize;

import java.util.logging.Level;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.common.D2RuntimeException;
import com.plasstech.lang.d2.phase.Phase;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.type.SymbolTable;

public class ILOptimizer extends DefaultOptimizer implements Phase {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private Level loggingLevel;
  private final ImmutableList<Optimizer> children;

  public ILOptimizer(int debugLevel) {
    this(
        ImmutableList.of(
            // Always run Nop at the top, so subsequent phases don't have to worry about Nops. 
            new NopOptimizer(),
            new NormalizeNegativesOptimizer(debugLevel),
            new AssociativeOptimizer(debugLevel),
            new ConstantPropagationOptimizer(debugLevel),
            new TempPropagationOptimizer(debugLevel),
            new IncDecOptimizer(debugLevel),
            new ArithmeticOptimizer(debugLevel),
            new AdjacentIncDecOptimizer(debugLevel),
            new AdjacentArithmeticOptimizer(debugLevel),
            new AdjacentLabelOptimizer(debugLevel),
            new PrintOptimizer(debugLevel),
            new DeadProcOptimizer(debugLevel),
            new DeadCodeOptimizer(debugLevel),
            new DeadLabelOptimizer(debugLevel),
            new DeadAssignmentOptimizer(debugLevel),
            new InlineOptimizer(debugLevel),
            // This doesn't work with field set or array set
            new LoopInvariantOptimizer(debugLevel)),
        debugLevel);
  }

  public ILOptimizer(ImmutableList<Optimizer> children, int debugLevel) {
    this.children = children;
    this.loggingLevel = toLoggingLevel(debugLevel);
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
  public ImmutableList<Op> optimize(ImmutableList<Op> input, SymbolTable symbolTable) {
    setChanged(false);

    ImmutableList<Op> program = ImmutableList.copyOf(input);
    int iterations = 0;

    boolean changed = false;
    // 0=fine = 500
    // 1=config = 700
    // 2=info = 800
    if (loggingLevel.intValue() >= Level.CONFIG.intValue()) {
      System.out.printf("\nPRE-OPTIMIZED:\n");
      System.out.println(Joiner.on("\n").join(program));
      System.out.println();
    }

    try {
      do {
        changed = false;

        for (Optimizer child : children) {
          program = child.optimize(program, symbolTable);
          if (child.isChanged()) {
            iterations++;
            if (loggingLevel.intValue() >= Level.INFO.intValue()) {
              System.out.printf("\n%s OPTIMIZED:\n", child.getClass().getSimpleName());
              System.out.println(Joiner.on("\n").join(program));
            }
            changed = true;
            setChanged(true);
            break; // start from the top
          }
        }
      } while (changed);
    } finally {
      logger.at(loggingLevel).log("Iterations: %d\n", iterations);

      if (loggingLevel.intValue() > Level.FINE.intValue()) {
        System.out.println("\nFINAL (maybe) OPTIMIZED:");
        System.out.println(Joiner.on("\n").join(program));
        System.out.println();
      }
    }
    return program;
  }
}
