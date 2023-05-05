package com.plasstech.lang.d2.optimize;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;
import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.il.AllocateOp;
import com.plasstech.lang.d2.codegen.il.ArraySet;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Call;
import com.plasstech.lang.d2.codegen.il.Dec;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.FieldSetOp;
import com.plasstech.lang.d2.codegen.il.IfOp;
import com.plasstech.lang.d2.codegen.il.Inc;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Return;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.SymTab;
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
 * </pre>
 */
class LoopInvariantOptimizer extends DefaultOptimizer {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final Level loggingLevel;
  private ArrayList<Op> code;

  LoopInvariantOptimizer(int debugLevel) {
    loggingLevel = toLoggingLevel(debugLevel);
  }

  @Override
  public ImmutableList<Op> optimize(ImmutableList<Op> program, SymTab symtab) {
    code = new ArrayList<>(program);
    setChanged(false);

    // Find loop starts & ends
    List<Block> loops = new LoopFinder(code).findLoops();
    logger.at(loggingLevel).log("Loops: %s", loops);

    // Optimize each loop. This works for all loops throughout the codebase.
    for (Block loop : loops) {
      if (optimizeLoop(loop)) {
        // Stop, because the loop locations may have chnaged.
        setChanged(true);
        break;
      }
    }

    return ImmutableList.copyOf(code);
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
    boolean changed = false;
    for (int ip = loop.start(); ip < loop.end(); ++ip) {
      mover.reset();
      Op op = code.get(ip);
      op.accept(mover);
      if (mover.liftedLast()) {
        changed = true;
        logger.at(loggingLevel).log("Moving op %s from %d to %d", op, ip, loop.start());
        code.remove(ip);
        code.add(loop.start(), op);
        loop.setStart(loop.start() + 1);
      }
    }

    return changed;
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
      // Assigning to a stack variable, a non-global that was not changed in the loop.
      if (isLocalOrParam(op.destination())
          && finder.setters.count(op.operand()) == 1
          && !finder.getters.contains(op.operand())) {

        logger.at(loggingLevel).log(
            "Lifting unary assignment to %s invariant: %s", op.destination().storage(), op);
        lifted = true;
      }
    }

    private boolean isLocalOrParam(Operand location) {
      return location.storage() == SymbolStorage.LOCAL || location.storage() == SymbolStorage.PARAM;
    }

    @Override
    public void visit(BinOp op) {
      if (isLocalOrParam(op.destination())
          && finder.setters.count(op.destination()) == 1
          && !finder.getters.contains(op.destination())
          && op.operator() != TokenType.DOT) {
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
              "Lifting binary assignment to %s invariant: %s", op.destination().storage(), op);
          lifted = true;
        }
      }
    }

    @Override
    public void visit(Transfer op) {
      if (isLocalOrParam(op.destination())) {
        // Transferring to a local or param that is only set once (here)
        if (op.source().isConstant()
            && finder.setters.count(op.destination()) == 1
            && !finder.getters.contains(op.destination())) {
          // It's only set this one time and isn't read anywhere else.
          // It might be a dead assignment, but that's not our problem.
          logger.at(loggingLevel).log(
              "Lifting assignment to %s of const: %s", op.destination().storage(), op);
          lifted = true;
        } else if (op.source().storage() != SymbolStorage.GLOBAL
            && op.source().storage() != SymbolStorage.HEAP
            && finder.setters.count(op.destination()) == 1
            && !finder.setters.contains(op.source())) {
          // Not reading from a global; the only time we are set is here, and our source
          // is not set within the loop.
          logger.at(loggingLevel).log(
              "Lifting assignment to %s of invariant: %s", op.destination().storage(), op);
          lifted = true;
        }
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
        setters.add(op.destination().get().baseLocation());
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
          setters.add(op.arg());
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
    public void visit(ArraySet op) {
      if (!op.source().isConstant()) {
        getters.add(op.source());
      }
      setters.add(op.array().baseLocation());
    }

    @Override
    public void visit(FieldSetOp op) {
      if (!op.source().isConstant()) {
        getters.add(op.source());
      }
      setters.add(op.recordLocation().baseLocation());
    }

    @Override
    public void visit(AllocateOp op) {
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
