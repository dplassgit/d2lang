package com.plasstech.lang.d2.optimize;

import com.google.common.collect.ImmutableSet;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Labels;
import com.plasstech.lang.d2.codegen.TempLocation;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.VarType;
import com.plasstech.lang.d2.type.VariableSymbol;
import java.util.Set;

/**
 * When comparing the first character of a string, optimizations can be done:
 *
 * <pre>
 * temp1 = s[0]
 * temp2 = temp1 == 'h'
 * </pre>
 *
 * becomes:
 *
 * <pre>
 * temp1 = asc(s)
 * temp3 = asc('h') // this only works if both are 1 character...
 * temp2 = temp1 == temp3
 * </pre>
 *
 * This avoids creating a new string for s[0], because asc just looks at the first character of a
 * string.
 */
public class StringCompareOptimizer extends LineOptimizer {
  private static final Set<TokenType> COMPARISONS =
      ImmutableSet.of(
          TokenType.EQEQ, TokenType.NEQ, TokenType.LEQ, TokenType.GEQ, TokenType.LT, TokenType.GT);

  StringCompareOptimizer(int debugLevel) {
    super(debugLevel);
  }

  @Override
  public void visit(BinOp first) {
    // Make sure first is: temp1=s[some int]
    if (first.operator() != TokenType.LBRACKET) {
      return;
    }
    if (first.left().type() != VarType.STRING) {
      return;
    }
    if (!first.destination().isTemp()) {
      return;
    }
    // make sure second is: anything = temp1 == (some string)
    BinOp second = getNext(BinOp.class);
    if (second == null) {
      // Not even binop
      return;
    }
    if (!second.left().equals(first.destination())) {
      return;
    }
    if (!COMPARISONS.contains(second.operator())) {
      return;
    }
    if (!second.right().isConstant()) {
      return;
    }
    String rightConstant = ConstantOperand.stringValueFromConstOperand(second.right());
    if (rightConstant.length() != 1) {
      // We know what to do with == and !=, so we can just replace it now.
      // Can't deal with inequalities because "a" < "bc" is unknown at compile time
      if (second.operator() == TokenType.EQEQ) {
        // replace with false
        deleteCurrent();
        replaceAt(
            ip() + 1, new Transfer(second.destination(), ConstantOperand.FALSE, second.position()));
      } else if (second.operator() == TokenType.NEQ) {
        // replace with true
        deleteCurrent();
        replaceAt(
            ip() + 1, new Transfer(second.destination(), ConstantOperand.TRUE, second.position()));
      }
      return;
    }

    // make sure temp1 = s[0]
    if (!ConstantOperand.isAnyZero(first.right())) {
      return;
    }
    // temp1 = s[0]
    // temp2 = temp1 == 'h'
    // becomes:
    // temp1 = asc(s)
    // temp3 = asc('h')
    // temp2 = temp1 == temp3
    TempLocation newTempFirst = newTemp(VarType.INT);
    replaceCurrent(new UnaryOp(newTempFirst, TokenType.ASC, first.left(), first.position()));
    TempLocation temp3 = newTemp(VarType.INT);
    replaceAt(ip() + 1, new UnaryOp(temp3, TokenType.ASC, second.right(), second.position()));
    code.add(
        ip() + 2,
        new BinOp(second.destination(), newTempFirst, second.operator(), temp3, second.position()));
    stop();
  }

  private TempLocation newTemp(VarType type) {
    VariableSymbol symbol = symtab.declareTemp(Labels.nextLabel("stringcompare"), type);
    TempLocation temp = new TempLocation(symbol);
    return temp;
  }
}
