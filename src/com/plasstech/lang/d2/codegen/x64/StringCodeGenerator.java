package com.plasstech.lang.d2.codegen.x64;

import static com.plasstech.lang.d2.codegen.x64.IntRegister.R10;
import static com.plasstech.lang.d2.codegen.x64.IntRegister.R11;
import static com.plasstech.lang.d2.codegen.x64.IntRegister.R8;
import static com.plasstech.lang.d2.codegen.x64.IntRegister.R9;
import static com.plasstech.lang.d2.codegen.x64.IntRegister.RAX;
import static com.plasstech.lang.d2.codegen.x64.IntRegister.RCX;
import static com.plasstech.lang.d2.codegen.x64.IntRegister.RDX;

import java.util.Map;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Emitter;
import com.plasstech.lang.d2.codegen.Labels;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.common.D2RuntimeException;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.VarType;

/** Generate nasm code for string operations. */
class StringCodeGenerator {

  private static final String STRING_INDEX_NEGATIVE_ERR =
      "STRING_INDEX_NEGATIVE_ERR: db \"Invalid index error at line %d: STRING index must be non-negative; was %d\", 10, 0";
  private static final String STRING_INDEX_OOB_ERR =
      "STRING_INDEX_OOB_ERR: db \"Invalid index error at line %d: STRING index out of bounds (length %d); was %d\", 10, 0";

  private static final Map<TokenType, String> COMPARISION_OPCODE =
      ImmutableMap.<TokenType, String>builder()
          .put(TokenType.EQEQ, "setz")
          .put(TokenType.NEQ, "setnz")
          .put(TokenType.GT, "setg")
          .put(TokenType.GEQ, "setge")
          .put(TokenType.LT, "setl")
          .put(TokenType.LEQ, "setle")
          .build();

  private static final Map<NullPair, String> LT_CONSTANT_MAP =
      ImmutableMap.of(
          // Include this for completeness
          new NullPair(Nullness.NULL, Nullness.NULL), "0",
          // null < constant is true
          new NullPair(Nullness.NULL, Nullness.CONSTANT), "1",
          // variable < null should always be false; If variable is null,
          // null < null is false. If variable is not null, not null < null is false.
          new NullPair(Nullness.VARIABLE, Nullness.NULL), "0",
          // constant < null is false
          new NullPair(Nullness.CONSTANT, Nullness.NULL), "0");
  private static final Map<NullPair, String> GT_CONSTANT_MAP =
      ImmutableMap.of(
          // Include this for completeness
          new NullPair(Nullness.NULL, Nullness.NULL), "0",
          // null > constant is false
          new NullPair(Nullness.NULL, Nullness.CONSTANT), "0",
          // constant > null is true
          new NullPair(Nullness.CONSTANT, Nullness.NULL), "1");
  private static final Map<NullPair, String> GEQ_CONSTANT_MAP =
      ImmutableMap.of(
          // Include this for completeness
          new NullPair(Nullness.NULL, Nullness.NULL), "1",
          // variable >= null should always be true; If variable is null,
          // null == null is true. If variable is not null, not null > null is true.
          new NullPair(Nullness.VARIABLE, Nullness.NULL), "1",
          // null >= constant is false
          new NullPair(Nullness.NULL, Nullness.CONSTANT), "0",
          // constant >= null is true
          new NullPair(Nullness.CONSTANT, Nullness.NULL), "1");
  private static final Map<NullPair, String> LEQ_CONTSANT_MAP =
      ImmutableMap.of(
          // Include this for completeness
          new NullPair(Nullness.NULL, Nullness.NULL), "1",
          // null <= constant is true
          new NullPair(Nullness.NULL, Nullness.CONSTANT), "1",
          // constant <= null is false
          new NullPair(Nullness.CONSTANT, Nullness.NULL), "0");
  private static final Map<NullPair, String> EQEQ_CONSTANT_MAP =
      ImmutableMap.of(
          // Include this for completeness
          new NullPair(Nullness.NULL, Nullness.NULL), "1",
          // null == constant is false
          new NullPair(Nullness.NULL, Nullness.CONSTANT), "0",
          // constant == null is false
          new NullPair(Nullness.CONSTANT, Nullness.NULL), "0");
  private static final Map<NullPair, String> NEQ_CONSTANT_MAP =
      ImmutableMap.of(
          // Include this for completeness
          new NullPair(Nullness.NULL, Nullness.NULL), "0",
          // null != constant is true
          new NullPair(Nullness.NULL, Nullness.CONSTANT), "1",
          // constant != null is true
          new NullPair(Nullness.CONSTANT, Nullness.NULL), "1");

  // Maps operator to which constant map, above. If missing, use cmp value, 0, with
  // COMPARISON_OPCODE as the comparator (setz, setnz, etc.)
  private static final ImmutableMap<TokenType, Map<NullPair, String>> CONSTANT_MAP =
      // Must use ImmutableMap.Builder because ImmutableMap.of only takes 5 pairs...
      new ImmutableMap.Builder<TokenType, Map<NullPair, String>>()
          .put(TokenType.EQEQ, EQEQ_CONSTANT_MAP)
          .put(TokenType.NEQ, NEQ_CONSTANT_MAP)
          .put(TokenType.LT, LT_CONSTANT_MAP)
          .put(TokenType.LEQ, LEQ_CONTSANT_MAP)
          .put(TokenType.GT, GT_CONSTANT_MAP)
          .put(TokenType.GEQ, GEQ_CONSTANT_MAP)
          .build();

  private final Resolver resolver;
  private final Emitter emitter;
  private final NullPointerCheckGenerator npeCheckGenerator;

  public StringCodeGenerator(Resolver resolver, Emitter emitter) {
    this.resolver = resolver;
    this.emitter = emitter;
    this.npeCheckGenerator = new NullPointerCheckGenerator(resolver, emitter);
  }

  /** Generate destName = left operator right */
  void generateStringCompare(BinOp op) {
    Operand left = op.left();
    Operand right = op.right();
    // this should never happen
    Preconditions.checkState(
        !(right.type().isNull() && left.type().isNull()),
        "Left and right cannot both be null type");

    String destName = resolver.resolve(op.destination());
    TokenType operator = op.operator();

    // Handle literal nulls
    if (right.type().isNull() || left.type().isNull()) {
      generateCompareVsNull(left, right, destName, operator);
      // NOTE RETURN:
      return;
    }

    String leftName = resolver.resolve(op.left());
    String rightName = resolver.resolve(op.right());
    String endLabel = Labels.nextLabel("string_compare_end");
    String compareLeftLabel = Labels.nextLabel("compare_left_to_null");
    if (!right.isConstant()) {
      // right is not constant, compare to null, and if not null, jump to comparing left for null
      emitter.emit(
          "cmp QWORD %s, 0  ; see if right is null so we can do a simple comparison", rightName);
      emitter.emit("jne %s", compareLeftLabel);

      // At this point in the runtime, we know left is not constant null and right is runtime null.
      emitter.emit("; now we know left is not constant null, right is runtime null");
      NullPair nullpair = new NullPair(Nullness.from(left), Nullness.NULL);
      // Do a simple comparison, then jump to end. Issue #218.
      generateCompareVsNull(destName, operator, nullpair, leftName, rightName);

      emitter.emit("jmp %s", endLabel);
    } else {
      // right is a constant; do nothing special (?)
    }

    emitter.emitLabel(compareLeftLabel);
    String strcmpLabel = Labels.nextLabel("strcmp");
    if (!left.isConstant()) {
      // left is not constant, so it may be null. At this point in the emitted code, right is
      // definitely not null.
      emitter.emit(
          "cmp QWORD %s, 0  ; see if left is null so we can do a simple comparison", leftName);
      emitter.emit("jne %s", strcmpLabel);

      emitter.emit("; now we know left is runtime null and right is not null");
      // At this point in the runtime, left is null and right is definitely not null,
      // so we can consider it a constant.
      NullPair nullpair = new NullPair(Nullness.NULL, Nullness.CONSTANT);
      // Do a simple comparison, then jump to end. Issue #218.
      generateCompareVsNull(destName, operator, nullpair, leftName, rightName);

      emitter.emit("jmp %s", endLabel);
    }

    // We know both strings are not null (though one may be a constant.) Do the strcmp.
    emitter.emitLabel(strcmpLabel);
    emitter.emit("; we know left and right are both not null");

    RegisterState registerState =
        RegisterState.condPush(emitter, resolver, Register.VOLATILE_REGISTERS);

    emitter.emit(
        "; strcmp: %s = %s %s %s",
        destName, resolver.resolve(left), operator, resolver.resolve(right));

    if (resolver.isInRegister(left, RDX) && resolver.isInRegister(right, RCX)) {
      if (operator == TokenType.EQEQ || operator == TokenType.NEQ) {
        emitter.emit("; no need to set up RCX, RDX for %s", operator);
      } else {
        // not an equality comparison, need to swap either the operator or the operands.
        emitter.emit("xchg RCX, RDX  ; left was rdx, right was rcx, so swap them");
      }
    } else {
      if (resolver.isInRegister(right, RCX)) {
        emitter.emit("; right is in RCX, so set RDX first.");
        // rcx is in the right register, need to set rdx first
        resolver.mov(right, RDX);
        resolver.mov(left, RCX);
      } else {
        resolver.mov(left, RCX);
        resolver.mov(right, RDX);
      }
    }
    emitter.emitExternCall("strcmp");
    emitter.emit("cmp RAX, 0");
    emitter.emit("%s %s  ; string %s", COMPARISION_OPCODE.get(operator), destName, operator);
    registerState.condPop();

    // Fin
    emitter.emitLabel(endLabel);
  }

  /** Either left or right is constant null. Do an "easy" comparison. Issue #218. */
  private void generateCompareVsNull(
      Operand left, Operand right, String destName, TokenType operator) {

    NullPair nullpair = new NullPair(left, right);
    String leftName = resolver.resolve(left);
    String rightName = resolver.resolve(right);

    generateCompareVsNull(destName, operator, nullpair, leftName, rightName);
  }

  private void generateCompareVsNull(
      String destName, TokenType operator, NullPair nullpair, String leftName, String rightName) {

    emitter.emit("; left or right is null");
    Map<NullPair, String> constantMap = CONSTANT_MAP.get(operator);
    if (constantMap == null) {
      throw new IllegalStateException(
          String.format("Cannot compare strings via %s operator", operator));
    }

    String constant = constantMap.get(nullpair);
    if (constant != null) {
      // we're comparing null with a constant or variable (or vice versa)
      emitter.emit("mov BYTE %s, %s  ; one operand is null", destName, constant);
    } else {
      // Comparing variable with null (or vice versa)
      if (nullpair.left == Nullness.NULL) {
        // see if right is not null.
        emitter.emit("cmp QWORD %s, 0  ; see if right is null", rightName);
      } else {
        // see if left is not null
        emitter.emit("cmp QWORD %s, 0  ; see if left is null", leftName);
      }
      emitter.emit("%s %s  ; string %s", COMPARISION_OPCODE.get(operator), destName, operator);
    }
  }

  /** generate dest = stringOperand[index] */
  void generateStringIndex(
      Location destination, Operand stringOperand, Operand index, Position position) {
    npeCheckGenerator.generateNullPointerCheck(position, stringOperand);

    String indexName = resolver.resolve(index);
    RegisterState registerState =
        RegisterState.condPush(emitter, resolver, Register.VOLATILE_REGISTERS);

    // Issue #94: Check index to be >= 0 and < length
    if (index.isConstant()) {
      // if index is constant, can skip some of this calculation.
      ConstantOperand<Integer> indexConst = (ConstantOperand<Integer>) index;
      int indexValue = indexConst.value();
      if (indexValue < 0) {
        throw new D2RuntimeException(
            String.format("STRING index must be non-negative; was %d", indexValue),
            position,
            "Invalid index");
      }
    } else {
      // TODO: make this an asm function instead of inlining each time?
      emitter.emit("; make sure index is >= 0");
      // Validate index part 1
      emitter.emit("cmp DWORD %s, 0  ; check index is >= 0", indexName);
      // Note, three underscores
      String continueLabel = Labels.nextLabel("continue");
      emitter.emit("jge %s", continueLabel);

      // print error and stop.
      emitter.emit0("\n  ; negative. no good. print error and stop");
      emitter.addData(STRING_INDEX_NEGATIVE_ERR);
      emitter.emit("mov RCX, STRING_INDEX_NEGATIVE_ERR");
      emitter.emit("mov RDX, %d  ; line number", position.line());
      resolver.mov(index, R8);
      emitter.emitExternCall("printf");
      emitter.emitExit(-1);

      emitter.emitLabel(continueLabel);
    }

    // Issue #112:
    // if indexname == stringname's length-1, just return stringname-1
    resolver.mov(stringOperand, RCX);
    emitter.emitExternCall("strlen");
    registerState.condPop();
    // Make sure index isn't >= length
    emitter.emit("cmp EAX, %s", indexName);
    String goodIndex = Labels.nextLabel("good_string_index");
    emitter.emit("jg %s", goodIndex);

    // else bad
    emitter.emit("; index out of bounds");
    emitter.addData(STRING_INDEX_OOB_ERR);
    resolver.mov(index, R9);
    emitter.emit("mov EDX, %s  ; line number", position.line());
    emitter.emit("mov R8d, EAX  ; length ");
    emitter.emit("mov RCX, STRING_INDEX_OOB_ERR");
    emitter.emitExternCall("printf");
    emitter.emitExit(-1);

    emitter.emitLabel(goodIndex);
    emitter.emit("dec RAX");
    if (!indexName.equals("0")) {
      // if index is 0, we're comparing to 0, and the ZF is set by dec.
      emitter.emit(
          "cmp %s, %s  ; see if index == length - 1", RAX.sizeByType(index.type()), indexName);
    }
    String allocateLabel = Labels.nextLabel("allocate_2_char_string");
    emitter.emit("jne %s", allocateLabel);

    // At runtime, if we got here, we're getting the last character of the given string.
    emitter.emit("; %s = %s[%s]", destination, stringOperand, index);
    if (indexName.equals("0")) {
      // Simplest case - first character of 1-character string
      resolver.mov(stringOperand, destination);
    } else {
      // At runtime, we're getting the last character of the given string. We don't know the
      // length at compile time though.
      String stringName = resolver.resolve(stringOperand);
      if (resolver.isInAnyRegister(destination)) {
        Register destReg = resolver.toRegister(destination);
        emitter.emit("; dest in %s", destReg);
        resolver.mov(index, destReg);
        emitter.emit("add %s, %s", destReg, stringName);
      } else {
        Register tempReg = resolver.allocate(VarType.INT);
        emitter.emit("; dest not in reg; allocated %s as tempReg", tempReg);
        resolver.mov(index, tempReg);
        emitter.emit("add %s, %s", tempReg, stringName);
        resolver.mov(tempReg, destination);
        resolver.deallocate(tempReg);
      }
    }
    String afterLabel = Labels.nextLabel("after_string_index");
    emitter.emit("jmp %s", afterLabel);

    // At runtime, when we get here, we need to copy the single source character to a new string.
    // 1. allocate a new 2-char string
    emitter.emitLabel(allocateLabel);
    registerState = RegisterState.condPush(emitter, resolver, Register.VOLATILE_REGISTERS);
    emitter.emit("mov RCX, 2");
    emitter.emitExternCall("malloc");
    registerState.condPop();
    // 2. copy the location to the dest
    resolver.mov(RAX, destination);

    Register indexReg = resolver.allocate(VarType.INT);
    emitter.emit("; allocated indexReg to %s", indexReg);
    Register charReg = resolver.allocate(VarType.INT);
    emitter.emit("; allocated charReg to %s", charReg);
    // 3. get the string
    resolver.mov(stringOperand, charReg);
    // 4. get the index
    resolver.mov(index, indexReg);
    // 5. get the actual character
    emitter.emit("mov %s, [%s + %s]  ; get the character", charReg, charReg, indexReg);
    resolver.deallocate(indexReg);
    emitter.emit("; deallocated indexReg from %s", indexReg);
    // 6. copy the character to the first location
    emitter.emit(
        "mov BYTE [RAX], %s  ; move the character into the first location", charReg.name8());
    resolver.deallocate(charReg);
    emitter.emit("; deallocated charReg from %s", charReg);
    // 7. clear the 2nd location
    emitter.emit("mov BYTE [RAX + 1], 0  ; clear the 2nd location");

    // jump destination for skipping the allocation step
    emitter.emitLabel(afterLabel);
  }

  /** Generate dest = leftOperand + rightOperand */
  void generateStringAdd(Location destination, Operand left, Operand right, Position position) {
    // Don't need to check for nulls, because generateStringLength will do it.

    // 1. get left length
    Register leftLengthReg = resolver.allocate(VarType.INT);
    emitter.emit("; Get left length into %s:", leftLengthReg);
    generateStringLength(
        position, new RegisterLocation("__leftLengthReg", leftLengthReg, VarType.INT), left);

    // 2. get right length
    Register rightLengthReg = resolver.allocate(VarType.INT);
    // TODO: if leftLengthReg is volatile, push it first (?!)
    emitter.emit0("");
    emitter.emit("; Get right length into %s:", rightLengthReg);
    generateStringLength(
        position, new RegisterLocation("__rightLengthReg", rightLengthReg, VarType.INT), right);

    // Optimize at runtime for concatenating empty strings
    String fin = Labels.nextLabel("string_add_end");
    String testRight = Labels.nextLabel("test_right_string");
    String justConcatenate = Labels.nextLabel("concatenate");
    emitter.emit0("");
    emitter.emit("; short-circuit for empty left");
    emitter.emit("cmp %s, 0", leftLengthReg.name32());
    emitter.emit("jne %s", testRight);
    resolver.mov(right, destination);
    emitter.emit("jmp %s", fin);

    emitter.emitLabel(testRight);
    emitter.emit("; short-circuit for empty right");
    emitter.emit("cmp %s, 0", rightLengthReg.name32());
    emitter.emit("jne %s", justConcatenate);
    resolver.mov(left, destination);
    emitter.emit("jmp %s", fin);

    emitter.emitLabel(justConcatenate);
    emitter.emit(
        "add %s, %s  ; Total new string length", leftLengthReg.name32(), rightLengthReg.name32());
    emitter.emit("inc %s  ; Plus 1 for end of string", leftLengthReg.name32());
    emitter.emit("; deallocating right length from %s", rightLengthReg);
    resolver.deallocate(rightLengthReg);

    // 3. allocate string of length left+right + 1
    emitter.emit0("");
    emitter.emit("; Allocate string of length %s", leftLengthReg.name32());

    RegisterState registerState =
        RegisterState.condPush(emitter, resolver, Register.VOLATILE_REGISTERS);
    resolver.mov(VarType.INT, leftLengthReg, RCX);
    // change to calloc?
    emitter.emitExternCall("malloc");
    emitter.emit("; deallocating left length from %s", leftLengthReg);
    resolver.deallocate(leftLengthReg);
    // 4. put string into dest
    registerState.condPop(); // this will pop leftlengthreg but it doesn't matter.

    // Move malloc'd pointer to destination
    Location newDestination = destination;
    if (destination.equals(left) || destination.equals(right)) {
      // if destination is also left or right then we can't overwrite
      // one of the operands, which would be bad. Use a temp.
      Register tempDestReg = resolver.allocate(VarType.STRING);
      newDestination = new RegisterLocation("_tempStringDest", tempDestReg, VarType.STRING);
      emitter.emit("; temporary destination %s", tempDestReg);
    }
    resolver.mov(RAX, newDestination);

    // 5. strcpy from left to dest
    emitter.emit0("");
    registerState = RegisterState.condPush(emitter, resolver, Register.VOLATILE_REGISTERS);
    String leftName = resolver.resolve(left);
    emitter.emit("; strcpy from %s to %s", leftName, newDestination);
    if (resolver.isInRegister(left, RCX) && resolver.isInRegister(newDestination, RDX)) {
      // just swap rdx & rcx
      emitter.emit("; left wants to be in rcx dest in rdx, just swap");
      emitter.emit("xchg RDX, RCX");
    } else if (resolver.isInRegister(left, RCX)) {
      emitter.emit("; opposite order, so that we don't munge rcx");
      resolver.mov(left, RDX);
      resolver.mov(newDestination, RCX);
    } else {
      resolver.mov(newDestination, RCX);
      resolver.mov(left, RDX);
    }
    emitter.emitExternCall("strcpy");
    registerState.condPop();

    // 6. strcat dest, right
    emitter.emit0("");
    registerState = RegisterState.condPush(emitter, resolver, Register.VOLATILE_REGISTERS);
    String rightName = resolver.resolve(right);
    emitter.emit("; strcat from %s to %s", rightName, newDestination);
    if (resolver.isInRegister(right, RCX) && resolver.isInRegister(newDestination, RDX)) {
      // just swap rdx & rcx
      emitter.emit("; right wants to be in rcx dest in rdx, just swap");
      emitter.emit("xchg RDX, RCX");
    } else if (resolver.isInRegister(right, RCX)) {
      emitter.emit("; opposite order, so that we don't munge rcx");
      resolver.mov(right, RDX);
      resolver.mov(newDestination, RCX);
    } else {
      resolver.mov(newDestination, RCX);
      resolver.mov(right, RDX);
    }
    emitter.emitExternCall("strcat");

    registerState.condPop();
    if (destination != newDestination) {
      // NOTE != not .equals
      emitter.emit("; assign to final destination");
      resolver.mov(newDestination, destination);
      resolver.deallocate(newDestination);
    }
    emitter.emitLabel(fin);
  }

  /** Generate destination = length(source) */
  void generateStringLength(Position position, Location destination, Operand source) {
    npeCheckGenerator.generateNullPointerCheck(position, source);
    String destinationName = resolver.resolve(destination);
    if (source.isConstant()) {
      // Constant string has constant length
      ConstantOperand<String> stringConst = (ConstantOperand<String>) source;
      String value = stringConst.value();
      emitter.emit("mov %s, %d  ; constant string", destinationName, value.length());
      return;
    }

    // We're doing something special with RAX & RCX
    RegisterState raxRcxState =
        RegisterState.condPush(emitter, resolver, ImmutableList.of(RAX, RCX));
    RegisterState registerState =
        RegisterState.condPush(emitter, resolver, ImmutableList.of(RDX, R8, R9, R10, R11));

    String sourceName = resolver.resolve(source);

    resolver.mov(source, RCX);
    emitter.emitExternCall("strlen");
    registerState.condPop();

    if (resolver.isInRegister(destination, RCX)) {
      // pseudo pop
      emitter.emit("; pseudo-pop; destination was already %s", destination);
      if (raxRcxState.wasPushed(RCX)) {
        emitter.emit("add RSP, 8");
      }
    } else {
      raxRcxState.condPop(RCX);
    }
    if (resolver.isInRegister(destination, RAX)) {
      // pseudo pop; eax already has the length.
      emitter.emit("; pseudo-pop; destination was already RAX");
      if (raxRcxState.wasPushed(RAX)) {
        emitter.emit("add RSP, 8");
      }
    } else {
      emitter.emit("; %s = strlen(%s)", destinationName, sourceName);
      resolver.mov(RAX, destination);
      raxRcxState.condPop(RAX);
    }
  }

  /** Generate destName = chr(sourceName), where sourceName is a number */
  void generateChr(Operand source, Location destination) {
    // realistically, nothing is ever kept in RAX because it's the return value register...
    RegisterState raxState = RegisterState.condPush(emitter, resolver, ImmutableList.of(RAX));
    RegisterState registerState =
        RegisterState.condPush(emitter, resolver, Register.VOLATILE_REGISTERS);

    // 1. allocate a new 2-char string
    emitter.emit("mov RCX, 2");
    emitter.emitExternCall("malloc");
    registerState.condPop();
    // 2. set dest to allocated string
    resolver.mov(RAX, destination);

    Register charReg = resolver.allocate(VarType.INT);
    // 3. get source char as character
    resolver.mov(source, charReg);
    // 4. write source char in first location
    emitter.emit(
        "mov BYTE [RAX], %s  ; move the character into the first location", charReg.name8());
    emitter.emit("mov BYTE [RAX+1], 0  ; clear the 2nd location");

    raxState.condPop();
    resolver.deallocate(charReg);
  }

  private enum Nullness {
    NULL,
    CONSTANT,
    VARIABLE;

    static Nullness from(Operand op) {
      if (op.type().isNull()) {
        return NULL;
      } else if (op.isConstant()) {
        return CONSTANT;
      }
      return VARIABLE;
    }
  }

  private static class NullPair {
    final Nullness left;
    final Nullness right;

    public NullPair(Nullness left, Nullness right) {
      this.left = left;
      this.right = right;
    }

    public NullPair(Operand left, Operand right) {
      this(Nullness.from(left), Nullness.from(right));
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null || !(obj instanceof NullPair)) {
        return false;
      }
      NullPair that = (NullPair) obj;
      return this.hashCode() == that.hashCode();
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(left, right);
    }
  }
}
