package com.plasstech.lang.d2.codegen;

import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.ArraySet;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Call;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.FieldSetOp;
import com.plasstech.lang.d2.codegen.il.IfOp;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Return;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.type.VarType;

class DoubleFinder extends DefaultOpcodeVisitor {
  private final DoubleTable doubleTable = new DoubleTable();

  public DoubleTable execute(ImmutableList<Op> code) {
    for (Op opcode : code) {
      opcode.accept(this);
    }
    return doubleTable;
  }

  private void addEntry(Operand operand) {
    if (operand.isConstant() && operand.type() == VarType.DOUBLE) {
      ConstantOperand<Double> constOp = (ConstantOperand<Double>) operand;
      doubleTable.addEntry(constOp.value());
    }
  }

  private void addEntry(Optional<Operand> op) {
    if (op.isPresent()) {
      addEntry(op.get());
    }
  }

  @Override
  public void visit(BinOp op) {
    addEntry(op.left());
    addEntry(op.right());
  }

  @Override
  public void visit(UnaryOp op) {
    addEntry(op.operand());
  }

  @Override
  public void visit(Return op) {
    addEntry(op.returnValueLocation());
  }

  @Override
  public void visit(Transfer op) {
    addEntry(op.source());
  }

  @Override
  public void visit(ArraySet op) {
    addEntry(op.source());
  }

  @Override
  public void visit(SysCall op) {
    addEntry(op.arg());
  }

  @Override
  public void visit(Call op) {
    for (Operand actual : op.actuals()) {
      addEntry(actual);
    }
  }

  @Override
  public void visit(IfOp op) {
    addEntry(op.condition());
  }

  @Override
  public void visit(FieldSetOp op) {
    addEntry(op.source());
  }
}
