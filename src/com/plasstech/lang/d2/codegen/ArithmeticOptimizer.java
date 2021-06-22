package com.plasstech.lang.d2.codegen;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;

import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.lex.Token;

class ArithmeticOptimizer extends LineOptimizer {
  private final FluentLogger logger = FluentLogger.forEnclosingClass();

  @Override
  public void visit(UnaryOp opcode) {
    Operand operand = opcode.operand();
    switch (opcode.operator()) {
      case LENGTH:
        if (operand.isConstant()) {
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
      case NOT:
        if (operand.isConstant()) {
          ConstantOperand<?> constant = (ConstantOperand<?>) operand;
          Object value = constant.value();
          if (value instanceof Boolean) {
            boolean valueBoolean = (Boolean) value;
            replaceCurrent(
                new Transfer(opcode.destination(), new ConstantOperand<Boolean>(!valueBoolean)));
          }
        }
        return;
      case MINUS:
        if (operand.isConstant()) {
          ConstantOperand<?> constant = (ConstantOperand<?>) operand;
          Object value = constant.value();
          if (value instanceof Integer) {
            int valueInt = (Integer) value;
            replaceCurrent(
                new Transfer(opcode.destination(), new ConstantOperand<Integer>(-valueInt)));
          }
        }
        return;
      case ASC:
        if (operand.isConstant()) {
          ConstantOperand<String> constant = (ConstantOperand<String>) operand;
          String value = constant.value();
          char first = value.charAt(0);
          replaceCurrent(
              new Transfer(opcode.destination(), new ConstantOperand<Integer>((int) first)));
        }
        return;
      case CHR:
        if (operand.isConstant()) {
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
    if (right.equals(ConstantOperand.ONE)) {
      replaceCurrent(new Transfer(op.destination(), ConstantOperand.ZERO));
      return;
    }
    try {
      optimizeArith(op.destination(), left, right, (t, u) -> t % u);
    } catch (ArithmeticException e) {
      logger.atWarning().log("Cannot optimize mod zero!");
    }
  }

  private void optimizeDivide(BinOp op, Operand left, Operand right) {
    if (right.equals(ConstantOperand.ONE)) {
      replaceCurrent(new Transfer(op.destination(), op.left()));
      return;
    }
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
    if (left.equals(ConstantOperand.ZERO)) {
      // Replace with destination = -right
      // This may not be any better than 0-right...
      replaceCurrent(new UnaryOp(op.destination(), Token.Type.MINUS, right));
      return;
    } else if (right.equals(ConstantOperand.ZERO)) {
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
    if (left.equals(ConstantOperand.ZERO)) {
      // replace with destination = right
      replaceCurrent(new Transfer(op.destination(), op.right()));
      return;
    } else if (right.equals(ConstantOperand.ZERO)) {
      // replace with destination = left
      replaceCurrent(new Transfer(op.destination(), op.left()));
      return;
    }
  }

  private void optimizeMultiply(BinOp op, Operand left, Operand right) {
    if (optimizeArith(op.destination(), left, right, (t, u) -> t * u)) {
      return;
    } else if (left.equals(ConstantOperand.ZERO) || right.equals(ConstantOperand.ZERO)) {
      // replace with destination = 0
      replaceCurrent(new Transfer(op.destination(), ConstantOperand.ZERO));
      return;
    } else if (left.equals(ConstantOperand.ONE)) {
      replaceCurrent(new Transfer(op.destination(), op.right()));
      return;
    } else if (right.equals(ConstantOperand.ONE)) {
      replaceCurrent(new Transfer(op.destination(), op.left()));
      return;
    }
  }

  private void optimizeAnd(BinOp op, Operand left, Operand right) {
    if (optimizeBoolArith(op.destination(), left, right, (t, u) -> t && u)) {
      return;
    } else if (left.equals(ConstantOperand.FALSE) || right.equals(ConstantOperand.FALSE)) {
      // replace with destination = FALSE
      replaceCurrent(new Transfer(op.destination(), ConstantOperand.FALSE));
      return;
    } else if (left.equals(ConstantOperand.TRUE)) {
      // true and right == right
      replaceCurrent(new Transfer(op.destination(), op.right()));
      return;
    } else if (right.equals(ConstantOperand.TRUE)) {
      // left and true == left
      replaceCurrent(new Transfer(op.destination(), op.left()));
      return;
    }
  }

  private void optimizeOr(BinOp op, Operand left, Operand right) {
    if (optimizeBoolArith(op.destination(), left, right, (t, u) -> t || u)) {
      return;
    } else if (left.equals(ConstantOperand.TRUE) || right.equals(ConstantOperand.TRUE)) {
      // either one is true, it's true
      replaceCurrent(new Transfer(op.destination(), ConstantOperand.TRUE));
      return;
    } else if (left.equals(ConstantOperand.FALSE)) {
      // false or right = right
      replaceCurrent(new Transfer(op.destination(), op.right()));
      return;
    } else if (right.equals(ConstantOperand.FALSE)) {
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
      BiPredicate<Integer, Integer> fun) {

    if (left instanceof ConstantOperand && right instanceof ConstantOperand) {
      ConstantOperand leftConstant = (ConstantOperand) left;
      if (leftConstant.value() instanceof Integer) {
        ConstantOperand rightConstant = (ConstantOperand) right;
        Integer leftval = (Integer) leftConstant.value();
        Integer rightval = (Integer) rightConstant.value();
        replaceCurrent(
            new Transfer(destination, new ConstantOperand<Boolean>(fun.test(leftval, rightval))));
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
      Location destination, Operand left, Operand right, BiPredicate<Object, Object> fun) {

    if (left instanceof ConstantOperand && right instanceof ConstantOperand) {
      ConstantOperand leftConstant = (ConstantOperand) left;
      ConstantOperand rightConstant = (ConstantOperand) right;
      replaceCurrent(
          new Transfer(
              destination,
              new ConstantOperand<Boolean>(
                  fun.test(leftConstant.value(), rightConstant.value()))));
      return true;
    }
    return false;
  }

  /**
   * If both operands are constant integers, apply the given function to those ints and replace the
   * opcode with the new constant. E.g., t=3+4 becomes t=7
   */
  private boolean optimizeArith(
      Location destination,
      Operand left,
      Operand right,
      BinaryOperator<Integer> fun) {

    if (left instanceof ConstantOperand && right instanceof ConstantOperand) {
      ConstantOperand leftConstant = (ConstantOperand) left;
      if (leftConstant.value() instanceof Integer) {
        ConstantOperand rightConstant = (ConstantOperand) right;
        int leftval = (int) leftConstant.value();
        int rightval = (int) rightConstant.value();
        replaceCurrent(
            new Transfer(destination, new ConstantOperand<Integer>(fun.apply(leftval, rightval))));
        return true;
      }
    }
    return false;
  }

  /**
   * If both operands are constant booleans, apply the given function to those booleans and replace
   * the opcode with the new constant. E.g., t=true or false becomes t=true
   */
  private boolean optimizeBoolArith(
      Location destination,
      Operand left,
      Operand right,
      BinaryOperator<Boolean> fun) {

    if (left instanceof ConstantOperand && right instanceof ConstantOperand) {
      ConstantOperand leftConstant = (ConstantOperand) left;
      if (leftConstant.value() instanceof Boolean) {
        ConstantOperand rightConstant = (ConstantOperand) right;
        boolean leftval = (boolean) leftConstant.value();
        boolean rightval = (boolean) rightConstant.value();
        replaceCurrent(
            new Transfer(destination, new ConstantOperand<Boolean>(fun.apply(leftval, rightval))));
        return true;
      }
    }
    return false;
  }
}
