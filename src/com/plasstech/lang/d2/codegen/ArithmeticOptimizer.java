package com.plasstech.lang.d2.codegen;

import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;

import com.google.common.base.Objects;
import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.lex.Token;

class ArithmeticOptimizer extends LineOptimizer {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  ArithmeticOptimizer(int debugLevel) {
    super(debugLevel);
  }

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
      case BIT_NOT:
        if (operand.isConstant()) {
          ConstantOperand<?> constant = (ConstantOperand<?>) operand;
          Object value = constant.value();
          if (value instanceof Integer) {
            Integer valueInt = (Integer) value;
            replaceCurrent(
                new Transfer(opcode.destination(), new ConstantOperand<Integer>(~valueInt)));
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

      case SHIFT_LEFT:
        optimizeShiftLeft(op, left, right);
        return;

      case SHIFT_RIGHT:
        optimizeShiftRight(op, left, right);
        return;

      case BIT_AND:
        optimizeBitAnd(op, left, right);
        return;

      case BIT_OR:
        optimizeBitOr(op, left, right);
        return;

      case BIT_XOR:
        optimizeArith(op.destination(), left, right, (t, u) -> t ^ u);
        return;

      case AND:
        optimizeAnd(op, left, right);
        return;

      case OR:
        optimizeOr(op, left, right);
        return;

      case XOR:
        optimizeBoolArith(op.destination(), left, right, (t, u) -> t ^ u);
        return;

      case EQEQ:
        optimizeEq(op.destination(), left, right, (a, b) -> Objects.equal(a, b));
        return;

      case NEQ:
        optimizeEq(op.destination(), left, right, (a, b) -> !Objects.equal(a, b));
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

  /** Bit or (for ints) */
  private void optimizeBitOr(BinOp op, Operand left, Operand right) {
    if (optimizeArith(op.destination(), left, right, (t, u) -> t | u)) {
      // Two constants.
      return;
    }
    // Anything | 0 = the thing
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

  private void optimizeBitAnd(BinOp op, Operand left, Operand right) {
    if (optimizeArith(op.destination(), left, right, (t, u) -> t & u)) {
      // Two constants.
      return;
    }
    // Anything & 0 = 0
    if (left.equals(ConstantOperand.ZERO)) {
      // replace with destination = 0
      replaceCurrent(new Transfer(op.destination(), ConstantOperand.ZERO));
      return;
    } else if (right.equals(ConstantOperand.ZERO)) {
      // replace with destination = 0
      replaceCurrent(new Transfer(op.destination(), ConstantOperand.ZERO));
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
      if (optimizeArith(op.destination(), left, right, (t, u) -> t / u)) {
        return;
      } else if (right.isConstant()) {
        int power = powerOfTwo(right);
        if (power != 0) {
          replaceCurrent(
              new BinOp(
                  op.destination(),
                  left,
                  Token.Type.SHIFT_RIGHT,
                  new ConstantOperand<Integer>(power)));
          return;
        }
      }
    } catch (ArithmeticException e) {
      logger.atWarning().log("Cannot optimize dividing by zero!");
    }
  }

  private void optimizeShiftLeft(BinOp op, Operand left, Operand right) {
    if (right.equals(ConstantOperand.ZERO)) {
      replaceCurrent(new Transfer(op.destination(), op.left()));
      return;
    }
    optimizeArith(op.destination(), left, right, (t, u) -> t << u);
  }

  private void optimizeShiftRight(BinOp op, Operand left, Operand right) {
    if (right.equals(ConstantOperand.ZERO)) {
      replaceCurrent(new Transfer(op.destination(), op.left()));
      return;
    }
    optimizeArith(op.destination(), left, right, (t, u) -> t >> u);
  }

  private void optimizeSubtract(BinOp op, Operand left, Operand right) {
    if (optimizeArith(op.destination(), left, right, (t, u) -> t - u)) {
      return;
    }
    // Replace foo - -32 with foo + 32
    if (right instanceof ConstantOperand) {
      ConstantOperand rightConstant = (ConstantOperand) right;
      if (rightConstant.value() instanceof Integer) {
        int rightval = (int) rightConstant.value();
        if (rightval < 0) {
          replaceCurrent(new BinOp(op.destination(), left, Token.Type.PLUS, 
                new ConstantOperand<Integer>(-rightval)));
        }
      }
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
        ConstantOperand<?> leftConstant = (ConstantOperand<?>) left;
        if (leftConstant.value() instanceof String) {
          @SuppressWarnings("unchecked")
          ConstantOperand<String> rightConstant = (ConstantOperand<String>) right;
          replaceCurrent(
              new Transfer(
                  op.destination(),
                  new ConstantOperand<String>(leftConstant.value() + rightConstant.value())));
          return;
        }
      }
    }
    // Replace foo + -32 with foo - 32
    if (right instanceof ConstantOperand) {
      ConstantOperand rightConstant = (ConstantOperand) right;
      if (rightConstant.value() instanceof Integer) {
        int rightval = (int) rightConstant.value();
        if (rightval < 0) {
          replaceCurrent(new BinOp(op.destination(), left, Token.Type.MINUS,
                new ConstantOperand<Integer>(-rightval)));
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
    } else if (left.isConstant()) {
      int power = powerOfTwo(left);
      if (power != 0) {
        replaceCurrent(
            new BinOp(
                op.destination(),
                right,
                Token.Type.SHIFT_LEFT,
                new ConstantOperand<Integer>(power)));
      }
    } else if (right.isConstant()) {
      int power = powerOfTwo(right);
      if (power != 0) {
        replaceCurrent(
            new BinOp(
                op.destination(),
                left,
                Token.Type.SHIFT_LEFT,
                new ConstantOperand<Integer>(power)));
      }
    }
  }

  private static int powerOfTwo(Operand operand) {
    @SuppressWarnings("unchecked")
    ConstantOperand<Integer> oc = (ConstantOperand<Integer>) operand;
    int value = oc.value();
    if (value < 2) {
      return 0;
    }
    int test = 1;
    int power = 0;
    do {
      if (test == value) {
        return power;
      }
      test *= 2;
      power++;
    } while (test <= value && power < 32);
    return 0;
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

  // this is for BOOLEAN or.
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
   *
   * @return true if both operands are constants.
   */
  private boolean optimizeCompare(
      Location destination, Operand left, Operand right, BiPredicate<Integer, Integer> fun) {

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
   *
   * @return true if both operands are constants.
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
   *
   * @return true if both operands are int constants.
   */
  private boolean optimizeArith(
      Location destination, Operand left, Operand right, BinaryOperator<Integer> fun) {

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
   *
   * @return true if both operands are boolean constants.
   */
  private boolean optimizeBoolArith(
      Location destination, Operand left, Operand right, BinaryOperator<Boolean> fun) {

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
