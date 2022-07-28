package com.plasstech.lang.d2.codegen;

import static com.plasstech.lang.d2.codegen.IntRegister.R8;

import com.plasstech.lang.d2.codegen.il.ArrayAlloc;
import com.plasstech.lang.d2.codegen.il.ArraySet;
import com.plasstech.lang.d2.common.D2RuntimeException;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.ArrayType;

/** Generates nasm code for array manipulation. */
class ArrayCodeGenerator {
  private static final String ARRAY_INDEX_NEGATIVE_ERR =
      "ARRAY_INDEX_NEGATIVE_ERR: db \"Invalid index error at line %d: ARRAY index must be non-negative; was %d\", 10, 0";
  private static final String ARRAY_INDEX_OOB_ERR =
      "ARRAY_INDEX_OOB_ERR: db \"Invalid index error at line %d: ARRAY index out of bounds (length %d); was %d\", 10, 0";
  private static final String ARRAY_SIZE_ERR =
      "ARRAY_SIZE_ERR: db \"Invalid value error at line %d: ARRAY size must be positive; was %d\", 10, 0";

  private final Resolver resolver;
  private final Emitter emitter;
  private final NullPointerCheckGenerator npeCheckGenerator;

  ArrayCodeGenerator(Resolver resolver, Emitter emitter) {
    this.resolver = resolver;
    this.emitter = emitter;
    this.npeCheckGenerator = new NullPointerCheckGenerator(resolver, emitter);
  }

  /** Generate dest:type[size] */
  void generate(ArrayAlloc op) {
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
      ConstantOperand<Integer> numEntriesOp = (ConstantOperand<Integer>) numEntriesLoc;
      int numEntries = numEntriesOp.value();
      if (numEntries <= 0) {
        throw new D2RuntimeException(
            String.format("ARRAY size must be positive; was %d", numEntries),
            op.position(),
            "Invalid index");
      }
      emitter.emit(
          "mov %s, %s  ; storage for # dimensions (1), dimension values (%d), actual storage (%d)",
          allocSizeBytesRegister,
          1 + 4 * dimensions + numEntries * entrySize,
          4 * dimensions,
          numEntries * entrySize);
    } else {
      // Validate array size is positive.
      emitter.emit("cmp DWORD %s, 1  ; check for non-positive size", numEntriesLocName);
      String continueLabel = resolver.nextLabel("continue");
      emitter.emit("jge _%s", continueLabel);

      emitter.addData(ARRAY_SIZE_ERR);
      emitter.emit("; no good; array size is not positive");
      emitter.emit("mov R8d, %s  ; number of entries", numEntriesLocName);
      emitter.emit("mov EDX, %s  ; line number", op.position().line());
      emitter.emit("mov RCX, ARRAY_SIZE_ERR");
      emitter.emitExternCall("printf");
      emitter.emitExit(-1);

      emitter.emitLabel(continueLabel);
      emitter.emit(
          "mov %s, %s  ; number of entries", allocSizeBytesRegister.name32(), numEntriesLocName);

      if (entrySize > 1) {
        emitter.emit(
            "imul %s, %s ; total size of entries", allocSizeBytesRegister.name32(), entrySize);
      }
      emitter.emit(
          "add %s, %s  ; add storage for # of dimensions, and %d dimension value(s)",
          allocSizeBytesRegister.name32(), 1 + 4 * dimensions, dimensions);
    }

    emitter.emit("mov RCX, 1  ; # of 'entries' for calloc");
    emitter.emitExternCall("calloc");
    String dest = resolver.resolve(op.destination());
    emitter.emit("mov %s, RAX  ; RAX has location of allocated memory", dest);
    registerState.condPop();
    emitter.emit("mov BYTE [RAX], %s  ; store # of dimensions", dimensions);

    Register numEntriesReg = null;
    if (!(resolver.isInAnyRegister(numEntriesLoc) || numEntriesLoc.isConstant())) {
      // not a constant and not in a registers; put it in a register
      numEntriesReg = resolver.allocate();
      emitter.emit(
          "; numEntriesLoc (%s) is not in a register; putting it into %s",
          numEntriesLoc, numEntriesReg);
      emitter.emit("mov DWORD %s, %s", numEntriesReg.name32(), numEntriesLocName);
      numEntriesLocName = numEntriesReg.name32();
    }
    // TODO: iterate over dimensions.
    emitter.emit("mov DWORD [RAX+1], %s  ; store size of the first dimension", numEntriesLocName);
    if (numEntriesReg != null) {
      emitter.emit("; deallocating numEntriesLoc from %s", numEntriesReg);
      resolver.deallocate(numEntriesReg);
    }
    resolver.deallocate(op.sizeLocation());
  }

  /** Generate destination=length(source) */
  void generateArrayLength(Location destination, Operand source) {
    String sourceName = resolver.resolve(source);
    String destName = resolver.resolve(destination);
    if (resolver.isInAnyRegister(source)) {
      emitter.emit("mov %s, [%s + 1]  ; get length from first dimension", destName, sourceName);
    } else {
      // if source is not a register we have to allocate a register first
      Register tempReg = resolver.allocate();
      emitter.emit("mov %s, %s  ; get array location into reg", tempReg, sourceName);
      emitter.emit("mov %s, [%s + 1]  ; get length from first dimension", destName, tempReg);
      resolver.deallocate(tempReg);
    }
  }

  /** Generate array[index]=source */
  void generateArraySet(ArraySet op) {
    if (!op.isArrayLiteral()) {
      // array literals are by definition never null.
      npeCheckGenerator.generateNullPointerCheck(op, op.array());
    }

    Operand sourceLoc = op.source();
    String sourceName = resolver.resolve(sourceLoc);
    ArrayType arrayType = op.arrayType();
    String baseTypeSize = Size.of(arrayType.baseType()).asmType;
    Register sourceReg = null;
    if (!(resolver.isInAnyRegister(sourceLoc) || sourceLoc.isConstant())) {
      // not a constant and not in a registers; put it in a register
      sourceReg = resolver.allocate();
      emitter.emit("; source (%s) is not in a register; putting it into %s:", sourceLoc, sourceReg);
      String sourceRegisterSized = sourceReg.sizeByType(arrayType.baseType());
      emitter.emit("mov %s %s, %s", baseTypeSize, sourceRegisterSized, sourceName);
      sourceName = sourceRegisterSized;
    }

    // calculate full index: indexName*basetype.size() + 1+4*dimensions+arrayLoc
    Operand indexLoc = op.index();

    Register fullIndex =
        generateArrayIndex(
            indexLoc, arrayType, resolver.resolve(op.array()), op.isArrayLiteral(), op.position());
    emitter.emit("mov %s [%s], %s  ; store it!", baseTypeSize, fullIndex, sourceName);
    if (sourceReg != null) {
      emitter.emit("; deallocating %s", sourceReg);
      resolver.deallocate(sourceReg);
    }
    resolver.deallocate(fullIndex);
    emitter.emit("; deallocating %s", fullIndex);

    resolver.deallocate(sourceLoc);
    resolver.deallocate(indexLoc);
  }

  /**
   * Generate code that puts the location of the given index into the given array into the register
   * returned. full index=indexLoc*basetype.size() + 1+4*dimensions+arrayLoc
   */
  Register generateArrayIndex(
      Operand indexLoc,
      ArrayType arrayType,
      String arrayLoc,
      boolean arrayLiteral,
      Position position) {
    Register lengthReg = resolver.allocate();
    emitter.emit("; allocated %s for calculations", lengthReg);
    if (indexLoc.isConstant()) {
      // if index is constant, can skip some of this calculation.
      ConstantOperand<Integer> indexConst = (ConstantOperand<Integer>) indexLoc;
      int index = indexConst.value();
      if (index < 0) {
        throw new D2RuntimeException(
            String.format("ARRAY index must be non-negative; was %d", index),
            position,
            "Invalid index");
      }

      if (index > 0 && !arrayLiteral) {
        // fun fact, we never have to calculate this for index 0, because all arrays
        // are at least size 1.
        // 1. get size from arrayloc
        emitter.emit0("\n  ; make sure index is < length");
        emitter.emit("mov %s, %s  ; get base of array", lengthReg, arrayLoc);
        emitter.emit("inc %s  ; skip past # dimensions", lengthReg);
        // this gets the size in the register
        emitter.emit("mov %s, [%s]  ; get array length", lengthReg, lengthReg);
        // 2. compare - NOTE SWAPPED ARGS
        emitter.emit("cmp %s, %s  ; check length > index (SIC)", lengthReg.name32(), index);
        // 3. if good, continue
        String continueLabel = resolver.nextLabel("good_array_index");
        emitter.emit("jg _%s", continueLabel);

        emitter.emit0("\n  ; no good. print error and stop");
        emitter.addData(ARRAY_INDEX_OOB_ERR);
        emitter.emit("mov R8d, %s  ; length ", lengthReg.name32());
        emitter.emit("mov R9d, %s  ; index", index);
        emitter.emit("mov EDX, %s  ; line number", position.line());
        emitter.emit("mov RCX, ARRAY_INDEX_OOB_ERR");
        emitter.emitExternCall("printf");
        emitter.emitExit(-1);

        emitter.emitLabel(continueLabel);
      } else {
        emitter.emit("; don't have to check if index is within bounds. :)");
      }

      // index is always a dword/int because I said so.
      emitter.emit(
          "mov %s, %d  ; const index; full index=1+dims*4+index*base size",
          lengthReg, 1 + 4 * arrayType.dimensions() + arrayType.baseType().size() * index);
    } else {
      String indexName = resolver.resolve(indexLoc);

      // TODO: make this an asm function instead of inlining each time?
      emitter.emit("; make sure index is >= 0");
      // Validate index part 1
      emitter.emit("cmp DWORD %s, 0  ; check index is >= 0", indexName);
      // Note, three underscores
      String continueLabel = resolver.nextLabel("continue");
      emitter.emit("jge _%s", continueLabel);

      // print error and stop.
      emitter.emit0("\n  ; negative. no good. print error and stop");
      emitter.addData(ARRAY_INDEX_NEGATIVE_ERR);
      if (lengthReg == R8) {
        emitter.emit("; index already in R8");
      } else {
        emitter.emit("mov R8d, %s  ; index", indexName);
      }
      emitter.emit("mov RDX, %d  ; line number", position.line());
      emitter.emit("mov RCX, ARRAY_INDEX_NEGATIVE_ERR");
      emitter.emitExternCall("printf");
      emitter.emitExit(-1);

      emitter.emitLabel(continueLabel);

      // 1. get size from arrayloc
      emitter.emit0("\n  ; make sure index is < length");
      emitter.emit("mov %s, %s  ; get base of array", lengthReg, arrayLoc);
      emitter.emit("inc %s  ; skip past # dimensions", lengthReg);
      // this gets the size in the register
      emitter.emit("mov %s, [%s]  ; get array length", lengthReg, lengthReg);
      // 2. compare
      emitter.emit("cmp DWORD %s, %s  ; check index is < length", indexName, lengthReg.name32());
      // 3. if good, continue
      continueLabel = resolver.nextLabel("continue");
      emitter.emit("jl _%s", continueLabel);

      emitter.emit0("\n  ; no good. print error and stop");
      emitter.addData(ARRAY_INDEX_OOB_ERR);
      if (lengthReg == R8) {
        emitter.emit("; index already in R8");
      } else {
        emitter.emit("mov R8d, %s  ; length ", lengthReg.name32());
      }
      emitter.emit("mov DWORD R9d, %s  ; index", indexName);
      emitter.emit("mov EDX, %s  ; line number", position.line());
      emitter.emit("mov RCX, ARRAY_INDEX_OOB_ERR");
      emitter.emitExternCall("printf");
      emitter.emitExit(-1);

      emitter.emitLabel(continueLabel);

      emitter.emit0("\n  ; calculate index*base size+1+dims*4");
      // index is always a dword/int because I said so.
      emitter.emit("mov DWORD %s, %s  ; index...", lengthReg.name32(), indexName);
      emitter.emit("imul %s, %s  ; ...*base size ...", lengthReg, arrayType.baseType().size());
      emitter.emit("add %s, %d  ; ... +1+dims*4", lengthReg, 1 + arrayType.dimensions() * 4);
    }
    emitter.emit("add %s, %s  ; actual location", lengthReg, arrayLoc);
    return lengthReg;
  }
}
