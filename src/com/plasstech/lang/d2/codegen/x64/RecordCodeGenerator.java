package com.plasstech.lang.d2.codegen.x64;

import static com.plasstech.lang.d2.codegen.Codegen.fail;
import static com.plasstech.lang.d2.codegen.x64.IntRegister.RCX;
import static com.plasstech.lang.d2.codegen.x64.IntRegister.RDX;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Emitter;
import com.plasstech.lang.d2.codegen.Labels;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.VariableLocation;
import com.plasstech.lang.d2.codegen.il.AllocateOp;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.FieldSetOp;
import com.plasstech.lang.d2.codegen.x64.Resolver.ResolvedOperand;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.RecordSymbol;
import com.plasstech.lang.d2.type.RecordSymbol.Field;
import com.plasstech.lang.d2.type.SymbolTable;
import com.plasstech.lang.d2.type.VarType;

class RecordCodeGenerator extends DefaultOpcodeVisitor {

  private static final Map<TokenType, String> BINARY_OPCODE =
      ImmutableMap.of(TokenType.EQEQ, "setz", TokenType.NEQ, "setnz");

  private final Resolver resolver;
  private final Emitter emitter;
  private final SymbolTable symTab;

  private final NullPointerCheckGenerator npeCheckGenerator;

  RecordCodeGenerator(Resolver resolver, SymbolTable symTab, Emitter emitter) {
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

    // TODO: does this need to be deallocated?
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
    VarType leftType = op.left().type();
    if (!leftType.isRecord()) {
      //      emitter.fail("Cannot call RecordGenerator with op %s", op.toString());
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
        fail(op.position(), "Still don't know how to do %s on records", op.operator());
        break;
    }
  }

  private void generateCompare(BinOp op) {
    String destName = resolver.resolve(op.destination());

    Operand left = op.left();
    Operand right = op.right();
    TokenType operator = op.operator();

    if (left.type().isNull() || right.type().isNull()) {
      generateNullCompare(destName, left, right, operator);
      // NOTE RETURN
      return;
    }

    String leftName = resolver.resolve(op.left());
    Register tempReg = resolver.allocate(VarType.INT);
    String rightName = resolver.resolve(op.right());
    // TODO this can be simpler if left is already in a register
    emitter.emit("; if they're the same objects we can stop now");
    emitter.emit("mov QWORD %s, %s ; record compare setup", tempReg.name64(), leftName);
    emitter.emit("cmp QWORD %s, %s", tempReg.name64(), rightName);
    resolver.deallocate(tempReg);
    String notSameObjectLabel = Labels.nextLabel("not_same_object");
    emitter.emit("jne %s", notSameObjectLabel);

    emitter.emit("; same objects, done.");
    emitter.emit("mov BYTE %s, %s", destName, (operator == TokenType.EQEQ) ? "1" : "0");
    String endLabel = Labels.nextLabel("record_cmp_end");
    emitter.emit("jmp %s", endLabel);

    emitter.emitLabel(notSameObjectLabel);
    emitter.emit("; not the same objects: test left for null");
    String leftNotNullLabel = Labels.nextLabel("left_not_null");
    emitter.emit("cmp QWORD %s, 0", leftName);
    emitter.emit("jne %s", leftNotNullLabel);
    emitter.emit("; left is null, right is not null (it's not === left), done.");
    emitter.emit("mov BYTE %s, %s", destName, (operator == TokenType.NEQ) ? "1" : "0");
    emitter.emit("jmp %s", endLabel);

    emitter.emitLabel(leftNotNullLabel);
    emitter.emit("; left is not null: test right for null");

    // if right == null: return op == NEQ
    String bothNotNullLabel = Labels.nextLabel("both_not_null");
    emitter.emit("cmp QWORD %s, 0", rightName);
    emitter.emit("jne %s", bothNotNullLabel);

    emitter.emit("; right is null, left is not (it's not === right)");
    emitter.emit("mov BYTE %s, %s", destName, (operator == TokenType.NEQ) ? "1" : "0");
    emitter.emit("jmp %s", endLabel);

    emitter.emitLabel(bothNotNullLabel);
    emitter.emit("; left and right both not null");
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

    RecordSymbol recordSymbol = (RecordSymbol) symTab.getRecursive(left.type().name());
    if (recordSymbol == null) {
      VariableLocation variable = (VariableLocation) left;
      recordSymbol = variable.symbol().recordSymbol();
    }

    emitter.emit("mov R8, %s  ; size", recordSymbol.allocatedSize());
    emitter.emitExternCall("memcmp");
    emitter.emit("cmp RAX, 0");
    emitter.emit("%s %s  ; record cmp %s", BINARY_OPCODE.get(operator), destName, operator);
    registerState.condPop();

    emitter.emitLabel(endLabel);

    resolver.deallocate(op.left());
    resolver.deallocate(op.right());
  }

  private void generateNullCompare(
      String destName, Operand left, Operand right, TokenType operator) {
    emitter.emit("; left or right is constant null, can do simple compare");
    if (left.type().isNull()) {
      // just compare right to null
      String rightName = resolver.resolve(right);
      emitter.emit("cmp QWORD %s, 0", rightName);
    } else {
      String leftName = resolver.resolve(left);
      emitter.emit("cmp QWORD %s, 0", leftName);
    }
    emitter.emit("%s %s", (operator == TokenType.NEQ) ? "setnz" : "setz", destName);
  }

  private void generateDot(BinOp op) {
    npeCheckGenerator.generateNullPointerCheck(op.position(), op.left());

    Operand record = op.left();
    if (!record.type().isRecord()) {
      fail(op.position(), "Cannot get field of non record %s", record);
    }
    VariableLocation variable = (VariableLocation) record;
    RecordSymbol recordSymbol = (RecordSymbol) symTab.getRecursive(record.type().name());
    if (recordSymbol == null) {
      recordSymbol = variable.symbol().recordSymbol();
    }

    String recordLoc = resolver.resolve(op.left());
    Register calcReg = resolver.allocate(VarType.INT);
    emitter.emit("; allocated %s for source location calculation", calcReg);
    // 1. if not already in register, put record location into a register
    emitter.emit(
        "mov %s, %s  ; base record location", calcReg, recordLoc);

    // 2. get offset of field
    Operand right = op.right();
    if (!right.isConstant()) {
      fail(op.position(), "Cannot get field name from right DOT operand: %s", right);
    }
    String fieldName = ConstantOperand.stringValueFromConstOperand(right);
    Field field = recordSymbol.getField(fieldName);
    int offset = field.offset();
    // 3. add to get actual field location
    if (offset > 0) {
      if (offset == 1) {
        emitter.emit("inc %s  ; field location including offset into record", calcReg);
      } else {
        emitter.emit("add %s, %d  ; field location including offset into record", calcReg, offset);
      }
    } else {
      emitter.emit("; offset was 0");
    }

    Location destination = op.destination();
    ResolvedOperand destRo = resolver.resolveFully(destination);
    // 4. mov register, source - take heed of size of RHS
    Size size = Size.of(destination.type());
    emitter.emit("\n  ; store it:");
    if (destRo.isRegister()) {
      if (destination.type() == VarType.DOUBLE) {
        emitter.emit("movq %s, [%s]", destRo.name(), calcReg);
      } else {
        emitter.emit("mov %s %s, [%s]", size, destRo.name(), calcReg);
      }
    } else {
      emitter.emit("; use indirection because we can't move from memory to memory");
      // It's fine to use an int because it's 64 bits, I tink.
      Register indirectReg = resolver.allocate(VarType.INT);
      emitter.emit("; allocated %s as indirect register", indirectReg);

      // 1. get source into indirectReg, e.g., mov BYTE indirectReg.sized, [calcReg]
      VarType type = destination.type();
      emitter.emit("mov %s %s, [%s]  ; load from memory into indirect register", size,
          indirectReg.sizeByType(type), calcReg);
      // 2. put indirect reg into destination, e.g., mov BYTE destRo.name(), indirectReg
      // note, this doesn't need movq because we're not moving to a XMM register 
      emitter.emit("mov %s %s, %s  ; store into memory from indirect register", size, destRo.name(),
          indirectReg.sizeByType(type));

      resolver.deallocate(indirectReg);
      emitter.emit("; deallocated %s from indirection", indirectReg);
    }
    resolver.deallocate(calcReg);
    emitter.emit("; deallocated %s from source calculation", calcReg);
    resolver.deallocate(op.left());
  }
}
