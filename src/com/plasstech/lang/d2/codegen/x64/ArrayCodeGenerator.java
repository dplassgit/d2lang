package com.plasstech.lang.d2.codegen.x64;

import static com.plasstech.lang.d2.codegen.Codegen.fail;
import static com.plasstech.lang.d2.codegen.x64.IntRegister.R8;
import static com.plasstech.lang.d2.codegen.x64.IntRegister.RCX;
import static com.plasstech.lang.d2.codegen.x64.IntRegister.RDX;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Emitter;
import com.plasstech.lang.d2.codegen.Labels;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.il.ArrayAlloc;
import com.plasstech.lang.d2.codegen.il.ArraySet;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.ArrayType;
import com.plasstech.lang.d2.type.VarType;

/** Generates nasm code for array manipulation. */
class ArrayCodeGenerator extends DefaultOpcodeVisitor {
  private static final Map<TokenType, String> BINARY_OPCODE =
      ImmutableMap.of(TokenType.EQEQ, "setz", TokenType.NEQ, "setnz");

  private final Resolver resolver;
  private final Emitter emitter;

  ArrayCodeGenerator(Resolver resolver, Emitter emitter) {
    this.resolver = resolver;
    this.emitter = emitter;
  }

  /** Generate dest:type[size] */
  @Override
  public void visit(ArrayAlloc op) {
    RegisterState registerState =
        RegisterState.condPush(emitter, resolver, Register.VOLATILE_REGISTERS);
    Operand numEntriesLoc = op.sizeLocation();
    String numEntriesLocName = resolver.resolve(numEntriesLoc);

    // 1. calculate # of bytes to allocate:
    //    size * entrySize +
    //    1 byte (# of dimensions) + 4 * # dimensions
    Register allocSizeBytesRegister = IntRegister.RDX;
    emitter.emit("; allocated RDX (hard-coded) for calculations");
    int dimensions = op.arrayType().dimensions();
    int entrySize = op.arrayType().baseType().size();
    if (numEntriesLoc.isConstant()) {
      // if numEntriesLocName is a constant, pre-calculate storage requirements
      int numEntries = ConstantOperand.valueFromConstOperand(numEntriesLoc).intValue();
      if (numEntries < 0) {
        fail("Invalid index", op.position(), "ARRAY size must be non-negative; was %d", numEntries);
      }
      emitter.emit(
          "mov %s, %s  ; storage for # dimensions (1), dimension values (%d), actual storage (%d)",
          allocSizeBytesRegister,
          1 + 4 * dimensions + numEntries * entrySize,
          4 * dimensions,
          numEntries * entrySize);
    } else {
      resolver.mov(numEntriesLoc, allocSizeBytesRegister);

      if (entrySize > 1) {
        emitter.emit(
            "imul %s, %s ; total size of entries", allocSizeBytesRegister.nameByType(VarType.INT),
            entrySize);
      }
      emitter.emit(
          "add %s, %s  ; add storage for # of dimensions, and %d dimension value(s)",
          allocSizeBytesRegister.nameByType(VarType.INT), 1 + 4 * dimensions, dimensions);
    }

    emitter.emit("mov RCX, 1  ; # of 'entries' for calloc");
    emitter.emitExternCall("calloc");
    resolver.mov(IntRegister.RAX, op.destination());
    registerState.condPop();
    emitter.emit("mov BYTE [RAX], %s  ; store # of dimensions", dimensions);

    Register numEntriesReg = null;
    if (!(resolver.isInAnyRegister(numEntriesLoc) || numEntriesLoc.isConstant())) {
      // not a constant and not in a registers; put it in a register
      numEntriesReg = resolver.allocate(VarType.INT);
      emitter.emit(
          "; numEntriesLoc (%s) is not in a register; putting it into %s",
          numEntriesLoc, numEntriesReg);
      resolver.mov(numEntriesLoc, numEntriesReg);
      numEntriesLocName = numEntriesReg.nameByType(VarType.INT);
    }
    // TODO(#38): iterate over dimensions.
    boolean setSize = true;
    if (numEntriesLoc.isConstant()) {
      // Peephole optimization: if size is 0, don't need to set the size, because we already
      // use calloc.
      int numEntries = ConstantOperand.valueFromConstOperand(numEntriesLoc).intValue();
      setSize = numEntries > 0;
    }
    if (setSize) {
      emitter.emit("mov DWORD [RAX + 1], %s  ; store size of first dimension", numEntriesLocName);
    }
    if (numEntriesReg != null) {
      emitter.emit("; deallocating numEntriesLoc from %s", numEntriesReg);
      resolver.deallocate(numEntriesReg);
    }
    resolver.deallocate(op.sizeLocation());
  }

  @Override
  public void visit(UnaryOp op) {
    if (!op.operand().type().isArray() || op.operator() != TokenType.LENGTH) {
      return;
    }
    generateArrayLength(op.destination(), op.operand());
  }

  /** Generate destination=length(source) */
  private void generateArrayLength(Location destination, Operand source) {
    String sourceName = resolver.resolve(source);
    String destName = resolver.resolve(destination);
    if (resolver.isInAnyRegister(source)) {
      emitter.emit(
          "mov DWORD %s, [%s + 1]  ; get length from first dimension", destName, sourceName);
    } else {
      if (resolver.isInAnyRegister(destination)) {
        Register destReg = resolver.toRegister(destination);
        // we can re-use the destination register
        resolver.mov(source, destination);
        emitter.emit(
            "mov DWORD %s, [%s + 1]  ; get length from first dimension",
            destReg.nameByType(VarType.INT), destReg.name());
      } else {
        // if source is not a register we have to allocate a register first
        Register tempReg = resolver.allocate(VarType.INT);
        resolver.mov(source, tempReg);
        emitter.emit(
            "mov DWORD %s, [%s + 1]  ; get length from first dimension", destName, tempReg);
        resolver.deallocate(tempReg);
      }
    }
    // NOTE: DO NOT deallocate "source" so that print arrays or array literals will work (?!)
    emitter.emit("; not deallocating %s", source);
  }

  /** Generate array[index]=source */
  @Override
  public void visit(ArraySet op) {
    Operand sourceLoc = op.source();
    String sourceName = resolver.resolve(sourceLoc);
    ArrayType arrayType = op.arrayType();
    VarType baseType = arrayType.baseType();
    String baseTypeSize = Size.of(baseType).asmType;
    Register sourceReg = null;
    if (!(resolver.isInAnyRegister(sourceLoc) || sourceLoc.isConstant())) {
      // not a constant and not in a registers; put it in a register
      sourceReg = resolver.allocate(baseType);
      emitter.emit("; source (%s) is not in a register; putting it into %s:", sourceLoc, sourceReg);
      String sourceRegisterSized = sourceReg.nameByType(baseType);
      resolver.mov(sourceLoc, sourceReg);
      sourceName = sourceRegisterSized;
    }

    // calculate full index: indexName*basetype.size() + 1+4*dimensions+arrayLoc
    Operand indexLoc = op.index();

    Register fullIndex =
        generateArrayIndex(
            indexLoc, arrayType, resolver.resolve(op.array()), op.isArrayLiteral(), op.position());
    if (baseType == VarType.DOUBLE) {
      // may need an intermediary
      if (sourceReg != null || resolver.isInAnyRegister(sourceLoc)) {
      } else {
        sourceReg = resolver.allocate(baseType);
        resolver.mov(sourceLoc, sourceReg);
        String sourceRegisterSized = sourceReg.nameByType(baseType);
        sourceName = sourceRegisterSized;
      }
      emitter.emit("movq [%s], %s  ; store it!", fullIndex, sourceName);
    } else {
      emitter.emit("mov %s [%s], %s  ; store it!", baseTypeSize, fullIndex, sourceName);
    }
    if (sourceReg != null) {
      resolver.deallocate(sourceReg);
    }
    resolver.deallocate(fullIndex);
    resolver.deallocate(sourceLoc);
    resolver.deallocate(indexLoc);
  }

  @Override
  public void visit(BinOp op) {
    VarType leftType = op.left().type();
    if (!leftType.isArray()) {
      return;
    }
    String leftName = resolver.resolve(op.left());
    String destName = resolver.resolve(op.destination());
    TokenType operator = op.operator();
    switch (operator) {
      case LBRACKET:
        ArrayType arrayType = (ArrayType) leftType;
        Register fullIndex =
            generateArrayIndex(op.right(), arrayType, leftName, false, op.position());
        if (arrayType.baseType() == VarType.DOUBLE) {
          emitter.emit("movq %s, [%s]", destName, fullIndex);
        } else {
          emitter.emit(
              "mov %s %s, [%s]", Size.of(arrayType.baseType()).asmType, destName, fullIndex);
        }
        resolver.deallocate(fullIndex);
        resolver.deallocate(op.left());
        break;

      case EQEQ:
      case NEQ:
        generateCmp(op);
        break;

      default:
        fail(op.position(), "Cannot do %s on %ss (yet?)", operator, leftType);
        break;
    }
  }

  private void generateCmp(BinOp op) {
    String destName = resolver.resolve(op.destination());

    Operand left = op.left();
    Operand right = op.right();
    String leftName = resolver.resolve(op.left());
    String rightName = resolver.resolve(op.right());
    TokenType operator = op.operator();
    emitter.emit(
        "; array cmp: %s = %s %s %s",
        destName, leftName, operator, rightName);

    boolean leftNull = leftName.equals("0") || left.type().isNull();
    boolean rightNull = rightName.equals("0") || right.type().isNull();
    if (leftNull && rightNull) {
      // They're the same so if we want EQEQ, it's 1.
      emitter.emit("mov BYTE %s, %s", destName, (operator == TokenType.EQEQ) ? "1" : "0");
    }
    if (leftNull) {
      emitter.emit("cmp QWORD %s, 0", rightName);
      emitter.emit("%s %s  ; array cmp %s", BINARY_OPCODE.get(operator), destName, operator);
    }
    if (rightNull) {
      emitter.emit("cmp QWORD %s, 0", leftName);
      emitter.emit("%s %s  ; array cmp %s", BINARY_OPCODE.get(operator), destName, operator);
    }
    if (leftNull || rightNull) {
      resolver.deallocate(left);
      resolver.deallocate(right);
      return;
    }

    ArrayType leftArrayType = (ArrayType) left.type();
    ArrayType rightArrayType = (ArrayType) right.type();
    if (leftArrayType.dimensions() != rightArrayType.dimensions() && operator == TokenType.NEQ) {
      // Different dimensions; definitely not the same.
      emitter.emit("mov BYTE %s, 1", destName);
      return;
    }

    String endLabel = Labels.nextLabel("array_cmp_short_circuit");
    String nonNullarraycmp = Labels.nextLabel("non_null_array_cmp");
    Register tempReg = resolver.allocate(VarType.INT);
    emitter.emit("; if they're the same objects we can stop now");
    emitter.emit("mov QWORD %s, %s ; array compare setup", tempReg.name(), leftName);
    emitter.emit("cmp QWORD %s, %s", tempReg.name(), rightName);
    resolver.deallocate(tempReg);

    String nextTest = Labels.nextLabel("next_arraycmp_test");
    emitter.emit("jne %s", nextTest);

    emitter.emit("; same objects");
    emitter.emit("mov BYTE %s, %s", destName, (operator == TokenType.EQEQ) ? "1" : "0");
    emitter.emit("jmp %s", endLabel);

    emitter.emit("; not the same objects: test for null");
    emitter.emitLabel(nextTest);
    // if left == null: return op == NEQ
    nextTest = Labels.nextLabel("next_arraycmp_test");
    emitter.emit("cmp QWORD %s, 0", leftName);
    emitter.emit("jne %s", nextTest);
    emitter.emit("; left is null, right is not");
    emitter.emit("mov BYTE %s, %s", destName, (operator == TokenType.NEQ) ? "1" : "0");
    emitter.emit("jmp %s", endLabel);
    emitter.emit("; left is not null, test right");
    emitter.emitLabel(nextTest);
    emitter.emit("cmp QWORD %s, 0", rightName);
    emitter.emit("jne %s", nonNullarraycmp);
    emitter.emit("; right is null, left is not");
    emitter.emit("mov BYTE %s, %s", destName, (operator == TokenType.NEQ) ? "1" : "0");
    emitter.emit("jmp %s", endLabel);

    emitter.emit("; left and right both not null");
    emitter.emitLabel(nonNullarraycmp);

    // Need to use memcmp; get left size, right size

    Register leftLengthReg = resolver.allocate(VarType.INT);
    generateArrayLength(new RegisterLocation("__leftLength", leftLengthReg, VarType.INT), left);
    Register rightLengthReg = resolver.allocate(VarType.INT);
    generateArrayLength(new RegisterLocation("__rightLength", rightLengthReg, VarType.INT),
        right);

    String continueLabel = Labels.nextLabel("array_memcmp");
    emitter.emit("cmp %s, %s", leftLengthReg.nameByType(VarType.INT),
        rightLengthReg.nameByType(VarType.INT));
    resolver.deallocate(rightLengthReg);
    emitter.emit("je %s", continueLabel);
    emitter.emit("; sizes are different; definitely not equal");
    emitter.emit("mov BYTE %s, %s", destName, (operator == TokenType.NEQ) ? "1" : "0");
    emitter.emit("jmp %s", endLabel);

    emitter.emitLabel(continueLabel);

    RegisterState registerState =
        RegisterState.condPush(emitter, resolver, Register.VOLATILE_REGISTERS);
    if ((resolver.isInRegister(left, RDX) && resolver.isInRegister(right, RCX))
        || (resolver.isInRegister(left, RCX) && resolver.isInRegister(right, RDX))) {
      emitter.emit("; no need to set up RCX, RDX (or RDX, RCX) for %s", operator);
    } else if (resolver.isInRegister(right, RCX)) {
      emitter.emit("; right is in RCX, so set RDX first.");
      // rcx is in the right register, need to set rdx first
      resolver.mov(right, RDX);
      resolver.mov(left, RCX);
    } else {
      resolver.mov(left, RCX);
      resolver.mov(right, RDX);
    }

    // calculate header (1+4*dimensions) + total length ( base type * length)
    emitter.emit(
        "imul %s, %s  ; ...*base size ...", leftLengthReg, leftArrayType.baseType().size());
    emitter.emit("add %s, %d  ; ... +1+dims*4", leftLengthReg,
        1 + leftArrayType.dimensions() * 4);
    // LeftLengthReg may or may not already be in r8
    resolver.mov(VarType.INT, leftLengthReg, R8);
    resolver.deallocate(leftLengthReg);
    emitter.emitExternCall("memcmp");
    emitter.emit("cmp RAX, 0");
    emitter.emit("%s %s  ; array cmp %s", BINARY_OPCODE.get(operator), destName, operator);
    registerState.condPop();

    emitter.emitLabel(endLabel);
    resolver.deallocate(left);
    resolver.deallocate(right);
  }

  /**
   * Generate code that puts the location of the given index into the given array into the register
   * returned. full index=indexLoc*basetype.size() + 1+4*dimensions+arrayLoc
   */
  private Register generateArrayIndex(
      Operand indexLoc,
      ArrayType arrayType,
      String arrayLoc,
      boolean arrayLiteral,
      Position position) {
    Register lengthReg = resolver.allocate(VarType.INT);
    emitter.emit("; allocated %s for calculations", lengthReg);
    if (indexLoc.isConstant()) {
      // if index is constant, can skip some of this calculation.
      int index = ConstantOperand.valueFromConstOperand(indexLoc).intValue();
      if (index < 0) {
        fail("Invalid index", position, "ARRAY index must be non-negative; was %d", index);
      }

      // index is always a dword/int because I said so.
      emitter.emit(
          "mov %s, %d  ; const index; full index=1+dims*4+index*base size",
          lengthReg, 1 + 4 * arrayType.dimensions() + arrayType.baseType().size() * index);
    } else {
      emitter.emit("; calculate index*base size+1+dims*4");
      // index is always a dword/int because I said so.
      resolver.mov(indexLoc, lengthReg);
      emitter.emit("imul %s, %s  ; ...*base size ...", lengthReg, arrayType.baseType().size());
      emitter.emit("add %s, %d  ; ... +1+dims*4", lengthReg, 1 + arrayType.dimensions() * 4);
    }
    emitter.emit("add %s, %s  ; actual location", lengthReg, arrayLoc);
    return lengthReg;
  }
}
