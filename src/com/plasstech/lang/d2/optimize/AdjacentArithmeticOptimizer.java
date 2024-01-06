package com.plasstech.lang.d2.optimize;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Dec;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.Inc;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VarType;

/**
 * Optimizer that optimizes TEMPS of the pattern: But there's really no reason why it can't also
 * deal with non-temps...
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
 * (Also works for subtraction, multiplication and division.)
 */
class AdjacentArithmeticOptimizer extends LineOptimizer {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final Set<TokenType> FIRST_OPERATORS = ImmutableSet.of(//
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
  public void visit(BinOp first) {
    TokenType firstOperator = first.operator();
    if (first.left().type().isNumeric() && first.right().isConstant()
        && FIRST_OPERATORS.contains(firstOperator)) {

      // Potential first in sequence: foo=bar+constant
      Op secondOp = getOpAt(ip() + 1);
      // if first is plus or minus and second is inc/dec, allow it after expanding the inc/dec.
      if (!(secondOp instanceof BinOp)) {
        secondOp = expand(secondOp);
        if (secondOp == null) {
          return;
        }
      }

      // second in sequence
      BinOp second = (BinOp) secondOp;
      TokenType secondOperator = second.operator();
      if (second.left().type().equals(first.left().type()) && second.right().isConstant()
          && areCompatible(secondOperator, firstOperator)
          && second.left().equals(first.destination())) {

        logger.at(loggingLevel).log("Potential pair: %s and %s", first, second);

        Operand combinedConstant = combine(first.right(),
            second.right(),
            firstOperator,
            secondOperator);
        if (combinedConstant == null) {
          return;
        }
        if (first.destination().storage() == SymbolStorage.TEMP
            && second.destination().storage() == SymbolStorage.TEMP) {
          // Only do it if all are temps. Otherwise it might nop an assignment
          deleteCurrent();
          replaceAt(ip() + 1,
              new BinOp(second.destination(), first.left(), firstOperator,
                  combinedConstant, second.position()));
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

  /**
   * Tries to combine the first and second values and operators.
   * 
   * @return the new constant operand, or null if they can't be combined.
   */
  private Operand combine(Operand firstOperand, Operand secondOperand, TokenType firstOperator,
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
          break;
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
          break;
      }
    }
    logger.at(loggingLevel).log("Cannot optimize operator %s yet", firstOperator);
    return null;
  }

  /** "Expand" the given op - Dec becomes i=i-1 and Inc becomes i=i+1 */
  private static BinOp expand(Op op) {
    if (op == null) {
      return null;
    }
    Expander expander = new Expander();
    op.accept(expander);
    return expander.expanded;
  }

  private static class Expander extends DefaultOpcodeVisitor {
    private BinOp expanded;

    @Override
    public void visit(BinOp op) {
      expanded = op;
    }

    @Override
    public void visit(Dec op) {
      ConstantOperand<? extends Number> one = ConstantOperand.fromValue(1, op.target().type());
      expanded = new BinOp(op.target(), op.target(), TokenType.MINUS, one, op.position());
    }

    @Override
    public void visit(Inc op) {
      ConstantOperand<? extends Number> one = ConstantOperand.fromValue(1, op.target().type());
      expanded = new BinOp(op.target(), op.target(), TokenType.PLUS, one, op.position());
    }
  }
}
