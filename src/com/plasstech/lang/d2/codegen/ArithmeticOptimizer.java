package com.plasstech.lang.d2.codegen;

import java.util.List;
import java.util.function.BiFunction;

import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Transfer;

public class ArithmeticOptimizer extends LineOptimizer {
  private static final ConstantOperand<Integer> ZERO = new ConstantOperand<Integer>(0);
  private static final ConstantOperand<Integer> ONE = new ConstantOperand<Integer>(1);
  private static final ConstantOperand<Boolean> FALSE = new ConstantOperand<Boolean>(false);
  private static final ConstantOperand<Boolean> TRUE = new ConstantOperand<Boolean>(true);

  private final FluentLogger logger = FluentLogger.forEnclosingClass();

  private Visitor visitor;

  public ArithmeticOptimizer(List<Op> code) {
    super(code);
    this.visitor = new Visitor();
  }

  @Override
  public void doOptimize(Op op) {
    op.accept(visitor);
  }

  private class Visitor extends DefaultOpcodeVisitor {
    @Override
    public void visit(BinOp op) {
      Operand left = op.left();
      Operand right = op.right();

      switch (op.operator()) {
        case MULT:
          optimizeMultiply(op, left, right);
          return;

        case PLUS:
          optimizeAdd(op, left, right);
          return;

        case MINUS:
          optimizeSubtract(op, left, right);
          return;

        case DIV:
          optimizeDivide(op, left, right);
          return;

        case MOD:
          optimizeModulo(op, left, right);
          return;

        case AND:
          optimizeAnd(op, left, right);
          return;

        case OR:
          optimizeOr(op, left, right);
          return;

        case EQEQ:
          optimizeEq(op.destination(), left, right, (a, b) -> a.equals(b));
          return;

        case NEQ:
          optimizeEq(op.destination(), left, right, (a, b) -> !a.equals(b));
          return;

        case LEQ:
          optimizeCompare(op.destination(), left, right, (a, b) -> a <= b);
          return;

        case LT:
          optimizeCompare(op.destination(), left, right, (a, b) -> a < b);
          return;

        case GEQ:
          optimizeCompare(op.destination(), left, right, (a, b) -> a >= b);
          return;

        case GT:
          optimizeCompare(op.destination(), left, right, (a, b) -> a > b);
          return;

        default:
          return;
      }
    }

    private void optimizeModulo(BinOp op, Operand left, Operand right) {
      optimizeArith(op.destination(), left, right, (t, u) -> t % u);
    }

    private void optimizeDivide(BinOp op, Operand left, Operand right) {
      try {
        optimizeArith(op.destination(), left, right, (t, u) -> t / u);
      } catch (ArithmeticException e) {
        logger.atWarning().log("Cannot optimize dividing by zero!");
      }
    }

    private void optimizeSubtract(BinOp op, Operand left, Operand right) {
      optimizeArith(op.destination(), left, right, (t, u) -> t - u);
    }

    private void optimizeAdd(BinOp op, Operand left, Operand right) {
      if (left instanceof ConstantOperand && right instanceof ConstantOperand) {
        if (optimizeArith(op.destination(), left, right, (t, u) -> t + u)) {
          return;
        } else {
          ConstantOperand leftConstant = (ConstantOperand) left;
          if (leftConstant.value() instanceof String) {
            ConstantOperand<String> rightConstant = (ConstantOperand<String>) right;
            replaceCurrent(
                new Transfer(
                    op.destination(),
                    new ConstantOperand<String>(leftConstant.value() + rightConstant.value())));
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

    private void optimizeMultiply(BinOp op, Operand left, Operand right) {
      if (optimizeArith(op.destination(), left, right, (t, u) -> t * u)) {
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
      if (optimizeBoolArith(op.destination(), left, right, (t, u) -> t && u)) {
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
      if (optimizeBoolArith(op.destination(), left, right, (t, u) -> t || u)) {
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

    private boolean optimizeCompare(
        Location destination,
        Operand left,
        Operand right,
        BiFunction<Integer, Integer, Boolean> fun) {

      if (left instanceof ConstantOperand && right instanceof ConstantOperand) {
        ConstantOperand leftConstant = (ConstantOperand) left;
        if (leftConstant.value() instanceof Integer) {
          ConstantOperand rightConstant = (ConstantOperand) right;
          Integer leftval = (Integer) leftConstant.value();
          Integer rightval = (Integer) rightConstant.value();
          replaceCurrent(
              new Transfer(
                  destination, new ConstantOperand<Boolean>(fun.apply(leftval, rightval))));
          return true;
        }
      }
      return false;
    }

    private boolean optimizeEq(
        Location destination,
        Operand left,
        Operand right,
        BiFunction<Object, Object, Boolean> fun) {

      if (left instanceof ConstantOperand && right instanceof ConstantOperand) {
        ConstantOperand leftConstant = (ConstantOperand) left;
        ConstantOperand rightConstant = (ConstantOperand) right;
        replaceCurrent(
            new Transfer(
                destination,
                new ConstantOperand<Boolean>(
                    fun.apply(leftConstant.value(), rightConstant.value()))));
        return true;
      }
      return false;
    }

    private boolean optimizeArith(
        Location destination,
        Operand left,
        Operand right,
        BiFunction<Integer, Integer, Integer> fun) {

      if (left instanceof ConstantOperand && right instanceof ConstantOperand) {
        ConstantOperand leftConstant = (ConstantOperand) left;
        if (leftConstant.value() instanceof Integer) {
          ConstantOperand rightConstant = (ConstantOperand) right;
          int leftval = (int) leftConstant.value();
          int rightval = (int) rightConstant.value();
          replaceCurrent(
              new Transfer(
                  destination, new ConstantOperand<Integer>(fun.apply(leftval, rightval))));
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
        if (leftConstant.value() instanceof Boolean) {
          ConstantOperand rightConstant = (ConstantOperand) right;
          boolean leftval = (boolean) leftConstant.value();
          boolean rightval = (boolean) rightConstant.value();
          replaceCurrent(
              new Transfer(
                  destination, new ConstantOperand<Boolean>(fun.apply(leftval, rightval))));
          return true;
        }
      }
      return false;
    }
  }
}
