package com.plasstech.lang.d2.codegen;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.IfOp;
import com.plasstech.lang.d2.codegen.il.Label;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.common.D2RuntimeException;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.phase.Phase;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.type.SymbolTable;
import com.plasstech.lang.d2.type.VarType;
import com.plasstech.lang.d2.type.VariableSymbol;
import java.util.ArrayList;
import java.util.List;

/**
 * "Part 2" of IL Code generation: currently only expands the null coalesce operator. This isn't
 * needed for other operators because there are typically direct CPU implementations of them, except
 * for null coalesce.
 */
public class ILCodeGeneratorPart2 extends DefaultOpcodeVisitor implements Phase {
  private final List<Op> augmentedCode = new ArrayList<>();
  private SymbolTable symbolTable;
  private boolean changed;

  @Override
  public State execute(State input) {
    symbolTable = input.symbolTable();
    augmentedCode.clear();
    try {
      // lastIlCode reads either the optimized or pre-optimized (if no optimized code)
      augment(input.lastIlCode());
      return input.setIlCode(ImmutableList.copyOf(augmentedCode));
    } catch (D2RuntimeException re) {
      return input.addException(re);
    }
  }

  private ImmutableList<Op> augment(ImmutableList<Op> code) {
    for (Op op : code) {
      changed = false;
      op.accept(this);
      if (!changed) {
        // The "accept/visit" didn't change anything; add this op manually.
        augmentedCode.add(op);
      }
    }
    return code;
  }

  private TempLocation allocateTemp(VarType varType) {
    String name = Labels.nextLabel("part2temp");
    VariableSymbol symbol = symbolTable.declareTemp(name, varType);
    return new TempLocation(symbol);
  }

  private Location allocateLongTemp(VarType varType) {
    String name = Labels.nextLabel("part2longtemp");
    VariableSymbol symbol = symbolTable.declareLongTemp(name, varType);
    return new LongTempLocation(symbol);
  }

  @Override
  public void visit(BinOp op) {
    if (op.operator() != TokenType.NULL_COALESCE) {
      return;
    }
    emitNullCoalesce(op);
  }

  private void emitNullCoalesce(BinOp op) {
    Location dest = op.destination();
    Operand left = op.left();
    Operand right = op.right();
    String endLabel = Labels.nextLabel("null_coalesce_end");
    if (dest.isTemp()) {
      // nullDest=left
      Location nullDest = allocateLongTemp(dest.type());
      emit(new Transfer(nullDest, left, op.position()));
      // temp=nullDest==null
      Location temp = allocateTemp(VarType.BOOL);
      Operand nullOfAppropriateType = new ConstantOperand<Void>(null, left.type());
      emit(new BinOp(temp, nullDest, TokenType.EQEQ, nullOfAppropriateType, op.position()));
      // if not temp goto end
      emit(new IfOp(temp, endLabel, true, op.position()));
      // nullDest=right
      emit(new Transfer(nullDest, right, op.position()));
      // end:
      emit(new Label(endLabel));
      // dest=nullDest
      emit(new Transfer(dest, nullDest, op.position()));
      return;
    }

    // if dest is not temp:
    // dest=left
    emit(new Transfer(dest, left, op.position()));
    // temp=dest==null
    Location temp = allocateTemp(VarType.BOOL);
    Operand nullOfAppropriateType = new ConstantOperand<Void>(null, left.type());
    emit(new BinOp(temp, dest, TokenType.EQEQ, nullOfAppropriateType, op.position()));
    // if not temp goto end
    emit(new IfOp(temp, endLabel, true, op.position()));
    // dest=right
    emit(new Transfer(dest, right, op.position()));
    // end:
    emit(new Label(endLabel));
  }

  private void emit(Op op) {
    augmentedCode.add(op);
    changed = true;
  }
}
