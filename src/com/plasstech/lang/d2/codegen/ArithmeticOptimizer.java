package com.plasstech.lang.d2.codegen;

import java.util.List;
import java.util.function.BiFunction;

import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Transfer;

public class ArithmeticOptimizer extends DefaultOpcodeVisitor implements Optimizer {
  private static final ConstantOperand<Integer> ZERO = new ConstantOperand<Integer>(0);
  private static final ConstantOperand<Integer> ONE = new ConstantOperand<Integer>(1);
  private static final ConstantOperand<Boolean> FALSE = new ConstantOperand<Boolean>(false);
  private static final ConstantOperand<Boolean> TRUE = new ConstantOperand<Boolean>(true);

  private final FluentLogger logger = FluentLogger.forEnclosingClass();
  private final List<Op> code;

  private boolean changed = false;
  private int ip;

  public ArithmeticOptimizer(List<Op> code) {
    this.code = code;
  }

  public void reset() {
    this.changed = false;
  }

  public boolean optimize() {
    changed = false;
    for (ip = 0; ip < code.size(); ++ip) {
      Op op = code.get(ip);
      op.accept(this);
    }
    return changed;
  }

  @Override
  public void visit(BinOp op) {
    Operand left = op.left();
    Operand right = op.right();

    switch (op.operator()) {
      case MULT:
        optimizeMult(op, left, right);
        return;

      case PLUS:
        optimizePlus(op, left, right);
        return;

      case MINUS:
        optimizeSubtract(op, left, right);
        return;

      case DIV:
        optimizeDiv(op, left, right);
        return;

      case MOD:
        optimizeMod(op, left, right);
        return;

      case AND:
        optimizeAnd(op, left, right);
        return;

      case OR:
        optimizeOr(op, left, right);
        return;

      default:
        return;
    }
  }

  private void optimizeMod(BinOp op, Operand left, Operand right) {
    optimizeArith(
        op.destination(),
        left,
        right,
        new BiFunction<Integer, Integer, Integer>() {
          @Override
          public Integer apply(Integer t, Integer u) {
            return t % u;
          }
        });
  }

  private void optimizeDiv(BinOp op, Operand left, Operand right) {
    try {
    optimizeArith(
        op.destination(),
        left,
        right,
        new BiFunction<Integer, Integer, Integer>() {
          @Override
          public Integer apply(Integer t, Integer u) {
            return t / u;
          }
        });
    } catch (ArithmeticException e) {
      logger.atWarning().log("Cannot optimize dividing by zero!");
    }
  }

  private void optimizeSubtract(BinOp op, Operand left, Operand right) {
    optimizeArith(
        op.destination(),
        left,
        right,
        new BiFunction<Integer, Integer, Integer>() {
          @Override
          public Integer apply(Integer t, Integer u) {
            return t - u;
          }
        });
  }

  private void optimizePlus(BinOp op, Operand left, Operand right) {
    if (left instanceof ConstantOperand && right instanceof ConstantOperand) {
      if (optimizeArith(
          op.destination(),
          left,
          right,
          new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer t, Integer u) {
              return t + u;
            }
          })) {
        return;
      } else {
        ConstantOperand leftConstant = (ConstantOperand) left;
        ConstantOperand rightConstant = (ConstantOperand) right;
        if (leftConstant.value() instanceof String) {
          String leftval = (String) leftConstant.value();
          String rightval = (String) rightConstant.value();
          replaceCurrent(
              new Transfer(op.destination(), new ConstantOperand<String>(leftval + rightval)));
          return;
        }
      }
    }
    if (left.equals(ZERO)) {
      // replace with destination = right
      replaceCurrent(new Transfer(op.destination(), op.right()));
      return;
    } else if (right.equals(ZERO)) {
      // replace with destination = left
      replaceCurrent(new Transfer(op.destination(), op.left()));
      return;
    }
  }

  private void optimizeMult(BinOp op, Operand left, Operand right) {
    if (optimizeArith(
        op.destination(),
        left,
        right,
        new BiFunction<Integer, Integer, Integer>() {
          @Override
          public Integer apply(Integer t, Integer u) {
            return t * u;
          }
        })) {
      return;
    } else if (left.equals(ZERO) || right.equals(ZERO)) {
      // replace with destination = 0
      replaceCurrent(new Transfer(op.destination(), ZERO));
      return;
    } else if (left.equals(ONE)) {
      replaceCurrent(new Transfer(op.destination(), op.right()));
      return;
    } else if (right.equals(ONE)) {
      replaceCurrent(new Transfer(op.destination(), op.left()));
      return;
    }
  }

  private void optimizeAnd(BinOp op, Operand left, Operand right) {
    if (optimizeBoolArith(
        op.destination(),
        left,
        right,
        new BiFunction<Boolean, Boolean, Boolean>() {
          @Override
          public Boolean apply(Boolean t, Boolean u) {
            return t && u;
          }
        })) {
      return;
    } else if (left.equals(FALSE) || right.equals(FALSE)) {
      // replace with destination = FALSE
      replaceCurrent(new Transfer(op.destination(), FALSE));
      return;
    } else if (left.equals(TRUE)) {
      replaceCurrent(new Transfer(op.destination(), op.right()));
      return;
    } else if (right.equals(TRUE)) {
      replaceCurrent(new Transfer(op.destination(), op.left()));
      return;
    }
  }

  private void optimizeOr(BinOp op, Operand left, Operand right) {
    if (optimizeBoolArith(
        op.destination(),
        left,
        right,
        new BiFunction<Boolean, Boolean, Boolean>() {
          @Override
          public Boolean apply(Boolean t, Boolean u) {
            return t || u;
          }
        })) {
      return;
    } else if (left.equals(TRUE) || right.equals(TRUE)) {
      // either one is true, it's true
      replaceCurrent(new Transfer(op.destination(), TRUE));
      return;
    } else if (left.equals(FALSE)) {
      // the other is false, use right
      replaceCurrent(new Transfer(op.destination(), op.right()));
      return;
    } else if (right.equals(FALSE)) {
      replaceCurrent(new Transfer(op.destination(), op.left()));
      return;
    }
  }

  private boolean optimizeArith(
      Location destination,
      Operand left,
      Operand right,
      BiFunction<Integer, Integer, Integer> fun) {

    if (left instanceof ConstantOperand && right instanceof ConstantOperand) {
      ConstantOperand leftConstant = (ConstantOperand) left;
      ConstantOperand rightConstant = (ConstantOperand) right;
      if (leftConstant.value() instanceof Integer) {
        int leftval = (int) leftConstant.value();
        int rightval = (int) rightConstant.value();
        replaceCurrent(
            new Transfer(destination, new ConstantOperand<Integer>(fun.apply(leftval, rightval))));
        return true;
      }
    }
    return false;
  }

  private boolean optimizeBoolArith(
      Location destination,
      Operand left,
      Operand right,
      BiFunction<Boolean, Boolean, Boolean> fun) {

    if (left instanceof ConstantOperand && right instanceof ConstantOperand) {
      ConstantOperand leftConstant = (ConstantOperand) left;
      ConstantOperand rightConstant = (ConstantOperand) right;
      if (leftConstant.value() instanceof Boolean) {
        boolean leftval = (boolean) leftConstant.value();
        boolean rightval = (boolean) rightConstant.value();
        replaceCurrent(
            new Transfer(destination, new ConstantOperand<Boolean>(fun.apply(leftval, rightval))));
        return true;
      }
    }
    return false;
  }

  private void replaceCurrent(Op newOp) {
    changed = true;
    logger.atInfo().log("Replacing ip %d: %s with %s", ip, code.get(ip), newOp);
    code.set(ip, newOp);
  }
}
