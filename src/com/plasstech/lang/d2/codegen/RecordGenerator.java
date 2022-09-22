package com.plasstech.lang.d2.codegen;

import static com.plasstech.lang.d2.codegen.IntRegister.RCX;
import static com.plasstech.lang.d2.codegen.IntRegister.RDX;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.plasstech.lang.d2.codegen.il.AllocateOp;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.FieldSetOp;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.RecordSymbol;
import com.plasstech.lang.d2.type.RecordSymbol.Field;
import com.plasstech.lang.d2.type.SymTab;
import com.plasstech.lang.d2.type.VarType;

class RecordGenerator extends DefaultOpcodeVisitor {

  private static final Map<TokenType, String> BINARY_OPCODE =
      ImmutableMap.of(TokenType.EQEQ, "setz", TokenType.NEQ, "setnz");

  private final Resolver resolver;
  private final Emitter emitter;
  private final SymTab symTab;

  private final NullPointerCheckGenerator npeCheckGenerator;

  RecordGenerator(Resolver resolver, SymTab symTab, Emitter emitter) {
    this.resolver = resolver;
    this.symTab = symTab;
    this.emitter = emitter;
    this.npeCheckGenerator = new NullPointerCheckGenerator(resolver, emitter);
  }

  /** Generate nasm code to allocate and assign a record. */
  @Override
  public void visit(AllocateOp op) {
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
  @Override
  public void visit(FieldSetOp op) {
    npeCheckGenerator.generateNullPointerCheck(op.position(), op.recordLocation());

    String recordLoc = resolver.resolve(op.recordLocation());
    Register calcReg = resolver.allocate(VarType.INT);
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
      if (source.type() != VarType.DOUBLE) {
        emitter.emit("mov %s [%s], %s  ; store it!", size, calcReg, sourceName);
      } else {
        // use an intermediate register
        Register tempXmm = resolver.allocate(VarType.DOUBLE);
        resolver.mov(source, tempXmm);
        emitter.emit("movq [%s], %s", calcReg, tempXmm);
        resolver.deallocate(tempXmm);
      }
    } else {
      // need an indirection, ugh.
      // NOTE: this is VarType.INT because it's anything but a double. Not sure what happens
      // if source is a double in memory...
      Register tempReg = resolver.allocate(VarType.INT);
      emitter.emit("; allocated %s for calculations", tempReg);
      emitter.emit(
          "mov %s %s, %s  ; get value to store",
          size, tempReg.sizeByType(source.type()), sourceName);
      emitter.emit(
          "mov %s [%s], %s  ; store it!", size, calcReg, tempReg.sizeByType(source.type()));
      resolver.deallocate(tempReg);
    }
    resolver.deallocate(source);
    resolver.deallocate(calcReg);
  }

  @Override
  public void visit(BinOp op) {
    if (!op.left().type().isRecord()) {
      emitter.fail("Cannot call RecordGenerator with op %s", op.toString());
      return;
    }
    switch (op.operator()) {
      case DOT:
        generateDot(op);
        break;
      case EQEQ:
      case NEQ:
        generateCompare(op);
        break;

      default:
        emitter.fail("Still don't know how to do %s on records", op.operator());
        break;
    }
  }

  private void generateCompare(BinOp op) {
    String destName = resolver.resolve(op.destination());

    Operand left = op.left();
    Operand right = op.right();
    TokenType operator = op.operator();

    String endLabel = resolver.nextLabel("reccmp_short_circuit");
    String nonNullreccmp = resolver.nextLabel("non_null_reccmp");
    Register tempReg = resolver.allocate(VarType.INT);
    String leftName = resolver.resolve(op.left());
    String rightName = resolver.resolve(op.right());
    // TODO this can be simpler
    emitter.emit("; if they're the same objects we can stop now");
    emitter.emit("mov QWORD %s, %s ; record compare setup", tempReg.name64(), leftName);
    emitter.emit("cmp QWORD %s, %s", tempReg.name64(), rightName);
    resolver.deallocate(tempReg);
    String nextTest = resolver.nextLabel("next_reccmp_test");
    emitter.emit("jne %s", nextTest);

    emitter.emit("; same objects");
    emitter.emit("mov BYTE %s, %s", destName, (operator == TokenType.EQEQ) ? "1" : "0");
    emitter.emit("jmp %s", endLabel);

    emitter.emit("; not the same objects: test for null");
    emitter.emitLabel(nextTest);
    // if left == null: return op == NEQ
    nextTest = resolver.nextLabel("next_reccmp_test");
    if (leftName.equals("0")) {
      emitter.emit("; left is literal null");
      emitter.emit("mov BYTE %s, %s", destName, (operator == TokenType.NEQ) ? "1" : "0");
      emitter.emit("jmp %s", endLabel);
    } else {
      emitter.emit("cmp QWORD %s, 0", leftName);
      emitter.emit("jne %s", nextTest);
      emitter.emit("; left is null, right is not");
      emitter.emit("mov BYTE %s, %s", destName, (operator == TokenType.NEQ) ? "1" : "0");
      emitter.emit("jmp %s", endLabel);
    }
    emitter.emit("; left is not null, test right");
    emitter.emitLabel(nextTest);
    // if right == null: return op == NEQ
    if (rightName.equals("0")) {
      emitter.emit("; right is literal null");
      emitter.emit("mov BYTE %s, %s", destName, (operator == TokenType.NEQ) ? "1" : "0");
      emitter.emit("jmp %s", endLabel);
    } else {
      emitter.emit("cmp QWORD %s, 0", rightName);
      emitter.emit("jne %s", nonNullreccmp);
      emitter.emit("; right is null, left is not");
      emitter.emit("mov BYTE %s, %s", destName, (operator == TokenType.NEQ) ? "1" : "0");
      emitter.emit("jmp %s", endLabel);
    }

    emitter.emit("; left and right both not null");
    emitter.emitLabel(nonNullreccmp);
    RegisterState registerState =
        RegisterState.condPush(emitter, resolver, Register.VOLATILE_REGISTERS);

    emitter.emit(
        "; record cmp: %s = %s %s %s",
        destName, resolver.resolve(left), operator, resolver.resolve(right));

    if (resolver.isInRegister(left, RDX) && resolver.isInRegister(right, RCX)) {
      emitter.emit("; no need to set up RCX, RDX for %s", operator);
    } else if (resolver.isInRegister(left, RCX) && resolver.isInRegister(right, RDX)) {
      emitter.emit("xchg RDX, RCX  ; swap");
    } else if (resolver.isInRegister(right, RCX)) {
      emitter.emit("; right is in RCX, so set RDX first.");
      // rcx is in the right register, need to set rdx first
      resolver.mov(right, RDX);
      resolver.mov(left, RCX);
    } else {
      resolver.mov(left, RCX);
      resolver.mov(right, RDX);
    }

    // need to set the size too!!!
    VarType leftType = left.type();
    RecordSymbol recordSymbol = (RecordSymbol) symTab.getRecursive(leftType.name());
    emitter.emit("mov R8, %s  ; size", recordSymbol.allocatedSize());
    emitter.emitExternCall("memcmp");
    emitter.emit("cmp RAX, 0");
    emitter.emit("%s %s  ; record cmp %s", BINARY_OPCODE.get(operator), destName, operator);
    registerState.condPop();

    emitter.emitLabel(endLabel);
  }

  private void generateDot(BinOp op) {
    npeCheckGenerator.generateNullPointerCheck(op.position(), op.left());

    Operand record = op.left();
    VarType type = record.type();
    RecordSymbol recordSymbol = (RecordSymbol) symTab.getRecursive(type.name());

    String recordLoc = resolver.resolve(op.left());
    Register calcReg = resolver.allocate(VarType.INT);
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
      if (destination.type() == VarType.DOUBLE) {
        emitter.emit("movq %s, [%s]  ; store it!", destName, calcReg);
      } else {
        emitter.emit("mov %s %s, [%s]  ; store it!", size, destName, calcReg);
      }
    } else {
      // need an indirection, ugh.
      Register indirectReg = resolver.allocate(VarType.INT);
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
