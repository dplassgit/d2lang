package com.plasstech.lang.d2.optimize;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.ParamLocation;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Call;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VarType;

/**
 * Optimizes
 * 
 * <pre>
 * temp1 = a + 3
 * b = temp1
 * </pre>
 * 
 * into:
 * 
 * <pre>
 * b = a + 3
 * </pre>
 * 
 * (Also UnaryOps) for b as a "param" only
 */
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
          TokenType.SHIFT_RIGHT,
          TokenType.COLON);

  TempPropagationOptimizer(int debugLevel) {
    super(debugLevel);
  }

  @Override
  public void visit(Call op) {
    op.destination().ifPresent(destination -> {
      if (!destination.isTemp()) {
        return;
      }
      // if the next line is an assignment to this destination, merge them.
      Transfer candidate = getNext(Transfer.class);
      if (candidate == null) {
        return;
      }
      if (destination.equals(candidate.source())) {
        deleteCurrent();
        // We don't need to worry about the destination type (bug #271) because the results of
        // calls are in RAX/XMM0 which can be MOV'd to any type of defination.
        replaceAt(ip() + 1,
            new Call(candidate.destination(),
                op.procSym(),
                op.actuals(),
                op.formals(),
                candidate.position()));
      }
    });
  }

  @Override
  public void visit(UnaryOp op) {
    if (!op.destination().isTemp()) {
      return;
    }
    // if the next line is an assignment to this destination, merge them.
    Transfer candidate = getNext(Transfer.class);
    if (candidate == null) {
      return;
    }
    if (op.destination().equals(candidate.source()) && canApply(op.operand(), candidate)) {
      deleteCurrent();
      replaceAt(ip() + 1, op.setDestination(candidate.destination()));
    }
  }

  @Override
  public void visit(BinOp op) {
    if (!op.destination().isTemp()) {
      return;
    }
    // if the next line is an assignment to this destination, merge them.
    Transfer candidate = getNext(Transfer.class);
    if (candidate == null) {
      return;
    }
    if (op.destination().equals(candidate.source()) && canApply(op, candidate)) {
      deleteCurrent();
      replaceAt(ip() + 1, op.setDestination(candidate.destination()));
    }
  }

  private static boolean canApply(BinOp op, Transfer candidate) {
    // only allow params (stored in registers), because all other destinations do not play
    // nicely with binary operations.
    // Bug #271: see if we really need these constraints.
    if (candidate.destination().storage() == SymbolStorage.PARAM) {
      // only return true if it's param 0-3, which will be in a register.
      ParamLocation param = (ParamLocation) candidate.destination();
      return param.index() <= 3;
    }
    if (!op.right().isConstant()) {
      // WHY?!
      return false;
    }
    if (op.left().type() == VarType.DOUBLE) {
      // double constants are globals, so we can't typically use them as a right-hand-side
      // WHY?!
      return false;
    }
    // we can only do memory = reg <op> constant if the op is allowed.
    return VALID_OPS.contains(op.operator());
  }

  private boolean canApply(Operand source, Transfer candidate) {
    // only allow params (stored in registers), because all other destinations do not play
    // nicely with binary operations.
    // Bug #271: see if we really need these constraints.
    if (candidate.destination().storage() == SymbolStorage.PARAM) {
      // only return true if it's param 0-3, which will be in a register.
      ParamLocation param = (ParamLocation) candidate.destination();
      return param.index() <= 3;
    }
    // Otherwise, not.
    return false;
  }
}
