package com.plasstech.lang.d2.optimize;

import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;

import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.common.D2RuntimeException;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.ArrayType;
import com.plasstech.lang.d2.type.VarType;

class ArithmeticOptimizer extends LineOptimizer {
  ArithmeticOptimizer(int debugLevel) {
    super(debugLevel);
  }

  @Override
  public void visit(UnaryOp op) {
    Operand operand = op.operand();
    if (!operand.isConstant() && !operand.type().isArray()) {
      return;
    }
    switch (op.operator()) {
      case LENGTH:
        if (operand.type() == VarType.STRING) {
          String value = ConstantOperand.stringValueFromConstOperand(operand);
          replaceCurrent(
              new Transfer(op.destination(), ConstantOperand.of(value.length()), op.position()));
          return;
        }
        if (operand.type().isArray()) {
          ArrayType arrayType = (ArrayType) operand.type();
          arrayType.knownLength().ifPresent(length -> {
            replaceCurrent(
                new Transfer(op.destination(), ConstantOperand.of(length), op.position()));
          });
          return;
        }
        return;

      case NOT: {
        boolean isTrue = operand.equals(ConstantOperand.TRUE);
        replaceCurrent(new Transfer(op.destination(), ConstantOperand.of(!isTrue), op.position()));
      }
        return;

      case BIT_NOT: {
        long oldValue = ConstantOperand.valueFromConstOperand(operand).longValue();
        replaceCurrent(
            new Transfer(op.destination(),
                ConstantOperand.fromValue(~oldValue, operand.type()), op.position()));
      }
        return;

      case MINUS: {
        Number oldValue = ConstantOperand.valueFromConstOperand(operand);
        if (operand.type().isIntegral()) {
          replaceCurrent(
              new Transfer(op.destination(),
                  ConstantOperand.fromValue(-oldValue.longValue(), operand.type()), op.position()));
        } else {
          replaceCurrent(
              new Transfer(op.destination(), ConstantOperand.of(-oldValue.doubleValue()),
                  op.position()));
        }
      }
        return;

      case PLUS:
        replaceCurrent(new Transfer(op.destination(), operand, op.position()));
        return;

      case ASC: {
        String value = ConstantOperand.stringValueFromConstOperand(operand);
        char first = value.charAt(0);
        replaceCurrent(new Transfer(op.destination(), ConstantOperand.of(first), op.position()));
      }
        return;

      case CHR: {
        int oldValue = ConstantOperand.valueFromConstOperand(operand).intValue();
        int value = oldValue & 0xff;
        replaceCurrent(
            new Transfer(
                op.destination(),
                ConstantOperand.of(Character.valueOf((char) value).toString()),
                op.position()));
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
    TokenType operator = op.operator();

    switch (operator) {
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
        optimizeBitXor(op, left, right);
        return;

      case AND:
        optimizeAnd(op, left, right);
        return;

      case OR:
        optimizeOr(op, left, right);
        return;

      case XOR:
        optimizeBoolArith(op, left, right, (t, u) -> t ^ u);
        return;

      case EQEQ:
      case NEQ:
        optimizeEq(op, (a, b) -> Objects.equal(a, b) == (operator == TokenType.EQEQ));
        return;

      case LEQ:
        optimizeCompare(
            op,
            (a, b) -> ComparisonChain.start().compare(a, b, Ordering.natural().nullsFirst())
                .result() <= 0);
        return;

      case LT:
        optimizeCompare(
            op,
            (a, b) -> ComparisonChain.start().compare(a, b, Ordering.natural().nullsFirst())
                .result() < 0);
        return;

      case GEQ:
        optimizeCompare(
            op,
            (a, b) -> ComparisonChain.start().compare(a, b, Ordering.natural().nullsFirst())
                .result() >= 0);
        return;

      case GT:
        optimizeCompare(
            op,
            (a, b) -> ComparisonChain.start().compare(a, b, Ordering.natural().nullsFirst())
                .result() > 0);
        return;

      case LBRACKET:
        // Replace "abc"[0] with "a".
        // Only works for constant strings and constant int indexes (modulo constant propagation!)
        if (left.isConstant() && right.isConstant() && left.type() == VarType.STRING) {
          String value = ConstantOperand.stringValueFromConstOperand(left);
          int index = ConstantOperand.valueFromConstOperand(right).intValue();
          if (index < 0) {
            throw new D2RuntimeException(
                String.format("must be non-negative; was %d", index),
                op.position(),
                "String index");
          }
          if (index >= value.length()) {
            throw new D2RuntimeException(
                String.format(
                    "out of bounds (length %d); was %d",
                    value.length(),
                    index),
                op.position(),
                "String index");
          }
          replaceCurrent(
              new Transfer(
                  op.destination(),
                  ConstantOperand.of(String.valueOf(value.charAt(index))),
                  op.position()));
        }
        return;

      default:
        return;
    }
  }

  private void optimizeBitXor(BinOp op, Operand left, Operand right) {
    if (ConstantOperand.isAnyZero(right)) {
      // a ^ 0 == a
      // replace with destination = left
      replaceCurrent(new Transfer(op.destination(), left, op.position()));
      return;
    }
    optimizeIntegralArith(op, left, right, (t, u) -> t ^ u);
  }

  /** Bit "or" (for ints, longs or bytes.) */
  private void optimizeBitOr(BinOp op, Operand left, Operand right) {
    if (ConstantOperand.isAnyZero(right)) {
      // a | 0 == a
      // replace with destination = left
      replaceCurrent(new Transfer(op.destination(), left, op.position()));
      return;
    }
    if (left.equals(right)) {
      // a|a == a
      replaceCurrent(new Transfer(op.destination(), left, op.position()));
      return;
    }
    optimizeIntegralArith(op, left, right, (t, u) -> t | u);
  }

  private void optimizeBitAnd(BinOp op, Operand left, Operand right) {
    // a & 0 = 0. Note: don't have to test for any zero on left because
    // the associative optimizer makes sure it's on the right.
    if (ConstantOperand.isAnyZero(right)) {
      // a & 0 = 0 
      replaceCurrent(new Transfer(op.destination(), right, op.position()));
      return;
    }
    if (left.equals(right)) {
      // x&x=x
      replaceCurrent(new Transfer(op.destination(), left, op.position()));
      return;
    }
    optimizeIntegralArith(op, left, right, (t, u) -> t & u);
  }

  private void optimizeModulo(BinOp op, Operand left, Operand right) {
    if (isAnyOne(right) || left.equals(right)) {
      replaceCurrent(
          new Transfer(op.destination(), ConstantOperand.fromValue(0, left.type()), op.position()));
      return;
    }
    try {
      optimizeIntegralArith(op, left, right, (t, u) -> t % u);
    } catch (ArithmeticException e) {
      throw new D2RuntimeException("Modulo by 0", op.position(), "Arithmetic");
    }
  }

  private void optimizeDivide(BinOp op, Operand left, Operand right) {
    if (ConstantOperand.isAnyZero(left)) {
      replaceCurrent(new Transfer(op.destination(), left, op.position()));
      return;
    }
    if (ConstantOperand.isAnyZero(right)) {
      throw new D2RuntimeException("Division by 0", op.position(), "Arithmetic");
    }
    if (left.equals(right)) {
      replaceCurrent(new Transfer(op.destination(), ConstantOperand.fromValue(1, left.type()),
          op.position()));
      return;
    }
    if (isAnyOne(right)) {
      replaceCurrent(new Transfer(op.destination(), left, op.position()));
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
    try {
      if (optimizeIntegralArith(op, left, right, (t, u) -> t / u)) {
        return;
      }
      if (optimizeDoubleArith(op, left, right, (t, u) -> t / u)) {
        return;
      }
    } catch (ArithmeticException e) {
      throw new D2RuntimeException("Division by 0", op.position(), "Arithmetic");
    }
  }

  private void optimizeShiftLeft(BinOp op, Operand left, Operand right) {
    if (ConstantOperand.isAnyZero(right)) {
      replaceCurrent(new Transfer(op.destination(), left, op.position()));
      return;
    }
    optimizeIntegralArith(op, left, right, (t, u) -> t << u);
  }

  private void optimizeShiftRight(BinOp op, Operand left, Operand right) {
    if (ConstantOperand.isAnyZero(right)) {
      replaceCurrent(new Transfer(op.destination(), left, op.position()));
      return;
    }
    optimizeIntegralArith(op, left, right, (t, u) -> t >> u);
  }

  private void optimizeSubtract(BinOp op, Operand left, Operand right) {
    if (left.equals(ConstantOperand.ZERO)
        || left.equals(ConstantOperand.ZERO_BYTE)
        || left.equals(ConstantOperand.ZERO_LONG)) {
      // NOTE: NOT for Doubles.
      // Replace with destination = -right
      // This may not be any better than 0-right...
      replaceCurrent(new UnaryOp(op.destination(), TokenType.MINUS, right, op.position()));
      return;
    }
    if (ConstantOperand.isAnyZero(right)) {
      // dest = left - 0
      // replace with destination = left
      replaceCurrent(new Transfer(op.destination(), left, op.position()));
      return;
    }
    if (left.equals(right)) {
      // dest = a - a = 0
      ConstantOperand<? extends Number> zero = ConstantOperand.fromValue(0, left.type());
      replaceCurrent(new Transfer(op.destination(), zero, op.position()));
      return;
    }
    if (optimizeIntegralArith(op, left, right, (t, u) -> t - u)) {
      return;
    }
    if (optimizeDoubleArith(op, left, right, (t, u) -> t - u)) {
      return;
    }
  }

  private void optimizeAdd(BinOp op, Operand left, Operand right) {
    if (ConstantOperand.isAnyZero(right)) {
      // replace with destination = left
      replaceCurrent(new Transfer(op.destination(), left, op.position()));
      return;
    }
    if (optimizeIntegralArith(op, left, right, (t, u) -> (t + u))) {
      return;
    }
    if (optimizeDoubleArith(op, left, right, (t, u) -> t + u)) {
      return;
    }
    // foo = a+a -> foo = a<<1
    if (left.equals(right) && left.type() == VarType.INT) {
      replaceCurrent(
          new BinOp(
              op.destination(), left, TokenType.SHIFT_LEFT, ConstantOperand.ONE, op.position()));
      return;
    }
    if (left.isConstant() && right.isConstant()) {
      // Strings
      if (left.type() != VarType.STRING) {
        return;
      }
      if (right.type().isNull()) {
        throw new D2RuntimeException("Cannot add NULL to STRING", op.position(), "Null pointer");
      }
      @SuppressWarnings("unchecked")
      ConstantOperand<String> leftConstant = (ConstantOperand<String>) left;
      if (leftConstant.value() == null) {
        throw new D2RuntimeException("Cannot add NULL to STRING", op.position(), "Null pointer");
      }
      @SuppressWarnings("unchecked")
      ConstantOperand<String> rightConstant = (ConstantOperand<String>) right;
      if (rightConstant.value() == null) {
        throw new D2RuntimeException("Cannot add NULL to STRING", op.position(), "Null pointer");
      }
      replaceCurrent(
          new Transfer(
              op.destination(),
              ConstantOperand.of(leftConstant.value() + rightConstant.value()),
              op.position()));
      return;
    }

    // Replace a + "" with a
    if (left.type() == VarType.STRING) {
      if (left.isConstant()) {
        @SuppressWarnings("unchecked")
        ConstantOperand<String> leftConstant = (ConstantOperand<String>) left;
        if (left.type().isNull() || leftConstant.value() == null) {
          throw new D2RuntimeException("Cannot add NULL to STRING", op.position(), "Null pointer");
        }
        if (leftConstant.value().isEmpty()) {
          replaceCurrent(new Transfer(op.destination(), right, op.position()));
          return;
        }
      }
      if (right.isConstant()) {
        @SuppressWarnings("unchecked")
        ConstantOperand<String> rightConstant = (ConstantOperand<String>) right;
        if (right.type().isNull() || rightConstant.value() == null) {
          throw new D2RuntimeException("Cannot add NULL to STRING", op.position(), "Null pointer");
        }
        if (rightConstant.value().isEmpty()) {
          replaceCurrent(new Transfer(op.destination(), left, op.position()));
          return;
        }
      }
    }
  }

  private void optimizeMultiply(BinOp op, Operand left, Operand right) {
    if (optimizeIntegralArith(op, left, right, (t, u) -> t * u)) {
      return;
    }
    if (optimizeDoubleArith(op, left, right, (t, u) -> t * u)) {
      return;
    }

    // Don't have to compare left to zero because either:
    // 1. it's a constant, which means right is also a constant (because we already swapped)
    //    and we already took care of it above in a call to optimize*Arith
    // 2. it's not a constant, so we only need to check right
    if (ConstantOperand.isAnyZero(right)) {
      // replace with destination = 0
      replaceCurrent(new Transfer(op.destination(), right, op.position()));
      return;
    }
    // Don't have to check left for 1 because of the same reasons as zero, above.
    if (isAnyOne(right)) {
      // replace with destination = left
      replaceCurrent(new Transfer(op.destination(), left, op.position()));
      return;
    }
    // Only deal with shifting ints left. I'm lazy.
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
        return;
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
    // I'm sure there's a way to do this with logs
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
    if (right.equals(ConstantOperand.FALSE)) {
      // left and false = false
      replaceCurrent(new Transfer(op.destination(), ConstantOperand.FALSE, op.position()));
      return;
    }
    if (right.equals(ConstantOperand.TRUE)) {
      // left and true == left
      replaceCurrent(new Transfer(op.destination(), left, op.position()));
      return;
    }
    optimizeBoolArith(op, left, right, (t, u) -> t && u);
  }

  // this is for BOOLEAN or.
  private void optimizeOr(BinOp op, Operand left, Operand right) {
    if (optimizeBoolArith(op, left, right, (t, u) -> t || u)) {
      return;
    }
    if (right.equals(ConstantOperand.TRUE)) {
      // anything or true = true
      replaceCurrent(new Transfer(op.destination(), ConstantOperand.TRUE, op.position()));
      return;
    }
    if (right.equals(ConstantOperand.FALSE)) {
      // left or false = left
      replaceCurrent(new Transfer(op.destination(), left, op.position()));
      return;
    }
  }

  /**
   * If both operands are constants, apply the given function to the constants and replace the
   * opcode with result. E.g., op=3<4 becomes op=true
   *
   * @return true if both operands are constants.
   */
  private boolean optimizeCompare(BinOp op, BiPredicate<Comparable<?>, Comparable<?>> fun) {
    Location destination = op.destination();
    Operand left = op.left();
    Operand right = op.right();

    if (left.equals(right)) {
      // they're equal, so we can optimize it. If it's LEQ or GEQ, we pass TRUE, otherwise FALSE.
      replaceCurrent(
          new Transfer(
              destination,
              ConstantOperand.of(op.operator() == TokenType.LEQ || op.operator() == TokenType.GEQ
                  || op.operator() == TokenType.EQEQ),
              op.position()));
      return true;
    }
    if (left.isConstant() && right.isConstant()) {
      ConstantOperand<?> leftConstant = (ConstantOperand<?>) left;
      ConstantOperand<?> rightConstant = (ConstantOperand<?>) right;
      Comparable<?> leftval = (Comparable<?>) leftConstant.value();
      Comparable<?> rightval = (Comparable<?>) rightConstant.value();

      try {
        boolean result = fun.test(leftval, rightval);
        replaceCurrent(new Transfer(destination, ConstantOperand.of(result), op.position()));
        return true;
      } catch (NullPointerException npe) {
        throw new D2RuntimeException("Null pointer error", op.position(), "Null pointer");
      }
    }
    return false;
  }

  /**
   * If both operands are constants, apply the given function to the constants and replace the
   * opcode with result. E.g., t = 'a' == 'b' becomes t = false
   *
   * <p>
   * If both operands are not constants, apply the == function and if they pass, replace the opcode
   * with true. E.g., t=a==a becomes t=true. This does not work for t=a!=b because it still has to
   * compare at runtime the values of a and b are the same or not.
   *
   * @return true if both objects are constants or they pass fun.test
   */
  private boolean optimizeEq(BinOp op, BiPredicate<Object, Object> fun) {
    Location destination = op.destination();
    Operand left = op.left();
    Operand right = op.right();

    if (left.isConstant() && right.isConstant()) {
      ConstantOperand<?> leftConstant = (ConstantOperand<?>) left;
      ConstantOperand<?> rightConstant = (ConstantOperand<?>) right;
      replaceCurrent(
          new Transfer(
              destination,
              ConstantOperand.of(fun.test(leftConstant.value(), rightConstant.value())),
              op.position()));
      return true;
    }
    if (left.equals(right)) {
      // replace t = a == a with t = true
      // replace t = a != a with t = false
      replaceCurrent(
          new Transfer(
              destination, ConstantOperand.of(op.operator() == TokenType.EQEQ), op.position()));
      return true;
    }

    return false;
  }

  /**
   * If both operands are constant integrals, apply the given function to them and replace the
   * opcode with the new constant. E.g., t=3*2 becomes t=6
   *
   * @return true if both operands are integral constants.
   */
  private boolean optimizeIntegralArith(
      BinOp op, Operand left, Operand right, BinaryOperator<Long> fun) {

    if (left.isConstant() && right.isConstant() && left.type().isIntegral()) {
      long leftValue = ConstantOperand.valueFromConstOperand(left).longValue();
      long rightValue = ConstantOperand.valueFromConstOperand(right).longValue();
      Location destination = op.destination();
      replaceCurrent(
          new Transfer(
              destination,
              ConstantOperand.fromValue(fun.apply(leftValue, rightValue), left.type()),
              op.position()));
      return true;
    }
    return false;
  }

  /**
   * If both operands are constant doubles, apply the given function to them and replace the opcode
   * with the new constant. E.g., t=3.14*2.0 becomes t=6.28
   *
   * @return true if both operands are double constants.
   */
  private boolean optimizeDoubleArith(
      BinOp op, Operand left, Operand right, BinaryOperator<Double> fun) {

    if (left.isConstant() && right.isConstant() && left.type() == VarType.DOUBLE) {
      //      ConstantOperand<Double> leftConstant = (ConstantOperand<Double>) left;
      //      ConstantOperand<Double> rightConstant = (ConstantOperand<Double>) right;
      //      double leftval = leftConstant.value();
      //      double rightval = rightConstant.value();
      double leftValue = ConstantOperand.valueFromConstOperand(left).doubleValue();
      double rightValue = ConstantOperand.valueFromConstOperand(right).doubleValue();
      Location destination = op.destination();
      replaceCurrent(
          new Transfer(
              destination, ConstantOperand.of(fun.apply(leftValue, rightValue)), op.position()));
      return true;
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
      BinOp op, Operand left, Operand right, BinaryOperator<Boolean> fun) {

    if (left.isConstant() && right.isConstant() && left.type() == VarType.BOOL) {
      boolean leftVal = left.equals(ConstantOperand.TRUE);
      boolean rightVal = right.equals(ConstantOperand.TRUE);
      Location destination = op.destination();
      replaceCurrent(
          new Transfer(
              destination, ConstantOperand.of(fun.apply(leftVal, rightVal)), op.position()));
      return true;
    }
    return false;
  }

  private static boolean isAnyOne(Operand operand) {
    return ConstantOperand.isAnyIntOne(operand) || operand.equals(ConstantOperand.ONE_DBL);
  }
}
