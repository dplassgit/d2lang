package com.plasstech.lang.d2.codegen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;
import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Call;
import com.plasstech.lang.d2.codegen.il.Dec;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.IfOp;
import com.plasstech.lang.d2.codegen.il.Inc;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Return;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.lex.Token;
import com.plasstech.lang.d2.type.SymbolStorage;

/**
 * If a variable is set in the loop but none of its dependencies are modified in the loop -> it's an
 * invariant.
 *
 * <pre>
 *  1. Find the next loop start & end. This is in the LoopFinder class.
 *  2. Find all vars that are set in the loop
 *  3. Find all vars that are read in the loop
 *  4. For each local var that is set, if all its dependencies are only set from temps or locals
 *    that are not set in the loop, it is invariant.
 *  5. For temps: if a temp is set to a value that is not itself set in the loop, it's invariant.
 * </pre>
 */
class LoopInvariantOptimizer implements Optimizer {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final Level loggingLevel;
  private boolean changed;
  private ArrayList<Op> code;

  LoopInvariantOptimizer(int debugLevel) {
    loggingLevel = toLoggingLevel(debugLevel);
  }

  @Override
  public ImmutableList<Op> optimize(ImmutableList<Op> program) {
    code = new ArrayList<>(program);
    changed = false;

    // Find loop starts & ends
    List<Block> loops = new LoopFinder(code).findLoops();
    logger.at(loggingLevel).log("Loops: %s", loops);

    // Optimize each loop. This works for all loops throughout the codebase.
    int iterations = 0;
    for (Block loop : loops) {
      while (optimizeLoop(loop)) {
        // OH NO the starts and ends may have moved...do we just give up? or re-start?
        iterations++;
        changed = true;
      }
    }

    logger.at(loggingLevel).log("LoopInvariant loops (heh): %d", iterations);

    return ImmutableList.copyOf(code);
  }

  @Override
  public boolean isChanged() {
    return changed;
  }

  private boolean optimizeLoop(Block loop) {
    logger.at(loggingLevel).log("Optimizing loop %s", loop);

    SetterGetterFinder finder = new SetterGetterFinder();
    for (int ip = loop.start(); ip < loop.end(); ++ip) {
      code.get(ip).accept(finder);
    }
    logger.at(loggingLevel).log("Setters = %s", finder.setters);
    logger.at(loggingLevel).log("Getters = %s", finder.getters);

    TransferMover mover = new TransferMover(finder);
    boolean optimizedLoop = false;
    for (int ip = loop.start(); ip < loop.end(); ++ip) {
      mover.reset();
      Op op = code.get(ip);
      op.accept(mover);
      if (mover.liftedLast()) {
        optimizedLoop = true;
        logger.at(loggingLevel).log("Moving op %s from %d to %d", op, ip, loop.start());
        code.remove(ip);
        code.add(loop.start(), op);
        loop.setStart(loop.start() + 1);
      }
    }

    return optimizedLoop;
  }

  private class TransferMover extends DefaultOpcodeVisitor {
    private SetterGetterFinder finder;
    private boolean lifted;

    TransferMover(SetterGetterFinder finder) {
      this.finder = finder;
    }

    boolean liftedLast() {
      return lifted;
    }

    void reset() {
      lifted = false;
    }

    @Override
    public void visit(UnaryOp op) {
      // Assigning to a temp, a non-global that was not changed in the loop.
      if (op.destination().storage() == SymbolStorage.TEMP
          && op.operand().storage() != SymbolStorage.GLOBAL
          && op.operand().storage() != SymbolStorage.HEAP
          && !finder.setters.contains(op.operand())) {

        logger.at(loggingLevel).log(
            "Lifting unary assignment to temp of non-global invariant: %s", op);
        lifted = true;
      }
    }

    @Override
    public void visit(BinOp op) {
      // why only storing in temp?!

      if (op.destination().storage() == SymbolStorage.TEMP && op.operator() != Token.Type.DOT) {
        // If left is not a global and its value is not set in this loop,
        // and right is not a global and its value is not set in this loop,
        // we can lift this one.
        Operand leftOp = op.left();
        boolean leftOk =
            leftOp.storage() != SymbolStorage.GLOBAL
                && leftOp.storage() != SymbolStorage.HEAP
                && !finder.setters.contains(leftOp);
        Operand rightOp = op.right();
        boolean rightOk =
            rightOp.storage() != SymbolStorage.GLOBAL
                && rightOp.storage() != SymbolStorage.HEAP
                && !finder.setters.contains(rightOp);
        if (leftOk && rightOk) {
          logger.at(loggingLevel).log(
              "Lifting binary assignment to temp of non-global invariant: %s", op);
          lifted = true;
        }
      }
    }

    @Override
    public void visit(Transfer op) {
      switch (op.destination().storage()) {
        case TEMP:
          // Transferring to a temp
          if (op.source().isConstant()) {
            // can lift
            logger.at(loggingLevel).log("Lifting to temp of const: %s", op);
            lifted = true;
          } else if (op.source().storage() != SymbolStorage.GLOBAL
              && op.source().storage() != SymbolStorage.HEAP
              && !finder.setters.contains(op.source())) {
            logger.at(loggingLevel).log(
                "Lifting assignment to temp of non-global invariant: %s", op);
            lifted = true;
          }
          break;

        case LOCAL:
        case PARAM:
          // Transferring to a local or param that is only set once (here)
          if (op.source().isConstant()
              && finder.setters.count(op.destination()) == 1
              && !finder.getters.contains(op.destination())) {
            // It's only set this one time and isn't read anywhere else.
            // It might be a dead assignment, but that's not our problem.
            logger.at(loggingLevel).log("Lifting assignment to local or param of const: %s", op);
            lifted = true;
          } else if (op.source().storage() != SymbolStorage.GLOBAL
              && op.source().storage() != SymbolStorage.HEAP
              && finder.setters.count(op.destination()) == 1
              && !finder.setters.contains(op.source())) {
            // Not reading from a global; the only time we are set is here, and our source
            // is not set within the loop.
            logger.at(loggingLevel).log(
                "Lifting assignment to local or param of invariant: %s", op);
            lifted = true;
          }
          break;

        default:
          break;
      }
    }
  }

  // Finds all the uses of an operand.
  private class SetterGetterFinder extends DefaultOpcodeVisitor {
    private Multiset<Operand> setters = HashMultiset.create();
    private Set<Operand> getters = new HashSet<>();

    @Override
    public void visit(Call op) {
      if (op.destination().isPresent()) {
        setters.add(op.destination().get());
      }
      for (Operand actual : op.actuals()) {
        if (!actual.isConstant()) {
          getters.add(actual);
          // Globals may be set in a call. This is conservative, shrug.
          setters.add(actual);
        }
      }
    }

    @Override
    public void visit(Dec op) {
      getters.add(op.target());
      setters.add(op.target());
    }

    @Override
    public void visit(Inc op) {
      getters.add(op.target());
      setters.add(op.target());
    }

    @Override
    public void visit(IfOp op) {
      if (!op.condition().isConstant()) {
        getters.add(op.condition());
      }
    }

    @Override
    public void visit(Return op) {
      if (op.returnValueLocation().isPresent()) {
        if (!op.returnValueLocation().get().isConstant()) {
          // oh this is tricky, if we're returning
          // foo.bar, then we have to ugh....
          getters.add(op.returnValueLocation().get());
        }
      }
    }

    @Override
    public void visit(SysCall op) {
      switch (op.call()) {
        case INPUT:
          setters.add(op.arg()); // is this right?!
          break;
        default:
          if (!op.arg().isConstant()) {
            getters.add(op.arg());
          }
          break;
      }
    }
    
    @Override
    public void visit(Transfer op) {
      if (!op.source().isConstant()) {
        getters.add(op.source());
      }
      setters.add(op.destination().baseLocation());
    }

    @Override
    public void visit(BinOp op) {
      if (!op.left().isConstant()) {
        getters.add(op.left());
      }
      if (!op.right().isConstant()) {
        getters.add(op.right());
      }
      setters.add(op.destination());
    }

    @Override
    public void visit(UnaryOp op) {
      if (!op.operand().isConstant()) {
        getters.add(op.operand());
      }
      setters.add(op.destination());
    }
  }
}
