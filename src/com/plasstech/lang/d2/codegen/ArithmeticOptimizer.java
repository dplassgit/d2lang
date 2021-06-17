package com.plasstech.lang.d2.codegen;

import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.lex.Token;
import java.util.List;
import java.util.function.BiFunction;

public class ArithmeticOptimizer extends LineOptimizer {
  private static final ConstantOperand<Integer> ZERO = new ConstantOperand<Integer>(0);
  private static final ConstantOperand<Integer> ONE = new ConstantOperand<Integer>(1);
  private static final ConstantOperand<Boolean> FALSE = new ConstantOperand<Boolean>(false);
  private static final ConstantOperand<Boolean> TRUE = new ConstantOperand<Boolean>(true);

  private final FluentLogger logger = FluentLogger.forEnclosingClass();

  @Override
  public void visit(UnaryOp opcode) {
    Operand operand = opcode.operand();
    switch (opcode.operator()) {
      case LENGTH:
        if (operand instanceof ConstantOperand<?>) {
          ConstantOperand<?> constant = (ConstantOperand<?>) operand;
          Object value = constant.value();
          if (value instanceof String) {
            String valueString = (String) value;
            replaceCurrent(
                new Transfer(
                    opcode.destination(), new ConstantOperand<Integer>(valueString.length())));
          } else if (value.getClass().isArray()) {
            Object[] array = (Object[]) value;
            replaceCurrent(
                new Transfer(opcode.destination(), new ConstantOperand<Integer>(array.length)));
          }
        }
        return;
      case ASC:
        if (operand instanceof ConstantOperand<?>) {
          ConstantOperand<String> constant = (ConstantOperand<String>) operand;
          String value = constant.value();
          char first = value.charAt(0);
          replaceCurrent(
              new Transfer(opcode.destination(), new ConstantOperand<Integer>((int) first)));
        }
        return;
      case CHR:
        if (operand instanceof ConstantOperand<?>) {
          ConstantOperand<Integer> constant = (ConstantOperand<Integer>) operand;
          int value = constant.value();
          replaceCurrent(
              new Transfer(
                  opcode.destination(),
                  new ConstantOperand<String>("" + Character.valueOf((char) value))));
        }
        return;
      default:
        return;
    }
  }

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
    if (optimizeArith(op.destination(), left, right, (t, u) -> t - u)) {
      return;
    }
    if (left.equals(ZERO)) {
      // Replace with destination = -right
      // This may not be any better than 0-right...
      replaceCurrent(new UnaryOp(op.destination(), Token.Type.MINUS, right));
      return;
    } else if (right.equals(ZERO)) {
      // replace with destination = left
      replaceCurrent(new Transfer(op.destination(), op.left()));
      return;
    }
  }

  private void optimizeAdd(BinOp op, Operand left, Operand right) {
    if (left instanceof ConstantOperand && right instanceof ConstantOperand) {
      // May be ints strings or arrays (?)
      if (optimizeArith(op.destination(), left, right, (t, u) -> t + u)) {
        // Ints.
        return;
      } else {
        // Strings
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
      // true and right == right
      replaceCurrent(new Transfer(op.destination(), op.right()));
      return;
    } else if (right.equals(TRUE)) {
      // left and true == left
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
      // false or right = right
      replaceCurrent(new Transfer(op.destination(), op.right()));
      return;
    } else if (right.equals(FALSE)) {
      // left or false = left
      replaceCurrent(new Transfer(op.destination(), op.left()));
      return;
    }
  }

  /**
   * If both operands are constant integers, apply the given function to the constants and replace
   * the opcode with result. E.g., op=3<4 becomes op=true
   */
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

  /**
   * If both operands are constants, apply the given function to the constants and replace the
   * opcode with result. E.g., t='a'=='b' becomes t=false
   */
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

  /**
   * If both operands are constant integers, apply the given function to those ints and replace
   * the opcode with the new constant. E.g., t=3+4 becomes t=7
   */
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

  /**
   * If both operands are constant booleans, apply the given function to those booleans and
   * replace the opcode with the new constant. E.g., t=true or false becomes t=true
   */
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
