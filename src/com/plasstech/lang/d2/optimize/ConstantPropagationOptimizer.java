package com.plasstech.lang.d2.optimize;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.ParamLocation;
import com.plasstech.lang.d2.codegen.StackLocation;
import com.plasstech.lang.d2.codegen.TempLocation;
import com.plasstech.lang.d2.codegen.il.ArrayAlloc;
import com.plasstech.lang.d2.codegen.il.ArraySet;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Call;
import com.plasstech.lang.d2.codegen.il.Dec;
import com.plasstech.lang.d2.codegen.il.FieldSetOp;
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

  // Map from temp name to its source value
  private final Map<String, Operand> tempAssignments = new HashMap<>();
  // Map from stack to source
  private final Map<String, Operand> stackAssignments = new HashMap<>();
  // TODO: Map from global to constant
  // private final Map<String, Operand> globalConstants = new HashMap<>();

  ConstantPropagationOptimizer(int debugLevel) {
    super(debugLevel);
  }

  @Override
  protected void preProcess() {
    tempAssignments.clear();
    stackAssignments.clear();
  }

  @Override
  public void visit(Inc op) {
    Operand replacement = findReplacement(op.target());
    if (replacement != null && replacement.isConstant()) {
      ConstantOperand<Integer> replacmentConst = (ConstantOperand<Integer>) replacement;
      int value = replacmentConst.value();
      stackAssignments.put(op.target().name(), ConstantOperand.of(value + 1));
      logger.at(loggingLevel).log("Incremented stackConstant %s to %d", op.target(), value + 1);
    }
  }

  @Override
  public void visit(Dec op) {
    Operand replacement = findReplacement(op.target());
    if (replacement != null && replacement.isConstant()) {
      ConstantOperand<Integer> replacmentConst = (ConstantOperand<Integer>) replacement;
      int value = replacmentConst.value();
      stackAssignments.put(op.target().name(), ConstantOperand.of(value + 1));
      logger.at(loggingLevel).log("Decremented stackConstant %s to %d", op.target(), value - 1);
    }
  }

  @Override
  public void visit(ProcEntry op) {
    // start of scope.
    stackAssignments.clear();
  }

  @Override
  public void visit(ProcExit op) {
    // end of scope.
    stackAssignments.clear();
  }

  @Override
  public void visit(Label op) {
    // a label means potentially a loop and we can't rely on values anymore.
    stackAssignments.clear();
  }

  @Override
  public void visit(Goto op) {
    // a goto means potentially a loop and we can't rely on stack values anymore.
    stackAssignments.clear();
  }

  @Override
  public void visit(Transfer op) {
    Operand source = op.source();
    Location dest = op.destination();

    if (dest instanceof StackLocation || dest instanceof ParamLocation) {
      // We're changing the value of a stack variable; remove any old setting
      stackAssignments.remove(dest.name());
    }

    if (dest instanceof TempLocation) {
      // temp = 1 or temp=a or temp=temp

      Operand replacement = findReplacement(source);
      if (replacement != null) {
        // the source was already replaced - use it instead.
        logger.at(loggingLevel).log(
            "Potentially replacing temp %s with const %s", dest.name(), replacement);
        tempAssignments.put(dest.name(), replacement);
        deleteCurrent();
      } else if (!(source instanceof TempLocation)) {
        // don't propagate temps
        logger.at(loggingLevel).log("Potentially replacing temp %s with value %s", dest.name(), source);
        tempAssignments.put(dest.name(), source);
        deleteCurrent();
      }
    } else if ((dest instanceof StackLocation || dest instanceof ParamLocation)
        && !(source instanceof TempLocation)) {
      // Do not propagate temps, because temps can only be read once.
      logger.at(loggingLevel).log(
          "Potentially replacing local/param %s with %s", dest.name(), source);
      stackAssignments.put(dest.name(), source);
    } else if (!source.isConstant()) {
      Operand replacement = findReplacement(source);
      if (replacement != null) {
        replaceCurrent(new Transfer(dest, replacement));
      }
    }
  }

  @Override
  public void visit(UnaryOp op) {
    Operand operand = op.operand();
    Operand replacement = findReplacement(operand);
    if (replacement != null) {
      replaceCurrent(new UnaryOp(op.destination(), op.operator(), replacement, op.position()));
    }
  }

  @Override
  public void visit(ArrayAlloc op) {
    Operand operand = op.sizeLocation();
    Operand replacement = findReplacement(operand);
    if (replacement != null) {
      replaceCurrent(new ArrayAlloc(op.destination(), op.arrayType(), replacement, null));
    }
  }

  @Override
  public void visit(IfOp op) {
    Operand operand = op.condition();
    Operand replacement = findReplacement(operand);
    if (replacement != null) {
      replaceCurrent(new IfOp(replacement, op.destination()));
    }
    // Going into an if, we can't rely on the value of the constant anymore, maybe.
    stackAssignments.clear();
  }

  @Override
  public void visit(SysCall op) {
    Operand operand = op.arg();
    Operand replacement = findReplacement(operand);
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
      Operand replacement = findReplacement(actual);
      if (replacement != null) {
        changed = true;
        replacementParams.add(replacement);
        continue;
      }
      replacementParams.add(actual);
    }

    if (changed) {
      replaceCurrent(
          new Call(op.destination(), op.procSym(), replacementParams.build(), op.formals()));
    }

    stackAssignments.clear();
  }

  @Override
  public void visit(Return op) {
    if (op.returnValueLocation().isPresent()) {
      Operand returnValue = op.returnValueLocation().get();
      Operand replacement = findReplacement(returnValue);
      if (replacement != null) {
        replaceCurrent(new Return(op.procName(), replacement));
      }
    }
    stackAssignments.clear();
  }

  @Override
  public void visit(BinOp op) {
    Operand left = op.left();
    Operand replacement = findReplacement(left);
    if (replacement != null) {
      left = replacement;
    }
    Operand right = op.right();
    replacement = findReplacement(right);
    if (replacement != null) {
      right = replacement;
    }
    if (left != op.left() || right != op.right()) {
      replaceCurrent(new BinOp(op.destination(), left, op.operator(), right, op.position()));
    }
  }

  @Override
  public void visit(ArraySet op) {
    Operand index = op.index();
    Operand replacement = findReplacement(index);
    if (replacement != null) {
      index = replacement;
    }
    Operand source = op.source();
    replacement = findReplacement(source);
    if (replacement != null) {
      source = replacement;
    }
    if (index != op.index() || source != op.source()) {
      replaceCurrent(
          new ArraySet(
              op.array(), op.arrayType(), index, source, op.isArrayLiteral(), op.position()));
    }
  }

  @Override
  public void visit(FieldSetOp op) {
    Operand source = op.source();
    Operand replacement = findReplacement(source);
    if (replacement != null) {
      replaceCurrent(
          new FieldSetOp(
              op.recordLocation(), op.recordSymbol(), op.field(), replacement, op.position()));
    }
  }

  private Operand findReplacement(Operand operand) {
    if (operand.isConstant()) {
      return null;
    }
    if (operand instanceof TempLocation) {
      // look it up
      TempLocation sourceTemp = (TempLocation) operand;
      if (tempAssignments.get(sourceTemp.name()) != null) {
        return tempAssignments.get(sourceTemp.name());
      }
    } else if (operand instanceof StackLocation || operand instanceof ParamLocation) {
      // look it up
      Location sourceTemp = (Location) operand;
      if (stackAssignments.get(sourceTemp.name()) != null) {
        return stackAssignments.get(sourceTemp.name());
      }
    }
    return null;
  }
}
