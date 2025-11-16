package com.plasstech.lang.d2.optimize;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Dec;
import com.plasstech.lang.d2.codegen.il.Inc;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.VarType;
import java.util.ArrayList;
import java.util.Set;

/**
 * Optimizer that optimizes binary ops of the pattern:
 *
 * <pre>
 *  __temp2 = __temp1 + 2
 *  __temp3 = __temp2 + 3
 * </pre>
 *
 * into:
 *
 * <pre>
 *   nop
 *   __temp3 = __temp1 + 5
 * </pre>
 *
 * Also works for subtraction, multiplication and division, and increment and decrement.
 */
class AdjacentArithmeticOptimizer extends LineOptimizer {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final Set<TokenType> FIRST_OPERATORS =
      ImmutableSet.of( //
          TokenType.BIT_AND,
          TokenType.BIT_OR, //
          TokenType.BIT_XOR, //
          TokenType.DIV, //
          TokenType.MINUS, //
          TokenType.MULT, //
          TokenType.PLUS);
  private static final Set<TokenType> PLUS_MINUS = ImmutableSet.of(TokenType.PLUS, TokenType.MINUS);
  private static final Set<TokenType> MULT_DIV = ImmutableSet.of(TokenType.MULT, TokenType.DIV);

  AdjacentArithmeticOptimizer(int debugLevel) {
    super(debugLevel);
  }

  @Override
  protected void preProcess() {
    // Converts a++ to a=a+1
    ExpandOptimizer expander = new ExpandOptimizer(debugLevel);
    code = new ArrayList<Op>(expander.optimize(ImmutableList.copyOf(code), symtab));
  }

  @Override
  protected void postProcess() {
    // Converts a=a+1 back to a++
    ContractOptimizer contract = new ContractOptimizer(debugLevel);
    code = new ArrayList<Op>(contract.optimize(ImmutableList.copyOf(code), symtab));
  }

  @Override
  public void visit(BinOp first) {
    TokenType firstOperator = first.operator();
    // Make sure left is numeric, right is constant, and the first operator is valid.
    if (!first.left().type().isNumeric()) {
      return;
    }
    if (!first.right().isConstant()) {
      return;
    }
    if (!FIRST_OPERATORS.contains(firstOperator)) {
      return;
    }

    // Potential first in sequence: foo=bar+constant
    BinOp second = getNext(BinOp.class);
    if (second == null) {
      return;
    }

    // Make sure second has compatible types with first, and that the second right
    // operand is a constant.
    if (!second.left().type().equals(first.left().type())) {
      return;
    }
    if (!second.right().isConstant()) {
      return;
    }
    TokenType secondOperator = second.operator();
    // Can only combine certain sets of operators.
    if (!compatibleOperators(secondOperator, firstOperator)) {
      return;
    }
    // Can only combine certain operands.
    if (!compatibleOperands(
        first.destination(), first.left(), second.destination(), second.left())) {
      return;
    }
    logger.at(loggingLevel).log("Potential pair: %s and %s", first, second);

    Operand combinedConstant =
        combine(first.right(), second.right(), firstOperator, secondOperator);
    if (combinedConstant == null) {
      return;
    }

    deleteCurrent();
    replaceAt(
        ip() + 1,
        new BinOp(
            second.destination(),
            first.left(),
            firstOperator,
            combinedConstant,
            second.position()));
  }

  // Only works for
  //  temp1 = temp2 + constant1
  //  temp3 = temp1 + constant2
  // OR
  //  var = var + constant1
  //  var = var + constant2
  private boolean compatibleOperands(
      Location firstDest, Operand firstLeft, Location secondDest, Operand secondLeft) {
    if (firstDest.isTemp() && firstDest.equals(secondLeft)) {
      // temp1 = temp2 + constant1
      // temp3 = temp1 + constant2
      return true;
    }
    // second case; all are the same.
    return firstDest.equals(firstLeft)
        && firstDest.equals(secondDest)
        && firstDest.equals(secondLeft);
  }

  private boolean compatibleOperators(TokenType firstOperator, TokenType secondOperator) {
    if (firstOperator == secondOperator) {
      return true;
    }
    if (PLUS_MINUS.contains(firstOperator) && PLUS_MINUS.contains(secondOperator)) {
      return true;
    }
    if (MULT_DIV.contains(firstOperator) && MULT_DIV.contains(secondOperator)) {
      return true;
    }
    return false;
  }

  /**
   * Tries to combine the first and second values and operators.
   *
   * @return the new constant operand, or null if they can't be combined.
   */
  private Operand combine(
      Operand firstOperand,
      Operand secondOperand,
      TokenType firstOperator,
      TokenType secondOperator) {
    Number firstConst = ConstantOperand.valueFromConstOperand(firstOperand);
    Number secondConst = ConstantOperand.valueFromConstOperand(secondOperand);

    if (firstOperand.type().isIntegral()) {
      long first = firstConst.longValue();
      long second = secondConst.longValue();
      switch (firstOperator) {
        case BIT_AND:
          return ConstantOperand.fromValue(first & second, firstOperand.type());

        case BIT_OR:
          return ConstantOperand.fromValue(first | second, firstOperand.type());

        case BIT_XOR:
          return ConstantOperand.fromValue(first ^ second, firstOperand.type());

        case MINUS:
        case PLUS:
          if (firstOperator == secondOperator) {
            // NOTE + for MINUS, because we're subtracting twice (e.g., -1 + -1 = -2)
            return ConstantOperand.fromValue(first + second, firstOperand.type());
          } else {
            // minus then plus:
            // temp2=temp1+first, temp3=temp2-second
            // =temp3=temp1+(first-second) (it uses PLUS)
            // OR plus, then minus:
            // temp2=temp1-first, temp3=temp2+second
            // temp3=temp1-first+second
            // =temp3=temp1-(first-second) (it uses MINUS)
            return ConstantOperand.fromValue(first - second, firstOperand.type());
          }
        case DIV:
        case MULT:
          if (firstOperator == secondOperator) {
            // NOTE * for DIV too, because we're dividing twice (e.g., /5/2 = /10)
            return ConstantOperand.fromValue(first * second, firstOperand.type());
          } else {
            // div, then mult
            // temp2=temp1/first, temp3=temp2*second
            // =temp3=(temp1/first)*second
            // =temp3=temp/(first/second)
            // mult then div:
            // temp2=temp1*first, temp3=temp2/second
            // =temp3=(temp1*first)/second
            // =temp3=temp*(first/second)
            if (firstConst.intValue() < secondConst.intValue() || secondConst.intValue() == 0) {
              logger.at(loggingLevel).log("Refusing to optimize due to rounding or div by 0");
              return null;
            }
            return ConstantOperand.fromValue(first / second, firstOperand.type());
          }
        default:
          return null;
      }
    }

    if (firstOperand.type() == VarType.DOUBLE) {
      switch (firstOperator) {
        case PLUS:
        case MINUS:
          if (firstOperator == secondOperator) {
            return ConstantOperand.of(firstConst.doubleValue() + secondConst.doubleValue());
          } else {
            return ConstantOperand.of(firstConst.doubleValue() - secondConst.doubleValue());
          }
        case DIV:
        case MULT:
          if (firstOperator == secondOperator) {
            return ConstantOperand.of(firstConst.doubleValue() * secondConst.doubleValue());
          } else {
            if (secondConst.doubleValue() == 0) {
              logger.at(loggingLevel).log("Refusing to optimize due to div by 0.0");
              return null;
            }
            return ConstantOperand.of(firstConst.doubleValue() / secondConst.doubleValue());
          }
        default:
          return null;
      }
    }

    logger.at(loggingLevel).log("Cannot optimize operator %s yet", firstOperator);
    return null;
  }

  private static class ExpandOptimizer extends LineOptimizer {
    ExpandOptimizer(int debugLevel) {
      super(debugLevel);
    }

    private void expand(Op op, TokenType operand) {
      Location destination = op.getDestination();
      Operand one = ConstantOperand.fromValue(1, destination.type());
      replaceCurrent(new BinOp(destination, destination, operand, one, op.position()));
    }

    @Override
    public void visit(Dec op) {
      expand(op, TokenType.MINUS);
    }

    @Override
    public void visit(Inc op) {
      expand(op, TokenType.PLUS);
    }
  }

  private static class ContractOptimizer extends LineOptimizer {
    ContractOptimizer(int debugLevel) {
      super(debugLevel);
    }

    @Override
    public void visit(BinOp op) {
      TokenType operator = op.operator();
      if (operator != TokenType.PLUS && operator != TokenType.MINUS) {
        return;
      }
      Location destination = op.destination();
      if (!destination.type().isIntegral()) {
        return;
      }
      if (!op.left().equals(destination)) {
        return;
      }
      if (!ConstantOperand.isAnyIntOne(op.right())) {
        return;
      }
      if (operator == TokenType.PLUS) {
        replaceCurrent(new Inc(destination, op.position()));
      } else {
        replaceCurrent(new Dec(destination, op.position()));
      }
    }
  }
}
