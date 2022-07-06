package com.plasstech.lang.d2.optimize;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.StackLocation;
import com.plasstech.lang.d2.codegen.TempLocation;
import com.plasstech.lang.d2.codegen.il.ArrayAlloc;
import com.plasstech.lang.d2.codegen.il.ArraySet;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Call;
import com.plasstech.lang.d2.codegen.il.Dec;
import com.plasstech.lang.d2.codegen.il.Goto;
import com.plasstech.lang.d2.codegen.il.IfOp;
import com.plasstech.lang.d2.codegen.il.Inc;
import com.plasstech.lang.d2.codegen.il.Label;
import com.plasstech.lang.d2.codegen.il.ProcEntry;
import com.plasstech.lang.d2.codegen.il.ProcExit;
import com.plasstech.lang.d2.codegen.il.Return;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;

class ConstantPropagationOptimizer extends LineOptimizer {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  // Map from temp name to constant value
  private final Map<String, ConstantOperand<?>> tempConstants = new HashMap<>();
  // Map from stack to constant
  private final Map<String, ConstantOperand<?>> stackConstants = new HashMap<>();
  // TODO: Map from global to constant
  // private final Map<String, ConstantOperand<?>> globalConstants = new HashMap<>();

  ConstantPropagationOptimizer(int debugLevel) {
    super(debugLevel);
  }

  @Override
  protected void preProcess() {
    tempConstants.clear();
    stackConstants.clear();
  }

  @Override
  public void visit(Inc op) {
    ConstantOperand<?> replacement = findReplacementConstant(op.target());
    if (replacement != null) {
      int value = (int) replacement.value();
      stackConstants.put(op.target().name(), ConstantOperand.of(value + 1));
      logger.at(loggingLevel).log("Incremented stackConstant %s to %d", op.target(), value + 1);
    }
  }

  @Override
  public void visit(Dec op) {
    ConstantOperand<?> replacement = findReplacementConstant(op.target());
    if (replacement != null) {
      int value = (int) replacement.value();
      stackConstants.put(op.target().name(), ConstantOperand.of(value - 1));
      logger.at(loggingLevel).log("Decremented stackConstant %s to %d", op.target(), value - 1);
    }
  }

  @Override
  public void visit(ProcEntry op) {
    // start of scope.
    stackConstants.clear();
  }

  @Override
  public void visit(ProcExit op) {
    // end of scope.
    stackConstants.clear();
  }

  @Override
  public void visit(Label op) {
    // a label means potentially a loop and we can't rely on stack values anymore.
    stackConstants.clear();
  }

  @Override
  public void visit(Goto op) {
    // a goto means potentially a loop and we can't rely on stack values anymore.
    stackConstants.clear();
  }

  @Override
  public void visit(Transfer op) {
    Operand source = op.source();
    Location dest = op.destination();

    if (dest instanceof StackLocation) {
      // We're changing the value of a stack variable; remove any old setting
      stackConstants.remove(dest.name());
    }

    if (dest instanceof TempLocation && source.isConstant()) {
      // easy case: temps are never overwritten.
      logger.at(loggingLevel).log("Potentially replacing temp %s with %s", dest.name(), source);
      tempConstants.put(dest.name(), (ConstantOperand<?>) source);
      deleteCurrent();
    } else if (dest instanceof StackLocation && source.isConstant()) {
      // save it, for now.
      logger.at(loggingLevel).log(
          "Potentially replacing local/param %s with %s", dest.name(), source);
      stackConstants.put(dest.name(), (ConstantOperand<?>) source);
    } else if (!source.isConstant()) {
      ConstantOperand<?> replacement = findReplacementConstant(source);
      if (replacement != null) {
        replaceCurrent(new Transfer(dest, replacement));
      }
    }
  }

  @Override
  public void visit(UnaryOp op) {
    Operand operand = op.operand();
    ConstantOperand<?> replacement = findReplacementConstant(operand);
    if (replacement != null) {
      replaceCurrent(new UnaryOp(op.destination(), op.operator(), replacement));
    }
  }

  @Override
  public void visit(ArrayAlloc op) {
    Operand operand = op.sizeLocation();
    ConstantOperand<?> replacement = findReplacementConstant(operand);
    if (replacement != null) {
      replaceCurrent(new ArrayAlloc(op.destination(), op.arrayType(), replacement));
    }
  }

  @Override
  public void visit(IfOp op) {
    Operand operand = op.condition();
    ConstantOperand<?> replacement = findReplacementConstant(operand);
    if (replacement != null) {
      replaceCurrent(new IfOp(replacement, op.destination()));
    }
    // Going into an if, we can't rely on the value of the constant anymore, maybe.
    stackConstants.clear();
  }

  @Override
  public void visit(SysCall op) {
    Operand operand = op.arg();
    ConstantOperand<?> replacement = findReplacementConstant(operand);
    if (replacement != null) {
      replaceCurrent(new SysCall(op.call(), replacement));
    }
  }

  @Override
  public void visit(Call op) {
    ImmutableList<Operand> actualParams = op.actuals();
    ImmutableList.Builder<Operand> replacementParams = ImmutableList.builder();
    boolean changed = false;
    for (Operand actual : actualParams) {
      ConstantOperand<?> replacement = findReplacementConstant(actual);
      if (replacement != null) {
        changed = true;
        replacementParams.add(replacement);
        continue;
      }
      replacementParams.add(actual);
    }

    if (changed) {
      replaceCurrent(
          new Call(op.destination(), op.procName(), replacementParams.build(), op.formals()));
    }

    stackConstants.clear();
  }

  @Override
  public void visit(Return op) {
    if (op.returnValueLocation().isPresent()) {
      Operand returnValue = op.returnValueLocation().get();
      ConstantOperand<?> replacement = findReplacementConstant(returnValue);
      if (replacement != null) {
        replaceCurrent(new Return(op.procName(), replacement));
      }
    }
    stackConstants.clear();
  }

  @Override
  public void visit(BinOp op) {
    Operand left = op.left();
    ConstantOperand<?> replacement = findReplacementConstant(left);
    if (replacement != null) {
      left = replacement;
    }
    Operand right = op.right();
    replacement = findReplacementConstant(right);
    if (replacement != null) {
      right = replacement;
    }
    if (left != op.left() || right != op.right()) {
      replaceCurrent(new BinOp(op.destination(), left, op.operator(), right));
    }
  }

  @Override
  public void visit(ArraySet op) {
    Operand index = op.index();
    ConstantOperand<?> replacement = findReplacementConstant(index);
    if (replacement != null) {
      index = replacement;
    }
    Operand source = op.source();
    replacement = findReplacementConstant(source);
    if (replacement != null) {
      source = replacement;
    }
    if (index != op.index() || source != op.source()) {
      replaceCurrent(new ArraySet(op.arrayType(), op.destination(), index, source));
    }
  }

  private ConstantOperand<?> findReplacementConstant(Operand operand) {
    if (operand.isConstant()) {
      return null;
    }
    if (operand instanceof TempLocation) {
      // look it up
      TempLocation sourceTemp = (TempLocation) operand;
      if (tempConstants.get(sourceTemp.name()) != null) {
        return tempConstants.get(sourceTemp.name());
      }
    } else if (operand instanceof StackLocation) {
      // look it up
      StackLocation sourceTemp = (StackLocation) operand;
      if (stackConstants.get(sourceTemp.name()) != null) {
        return stackConstants.get(sourceTemp.name());
      }
    }
    return null;
  }
}
