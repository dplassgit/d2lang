package com.plasstech.lang.d2.optimize;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Function;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.common.InvalidIndexException;
import com.plasstech.lang.d2.common.Range;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.optimize.matcher.BinOpLeftRightOptimizer;
import com.plasstech.lang.d2.optimize.matcher.BinOpOptimizer;
import com.plasstech.lang.d2.optimize.matcher.Matchers;
import com.plasstech.lang.d2.optimize.matcher.OpcodeOptimizer;
import com.plasstech.lang.d2.optimize.matcher.UnaryOpOptimizer;
import com.plasstech.lang.d2.type.ArrayType;
import com.plasstech.lang.d2.type.VarType;

/**
 * Constant folding and single-line opcode simplification optimizations, via a declarative list of
 * optimizations.
 */
class ArithmeticOptimizer extends LineOptimizer {
  ArithmeticOptimizer(int debugLevel) {
    super(debugLevel);
  }

  @Override
  public void visit(UnaryOp op) {
    for (OpcodeOptimizer optimizer : UNOP_OPTIMIZERS) {
      if (optimizer.matches(op)) {
        Op newOp = optimizer.optimize(op);
        replaceCurrent(newOp);
        return;
      }
    }
  }

  @Override
  public void visit(BinOp op) {
    for (OpcodeOptimizer optimizer : BINOP_OPTIMIZERS) {
      if (optimizer.matches(op)) {
        Op newOp = optimizer.optimize(op);
        replaceCurrent(newOp);
        return;
      }
    }
  }

  private static final List<OpcodeOptimizer> BINOP_OPTIMIZERS = ImmutableList.of(
      // Fold integral constants
      new BinOpOptimizer(
          Matchers.isIntegralConstant(), TokenType.PLUS, Matchers.isIntegralConstant(),
          optimizeIntBinOp((left, right) -> left + right)),
      new BinOpOptimizer(
          Matchers.isIntegralConstant(), TokenType.MINUS, Matchers.isIntegralConstant(),
          optimizeIntBinOp((left, right) -> left - right)),
      new BinOpOptimizer(
          Matchers.isIntegralConstant(), TokenType.MULT, Matchers.isIntegralConstant(),
          optimizeIntBinOp((left, right) -> left * right)),
      new BinOpOptimizer(
          Matchers.isIntegralConstant(),
          TokenType.DIV,
          Matchers.and(Matchers.not(Matchers.isAnyZero()), Matchers.isConstant()),
          optimizeIntBinOp((left, right) -> left / right)),
      new BinOpOptimizer(
          Matchers.isIntegralConstant(), TokenType.SHIFT_LEFT, Matchers.isIntegralConstant(),
          optimizeIntBinOp((left, right) -> left << right)),
      new BinOpOptimizer(
          Matchers.isIntegralConstant(), TokenType.SHIFT_RIGHT, Matchers.isIntegralConstant(),
          optimizeIntBinOp((left, right) -> left >> right)),
      new BinOpOptimizer(
          Matchers.isIntegralConstant(), TokenType.MOD,
          Matchers.and(Matchers.not(Matchers.isAnyZero()), Matchers.isConstant()),
          optimizeIntBinOp((left, right) -> left % right)),
      new BinOpOptimizer(
          Matchers.isConstant(), TokenType.BIT_AND, Matchers.isConstant(),
          optimizeIntBinOp((left, right) -> left & right)),
      new BinOpOptimizer(
          Matchers.isConstant(), TokenType.BIT_OR, Matchers.isConstant(),
          optimizeIntBinOp((left, right) -> left | right)),
      new BinOpOptimizer(
          Matchers.isConstant(), TokenType.BIT_XOR, Matchers.isConstant(),
          optimizeIntBinOp((left, right) -> left ^ right)),

      // Fold comparisons
      new BinOpOptimizer(
          Matchers.isConstant(), TokenType.EQEQ, Matchers.isConstant(),
          optimizeComparison(
              (a, b) -> //
              ComparisonChain.start().compare(a, b, Ordering.natural().nullsFirst())
                  .result() == 0)),
      new BinOpOptimizer(
          Matchers.isConstant(), TokenType.NEQ, Matchers.isConstant(),
          optimizeComparison(
              (a, b) -> //
              ComparisonChain.start().compare(a, b, Ordering.natural().nullsFirst())
                  .result() != 0)),
      new BinOpOptimizer(
          Matchers.isConstant(), TokenType.LT, Matchers.isConstant(),
          optimizeComparison(
              (a, b) -> //
              ComparisonChain.start().compare(a, b, Ordering.natural().nullsFirst())
                  .result() < 0)),
      new BinOpOptimizer(
          Matchers.isConstant(), TokenType.GT, Matchers.isConstant(),
          optimizeComparison(
              (a, b) -> //
              ComparisonChain.start().compare(a, b, Ordering.natural().nullsFirst())
                  .result() > 0)),
      new BinOpOptimizer(
          Matchers.isConstant(), TokenType.LEQ, Matchers.isConstant(),
          optimizeComparison(
              (a, b) -> //
              ComparisonChain.start().compare(a, b, Ordering.natural().nullsFirst())
                  .result() <= 0)),
      new BinOpOptimizer(
          Matchers.isConstant(), TokenType.GEQ, Matchers.isConstant(),
          optimizeComparison(
              (a, b) -> //
              ComparisonChain.start().compare(a, b, Ordering.natural().nullsFirst())
                  .result() >= 0)),
      // x == x => true
      new BinOpLeftRightOptimizer(ImmutableList.of(TokenType.EQEQ, TokenType.LEQ, TokenType.GEQ),
          transferFrom(ConstantOperand.TRUE)),
      // x != x => false
      new BinOpLeftRightOptimizer(ImmutableList.of(TokenType.NEQ, TokenType.LT, TokenType.GT),
          transferFrom(ConstantOperand.FALSE)),

      // Fold double constants
      new BinOpOptimizer(Matchers.isDoubleConstant(), TokenType.PLUS, Matchers.isDoubleConstant(),
          optimizeDoubleBinOp((left, right) -> left + right)),
      new BinOpOptimizer(Matchers.isDoubleConstant(), TokenType.MINUS, Matchers.isDoubleConstant(),
          optimizeDoubleBinOp((left, right) -> left - right)),
      new BinOpOptimizer(Matchers.isDoubleConstant(), TokenType.MULT, Matchers.isDoubleConstant(),
          optimizeDoubleBinOp((left, right) -> left * right)),
      new BinOpOptimizer(
          Matchers.isDoubleConstant(),
          TokenType.DIV,
          Matchers.and(Matchers.not(Matchers.isAnyZero()), Matchers.isDoubleConstant()),
          optimizeDoubleBinOp((left, right) -> left / right)),

      // Fold boolean constants 
      new BinOpOptimizer(Matchers.isConstant(), TokenType.AND, Matchers.isConstant(),
          optimizeBoolBinOp((left, right) -> left && right)),
      new BinOpOptimizer(Matchers.isConstant(), TokenType.OR, Matchers.isConstant(),
          optimizeBoolBinOp((left, right) -> left || right)),
      new BinOpOptimizer(Matchers.isConstant(), TokenType.XOR, Matchers.isConstant(),
          optimizeBoolBinOp((left, right) -> left ^ right)),

      // Fold String constants
      // constand a + constant b
      new BinOpOptimizer(
          Matchers.and(Matchers.isConstant(), Matchers.hasType(VarType.STRING)),
          TokenType.PLUS,
          Matchers.and(Matchers.isConstant(), Matchers.hasType(VarType.STRING)),
          op -> {
            BinOp binop = (BinOp) op;
            String left = ConstantOperand.stringValueFromConstOperand(binop.left());
            String right = ConstantOperand.stringValueFromConstOperand(binop.right());
            return new Transfer(op.getDestination(),
                ConstantOperand.of(left + right), op.position());
          }),
      // anything + empty = anything
      new BinOpOptimizer(
          Matchers.hasType(VarType.STRING),
          TokenType.PLUS,
          Matchers.isEqualTo(ConstantOperand.EMPTY_STRING),
          transferFromLeft()),
      // empty + anything = anything
      new BinOpOptimizer(
          Matchers.isEqualTo(ConstantOperand.EMPTY_STRING),
          TokenType.PLUS,
          Matchers.hasType(VarType.STRING),
          transferFromRight()),

      // Other simplifications
      // a=b*-1 or b/-1 -> a=-b
      new BinOpOptimizer(
          Matchers.any(),
          ImmutableList.of(TokenType.MULT, TokenType.DIV),
          Matchers.isAnyNegativeOne(),
          op -> {
            BinOp binop = (BinOp) op;
            return new UnaryOp(op.getDestination(), TokenType.MINUS, binop.left(), op.position());
          }),
      // a * (power of 2) -> a << (log of power of 2)
      new BinOpOptimizer(Matchers.hasType(VarType.INT), TokenType.MULT, Matchers.isPowerOf2(),
          op -> {
            BinOp binop = (BinOp) op;
            int right = ConstantOperand.valueFromConstOperand(binop.right()).intValue();
            int power = Integer.numberOfTrailingZeros(right);
            return new BinOp(op.getDestination(), binop.left(), TokenType.SHIFT_LEFT,
                ConstantOperand.of(power), op.position());
          }),
      // a / (power of 2) -> a >> (log of power of 2)
      new BinOpOptimizer(Matchers.hasType(VarType.INT), TokenType.DIV, Matchers.isPowerOf2(),
          op -> {
            BinOp binop = (BinOp) op;
            int right = ConstantOperand.valueFromConstOperand(binop.right()).intValue();
            int power = Integer.numberOfTrailingZeros(right);
            return new BinOp(op.getDestination(), binop.left(), TokenType.SHIFT_RIGHT,
                ConstantOperand.of(power), op.position());
          }),
      // a=b*0 or b&0 -> a=0
      new BinOpOptimizer(
          Matchers.any(), ImmutableList.of(TokenType.MULT, TokenType.BIT_AND), Matchers.isAnyZero(),
          transferFromRight()),
      // a=b+0 or b|0 or b-0 or b^0 -> a=b
      new BinOpOptimizer(
          Matchers.any(),
          ImmutableList.of(TokenType.PLUS, TokenType.BIT_OR, TokenType.MINUS, TokenType.BIT_XOR,
              TokenType.SHIFT_LEFT, TokenType.SHIFT_RIGHT),
          Matchers.isAnyZero(),
          transferFromLeft()),
      // a=0/b -> a=0
      new BinOpOptimizer(Matchers.isAnyZero(), TokenType.DIV, Matchers.not(Matchers.isAnyZero()),
          transferFromLeft()),
      // a=b*1 or b/1 -> a=b
      new BinOpOptimizer(
          Matchers.any(), ImmutableList.of(TokenType.MULT, TokenType.DIV), Matchers.isAnyOne(),
          transferFromLeft()),
      // x - x = 0
      new BinOpLeftRightOptimizer(ImmutableList.of(TokenType.MINUS, TokenType.MOD),
          transferFromZero()),
      // x / x = 1
      new BinOpLeftRightOptimizer(ImmutableList.of(TokenType.DIV), transferFrom(1)),
      // x + x = x<<1, only for int
      new BinOpLeftRightOptimizer(ImmutableList.of(TokenType.PLUS), op -> {
        BinOp binop = (BinOp) op;
        Operand left = binop.left();
        return new BinOp(op.getDestination(), left, TokenType.SHIFT_LEFT, ConstantOperand.of(1),
            op.position());
      }, Matchers.hasType(VarType.INT), Matchers.any()),
      // 0 - x = -x
      new BinOpOptimizer(
          Matchers.and(Matchers.isAnyZero(), Matchers.isIntegralConstant()),
          TokenType.MINUS,
          Matchers.any(),
          op -> {
            BinOp binop = (BinOp) op;
            return new UnaryOp(op.getDestination(), TokenType.MINUS, binop.right(), op.position());
          }),
      // a = b + (-C) => a = b - C
      new BinOpOptimizer(
          Matchers.isNumeric(), TokenType.PLUS, Matchers.isNegativeConstant(),
          op -> {
            BinOp binop = (BinOp) op;
            if (binop.right().type().equals(VarType.DOUBLE)) {
              double value = ConstantOperand.valueFromConstOperand(binop.right()).doubleValue();
              return new BinOp(op.getDestination(), binop.left(),
                  TokenType.MINUS, ConstantOperand.of(-value),
                  op.position());
            }
            long value = ConstantOperand.valueFromConstOperand(binop.right())
                .longValue();
            return new BinOp(op.getDestination(), binop.left(), TokenType.MINUS,
                ConstantOperand.fromValue(-value, binop.right().type()), op.position());
          }),
      // a = b - (-C) => a = b + C
      new BinOpOptimizer(
          Matchers.isNumeric(), TokenType.MINUS, Matchers.isNegativeConstant(),
          op -> {
            BinOp binop = (BinOp) op;
            if (binop.right().type().equals(VarType.DOUBLE)) {
              double value = ConstantOperand.valueFromConstOperand(binop.right()).doubleValue();
              return new BinOp(op.getDestination(), binop.left(),
                  TokenType.PLUS,
                  ConstantOperand.of(-value), op.position());
            }
            long value = ConstantOperand.valueFromConstOperand(binop.right())
                .longValue();
            return new BinOp(op.getDestination(), binop.left(), TokenType.PLUS,
                ConstantOperand.fromValue(-value, binop.right().type()), op.position());
          }),
      // x and false = false
      new BinOpOptimizer(Matchers.hasType(VarType.BOOL), TokenType.AND,
          Matchers.isEqualTo(ConstantOperand.FALSE),
          transferFrom(ConstantOperand.FALSE)),
      // x and true = x
      new BinOpOptimizer(Matchers.hasType(VarType.BOOL), TokenType.AND,
          Matchers.isEqualTo(ConstantOperand.TRUE),
          transferFromLeft()),
      // x and x, x or x = x
      new BinOpLeftRightOptimizer(ImmutableList.of(TokenType.AND, TokenType.OR),
          transferFromLeft()),
      // x or false = x
      new BinOpOptimizer(Matchers.any(), TokenType.OR, Matchers.isEqualTo(ConstantOperand.FALSE),
          transferFromLeft()),
      // x or true = true
      new BinOpOptimizer(Matchers.any(), TokenType.OR, Matchers.isEqualTo(ConstantOperand.TRUE),
          transferFrom(ConstantOperand.TRUE)),
      // constant string indexed with a constant int:
      new BinOpOptimizer(Matchers.and(Matchers.isConstant(), Matchers.hasType(VarType.STRING)),
          TokenType.LBRACKET, Matchers.isIntegralConstant(), op -> {
            BinOp binop = (BinOp) op;
            String s = ConstantOperand.stringValueFromConstOperand(binop.left());
            int i = ConstantOperand.valueFromConstOperand(binop.right()).intValue();
            if (i >= s.length()) {
              throw new InvalidIndexException(op.position(),
                  "STRING index out of bounds (length %d); was %d", s.length(), i);
            }

            return new Transfer(op.getDestination(), ConstantOperand.of(s.substring(i, i + 1)),
                op.position());
          }),
      // Constant range indexed with a constant index
      new BinOpOptimizer(Matchers.and(Matchers.isConstant(), Matchers.hasType(VarType.RANGE)),
          TokenType.LBRACKET, Matchers.isIntegralConstant(), op -> {
            BinOp binop = (BinOp) op;
            Range r = ConstantOperand.rangeValueFromConstOperand(binop.left());
            int i = ConstantOperand.valueFromConstOperand(binop.right()).intValue();
            if (i < 0) {
              throw new InvalidIndexException(op.position(),
                  "RANGE slice start must be non-negative; was %d", i);
            }
            if (i > 1) {
              throw new InvalidIndexException(op.position(),
                  "RANGE index out of bounds (length 2); was %d", i);
            }

            return new Transfer(op.getDestination(), ConstantOperand.of(r.value(i)), op.position());
          }),
      // constant string indexed with constant range
      new BinOpOptimizer(Matchers.and(Matchers.isConstant(), Matchers.hasType(VarType.STRING)),
          TokenType.LBRACKET, Matchers.and(Matchers.isConstant(), Matchers.hasType(VarType.RANGE)),
          op -> {
            BinOp binop = (BinOp) op;
            String s = ConstantOperand.stringValueFromConstOperand(binop.left());
            Range range = ConstantOperand.rangeValueFromConstOperand(binop.right());
            if (range.start() < 0) {
              throw new InvalidIndexException(op.position(),
                  "STRING slice start must be non-negative; was %d", range.start());
            }
            if (range.end() > s.length()) {
              throw new InvalidIndexException(op.position(),
                  "STRING slice out of bounds (length %d); was %d", s.length(), range.end());
            }
            return new Transfer(binop.destination(),
                ConstantOperand.of(s.substring(range.start(), range.end())), op.position());
          }),
      new BinOpOptimizer(Matchers.and(Matchers.isConstant(), Matchers.hasType(VarType.INT)),
          TokenType.COLON, Matchers.and(Matchers.isConstant(), Matchers.hasType(VarType.INT)),
          op -> {
            BinOp binop = (BinOp) op;
            int left = ConstantOperand.valueFromConstOperand(binop.left()).intValue();
            int right = ConstantOperand.valueFromConstOperand(binop.right()).intValue();
            Range range = new Range(left, right);
            return new Transfer(op.getDestination(),
                new ConstantOperand<Range>(range, VarType.RANGE), op.position());
          }));

  private static final List<OpcodeOptimizer> UNOP_OPTIMIZERS = ImmutableList.of(
      // length(range) is always 2
      new UnaryOpOptimizer(Matchers.hasType(VarType.RANGE), TokenType.LENGTH,
          op -> new Transfer(op.getDestination(), ConstantOperand.of(2), op.position())),
      // Length(constant string) is known
      new UnaryOpOptimizer(Matchers.and(Matchers.isConstant(), Matchers.hasType(VarType.STRING)),
          TokenType.LENGTH,
          op -> {
            UnaryOp unop = (UnaryOp) op;
            String s = ConstantOperand.stringValueFromConstOperand(unop.operand());
            return new Transfer(op.getDestination(), ConstantOperand.of(s.length()), op.position());
          }),
      // Length(array with known length)
      new UnaryOpOptimizer(Matchers.and(Matchers.isArray(), operand -> {
        ArrayType arrayType = (ArrayType) operand.type();
        return arrayType.knownLength().isPresent();
      }),
          TokenType.LENGTH,
          op -> {
            UnaryOp unop = (UnaryOp) op;
            ArrayType arrayType = (ArrayType) unop.operand().type();

            return new Transfer(unop.destination(),
                ConstantOperand.of(arrayType.knownLength().get()),
                op.position());
          }),
      // not true is false
      new UnaryOpOptimizer(Matchers.isEqualTo(ConstantOperand.TRUE), TokenType.NOT,
          transferFrom(ConstantOperand.FALSE)),
      // not false is true
      new UnaryOpOptimizer(Matchers.isEqualTo(ConstantOperand.FALSE), TokenType.NOT,
          transferFrom(ConstantOperand.TRUE)),
      // -(C) becomes -C for all numbers
      new UnaryOpOptimizer(Matchers.isIntegralConstant(), TokenType.MINUS,
          op -> {
            UnaryOp unop = (UnaryOp) op;
            long operand = ConstantOperand.valueFromConstOperand(unop.operand())
                .longValue();
            return new Transfer(op.getDestination(),
                ConstantOperand.fromValue(-operand, unop.operand().type()), op.position());
          }),
      new UnaryOpOptimizer(Matchers.isDoubleConstant(), TokenType.MINUS,
          op -> {
            UnaryOp unop = (UnaryOp) op;
            double operand = ConstantOperand.valueFromConstOperand(unop.operand()).doubleValue();
            return new Transfer(op.getDestination(), ConstantOperand.of(-operand), op.position());
          }),
      // Bit not: ~(C) becomes ~C for all numbers
      new UnaryOpOptimizer(Matchers.isIntegralConstant(), TokenType.BIT_NOT,
          op -> {
            UnaryOp unop = (UnaryOp) op;
            long operand = ConstantOperand.valueFromConstOperand(unop.operand()).longValue();
            return new Transfer(op.getDestination(),
                ConstantOperand.fromValue(~operand, unop.operand().type()), op.position());
          }),
      // +x = x
      new UnaryOpOptimizer(Matchers.any(), TokenType.PLUS,
          op -> new Transfer(op.getDestination(), op.getSources().get(0), op.position())),

      // chr(constant) -> constant.toString
      new UnaryOpOptimizer(
          Matchers.and(Matchers.isConstant(), Matchers.hasType(VarType.INT)), TokenType.CHR,
          op -> {
            UnaryOp unop = (UnaryOp) op;
            Operand operand = unop.operand();
            int oldValue = ConstantOperand.valueFromConstOperand(operand).intValue();
            int value = oldValue & 0xff;
            return new Transfer(
                unop.destination(),
                ConstantOperand.of(Character.valueOf((char) value).toString()),
                op.position());
          }),
      // asc(constant) -> constant
      new UnaryOpOptimizer(
          Matchers.and(Matchers.isConstant(), Matchers.hasType(VarType.STRING)), TokenType.ASC,
          op -> {
            UnaryOp unop = (UnaryOp) op;
            Operand operand = unop.operand();
            String value = ConstantOperand.stringValueFromConstOperand(operand);
            if (value.length() < 1) {
              throw new InvalidIndexException(
                  op.position(),
                  "Cannot take ASC of empty STRING");
            }
            char first = value.charAt(0);
            return new Transfer(
                unop.destination(),
                ConstantOperand.of(first),
                op.position());
          }));

  private static Function<Op, Op> optimizeIntBinOp(BinaryOperator<Long> fun) {
    return op -> {
      BinOp binop = (BinOp) op;
      long left = ConstantOperand.valueFromConstOperand(binop.left()).longValue();
      long right = ConstantOperand.valueFromConstOperand(binop.right()).longValue();
      return new Transfer(op.getDestination(),
          ConstantOperand.fromValue(fun.apply(left, right), binop.left().type()), op.position());
    };
  }

  private static Function<Op, Op> optimizeComparison(
      BiPredicate<Comparable<?>, Comparable<?>> fun) {
    return op -> {
      BinOp binop = (BinOp) op;
      ConstantOperand<?> left = (ConstantOperand<?>) binop.left();
      ConstantOperand<?> right = (ConstantOperand<?>) binop.right();
      Comparable<?> leftval = (Comparable<?>) left.value();
      Comparable<?> rightval = (Comparable<?>) right.value();
      return new Transfer(op.getDestination(), ConstantOperand.of(fun.test(leftval, rightval)),
          op.position());
    };
  }

  private static Function<Op, Op> optimizeDoubleBinOp(BinaryOperator<Double> fun) {
    return op -> {
      BinOp binop = (BinOp) op;
      double left = ConstantOperand.valueFromConstOperand(binop.left()).doubleValue();
      double right = ConstantOperand.valueFromConstOperand(binop.right()).doubleValue();
      return new Transfer(op.getDestination(),
          ConstantOperand.of(fun.apply(left, right)), op.position());
    };
  }

  private static Function<Op, Op> optimizeBoolBinOp(BinaryOperator<Boolean> fun) {
    return op -> {
      BinOp binop = (BinOp) op;
      boolean left = binop.left().equals(ConstantOperand.TRUE);
      boolean right = binop.right().equals(ConstantOperand.TRUE);
      return new Transfer(op.getDestination(),
          ConstantOperand.of(fun.apply(left, right)), op.position());
    };
  }

  private static Function<Op, Op> transferFrom(Operand source) {
    return op -> new Transfer(op.getDestination(), source, op.position());
  }

  private static Function<Op, Op> transferFrom(long value) {
    return op -> {
      BinOp binop = (BinOp) op;
      return new Transfer(op.getDestination(),
          ConstantOperand.fromValue(value, binop.left().type()), op.position());
    };
  }

  private static Function<Op, Op> transferFromZero() {
    return transferFrom(0);
  }

  private static Function<Op, Op> transferFromLeft() {
    return op -> {
      BinOp binop = (BinOp) op;
      return new Transfer(op.getDestination(), binop.left(), op.position());
    };
  }

  private static Function<Op, Op> transferFromRight() {
    return op -> {
      BinOp binop = (BinOp) op;
      return new Transfer(op.getDestination(), binop.right(), op.position());
    };
  }
}
