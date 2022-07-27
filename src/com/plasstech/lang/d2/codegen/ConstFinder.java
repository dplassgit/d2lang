package com.plasstech.lang.d2.codegen;

import java.util.Optional;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.AllocateOp;
import com.plasstech.lang.d2.codegen.il.ArrayAlloc;
import com.plasstech.lang.d2.codegen.il.ArraySet;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Call;
import com.plasstech.lang.d2.codegen.il.Dec;
import com.plasstech.lang.d2.codegen.il.FieldSetOp;
import com.plasstech.lang.d2.codegen.il.Goto;
import com.plasstech.lang.d2.codegen.il.IfOp;
import com.plasstech.lang.d2.codegen.il.Inc;
import com.plasstech.lang.d2.codegen.il.Label;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.OpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.ProcEntry;
import com.plasstech.lang.d2.codegen.il.ProcExit;
import com.plasstech.lang.d2.codegen.il.Return;
import com.plasstech.lang.d2.codegen.il.Stop;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.type.VarType;

/** Traverses all opcodes and finds constants of the given type. */
class ConstFinder<T> implements OpcodeVisitor {
  private final ConstTable<T> constTable;
  private final Predicate<VarType> correctType;

  ConstFinder(ConstTable<T> constTable, Predicate<VarType> correctTypePredicate) {
    this.constTable = constTable;
    this.correctType = correctTypePredicate;
  }

  void find(ImmutableList<Op> code) {
    for (Op opcode : code) {
      opcode.accept(this);
    }
  }

  private void addEntry(Operand operand) {
    if (operand.isConstant() && correctType.test(operand.type())) {
      ConstantOperand<T> constOp = (ConstantOperand<T>) operand;
      constTable.add(constOp.value());
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

  @Override
  public void visit(Label op) {
  }

  @Override
  public void visit(Stop op) {
  }

  @Override
  public void visit(Goto op) {
  }

  @Override
  public void visit(ProcExit op) {
  }

  @Override
  public void visit(ProcEntry op) {
  }

  @Override
  public void visit(Dec op) {
  }

  @Override
  public void visit(Inc op) {
  }

  @Override
  public void visit(AllocateOp op) {}

  @Override
  public void visit(ArrayAlloc arrayAlloc) {
    addEntry(arrayAlloc.sizeLocation());
  }
}
