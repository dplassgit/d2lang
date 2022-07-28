package com.plasstech.lang.d2.codegen;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.plasstech.lang.d2.codegen.il.AllocateOp;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.FieldSetOp;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.RecordSymbol;
import com.plasstech.lang.d2.type.RecordSymbol.Field;
import com.plasstech.lang.d2.type.SymTab;
import com.plasstech.lang.d2.type.VarType;

// TODO: Should this extend DefaultOpcodeGenerator?
class RecordGenerator {

  private static final Map<TokenType, String> BINARY_OPCODE =
      ImmutableMap.of(TokenType.EQEQ, "setz", TokenType.NEQ, "setnz");

  private final Resolver resolver;
  private final Emitter emitter;
  private final SymTab symTab;

  private final NullPointerCheckGenerator npeCheckGenerator;

  public RecordGenerator(Resolver resolver, SymTab symTab, Emitter emitter) {
    this.resolver = resolver;
    this.symTab = symTab;
    this.emitter = emitter;
    this.npeCheckGenerator = new NullPointerCheckGenerator(resolver, emitter);
  }

  /** Generate nasm code to allocate and assign a record. */
  void generate(AllocateOp op) {
    RegisterState state = RegisterState.condPush(emitter, resolver, Register.VOLATILE_REGISTERS);
    String dest = resolver.resolve(op.destination());
    emitter.emit("mov RCX, 1");
    // Allocate at least 1 byte
    int totalSize = Math.max(1, op.record().allocatedSize());
    emitter.emit("mov EDX, %d  ; total record size", totalSize);
    emitter.emitExternCall("calloc");
    emitter.emit("mov %s, RAX", dest);
    state.condPop();
  }

  /** Generate nasm code to set a field value: record.field = source */
  void generate(FieldSetOp op) {
    npeCheckGenerator.generateNullPointerCheck(op, op.recordLocation());

    String recordLoc = resolver.resolve(op.recordLocation());
    Register calcReg = resolver.allocate();
    // 1. if not already in register, put record location into a register
    emitter.emit(
        "mov %s, %s  ; put record location in register for calculations", calcReg, recordLoc);

    // 2. get offset of field
    int offset = op.recordSymbol().getField(op.field()).offset();
    // 3. add to get actual field location
    if (offset > 0) {
      if (offset == 1) {
        emitter.emit("inc %s  ; get actual field location via offset", calcReg);
      } else {
        emitter.emit("add %s, %d  ; get actual field location via offset", calcReg, offset);
      }
    } else {
      emitter.emit("; offset was 0");
    }

    Operand source = op.source();
    String sourceName = resolver.resolve(source);
    // 4. mov register, source - take heed of size of RHS
    Size size = Size.of(source.type());
    if (source.isConstant() || resolver.isInAnyRegister(source)) {
      emitter.emit("mov %s [%s], %s  ; store it!", size, calcReg, sourceName);
    } else {
      // need an indirection, ugh.
      Register indirectReg = resolver.allocate();
      emitter.emit("; allocated %s for calculations", indirectReg);
      emitter.emit(
          "mov %s %s, %s  ; get value to store",
          size, indirectReg.sizeByType(source.type()), sourceName);
      emitter.emit("mov [%s], %s  ; store it!", calcReg, indirectReg);
      resolver.deallocate(indirectReg);
    }
    resolver.deallocate(source);
    resolver.deallocate(calcReg);
  }

  void generate(BinOp op) {
    switch (op.operator()) {
      case DOT:
        generateDot(op);
        break;
      case EQEQ:
      case NEQ:
        Register tempReg = resolver.allocate();
        String leftName = resolver.resolve(op.left());
        String rightName = resolver.resolve(op.right());
        String destName = resolver.resolve(op.destination());

        emitter.emit("mov QWORD %s, %s ; record compare setup", tempReg.name64(), leftName);
        emitter.emit("cmp %s, %s", tempReg.name64(), rightName);
        emitter.emit(
            "%s %s  ; QWORD compare %s", BINARY_OPCODE.get(op.operator()), destName, op.operator());
        resolver.deallocate(tempReg);
        break;

      default:
        emitter.fail("Still don't know how to do %s on records", op.operator());
        break;
    }
  }

  private void generateDot(BinOp op) {
    npeCheckGenerator.generateNullPointerCheck(op, op.left());

    Operand record = op.left();
    VarType type = record.type();
    RecordSymbol recordSymbol = (RecordSymbol) symTab.getRecursive(type.name());

    String recordLoc = resolver.resolve(op.left());
    Register calcReg = resolver.allocate();
    // 1. if not already in register, put record location into a register
    emitter.emit(
        "mov %s, %s  ; put record location in register for calculations", calcReg, recordLoc);

    // 2. get offset of field
    Operand right = op.right();
    if (!right.isConstant()) {
      emitter.fail("Cannot get field name from right DOT operand: %s", right);
    }
    ConstantOperand<String> rightConst = (ConstantOperand<String>) right;
    String fieldName = rightConst.value();
    Field field = recordSymbol.getField(fieldName);
    int offset = field.offset();
    // 3. add to get actual field location
    if (offset > 0) {
      if (offset == 1) {
        emitter.emit("inc %s  ; get actual field location via offset", calcReg);
      } else {
        emitter.emit("add %s, %d  ; get actual field location via offset", calcReg, offset);
      }
    } else {
      emitter.emit("; offset was 0");
    }

    Location destination = op.destination();
    String destName = resolver.resolve(destination);
    // 4. mov register, source - take heed of size of RHS
    Size size = Size.of(destination.type());
    if (resolver.isInAnyRegister(destination)) {
      emitter.emit("mov %s %s, [%s]  ; store it!", size, destName, calcReg);
    } else {
      // need an indirection, ugh.
      Register indirectReg = resolver.allocate();
      emitter.emit("; allocated %s for calculations", indirectReg);
      emitter.emit(
          "mov %s %s, %s  ; get value to get",
          size, indirectReg.sizeByType(destination.type()), calcReg.sizeByType(destination.type()));
      emitter.emit("mov %s, [%s]  ; get it into the destination", size, destName, indirectReg);
      resolver.deallocate(indirectReg);
    }
    resolver.deallocate(calcReg);
  }
}
