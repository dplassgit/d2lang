package com.plasstech.lang.d2.codegen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Call;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.IfOp;
import com.plasstech.lang.d2.codegen.il.Nop;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Return;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;

public class ConstantPropagationOptimizer extends LineOptimizer {
  private final FluentLogger logger = FluentLogger.forEnclosingClass();

  private Visitor visitor;
  // Map from temp name to constant value
  private Map<String, ConstantOperand<?>> tempConstants = new HashMap<>();
  // Map from temp name to temp value (canonical value)
  private Map<String, TempLocation> simpleTemps = new HashMap<>();

  public ConstantPropagationOptimizer(List<Op> code) {
    super(code);
    this.visitor = new Visitor();
  }

  @Override
  public void doOptimize(Op op) {
    op.accept(visitor);
  }

  private class Visitor extends DefaultOpcodeVisitor {

    @Override
    public void visit(Transfer op) {
      Location dest = op.destination();
      Operand source = op.source();
      if (dest instanceof TempLocation && source instanceof ConstantOperand) {
        // easy case: temps are never overwritten.
        logger.atInfo().log("Potentially replacing %s with %s", dest.name(), source);
        tempConstants.put(dest.name(), (ConstantOperand<?>) source);
        replaceCurrent(new Nop());
      } else if (source instanceof TempLocation) {
        // look it up
        TempLocation sourceTemp = (TempLocation) source;
        ConstantOperand<?> replacement = tempConstants.get(sourceTemp.name());
        if (replacement != null) {
          replaceCurrent(new Transfer(dest, replacement));
        }
      }
    }

    @Override
    public void visit(UnaryOp op) {
      Operand operand = op.operand();
      if (operand instanceof TempLocation) {
        // look it up
        TempLocation sourceTemp = (TempLocation) operand;
        ConstantOperand<?> replacement = tempConstants.get(sourceTemp.name());
        if (replacement != null) {
          replaceCurrent(new UnaryOp(op.destination(), op.operator(), replacement));
        }
      }
    }

    @Override
    public void visit(IfOp op) {
      Operand operand = op.condition();
      if (operand instanceof TempLocation) {
        // look it up
        TempLocation sourceTemp = (TempLocation) operand;
        ConstantOperand<?> replacement = tempConstants.get(sourceTemp.name());
        if (replacement != null) {
          replaceCurrent(new IfOp(replacement, op.destination()));
        }
      }
    }

    @Override
    public void visit(SysCall op) {
      Operand operand = op.arg();
      if (operand instanceof TempLocation) {
        // look it up
        TempLocation sourceTemp = (TempLocation) operand;
        ConstantOperand<?> replacement = tempConstants.get(sourceTemp.name());
        if (replacement != null) {
          replaceCurrent(new SysCall(op.call(), replacement));
        }
      }
    }

    @Override
    public void visit(Call op) {
      ImmutableList<Operand> actualParams = op.actualLocations();
      ImmutableList.Builder<Operand> replacementParams = ImmutableList.builder();
      boolean changed = false;
      for (Operand actual : actualParams) {
        if (actual instanceof TempLocation) {
          // look it up
          TempLocation sourceTemp = (TempLocation) actual;
          ConstantOperand<?> replacement = tempConstants.get(sourceTemp.name());
          if (replacement != null) {
            changed = true;
            replacementParams.add(replacement);
            continue;
          }
        }
        replacementParams.add(actual);
      }

      if (changed) {
        replaceCurrent(new Call(op.destination(), op.functionToCall(), replacementParams.build()));
      }
    }

    @Override
    public void visit(Return op) {
      if (op.returnValueLocation().isPresent()) {
        Operand returnValue = op.returnValueLocation().get();
        if (returnValue instanceof TempLocation) {
          TempLocation sourceTemp = (TempLocation) returnValue;
          ConstantOperand<?> replacement = tempConstants.get(sourceTemp.name());
          if (replacement != null) {
            replaceCurrent(new Return(replacement));
          }
        }
      }
    }

    @Override
    public void visit(BinOp op) {
      Operand left = op.left();
      if (left instanceof TempLocation) {
        // look it up
        TempLocation sourceTemp = (TempLocation) left;
        ConstantOperand<?> replacement = tempConstants.get(sourceTemp.name());
        if (replacement != null) {
          left = replacement;
        }
      }
      Operand right = op.right();
      if (right instanceof TempLocation) {
        // look it up
        TempLocation sourceTemp = (TempLocation) right;
        ConstantOperand<?> replacement = tempConstants.get(sourceTemp.name());
        if (replacement != null) {
          right = replacement;
        }
      }
      if (left != op.left() || right != op.right()) {
        replaceCurrent(new BinOp(op.destination(), left, op.operator(), right));
      }
    }
  }
}
