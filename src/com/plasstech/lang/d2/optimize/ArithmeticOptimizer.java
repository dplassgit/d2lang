package com.plasstech.lang.d2.optimize;

import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;

import com.google.common.base.Objects;
import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.common.D2RuntimeException;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.VarType;

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
                new Transfer(opcode.destination(), ConstantOperand.of(valueString.length())));
          } else if (value.getClass().isArray()) {
            Object[] array = (Object[]) value;
            replaceCurrent(new Transfer(opcode.destination(), ConstantOperand.of(array.length)));
          }
        }
        return;
      case NOT:
        if (operand.isConstant()) {
          ConstantOperand<?> constant = (ConstantOperand<?>) operand;
          Object value = constant.value();
          if (value instanceof Boolean) {
            boolean valueBoolean = (Boolean) value;
            replaceCurrent(new Transfer(opcode.destination(), ConstantOperand.of(!valueBoolean)));
          }
        }
        return;
      case BIT_NOT:
        if (operand.isConstant()) {
          ConstantOperand<?> constant = (ConstantOperand<?>) operand;
          Object value = constant.value();
          if (value instanceof Integer) {
            Integer valueInt = (Integer) value;
            replaceCurrent(new Transfer(opcode.destination(), ConstantOperand.of(~valueInt)));
          }
        }
        return;
      case MINUS:
        if (operand.isConstant()) {
          ConstantOperand<?> constant = (ConstantOperand<?>) operand;
          Object value = constant.value();
          if (value instanceof Integer) {
            int valueInt = (Integer) value;
            replaceCurrent(new Transfer(opcode.destination(), ConstantOperand.of(-valueInt)));
          } else if (value instanceof Double) {
            double valueInt = (double) value;
            replaceCurrent(new Transfer(opcode.destination(), ConstantOperand.of(-valueInt)));
          }
        }
        return;
      case ASC:
        if (operand.isConstant()) {
          @SuppressWarnings("unchecked")
          ConstantOperand<String> constant = (ConstantOperand<String>) operand;
          String value = constant.value();
          char first = value.charAt(0);
          replaceCurrent(new Transfer(opcode.destination(), ConstantOperand.of(first)));
        }
        return;
      case CHR:
        if (operand.isConstant()) {
          @SuppressWarnings("unchecked")
          ConstantOperand<Integer> constant = (ConstantOperand<Integer>) operand;
          int value = constant.value();
          replaceCurrent(
              new Transfer(
                  opcode.destination(),
                  ConstantOperand.of(Character.valueOf((char) value).toString())));
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
        optimizeCompare(op.destination(), left, right, (a, b) -> a.compareTo(b) <= 0);
        return;

      case LT:
        optimizeCompare(op.destination(), left, right, (a, b) -> a.compareTo(b) < 0);
        return;

      case GEQ:
        optimizeCompare(op.destination(), left, right, (a, b) -> a.compareTo(b) >= 0);
        return;

      case GT:
        optimizeCompare(op.destination(), left, right, (a, b) -> a.compareTo(b) > 0);
        return;

      case LBRACKET:
        // Replace "abc"[0] with "a".
        // Only works for constant strings and constant int indexes (modulo constant propagation!)
        if (left.isConstant() && right.isConstant()) {
          ConstantOperand<?> constant = (ConstantOperand<?>) left;
          Object value = constant.value();
          if (value instanceof String) {
            ConstantOperand<Integer> indexOperand = (ConstantOperand<Integer>) right;
            int index = indexOperand.value();
            String valueString = (String) value;
            replaceCurrent(
                new Transfer(
                    op.destination(),
                    ConstantOperand.of(String.valueOf(valueString.charAt(index)))));
          }
        }
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
    } 
    if (right.equals(ConstantOperand.ZERO)) {
      // replace with destination = left
      replaceCurrent(new Transfer(op.destination(), op.left()));
      return;
    }
    if (left.equals(right)) {
      replaceCurrent(new Transfer(op.destination(), left));
      return;
    }
  }

  @Override
  public void visit(SysCall op) {
    if (op.call() != SysCall.Call.PRINT) {
      return;
    }
    Operand arg = op.arg();
    if (!arg.isConstant()) {
      return;
    }
    ConstantOperand<?> operand = (ConstantOperand<?>) arg;
    if (operand.value() instanceof String) {
      return;
    }
    if (operand.value() == null) {
      replaceCurrent(new SysCall(op.call(), ConstantOperand.of("null")));
      return;
    }
    String asString = operand.value().toString();
    replaceCurrent(new SysCall(op.call(), ConstantOperand.of(asString)));
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
    }
    if (right.equals(ConstantOperand.ZERO)) {
      // replace with destination = 0
      replaceCurrent(new Transfer(op.destination(), ConstantOperand.ZERO));
      return;
    }
    if (left.equals(right)) {
      replaceCurrent(new Transfer(op.destination(), left));
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
    if (right.equals(ConstantOperand.ONE) || right.equals(ConstantOperand.ONE_DBL)) {
      replaceCurrent(new Transfer(op.destination(), op.left()));
      return;
    }
    try {
      if (right.equals(ConstantOperand.ZERO_DBL) || right.equals(ConstantOperand.ZERO)) {
        throw new D2RuntimeException("Division by 0", op.position(), "Arithmetic");
      }
      if (optimizeArith(op.destination(), left, right, (t, u) -> t / u)) {
        return;
      }
      if (optimizeDoubleArith(op.destination(), left, right, (t, u) -> t / u)) {
        return;
      }
      if (right.isConstant() && right.type() == VarType.INT) {
        int power = powerOfTwo(right);
        if (power != 0) {
          replaceCurrent(
              new BinOp(
                  op.destination(),
                  left,
                  TokenType.SHIFT_RIGHT,
                  ConstantOperand.of(power),
                  op.position()));
          return;
        }
      }
      if (left.equals(right) && left.type() == VarType.INT) {
        // a/a = 1
        replaceCurrent(new Transfer(op.destination(), ConstantOperand.ONE));
        return;
      }
      if (left.equals(right) && left.type() == VarType.DOUBLE) {
        // a/a = 1
        replaceCurrent(new Transfer(op.destination(), ConstantOperand.ONE_DBL));
        return;
      }
    } catch (ArithmeticException e) {
      throw new D2RuntimeException("Division by 0", op.position(), "Arithmetic");
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
    if (optimizeDoubleArith(op.destination(), left, right, (t, u) -> t - u)) {
      return;
    }
    // Replace foo - -32 with foo + 32
    if (right.isConstant()) {
      ConstantOperand<?> rightConstant = (ConstantOperand<?>) right;
      if (rightConstant.value() instanceof Integer) {
        int rightval = (int) rightConstant.value();
        if (rightval < 0) {
          replaceCurrent(
              new BinOp(
                  op.destination(),
                  left,
                  TokenType.PLUS,
                  ConstantOperand.of(-rightval),
                  op.position()));
        }
      } else if (rightConstant.value() instanceof Double) {
        double rightval = (double) rightConstant.value();
        if (rightval < 0) {
          replaceCurrent(
              new BinOp(
                  op.destination(),
                  left,
                  TokenType.PLUS,
                  ConstantOperand.of(-rightval),
                  op.position()));
        }
      }
    }
    if (left.equals(ConstantOperand.ZERO)) {
      // NOTE: NOT for Doubles.
      // Replace with destination = -right
      // This may not be any better than 0-right...
      replaceCurrent(new UnaryOp(op.destination(), TokenType.MINUS, right, op.position()));
      return;
    }
    if (right.equals(ConstantOperand.ZERO) || right.equals(ConstantOperand.ZERO_DBL)) {
      // replace with destination = left
      replaceCurrent(new Transfer(op.destination(), op.left()));
      return;
    }
    if (left.equals(right) && left.type() == VarType.INT) {
      // a - a = 0
      replaceCurrent(new Transfer(op.destination(), ConstantOperand.ZERO));
      return;
    }
    if (left.equals(right) && left.type() == VarType.DOUBLE) {
      // a - a = 0.0
      replaceCurrent(new Transfer(op.destination(), ConstantOperand.ZERO_DBL));
      return;
    }
  }

  private void optimizeAdd(BinOp op, Operand left, Operand right) {
    if (left.isConstant() && right.isConstant()) {
      // May be ints, doubles or strings
      if (optimizeArith(op.destination(), left, right, (t, u) -> t + u)) {
        // Ints.
        return;
      }
      if (optimizeDoubleArith(op.destination(), left, right, (t, u) -> t + u)) {
        // Doubles.
        return;
      }
      // Strings
      ConstantOperand<?> leftConstant = (ConstantOperand<?>) left;
      if (leftConstant.value() instanceof String) {
        @SuppressWarnings("unchecked")
        ConstantOperand<String> rightConstant = (ConstantOperand<String>) right;
        replaceCurrent(
            new Transfer(
                op.destination(),
                ConstantOperand.of(leftConstant.value() + rightConstant.value())));
        return;
      }
    }
    // Replace foo + -32 with foo - 32
    if (right.isConstant() && right.type() == VarType.INT) {
      ConstantOperand<Integer> rightConstant = (ConstantOperand<Integer>) right;
      int rightval = rightConstant.value();
      if (rightval < 0) {
        replaceCurrent(
            new BinOp(
                op.destination(),
                left,
                TokenType.MINUS,
                ConstantOperand.of(-rightval),
                op.position()));
        return;
      }
    }
    if (right.isConstant() && right.type() == VarType.DOUBLE) {
      ConstantOperand<Double> rightConstant = (ConstantOperand<Double>) right;
      double rightval = rightConstant.value();
      if (rightval < 0) {
        replaceCurrent(
            new BinOp(
                op.destination(),
                left,
                TokenType.MINUS,
                ConstantOperand.of(-rightval),
                op.position()));
      }
    }

    if (left.equals(ConstantOperand.ZERO) || left.equals(ConstantOperand.ZERO_DBL)) {
      // replace with destination = right
      replaceCurrent(new Transfer(op.destination(), op.right()));
      return;
    }
    if (right.equals(ConstantOperand.ZERO) || right.equals(ConstantOperand.ZERO_DBL)) {
      // replace with destination = left
      replaceCurrent(new Transfer(op.destination(), op.left()));
      return;
    }
    // foo = a+a -> foo = a<<1
    if (left.equals(right) && left.type() == VarType.INT) {
      replaceCurrent(
          new BinOp(
              op.destination(), left, TokenType.SHIFT_LEFT, ConstantOperand.ONE, op.position()));
      return;
    }
  }

  private void optimizeMultiply(BinOp op, Operand left, Operand right) {
    if (optimizeArith(op.destination(), left, right, (t, u) -> t * u)) {
      return;
    }
    if (optimizeDoubleArith(op.destination(), left, right, (t, u) -> t * u)) {
      return;
    }
    if (left.equals(ConstantOperand.ZERO_DBL) || right.equals(ConstantOperand.ZERO_DBL)) {
      // replace with destination = 0.0
      replaceCurrent(new Transfer(op.destination(), ConstantOperand.ZERO_DBL));
      return;
    }
    if (left.equals(ConstantOperand.ZERO) || right.equals(ConstantOperand.ZERO)) {
      // replace with destination = 0
      replaceCurrent(new Transfer(op.destination(), ConstantOperand.ZERO));
      return;
    }
    if (left.equals(ConstantOperand.ONE) || left.equals(ConstantOperand.ONE_DBL)) {
      replaceCurrent(new Transfer(op.destination(), op.right()));
      return;
    }
    if (right.equals(ConstantOperand.ONE) || right.equals(ConstantOperand.ONE_DBL)) {
      replaceCurrent(new Transfer(op.destination(), op.left()));
      return;
    }
    if (left.isConstant() && left.type() == VarType.INT) {
      int power = powerOfTwo(left);
      if (power != 0) {
        replaceCurrent(
            new BinOp(
                op.destination(),
                right,
                TokenType.SHIFT_LEFT,
                ConstantOperand.of(power),
                op.position()));
        return;
      }
    }
    if (right.isConstant() && left.type() == VarType.INT) {
      int power = powerOfTwo(right);
      if (power != 0) {
        replaceCurrent(
            new BinOp(
                op.destination(),
                left,
                TokenType.SHIFT_LEFT,
                ConstantOperand.of(power),
                op.position()));
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
    }
    if (left.equals(ConstantOperand.FALSE) || right.equals(ConstantOperand.FALSE)) {
      // replace with destination = FALSE
      replaceCurrent(new Transfer(op.destination(), ConstantOperand.FALSE));
      return;
    }
    if (left.equals(ConstantOperand.TRUE)) {
      // true and right == right
      replaceCurrent(new Transfer(op.destination(), op.right()));
      return;
    }
    if (right.equals(ConstantOperand.TRUE)) {
      // left and true == left
      replaceCurrent(new Transfer(op.destination(), op.left()));
      return;
    }
  }

  // this is for BOOLEAN or.
  private void optimizeOr(BinOp op, Operand left, Operand right) {
    if (optimizeBoolArith(op.destination(), left, right, (t, u) -> t || u)) {
      return;
    }
    if (left.equals(ConstantOperand.TRUE) || right.equals(ConstantOperand.TRUE)) {
      // either one is true, it's true
      replaceCurrent(new Transfer(op.destination(), ConstantOperand.TRUE));
      return;
    }
    if (left.equals(ConstantOperand.FALSE)) {
      // false or right = right
      replaceCurrent(new Transfer(op.destination(), op.right()));
      return;
    }
    if (right.equals(ConstantOperand.FALSE)) {
      // left or false = left
      replaceCurrent(new Transfer(op.destination(), op.left()));
      return;
    }
  }

  /**
   * If both operands are constants, apply the given function to the constants and replace the
   * opcode with result. E.g., op=3<4 becomes op=true
   *
   * @return true if both operands are constants.
   */
  private boolean optimizeCompare(
      Location destination, Operand left, Operand right, BiPredicate<Comparable, Comparable> fun) {

    if (left.isConstant() && right.isConstant()) {
      ConstantOperand<?> leftConstant = (ConstantOperand<?>) left;
      ConstantOperand<?> rightConstant = (ConstantOperand<?>) right;
      Comparable leftval = (Comparable) leftConstant.value();
      Comparable rightval = (Comparable) rightConstant.value();
      replaceCurrent(new Transfer(destination, ConstantOperand.of(fun.test(leftval, rightval))));
      return true;
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

    if (left.isConstant() && right.isConstant()) {
      ConstantOperand<?> leftConstant = (ConstantOperand<?>) left;
      ConstantOperand<?> rightConstant = (ConstantOperand<?>) right;
      replaceCurrent(
          new Transfer(
              destination,
              ConstantOperand.of(fun.test(leftConstant.value(), rightConstant.value()))));
      return true;
    }
    return false;
  }

  /**
   * If both operands are constants, apply the given function to those numbers and replace the
   * opcode with the new constant. E.g., t=3+4 becomes t=7
   *
   * @return true if both operands are numeric constants.
   */
  private boolean optimizeArith(
      Location destination, Operand left, Operand right, BinaryOperator<Integer> fun) {

    if (left.isConstant() && right.isConstant()) {
      ConstantOperand<?> leftConstant = (ConstantOperand<?>) left;
      if (leftConstant.value() instanceof Integer) {
        ConstantOperand<?> rightConstant = (ConstantOperand<?>) right;
        int leftval = (int) leftConstant.value();
        int rightval = (int) rightConstant.value();
        replaceCurrent(new Transfer(destination, ConstantOperand.of(fun.apply(leftval, rightval))));
        return true;
      }
    }
    return false;
  }

  /**
   * If both operands are constant doubles, apply the given function to those booleans and replace
   * the opcode with the new constant. E.g.,  t=3.14*2.0 becomes t=6.28
   *
   * @return true if both operands are double constants.
   */
  private boolean optimizeDoubleArith(
      Location destination, Operand left, Operand right, BinaryOperator<Double> fun) {

    // TODO: use left.varType() == VarType.DOUBLE instead of the instanceof
    if (left.isConstant() && right.isConstant()) {
      ConstantOperand<?> leftConstant = (ConstantOperand<?>) left;
      if (leftConstant.value() instanceof Double) {
        ConstantOperand<Double> rightConstant = (ConstantOperand<Double>) right;
        double leftval = (double) leftConstant.value();
        double rightval = rightConstant.value();
        replaceCurrent(new Transfer(destination, ConstantOperand.of(fun.apply(leftval, rightval))));
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

    if (left.isConstant() && right.isConstant() && left.type() == VarType.BOOL) {
      ConstantOperand<Boolean> leftConstant = (ConstantOperand<Boolean>) left;
      ConstantOperand<Boolean> rightConstant = (ConstantOperand<Boolean>) right;
      boolean leftval = leftConstant.value();
      boolean rightval = rightConstant.value();
      replaceCurrent(new Transfer(destination, ConstantOperand.of(fun.apply(leftval, rightval))));
      return true;
    }
    return false;
  }
}
