package com.plasstech.lang.d2.codegen;

import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Call;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.IfOp;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Return;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;

/*
 * what should the output of this class be?
 *
 * map string -> string entry
 * where a string entry is:
 *    synthetic name
 *    AND:
 *      string
 *      OR
 *      other string entry plus offset
 * what's "name"? how will the code generator know what to use where?
 * thoughts:
 *  * codegen asks the string finder for a list of data entries  < easy
 *  * for each string codegen asks the string finder -- based on the constant - which name to reference
 */

public class StringFinder extends DefaultOpcodeVisitor {
  private final StringTable stringTable = new StringTable();

  public StringTable execute(ImmutableList<Op> code) {
    for (Op opcode : code) {
      opcode.accept(this);
    }
    return stringTable;
  }

  private void addEntry(Operand operand) {
    if (operand.isConstant()) {
      ConstantOperand<?> constOp = (ConstantOperand<?>) operand;
      Object value = constOp.value();
      if (value instanceof String) {
        stringTable.addEntry((String) value);
      }
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
}