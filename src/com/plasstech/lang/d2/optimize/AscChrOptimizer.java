package com.plasstech.lang.d2.optimize;

import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.common.TokenType;

/**
 * When there's an asc next to a chr, optimizes.
 * 
 * <pre>
 * temp1 = chr(anything)
 * temp2 = asc(temp1)
 * </pre>
 * 
 * becomes `temp2 = anything`
 * 
 * <p>
 * Similarly,
 * 
 * <pre>
 * temp1 = asc(anything)
 * temp2 = chr(temp1)
 * </pre>
 * 
 * becomes `temp2 = anything[0]`
 */
public class AscChrOptimizer extends LineOptimizer {
  AscChrOptimizer(int debugLevel) {
    super(debugLevel);
  }

  @Override
  public void visit(UnaryOp first) {
    if (first.operator() != TokenType.ASC && first.operator() != TokenType.CHR) {
      return;
    }
    UnaryOp second = getNext(UnaryOp.class);
    if (second == null) {
      return;
    }
    // Make sure the intermediates are temps
    if (!first.destination().isTemp() || !second.destination().isTemp()) {
      return;
    }
    // Make sure second works on first
    if (!second.operand().equals(first.destination())) {
      return;
    }
    // Need to be asc/chr or chr/asc
    if (second.operator() != opposite(first.operator())) {
      return;
    }
    if (first.operator() == TokenType.CHR) {
      // temp1 = chr(anything)
      // temp2 = asc(temp1)
      // becomes `temp2 = anything`
      deleteCurrent();
      replaceAt(ip() + 1, new Transfer(second.destination(), first.operand(), first.position()));
    } else {
      // temp1 = asc(anything)
      // temp2 = chr(temp1)
      // becomes `temp2 = anything[0]`
      deleteCurrent();
      replaceAt(ip() + 1, new BinOp(second.destination(), first.operand(), TokenType.LBRACKET,
          ConstantOperand.of(0), first.position()));
    }
  }

  private TokenType opposite(TokenType operator) {
    if (operator == TokenType.ASC) {
      return TokenType.CHR;
    }
    if (operator == TokenType.CHR) {
      return TokenType.ASC;
    }
    return null;
  }
}
