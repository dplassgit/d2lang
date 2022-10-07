package com.plasstech.lang.d2.optimize;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.VarType;

/**
 * Optimizer that optimizes:
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
 * (Also works for multiplication.)
 *
 * <p>For future expansion, it will also do:
 *
 * <pre>
 *  __temp2 = __temp1 - 2
 *  __temp3 = __temp2 + 3
 * </pre>
 *
 * into:
 *
 * <pre>
 *   nop
 *   __temp3 = __temp1 + 1
 * </pre>
 */
class AdjacentArithmeticOptimizer extends LineOptimizer {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private static final Set<TokenType> FIRST_OPERATORS =
      ImmutableSet.of(TokenType.PLUS, TokenType.MULT, TokenType.MINUS, TokenType.DIV);
  private static final Set<TokenType> PLUS_MINUS = ImmutableSet.of(TokenType.PLUS, TokenType.MINUS);
  private static final Set<TokenType> MULT_DIV = ImmutableSet.of(TokenType.MULT, TokenType.DIV);

  AdjacentArithmeticOptimizer(int debugLevel) {
    super(debugLevel);
  }

  @Override
  public void visit(BinOp first) {
    TokenType firstOperator = first.operator();
    if (VarType.isNumeric(first.left().type())
        && first.right().isConstant()
        // TODO: this can be expanded to other operators
        && (FIRST_OPERATORS.contains(firstOperator))) {

      // Potential first in sequence: foo=bar+constant
      Op secondOp = getOpAt(ip() + 1);
      if (!(secondOp instanceof BinOp)) {
        return;
      }

      // second in sequence
      BinOp second = (BinOp) secondOp;
      TokenType secondOperator = second.operator();
      if (second.left().type().equals(first.left().type())
          && second.right().isConstant()
          && areCompatible(secondOperator, firstOperator)
          && second.left().equals(first.destination())) {

        logger.at(loggingLevel).log("Potential pair: %s and %s", first, second);

        Operand combinedOperand =
            combine(first.right(), second.right(), firstOperator, secondOperator);
        if (combinedOperand != null) {
          deleteCurrent();
          replaceAt(
              ip() + 1,
              new BinOp(
                  second.destination(),
                  first.left(),
                  firstOperator,
                  combinedOperand,
                  second.position()));
        }
      }
    }
  }

  private boolean areCompatible(TokenType firstOperator, TokenType secondOperator) {
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

  private Operand combine(
      Operand left, Operand right, TokenType firstOperator, TokenType secondOperator) {
    Number firstConst = fromConstOperand(left);
    Number secondConst = fromConstOperand(right);
    if (left.type() == VarType.INT) {
      switch (firstOperator) {
        case MINUS:
        case PLUS:
          if (firstOperator == secondOperator) {
            // NOTE + for MINUS, because we're subtracting twice (e.g., -1 + -1 = -2)
            return ConstantOperand.of(firstConst.intValue() + secondConst.intValue());
          } else {
            // minus then plus:
            // temp2=temp1+first, temp3=temp2-second
            // =temp3=temp1+(first-second) (it uses PLUS)
            // OR plus, then minus:
            // temp2=temp1-first, temp3=temp2+second
            // temp3=temp1-first+second
            // =temp3=temp1-(first-second) (it uses MINUS)
            return ConstantOperand.of(firstConst.intValue() - secondConst.intValue());
          }
        case DIV:
        case MULT:
          if (firstOperator == secondOperator) {
            // NOTE * for DIV too, because we're dividing twice (e.g., /5/2 = /10)
            return ConstantOperand.of(firstConst.intValue() * secondConst.intValue());
          } else {
            // div, then mult
            // temp2=temp1/first, temp3=temp2*second
            // =temp3=(temp1/first)*second
            // =temp3=temp/(first/second) (NOTE DIV) THIS IS BROKEN because of rounding.
            // should be instead temp*(second/first)
            // mult then div:
            // temp2=temp1*first, temp3=temp2/second
            // =temp3=(temp1*first)/second
            // =temp3=temp*(first/second)
            if (firstConst.intValue() < secondConst.intValue() || secondConst.intValue() == 0) {
              logger.at(loggingLevel).log("Refusing to optimize due to rounding or div by 0");
              return null;
            }
            return ConstantOperand.of(firstConst.intValue() / secondConst.intValue());
          }
        default:
          break;
      }
    } else if (left.type() == VarType.DOUBLE) {
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
          break;
      }
    }
    logger.at(loggingLevel).log("Cannot optimize operator yet %s", firstOperator);
    return null;
  }

  private Number fromConstOperand(Operand op) {
    ConstantOperand<?> co = (ConstantOperand<?>) op;
    return (Number) co.value();
  }
}