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
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.common.D2RuntimeException;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.VarType;

class ArithmeticOptimizer extends LineOptimizer {
  ArithmeticOptimizer(int debugLevel) {
    super(debugLevel);
  }

  @Override
  public void visit(UnaryOp op) {
    Operand operand = op.operand();
    switch (op.operator()) {
      case LENGTH:
        if (operand.isConstant() && operand.type() == VarType.STRING) {
          ConstantOperand<String> constant = (ConstantOperand<String>) operand;
          String value = constant.value();
          replaceCurrent(
              new Transfer(op.destination(), ConstantOperand.of(value.length()), op.position()));
        }
        return;
      case NOT:
        if (operand.isConstant() && operand.type() == VarType.BOOL) {
          ConstantOperand<Boolean> constant = (ConstantOperand<Boolean>) operand;
          boolean valueBoolean = constant.value();
          replaceCurrent(
              new Transfer(op.destination(), ConstantOperand.of(!valueBoolean), op.position()));
        }
        return;
      case BIT_NOT:
        if (operand.isConstant() && operand.type() == VarType.INT) {
          ConstantOperand<Integer> constant = (ConstantOperand<Integer>) operand;
          int value = constant.value();
          replaceCurrent(new Transfer(op.destination(), ConstantOperand.of(~value), op.position()));
        } else if (operand.isConstant() && operand.type() == VarType.LONG) {
          ConstantOperand<Long> constant = (ConstantOperand<Long>) operand;
          long value = constant.value();
          replaceCurrent(new Transfer(op.destination(), ConstantOperand.of(~value), op.position()));
        } else if (operand.isConstant() && operand.type() == VarType.BYTE) {
          ConstantOperand<Byte> constant = (ConstantOperand<Byte>) operand;
          byte value = constant.value();
          replaceCurrent(
              new Transfer(op.destination(), ConstantOperand.of((byte) ~value), op.position()));
        }
        return;

      case MINUS:
        if (operand.isConstant()) {
          if (operand.type() == VarType.INT) {
            ConstantOperand<Integer> constant = (ConstantOperand<Integer>) operand;
            int value = constant.value();
            replaceCurrent(
                new Transfer(op.destination(), ConstantOperand.of(-value), op.position()));
          } else if (operand.type() == VarType.LONG) {
            ConstantOperand<Long> constant = (ConstantOperand<Long>) operand;
            long value = constant.value();
            replaceCurrent(
                new Transfer(op.destination(), ConstantOperand.of(-value), op.position()));
          } else if (operand.type() == VarType.DOUBLE) {
            ConstantOperand<Double> constant = (ConstantOperand<Double>) operand;
            double value = constant.value();
            replaceCurrent(
                new Transfer(op.destination(), ConstantOperand.of(-value), op.position()));
          } else if (operand.type() == VarType.BYTE) {
            ConstantOperand<Byte> constant = (ConstantOperand<Byte>) operand;
            byte value = constant.value();
            replaceCurrent(
                new Transfer(op.destination(), ConstantOperand.of((byte) -value), op.position()));
          }
        }
        return;
      case ASC:
        if (operand.isConstant()) {
          @SuppressWarnings("unchecked")
          ConstantOperand<String> constant = (ConstantOperand<String>) operand;
          String value = constant.value();
          char first = value.charAt(0);
          replaceCurrent(new Transfer(op.destination(), ConstantOperand.of(first), op.position()));
        }
        return;
      case CHR:
        if (operand.isConstant()) {
          @SuppressWarnings("unchecked")
          ConstantOperand<Integer> constant = (ConstantOperand<Integer>) operand;
          int value = constant.value();
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

  // TODO: Move this to PrintOptimizer
  @Override
  public void visit(SysCall op) {
    if (op.call() != SysCall.Call.PRINT && op.call() != SysCall.Call.PRINTLN) {
      return;
    }
    Operand arg = op.arg();
    if (!arg.isConstant()) {
      return;
    }
    ConstantOperand<?> operand = (ConstantOperand<?>) arg;
    if (operand.type() == VarType.STRING) {
      return;
    }
    if (operand.value() == null) {
      replaceCurrent(new SysCall(op.call(), ConstantOperand.of("null")));
      return;
    }
    String asString = operand.value().toString();
    replaceCurrent(new SysCall(op.call(), ConstantOperand.of(asString)));
  }

  @Override
  public void visit(BinOp op) {
    Operand left = op.left();
    Operand right = op.right();
    TokenType operator = op.operator();

    if (left.isConstant()
        && !right.isConstant()
        && (operator == TokenType.MULT || operator == TokenType.PLUS)
        && VarType.isNumeric(left.type())) {
      // swap it so the constant is on the right.
      replaceCurrent(new BinOp(op.destination(), right, operator, left, op.position()));
      return;
    }

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
        if (optimizeIntArith(op, left, right, (t, u) -> t ^ u)) {
          return;
        }
        if (optimizeLongArith(op, left, right, (t, u) -> (long) (t ^ u))) {
          return;
        }
        if (optimizeByteArith(op, left, right, (t, u) -> (byte) (t ^ u))) {
          return;
        }
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
            (a, b) ->
                ComparisonChain.start().compare(a, b, Ordering.natural().nullsFirst()).result()
                    <= 0);
        return;

      case LT:
        optimizeCompare(
            op,
            (a, b) ->
                ComparisonChain.start().compare(a, b, Ordering.natural().nullsFirst()).result()
                    < 0);
        return;

      case GEQ:
        optimizeCompare(
            op,
            (a, b) ->
                ComparisonChain.start().compare(a, b, Ordering.natural().nullsFirst()).result()
                    >= 0);
        return;

      case GT:
        optimizeCompare(
            op,
            (a, b) ->
                ComparisonChain.start().compare(a, b, Ordering.natural().nullsFirst()).result()
                    > 0);
        return;

      case LBRACKET:
        // Replace "abc"[0] with "a".
        // Only works for constant strings and constant int indexes (modulo constant propagation!)
        if (left.isConstant() && right.isConstant() && left.type() == VarType.STRING) {
          ConstantOperand<String> constant = (ConstantOperand<String>) left;
          String value = constant.value();
          ConstantOperand<Integer> indexOperand = (ConstantOperand<Integer>) right;
          int index = indexOperand.value();
          if (index < 0) {
            throw new D2RuntimeException(
                String.format("STRING index must be non-negative; was %d", index),
                op.position(),
                "String index");
          }
          if (index > value.length()) {
            throw new D2RuntimeException(
                String.format(
                    "STRING index out of bounds (length %d); was %d", value.length(), index),
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

  /** Bit or (for ints, longs or bytes.) */
  private void optimizeBitOr(BinOp op, Operand left, Operand right) {
    if (optimizeIntArith(op, left, right, (t, u) -> t | u)) {
      return;
    }
    if (optimizeLongArith(op, left, right, (t, u) -> (long) (t | u))) {
      return;
    }
    if (optimizeByteArith(op, left, right, (t, u) -> (byte) (t | u))) {
      return;
    }

    // Anything | 0 = the thing
    if (ConstantOperand.isAnyZero(left)) {
      // replace with destination = right
      replaceCurrent(new Transfer(op.destination(), op.right(), op.position()));
      return;
    }
    if (ConstantOperand.isAnyZero(right)) {
      // replace with destination = left
      replaceCurrent(new Transfer(op.destination(), op.left(), op.position()));
      return;
    }
    if (left.equals(right)) {
      replaceCurrent(new Transfer(op.destination(), left, op.position()));
      return;
    }
  }

  private void optimizeBitAnd(BinOp op, Operand left, Operand right) {
    if (optimizeIntArith(op, left, right, (t, u) -> t & u)) {
      return;
    }
    if (optimizeLongArith(op, left, right, (t, u) -> (long) (t & u))) {
      return;
    }
    if (optimizeByteArith(op, left, right, (t, u) -> (byte) (t & u))) {
      return;
    }

    // Anything & 0 = 0
    if (ConstantOperand.isAnyZero(left)) {
      // replace with destination = 0
      replaceCurrent(new Transfer(op.destination(), left, op.position()));
      return;
    }
    if (ConstantOperand.isAnyZero(right)) {
      // replace with destination = 0
      replaceCurrent(new Transfer(op.destination(), right, op.position()));
      return;
    }
    // x&x=x
    if (left.equals(right)) {
      replaceCurrent(new Transfer(op.destination(), left, op.position()));
      return;
    }
  }

  private void optimizeModulo(BinOp op, Operand left, Operand right) {
    if (right.equals(ConstantOperand.ONE)) {
      replaceCurrent(new Transfer(op.destination(), ConstantOperand.ZERO, op.position()));
      return;
    }
    if (right.equals(ConstantOperand.ONE_LONG)) {
      replaceCurrent(new Transfer(op.destination(), ConstantOperand.ZERO_LONG, op.position()));
      return;
    }
    if (right.equals(ConstantOperand.ONE_BYTE)) {
      replaceCurrent(new Transfer(op.destination(), ConstantOperand.ZERO_BYTE, op.position()));
      return;
    }
    if (left.equals(right)) {
      if (left.type() == VarType.INT) {
        replaceCurrent(new Transfer(op.destination(), ConstantOperand.ZERO, op.position()));
        return;
      }
      if (left.type() == VarType.LONG) {
        replaceCurrent(new Transfer(op.destination(), ConstantOperand.ZERO_LONG, op.position()));
        return;
      }
      if (left.type() == VarType.BYTE) {
        replaceCurrent(new Transfer(op.destination(), ConstantOperand.ZERO_BYTE, op.position()));
        return;
      }
    }
    try {
      optimizeIntArith(op, left, right, (t, u) -> t % u);
      optimizeLongArith(op, left, right, (t, u) -> (long) (t % u));
      optimizeByteArith(op, left, right, (t, u) -> (byte) (t % u));
    } catch (ArithmeticException e) {
      throw new D2RuntimeException("Modulo by 0", op.position(), "Arithmetic");
    }
  }

  private void optimizeDivide(BinOp op, Operand left, Operand right) {
    if (isAnyOne(right)) {
      replaceCurrent(new Transfer(op.destination(), op.left(), op.position()));
      return;
    }
    try {
      if (ConstantOperand.isAnyZero(right)) {
        throw new D2RuntimeException("Division by 0", op.position(), "Arithmetic");
      }
      if (optimizeIntArith(op, left, right, (t, u) -> t / u)) {
        return;
      }
      if (optimizeLongArith(op, left, right, (t, u) -> (long) (t / u))) {
        return;
      }
      if (optimizeDoubleArith(op, left, right, (t, u) -> t / u)) {
        return;
      }
      if (optimizeByteArith(op, left, right, (t, u) -> (byte) (t / u))) {
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
        replaceCurrent(new Transfer(op.destination(), ConstantOperand.ONE, op.position()));
        return;
      }
      if (left.equals(right) && left.type() == VarType.LONG) {
        // a/a = 1
        replaceCurrent(new Transfer(op.destination(), ConstantOperand.ONE_LONG, op.position()));
        return;
      }
      if (left.equals(right) && left.type() == VarType.DOUBLE) {
        // a/a = 1
        replaceCurrent(new Transfer(op.destination(), ConstantOperand.ONE_DBL, op.position()));
        return;
      }
      if (left.equals(right) && left.type() == VarType.BYTE) {
        // a/a = 1
        replaceCurrent(new Transfer(op.destination(), ConstantOperand.ONE_BYTE, op.position()));
        return;
      }
    } catch (ArithmeticException e) {
      throw new D2RuntimeException("Division by 0", op.position(), "Arithmetic");
    }
  }

  private void optimizeShiftLeft(BinOp op, Operand left, Operand right) {
    if (ConstantOperand.isAnyZero(right)) {
      replaceCurrent(new Transfer(op.destination(), op.left(), op.position()));
      return;
    }
    if (optimizeIntArith(op, left, right, (t, u) -> t << u)) {
      return;
    }
    optimizeLongArith(op, left, right, (t, u) -> (long) (t << u));
  }

  private void optimizeShiftRight(BinOp op, Operand left, Operand right) {
    if (ConstantOperand.isAnyZero(right)) {
      replaceCurrent(new Transfer(op.destination(), op.left(), op.position()));
      return;
    }
    if (optimizeIntArith(op, left, right, (t, u) -> t >> u)) {
      return;
    }
    optimizeLongArith(op, left, right, (t, u) -> (long) (t >> u));
  }

  private void optimizeSubtract(BinOp op, Operand left, Operand right) {
    if (optimizeIntArith(op, left, right, (t, u) -> t - u)) {
      return;
    }
    if (optimizeLongArith(op, left, right, (t, u) -> (long) (t - u))) {
      return;
    }
    if (optimizeDoubleArith(op, left, right, (t, u) -> t - u)) {
      return;
    }
    if (optimizeByteArith(op, left, right, (t, u) -> (byte) (t - u))) {
      return;
    }
    // Replace foo - -32 with foo + 32
    if (right.isConstant()) {
      ConstantOperand<?> rightConstant = (ConstantOperand<?>) right;
      if (rightConstant.type() == VarType.INT) {
        int rightval = (int) rightConstant.value();
        if (rightval < 0) {
          replaceCurrent(
              new BinOp(
                  op.destination(),
                  left,
                  TokenType.PLUS,
                  ConstantOperand.of(-rightval),
                  op.position()));
          return;
        }
      }
      if (rightConstant.type() == VarType.LONG) {
        long rightval = (long) rightConstant.value();
        if (rightval < 0) {
          replaceCurrent(
              new BinOp(
                  op.destination(),
                  left,
                  TokenType.PLUS,
                  ConstantOperand.of(-rightval),
                  op.position()));
          return;
        }
      }
      if (rightConstant.type() == VarType.DOUBLE) {
        double rightval = (double) rightConstant.value();
        if (rightval < 0) {
          replaceCurrent(
              new BinOp(
                  op.destination(),
                  left,
                  TokenType.PLUS,
                  ConstantOperand.of(-rightval),
                  op.position()));
          return;
        }
      }
      if (rightConstant.type() == VarType.BYTE) {
        byte rightval = (byte) rightConstant.value();
        if (rightval < 0) {
          replaceCurrent(
              new BinOp(
                  op.destination(),
                  left,
                  TokenType.PLUS,
                  ConstantOperand.of(-rightval),
                  op.position()));
          return;
        }
      }
    }
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
      // replace with destination = left
      replaceCurrent(new Transfer(op.destination(), op.left(), op.position()));
      return;
    }
    if (left.equals(right) && left.type() == VarType.INT) {
      // a - a = 0
      replaceCurrent(new Transfer(op.destination(), ConstantOperand.ZERO, op.position()));
      return;
    }
    if (left.equals(right) && left.type() == VarType.LONG) {
      // a - a = 0
      replaceCurrent(new Transfer(op.destination(), ConstantOperand.ZERO_LONG, op.position()));
      return;
    }
    if (left.equals(right) && left.type() == VarType.DOUBLE) {
      // a - a = 0.0
      replaceCurrent(new Transfer(op.destination(), ConstantOperand.ZERO_DBL, op.position()));
      return;
    }
    if (left.equals(right) && left.type() == VarType.BYTE) {
      // a - a = 0
      replaceCurrent(new Transfer(op.destination(), ConstantOperand.ZERO_BYTE, op.position()));
      return;
    }
  }

  private void optimizeAdd(BinOp op, Operand left, Operand right) {
    if (left.isConstant() && right.isConstant()) {
      // May be ints, longs, doubles or strings
      if (optimizeIntArith(op, left, right, (t, u) -> t + u)) {
        return;
      }
      if (optimizeLongArith(op, left, right, (t, u) -> (long) (t + u))) {
        return;
      }
      if (optimizeByteArith(op, left, right, (t, u) -> (byte) (t + u))) {
        return;
      }
      if (optimizeDoubleArith(op, left, right, (t, u) -> t + u)) {
        return;
      }
      // Strings
      if (left.type() == VarType.STRING) {
        if (right.type().isNull()) {
          throw new D2RuntimeException("Cannot add NULL to STRING", op.position(), "Null pointer");
        }
        @SuppressWarnings("unchecked")
        ConstantOperand<String> leftConstant = (ConstantOperand<String>) left;
        @SuppressWarnings("unchecked")
        ConstantOperand<String> rightConstant = (ConstantOperand<String>) right;
        replaceCurrent(
            new Transfer(
                op.destination(),
                ConstantOperand.of(leftConstant.value() + rightConstant.value()),
                op.position()));
        return;
      }
    }

    // Replace a + "" with a
    if (left.type() == VarType.STRING) {
      if (left.isConstant()) {
        @SuppressWarnings("unchecked")
        ConstantOperand<String> leftConstant = (ConstantOperand<String>) left;
        if (leftConstant.value().isEmpty()) {
          replaceCurrent(new Transfer(op.destination(), right, op.position()));
          return;
        }
      } else if (right.isConstant()) {
        @SuppressWarnings("unchecked")
        ConstantOperand<String> rightConstant = (ConstantOperand<String>) right;
        if (right.type().isNull()) {
          throw new D2RuntimeException("Cannot add NULL to STRING", op.position(), "Null pointer");
        }
        if (rightConstant.value().isEmpty()) {
          replaceCurrent(new Transfer(op.destination(), left, op.position()));
          return;
        }
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
    if (right.isConstant() && right.type() == VarType.LONG) {
      ConstantOperand<Long> rightConstant = (ConstantOperand<Long>) right;
      long rightval = rightConstant.value();
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
    if (right.isConstant() && right.type() == VarType.BYTE) {
      ConstantOperand<Byte> rightConstant = (ConstantOperand<Byte>) right;
      byte rightval = rightConstant.value();
      if (rightval < 0) {
        replaceCurrent(
            new BinOp(
                op.destination(),
                left,
                TokenType.MINUS,
                ConstantOperand.of((byte) -rightval),
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

    if (ConstantOperand.isAnyZero(left)) {
      // replace with destination = right
      replaceCurrent(new Transfer(op.destination(), op.right(), op.position()));
      return;
    }
    if (ConstantOperand.isAnyZero(right)) {
      // replace with destination = left
      replaceCurrent(new Transfer(op.destination(), op.left(), op.position()));
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
    if (optimizeIntArith(op, left, right, (t, u) -> t * u)) {
      return;
    }
    if (optimizeLongArith(op, left, right, (t, u) -> (long) (t * u))) {
      return;
    }
    if (optimizeDoubleArith(op, left, right, (t, u) -> t * u)) {
      return;
    }
    if (optimizeByteArith(op, left, right, (t, u) -> (byte) (t * u))) {
      // bytes
      return;
    }
    if (left.equals(ConstantOperand.ZERO) || right.equals(ConstantOperand.ZERO)) {
      // replace with destination = 0
      replaceCurrent(new Transfer(op.destination(), ConstantOperand.ZERO, op.position()));
      return;
    }
    if (left.equals(ConstantOperand.ZERO_LONG) || right.equals(ConstantOperand.ZERO_LONG)) {
      // replace with destination = 0L
      replaceCurrent(new Transfer(op.destination(), ConstantOperand.ZERO_LONG, op.position()));
      return;
    }
    if (left.equals(ConstantOperand.ZERO_DBL) || right.equals(ConstantOperand.ZERO_DBL)) {
      // replace with destination = 0.0
      replaceCurrent(new Transfer(op.destination(), ConstantOperand.ZERO_DBL, op.position()));
      return;
    }
    if (left.equals(ConstantOperand.ZERO_BYTE) || right.equals(ConstantOperand.ZERO_BYTE)) {
      // replace with destination = 0
      replaceCurrent(new Transfer(op.destination(), ConstantOperand.ZERO_BYTE, op.position()));
      return;
    }
    if (isAnyOne(left)) {
      replaceCurrent(new Transfer(op.destination(), op.right(), op.position()));
      return;
    }
    if (isAnyOne(right)) {
      replaceCurrent(new Transfer(op.destination(), op.left(), op.position()));
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
    if (optimizeBoolArith(op, left, right, (t, u) -> t && u)) {
      return;
    }
    if (left.equals(ConstantOperand.FALSE) || right.equals(ConstantOperand.FALSE)) {
      // replace with destination = FALSE
      replaceCurrent(new Transfer(op.destination(), ConstantOperand.FALSE, op.position()));
      return;
    }
    if (left.equals(ConstantOperand.TRUE)) {
      // true and right == right
      replaceCurrent(new Transfer(op.destination(), op.right(), op.position()));
      return;
    }
    if (right.equals(ConstantOperand.TRUE)) {
      // left and true == left
      replaceCurrent(new Transfer(op.destination(), op.left(), op.position()));
      return;
    }
  }

  // this is for BOOLEAN or.
  private void optimizeOr(BinOp op, Operand left, Operand right) {
    if (optimizeBoolArith(op, left, right, (t, u) -> t || u)) {
      return;
    }
    if (left.equals(ConstantOperand.TRUE) || right.equals(ConstantOperand.TRUE)) {
      // either one is true, it's true
      replaceCurrent(new Transfer(op.destination(), ConstantOperand.TRUE, op.position()));
      return;
    }
    if (left.equals(ConstantOperand.FALSE)) {
      // false or right = right
      replaceCurrent(new Transfer(op.destination(), op.right(), op.position()));
      return;
    }
    if (right.equals(ConstantOperand.FALSE)) {
      // left or false = left
      replaceCurrent(new Transfer(op.destination(), op.left(), op.position()));
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
    if (left.equals(right)) {
      // they're equal, so we can optimize it. If it's LEQ or GEQ, we pass TRUE, otherwise FALSE.
      replaceCurrent(
          new Transfer(
              destination,
              ConstantOperand.of(op.operator() == TokenType.LEQ || op.operator() == TokenType.GEQ),
              op.position()));
      return true;
    }
    return false;
  }

  /**
   * If both operands are constants, apply the given function to the constants and replace the
   * opcode with result. E.g., t = 'a' == 'b' becomes t = false
   *
   * <p>If both operands are not constants, apply the == function and if they pass, replace the
   * opcode with true. E.g., t=a==a becomes t=true. This does not work for t=a!=b because it still
   * has to compare at runtime the values of a and b are the same or not.
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
   * If both operands are constant ints, apply the given function to those numbers and replace the
   * opcode with the new constant. E.g., t=3+4 becomes t=7
   *
   * @return true if both operands are int constants.
   */
  private boolean optimizeIntArith(
      BinOp op, Operand left, Operand right, BinaryOperator<Integer> fun) {
    Location destination = op.destination();

    if (left.isConstant() && right.isConstant() && left.type() == VarType.INT) {
      ConstantOperand<Integer> leftConstant = (ConstantOperand<Integer>) left;
      ConstantOperand<Integer> rightConstant = (ConstantOperand<Integer>) right;
      int leftval = leftConstant.value();
      int rightval = rightConstant.value();
      replaceCurrent(
          new Transfer(
              destination, ConstantOperand.of(fun.apply(leftval, rightval)), op.position()));
      return true;
    }
    return false;
  }

  /**
   * If both operands are constant bytes, apply the given function to those numbers and replace the
   * opcode with the new constant. E.g., t=3+4 becomes t=7
   *
   * @return true if both operands are byte constants.
   */
  private boolean optimizeByteArith(
      BinOp op, Operand left, Operand right, BinaryOperator<Byte> fun) {

    Location destination = op.destination();

    if (left.isConstant() && right.isConstant() && left.type() == VarType.BYTE) {
      ConstantOperand<Byte> leftConstant = (ConstantOperand<Byte>) left;
      ConstantOperand<Byte> rightConstant = (ConstantOperand<Byte>) right;
      byte leftval = leftConstant.value();
      byte rightval = rightConstant.value();
      replaceCurrent(
          new Transfer(
              destination, ConstantOperand.of(fun.apply(leftval, rightval)), op.position()));
      return true;
    }
    return false;
  }

  /**
   * If both operands are constant longs, apply the given function to those numbers and replace the
   * opcode with the new constant. E.g., t=3L+4L becomes t=7L
   *
   * @return true if both operands are long constants.
   */
  private boolean optimizeLongArith(
      BinOp op, Operand left, Operand right, BinaryOperator<Long> fun) {
    Location destination = op.destination();

    if (left.isConstant() && right.isConstant() && left.type() == VarType.LONG) {
      ConstantOperand<Long> leftConstant = (ConstantOperand<Long>) left;
      ConstantOperand<Long> rightConstant = (ConstantOperand<Long>) right;
      long leftval = leftConstant.value();
      long rightval = rightConstant.value();
      replaceCurrent(
          new Transfer(
              destination, ConstantOperand.of(fun.apply(leftval, rightval)), op.position()));
      return true;
    }
    return false;
  }

  /**
   * If both operands are constant doubles, apply the given function to those booleans and replace
   * the opcode with the new constant. E.g., t=3.14*2.0 becomes t=6.28
   *
   * @return true if both operands are double constants.
   */
  private boolean optimizeDoubleArith(
      BinOp op, Operand left, Operand right, BinaryOperator<Double> fun) {
    Location destination = op.destination();
    if (left.isConstant() && right.isConstant() && left.type() == VarType.DOUBLE) {
      ConstantOperand<Double> leftConstant = (ConstantOperand<Double>) left;
      ConstantOperand<Double> rightConstant = (ConstantOperand<Double>) right;
      double leftval = leftConstant.value();
      double rightval = rightConstant.value();
      replaceCurrent(
          new Transfer(
              destination, ConstantOperand.of(fun.apply(leftval, rightval)), op.position()));
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

    Location destination = op.destination();
    if (left.isConstant() && right.isConstant() && left.type() == VarType.BOOL) {
      ConstantOperand<Boolean> leftConstant = (ConstantOperand<Boolean>) left;
      ConstantOperand<Boolean> rightConstant = (ConstantOperand<Boolean>) right;
      boolean leftval = leftConstant.value();
      boolean rightval = rightConstant.value();
      replaceCurrent(
          new Transfer(
              destination, ConstantOperand.of(fun.apply(leftval, rightval)), op.position()));
      return true;
    }
    return false;
  }

  private static boolean isAnyOne(Operand operand) {
    return operand.equals(ConstantOperand.ONE)
        || operand.equals(ConstantOperand.ONE_LONG)
        || operand.equals(ConstantOperand.ONE_DBL)
        || operand.equals(ConstantOperand.ONE_BYTE);
  }
}
