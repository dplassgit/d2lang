package com.plasstech.lang.d2.optimize;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.il.ArrayAlloc;
import com.plasstech.lang.d2.codegen.il.ArraySet;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.FieldSetOp;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.common.ArraySizeException;
import com.plasstech.lang.d2.common.D2RuntimeException;
import com.plasstech.lang.d2.common.DivisionByZeroException;
import com.plasstech.lang.d2.common.InvalidIndexException;
import com.plasstech.lang.d2.common.Range;
import com.plasstech.lang.d2.phase.Phase;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.type.SymbolTable;
import com.plasstech.lang.d2.type.VarType;

/**
 * Checks ranges, e.g., negative array sizes, negative indexes, nulls, etc. Can be run as a Phase or
 * as an Optimizer
 */
public class RangeChecker extends DefaultOpcodeVisitor implements Phase, Optimizer {

  @Override
  public ImmutableList<Op> optimize(ImmutableList<Op> program, SymbolTable symtab) {
    for (Op op : program) {
      op.accept(this);
    }
    return program;
  }

  @Override
  public State execute(State input) {
    ImmutableList<Op> code = input.lastIlCode();
    try {
      optimize(code, null);
    } catch (D2RuntimeException e) {
      return input.addException(e);
    }

    return input;
  }

  @Override
  public void visit(ArrayAlloc op) {
    if (!op.sizeLocation().isConstant()) {
      return;
    }
    int size = ConstantOperand.valueFromConstOperand(op.sizeLocation()).intValue();
    if (size < 0) {
      throw new ArraySizeException(op.position(), "ARRAY size must be non-negative; was %d", size);
    }
  }

  @Override
  public void visit(UnaryOp op) {
    switch (op.operator()) {
      case LENGTH:
      case ASC:
        if (op.operand().isNull()) {
          throw new D2RuntimeException("Null pointer error", op.position(), "Null");
        }
        break;

      default:
        break;
    }
  }

  @Override
  public void visit(BinOp op) {
    Operand left = op.left();
    Operand right = op.right();
    switch (op.operator()) {
      case DIV:
      case MOD:
        if (ConstantOperand.isAnyZero(right)) {
          throw new DivisionByZeroException(op.position());
        }
        break;

      case PLUS:
        if (left.type() == VarType.STRING || right.type() == VarType.STRING) {
          if (right.isNull() || left.isNull()) {
            throw new D2RuntimeException("Cannot add NULL to STRING", op.position(),
                "Null pointer");
          }
        }
        break;

      case DOT:
        if (left.isNull()) {
          throw new D2RuntimeException(
              String.format("Cannot retrieve field %s of NULL RECORD", right.toString()),
              op.position(), "Null pointer");
        }
        break;

      case LBRACKET:
        if (left.isNull()) {
          throw new D2RuntimeException("Cannot index into NULL object", op.position(),
              "Null pointer");
        }
        if (!right.isConstant()) {
          return;
        }
        if (right.type() == VarType.INT) {
          int index = ConstantOperand.valueFromConstOperand(right).intValue();
          if (left.type() == VarType.RANGE) {
            if (index < 0 || index > 1) {
              throw new InvalidIndexException(
                  op.position(), "RANGE index must be 0 or 1; was %d", index);
            }
          }
          if (index < 0) {
            throw new InvalidIndexException(
                op.position(), "%s index must be non-negative; was %d", left.type(), index);
          }
          if (left.isConstant() && left.type() == VarType.STRING) {
            String value = ConstantOperand.stringValueFromConstOperand(left);
            if (index >= value.length()) {
              throw new InvalidIndexException(
                  op.position(),
                  "STRING index out of bounds (length %d); was %d",
                  value.length(), index);
            }
          }
        }
        if (right.type() == VarType.RANGE) {
          Range range = ConstantOperand.rangeValueFromConstOperand(right);
          if (range.start() < 0) {
            throw new InvalidIndexException(
                op.position(),
                "%s RANGE start index must be non-negative; was %d", left.type(), range.start());
          }
          if (range.end() < 0) {
            throw new InvalidIndexException(
                op.position(),
                "%s RANGE end index must be non-negative; was %d", left.type(), range.end());
          }
          if (left.isConstant() && left.type() == VarType.STRING) {
            String value = ConstantOperand.stringValueFromConstOperand(left);
            if (range.start() >= value.length()) {
              throw new InvalidIndexException(
                  op.position(),
                  "STRING start RANGE out of bounds (length %d); was %d",
                  value.length(), range.start());
            }
            if (range.end() > value.length()) {
              throw new InvalidIndexException(
                  op.position(),
                  "STRING end RANGE out of bounds (length %d); was %d",
                  value.length(), range.end());
            }
          }
        }
        break;

      default:
        break;
    }
  }

  @Override
  public void visit(ArraySet op) {
    Operand array = op.source();
    if (array.isNull()) {
      throw new D2RuntimeException("Cannot set value of NULL ARRAY", op.position(), "Null pointer");
    }
    if (!op.index().isConstant()) {
      return;
    }
    int size = ConstantOperand.valueFromConstOperand(op.index()).intValue();
    if (size < 0) {
      throw new InvalidIndexException(
          op.position(), "ARRAY index must be non-negative; was %d", size);
    }
  }

  @Override
  public void visit(FieldSetOp op) {
    Operand record = op.recordLocation();
    if (record.isNull()) {
      throw new D2RuntimeException(
          String.format("Cannot set field \"%s\" of NULL RECORD", op.field()), op.position(),
          "Null pointer");
    }
  }

  @Override
  public boolean isChanged() {
    return false;
  }
}
