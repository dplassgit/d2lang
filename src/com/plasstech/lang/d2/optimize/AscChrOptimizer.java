package com.plasstech.lang.d2.optimize;

import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.VarType;

/**
 * When there's an asc next to a chr, optimizes.
 *
 * <pre>
 * temp1 = chr(anyint) // must be temp
 * anystring = asc(temp1)
 * </pre>
 *
 * becomes `anystring = anyint`
 *
 * <p>Similarly,
 *
 * <pre>
 * temp1 = asc(anystring[0]) // must be temp
 * anystring2 = chr(temp1)
 * </pre>
 *
 * becomes `anystring2 = anything[0]`
 *
 * <p>Also,
 *
 * <pre>
 *  temp=anystring[0] // must be temp
 *  anyint=asc(temp)
 * </pre>
 *
 * becomes `anyint=asc(anystring)`
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
      replaceAt(
          ip() + 1,
          new BinOp(
              second.destination(),
              first.operand(),
              TokenType.LBRACKET,
              ConstantOperand.of(0),
              first.position()));
    }
  }

  @Override
  public void visit(BinOp first) {
    if (first.operator() != TokenType.LBRACKET) {
      return;
    }
    if (!ConstantOperand.isAnyZero(first.right())) {
      return;
    }
    if (first.left().type() != VarType.STRING) {
      return;
    }
    if (!first.destination().isTemp()) {
      return;
    }
    // first is temp=somestring[0]
    UnaryOp second = getNext(UnaryOp.class);
    if (second == null) {
      return;
    }
    if (second.operator() != TokenType.ASC) {
      return;
    }
    if (!second.operand().equals(first.destination())) {
      return;
    }
    // Second is anything=asc(temp)
    // Replace second with anything=asc(somestring)
    deleteCurrent();
    replaceAt(ip() + 1, second.setSource(second.operand(), first.left()));
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
