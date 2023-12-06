package com.plasstech.lang.d2.codegen.x64;

import static com.plasstech.lang.d2.codegen.Codegen.fail;

import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Emitter;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.x64.Resolver.ResolvedOperand;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.VarType;

class RangeCodeGenerator extends DefaultOpcodeVisitor {

  private Resolver resolver;
  private Emitter emitter;

  public RangeCodeGenerator(Resolver resolver, Emitter emitter) {
    this.resolver = resolver;
    this.emitter = emitter;
  }

  /** Generate destName = left operator right for [ and comparisons. */
  @Override
  public void visit(BinOp op) {
    if (op.destination().type() != VarType.RANGE && op.left().type() != VarType.RANGE) {
      return;
    }

    TokenType operator = op.operator();
    switch (operator) {
      case COLON:
        createRange(op);
        return;

      case LBRACKET:
        generateIndex(op);
        return;

      case EQEQ:
      case NEQ:
        //        generateComparison(op);
        break;

      default:
        break;
    }
    fail(op.position(), "Cannot do %s on RANGEs (yet?)", operator);
  }

  private void createRange(BinOp op) {
    ResolvedOperand leftRo = resolver.resolveFully(op.left());
    ResolvedOperand rightRo = resolver.resolveFully(op.right());

    Location dest = op.destination();
    ResolvedOperand destRo = resolver.resolveFully(dest);

    // The plan:
    //  dest = left
    //  dest = dest << 32
    //  dest += right

    // 1. mov left into dest
    if (destRo.isRegister()) {
      // if dest is a register, need to specify the 32 bit version
      emitter.emit("mov DWORD %s, %s   ; move left to dest",
          destRo.register().nameByType(VarType.INT), leftRo.name());
    } else {
      emitter.emit("; move left to dest");
      resolver.mov(leftRo, destRo);
    }

    // 2. shift dest 32 times so that left is in the upper 32 bits
    emitter.emit("shl QWORD %s, 32  ; shift dest left 32 to make room for right",
        destRo.name());
    // 3. add right (lower 32 bits)
    if (destRo.isRegister()) {
      // if dest is a register, need to specify the 32 bit version
      emitter.emit("add %s, %s  ; right part of range",
          destRo.register().nameByType(VarType.RANGE), rightRo.name());
    } else {
      if (rightRo.isRegister()) {
        emitter.emit("add QWORD %s, %s  ; right part of range",
            destRo.name(), rightRo.name());
      } else {
        // both are not in registers; put right into a reg
        Register tempReg = resolver.allocate(VarType.INT);
        emitter.emit("; move right to temp register");
        resolver.mov(rightRo, tempReg);
        emitter.emit("add QWORD %s, %s  ; right part of range",
            destRo.name(),
            tempReg.nameByType(VarType.RANGE));
        resolver.deallocate(tempReg);
      }
    }
    resolver.deallocate(op.left());
    resolver.deallocate(op.right());
  }

  private void generateIndex(BinOp op) {
    // get range into a register
    Operand range = op.left();
    Register rangeReg = resolver.allocate(VarType.RANGE);
    resolver.mov(range, rangeReg);

    if (op.right().isConstant()) {
      int value = ConstantOperand.valueFromConstOperand(op.right()).intValue();
      if (value == 0) {
        emitter.emit("sar %s, 32", rangeReg);
      } else {
        // don't modify rangeReg - note, we can probably optimize this a little bit 
      }
    } else {
      // put index into rcx
      boolean rcxUsed = resolver.isAllocated(IntRegister.RCX);
      if (rcxUsed) {
        emitter.emit("push RCX");
      }
      resolver.mov(op.right(), IntRegister.RCX);
      // twiddle the low bit so 0 becomes 1 and 1 becomes 0
      emitter.emit("xor RCX, 1");
      // multiply by 32, so 0 becomes 1 becomes 32; 1 becomes 0 becomes 0
      emitter.emit("imul RCX, 32");
      // shift rangeReg to the right by CL
      emitter.emit("sar %s, CL", rangeReg);
      if (rcxUsed) {
        emitter.emit("pop RCX");
      }
    }
    // now the (low) 32 bits of rangeReg has the value.
    resolver.mov(rangeReg, op.destination());
    resolver.deallocate(rangeReg);
  }
}
