package com.plasstech.lang.d2.optimize;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VarType;

class TempPropagationOptimizer extends LineOptimizer {

  private static final ImmutableList<TokenType> VALID_OPS =
      ImmutableList.of(
          TokenType.EQEQ,
          TokenType.NEQ,
          TokenType.LT,
          TokenType.GT,
          TokenType.DOT,
          TokenType.LEQ,
          TokenType.GEQ,
          TokenType.PLUS,
          TokenType.MINUS,
          TokenType.BIT_AND,
          TokenType.BIT_OR,
          TokenType.AND,
          TokenType.OR,
          TokenType.XOR,
          TokenType.DOT,
          TokenType.SHIFT_LEFT,
          TokenType.SHIFT_RIGHT);

  TempPropagationOptimizer(int debugLevel) {
    super(debugLevel);
  }

  @Override
  public void visit(BinOp op) {
    if (!op.destination().isTemp()) {
      return;
    }
    // if the next line is an assignment to this destination, merge them.
    Op next = getOpAt(ip() + 1);
    if (next != null && next instanceof Transfer) {
      Transfer candidate = (Transfer) next;
      if (op.destination().equals(candidate.source())
          && canApply(op, candidate)) {
        replaceAt(ip() + 1,
            new BinOp(candidate.destination(),
                op.left(), op.operator(), op.right(), op.position()));
        deleteCurrent();
      }
    }
  }

  private static boolean canApply(BinOp op, Transfer candidate) {
    // only allow params (stored in registers), because all other destinations do not play
    // nicely with binary operations.
    if (candidate.destination().storage() == SymbolStorage.PARAM) {
      return true;
    }
    if (!op.right().isConstant()) {
      return false;
    }
    if (op.left().type() == VarType.DOUBLE) {
      // double constants are globals, so we can't typically use them as a right-hand-side
      return false;
    }
    // we can only do memory = reg <op> constant if the op is allowed.
    return VALID_OPS.contains(op.operator());
  }
}
