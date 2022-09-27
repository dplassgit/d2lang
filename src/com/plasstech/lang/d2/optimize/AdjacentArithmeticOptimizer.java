package com.plasstech.lang.d2.optimize;

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

  AdjacentArithmeticOptimizer(int debugLevel) {
    super(debugLevel);
  }

  private static int fromIntOperand(Operand op) {
    return ((ConstantOperand<Integer>) op).value();
  }

  @Override
  public void visit(BinOp first) {
    TokenType firstOperator = first.operator();
    if (first.left().type() == VarType.INT // VarType.isNumeric(first.left().type())
        && first.right().isConstant()
        // TODO: this can be expanded to other operators
        && (firstOperator == TokenType.MULT || firstOperator == TokenType.PLUS)) {

      // Potential first in sequence: foo=bar+constant
      Op secondOp = getOpAt(ip() + 1);
      if (!(secondOp instanceof BinOp)) {
        return;
      }

      // second in sequence
      BinOp second = (BinOp) secondOp;
      if (second.left().type().equals(first.left().type())
          && second.right().isConstant()
          // TODO: relax this later
          && second.operator() == firstOperator
          && second.left().equals(first.destination())) {
        logger.atInfo().log("Potential pair: %s and %s", first, second);
        int firstConst = fromIntOperand(first.right());
        int secondConst = fromIntOperand(second.right());
        int newValue = 0;
        switch (firstOperator) {
          case PLUS:
            newValue = firstConst + secondConst;
            break;
          case MULT:
            newValue = firstConst * secondConst;
            break;
          default:
            logger.atInfo().log("Cannot optimize operator yet %s", firstOperator);
            return;
        }
        deleteCurrent();
        replaceAt(
            ip() + 1,
            new BinOp(
                second.destination(),
                first.left(),
                firstOperator,
                ConstantOperand.of(newValue),
                second.position()));
      }
    }
  }
}
