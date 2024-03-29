package com.plasstech.lang.d2.optimize;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
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

/**
 * Optimizes
 * 
 * <pre>
 * temp1 = 3
 * b = temp1 + 1
 * </pre>
 * 
 * into:
 * 
 * <pre>
 * b = 3 + 1
 * </pre>
 */
class ConstantPropagationOptimizer extends LineOptimizer {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  // Map from operand name to constant source (cache)
  private final Map<Location, Operand> replacements = new HashMap<>();

  // Map from location to line number where it was originally assigned (for deleting temps)
  private final Map<Location, Integer> assignmentLocations = new HashMap<>();

  ConstantPropagationOptimizer(int debugLevel) {
    super(debugLevel);
  }

  @Override
  protected void preProcess() {
    replacements.clear();
    assignmentLocations.clear();
  }

  @Override
  public void visit(Inc op) {
    Operand replacement = findReplacement(op.target(), false);
    if (replacement == null || !replacement.isConstant()) {
      replacements.remove(op.target());
      assignmentLocations.remove(op.target());
      return;
    }
    deleteSource(op.target());
    long newValue = ConstantOperand.valueFromConstOperand(replacement).longValue() + 1;
    ConstantOperand<? extends Number> newConst =
        ConstantOperand.fromValue(newValue, op.target().type());
    replacements.put(op.target(), newConst);
    assignmentLocations.put(op.target(), ip());
    // Replace with a constant of the new value
    replaceCurrent(new Transfer(op.target(), newConst, op.position()));
  }

  @Override
  public void visit(Dec op) {
    Operand replacement = findReplacement(op.target(), false);
    if (replacement == null || !replacement.isConstant()) {
      replacements.remove(op.target());
      assignmentLocations.remove(op.target());
      return;
    }
    deleteSource(op.target());
    long newValue = ConstantOperand.valueFromConstOperand(replacement).longValue() - 1;
    ConstantOperand<? extends Number> newConst =
        ConstantOperand.fromValue(newValue, op.target().type());
    replacements.put(op.target(), newConst);
    assignmentLocations.put(op.target(), ip());
    // Replace with a constant of the new value
    replaceCurrent(new Transfer(op.target(), newConst, op.position()));
  }

  @Override
  public void visit(ProcEntry op) {
    // start of scope.
    replacements.clear();
    assignmentLocations.clear();
  }

  @Override
  public void visit(ProcExit op) {
    // end of scope.
    replacements.clear();
    assignmentLocations.clear();
  }

  @Override
  public void visit(Label op) {
    // a label means potentially a loop and we can't rely on values anymore.
    replacements.clear();
    assignmentLocations.clear();
  }

  @Override
  public void visit(Goto op) {
    // a goto means potentially a loop and we can't rely on values anymore.
    replacements.clear();
    assignmentLocations.clear();
  }

  @Override
  public void visit(Transfer op) {
    Operand source = op.source();
    Location dest = op.destination();

    // Remove any old setting
    replacements.remove(dest);
    assignmentLocations.remove(dest);

    if (canCache(source)) {
      logger.at(loggingLevel).log(
          "Line %d: Caching replacement of %s %s = %s",
          ip(), dest.getClass().getSimpleName(), dest.name(), source);
      replacements.put(dest, source);
      assignmentLocations.put(dest, ip());
    }

    // Now that we cached dest=source, let's see if we can replace
    // source itself with something:
    Operand replacement = findReplacement(source, false);
    if (replacement != null) {
      replaceCurrent(new Transfer(dest, replacement, op.position()));

      // We have a NEW replacement for "dest" - "source"s' replacement, so cache that instead.
      replacements.put(dest, replacement);
      // We've used the source, so delete it (deleteSource only works on temps)
      deleteSource(source);
      // We also used its replacement, so delete IT too. (deleteSource only works on temps)
      deleteSource(replacement);
    }
  }

  // never replace a non-temp with a temp
  private static boolean canCache(Operand replacement) {
    // temp = constant: true
    // temp = variable: true
    // temp = temp: FALSE (this should never happen)
    // variable = constant: true
    // variable = variable: true (???)
    // variable = temp: FALSE
    return !replacement.isTemp();
  }

  @Override
  public void visit(UnaryOp op) {
    Operand operand = op.operand();
    Operand replacement = findReplacement(operand);
    if (replacement != null) {
      replaceCurrent(new UnaryOp(op.destination(), op.operator(), replacement, op.position()));
    }
    // This value has changed; remove any old settings
    replacements.remove(op.destination());
    assignmentLocations.remove(op.destination());
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
      replaceCurrent(new IfOp(replacement, op.destination(), op.isNot(), op.position()));
    }
    // Going into an if, we can't rely on the value of the constant anymore, maybe.
    replacements.clear();
    assignmentLocations.clear();
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
      } else {
        replacementParams.add(actual);
      }
    }

    if (changed) {
      replaceCurrent(
          new Call(
              op.destination(),
              op.procSym(),
              replacementParams.build(),
              op.formals(),
              op.position()));
    }

    // a call means potentially a change in values so we clear it all
    replacements.clear();
    assignmentLocations.clear();
  }

  @Override
  public void visit(Return op) {
    op.returnValueLocation()
        .ifPresent(
            returnValue -> {
              Operand replacement = findReplacement(returnValue);
              if (replacement != null) {
                replaceCurrent(new Return(op.procName(), replacement));
              }
            });

    // a return means potentially a change in values so we clear it all
    replacements.clear();
    assignmentLocations.clear();
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
    // This value has changed; remove any old settings
    replacements.remove(op.destination());
    assignmentLocations.remove(op.destination());
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

  private void deleteSource(Operand operand) {
    if (!operand.isTemp()) {
      return;
    }

    Integer lineNumber = assignmentLocations.get(operand);
    if (lineNumber != null) {
      deleteAt(lineNumber);
    }
  }

  private Operand findReplacement(Operand operand) {
    return findReplacement(operand, true);
  }

  /**
   * Find a replacement for the given operand in the cache.
   *
   * @return a new replacement, or null if none is found.
   */
  private Operand findReplacement(Operand operand, boolean deleteSource) {
    if (operand.isConstant()) {
      return null;
    }

    if (operand instanceof Location) {
      // look it up
      Location sourceLocation = (Location) operand;
      Operand replacement = replacements.get(sourceLocation);
      if (deleteSource && replacement != null) {
        deleteSource(operand);
      }

      return replacement;
    }
    return null;
  }
}
