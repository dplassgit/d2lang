package com.plasstech.lang.d2.codegen;

import static com.plasstech.lang.d2.codegen.IntRegister.R10;
import static com.plasstech.lang.d2.codegen.IntRegister.R11;
import static com.plasstech.lang.d2.codegen.IntRegister.R8;
import static com.plasstech.lang.d2.codegen.IntRegister.R9;
import static com.plasstech.lang.d2.codegen.IntRegister.RAX;
import static com.plasstech.lang.d2.codegen.IntRegister.RCX;
import static com.plasstech.lang.d2.codegen.IntRegister.RDX;

import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Op;
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

  private final Resolver resolver;
  private final Emitter emitter;
  private final NullPointerCheckGenerator npeCheckGenerator;

  public StringCodeGenerator(Resolver resolver, Emitter emitter) {
    this.resolver = resolver;
    this.emitter = emitter;
    this.npeCheckGenerator = new NullPointerCheckGenerator(resolver, emitter);
  }

  /** Generate destName = left operator right */
  void generateStringCompare(BinOp op, String destName) {

    Operand left = op.left();
    Operand right = op.right();
    TokenType operator = op.operator();

    String endLabel = null;
    String nonNullStrcmp = null;
    if (operator != TokenType.EQEQ && operator != TokenType.NEQ) {
      // do not allow comparing nulls
      npeCheckGenerator.generateNullPointerCheck(op.position(), left);
      npeCheckGenerator.generateNullPointerCheck(op.position(), right);
    } else {
      endLabel = resolver.nextLabel("strcmp_short_circuit");
      Register tempReg = resolver.allocate(VarType.STRING);
      String leftName = resolver.resolve(op.left());
      String rightName = resolver.resolve(op.right());
      // TODO this can be simpler
      emitter.emit("; if they're identical we can stop now");
      emitter.emit("mov QWORD %s, %s ; string compare setup", tempReg.name64(), leftName);
      emitter.emit("cmp QWORD %s, %s", tempReg.name64(), rightName);
      resolver.deallocate(tempReg);
      String nextTest = resolver.nextLabel("next_strcmp_test");
      emitter.emit("jne %s", nextTest);

      emitter.emit("mov BYTE %s, %s", destName, (operator == TokenType.EQEQ) ? "1" : "0");
      emitter.emit("jmp %s", endLabel);

      emitter.emit("; not identical; test for null");
      emitter.emitLabel(nextTest);
      // if left == null: return op == NEQ
      nextTest = resolver.nextLabel("next_strcmp_test");
      if (leftName.equals("0")) {
        emitter.emit("; left is literal null");
        emitter.emit("mov BYTE %s, %s", destName, (operator == TokenType.NEQ) ? "1" : "0");
        emitter.emit("jmp %s", endLabel);
      } else if (!left.isConstant()) {
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
      } else if (!right.isConstant()) {
        emitter.emit("cmp QWORD %s, 0", rightName);
        nonNullStrcmp = resolver.nextLabel("non_null_strcmp");
        emitter.emit("jne %s", nonNullStrcmp);
        emitter.emit("; right is null, left is not");
        emitter.emit("mov BYTE %s, %s", destName, (operator == TokenType.NEQ) ? "1" : "0");
        emitter.emit("jmp %s", endLabel);
      }
    }

    emitter.emit("; left and right both not null");
    emitter.emitLabel(nonNullStrcmp);
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

    emitter.emitLabel(endLabel);
  }

  /** generate dest = stringOperand[index] */
  void generateStringIndex(BinOp op) {
    Operand stringOperand = op.left();

    npeCheckGenerator.generateNullPointerCheck(op.position(), stringOperand);

    String stringName = resolver.resolve(stringOperand);
    Operand index = op.right();
    String indexName = resolver.resolve(index);
    Location destination = op.destination();
    String destName = resolver.resolve(destination);
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
            op.position(),
            "Invalid index");
      }
    } else {
      // TODO: make this an asm function instead of inlining each time?
      emitter.emit("; make sure index is >= 0");
      // Validate index part 1
      emitter.emit("cmp DWORD %s, 0  ; check index is >= 0", indexName);
      // Note, three underscores
      String continueLabel = resolver.nextLabel("continue");
      emitter.emit("jge %s", continueLabel);

      // print error and stop.
      emitter.emit0("\n  ; negative. no good. print error and stop");
      emitter.addData(STRING_INDEX_NEGATIVE_ERR);
      emitter.emit("mov RCX, STRING_INDEX_NEGATIVE_ERR");
      emitter.emit("mov RDX, %d  ; line number", op.position().line());
      emitter.emit("mov R8d, %s  ; index", indexName);
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
    String goodIndex = resolver.nextLabel("good_string_index");
    emitter.emit("jg %s", goodIndex);

    // else bad
    emitter.emit("; index out of bounds");
    emitter.addData(STRING_INDEX_OOB_ERR);
    emitter.emit("mov R9d, %s  ; index", indexName);
    emitter.emit("mov EDX, %s  ; line number", op.position().line());
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
    String allocateLabel = resolver.nextLabel("allocate_2_char_string");
    emitter.emit("jne %s", allocateLabel);

    // At runtime, if we got here, we're getting the last character of the given string.
    emitter.emit("; %s = %s[%s]", destName, stringName, indexName);
    if (indexName.equals("0")) {
      // Simplest case - first character of 1-character string
      resolver.mov(stringOperand, destination);
    } else {
      // At runtime, we're getting the last character of the given string. We don't know the
      // length at compile time though.
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
    String afterLabel = resolver.nextLabel("after_string_index");
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
  // TODO: change String dest to ResolvedOperand
  void generateStringAdd(Op op, String dest, Operand left, Operand right) {
    Position position = op.position();
    npeCheckGenerator.generateNullPointerCheck(position, left);
    npeCheckGenerator.generateNullPointerCheck(position, right);

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
    emitter.emit0("");
    emitter.emit(
        "add %s, %s  ; Total new string length", leftLengthReg.name32(), rightLengthReg.name32());
    emitter.emit("inc %s  ; Plus 1 for end of string", leftLengthReg.name32());
    emitter.emit("; deallocating right length %s", rightLengthReg);
    resolver.deallocate(rightLengthReg);

    // 3. allocate string of length left+right + 1
    emitter.emit0("");
    emitter.emit("; Allocate string of length %s", leftLengthReg.name32());

    RegisterState registerState =
        RegisterState.condPush(emitter, resolver, Register.VOLATILE_REGISTERS);
    emitter.emit("mov ECX, %s", leftLengthReg.name32());
    // change to calloc?
    emitter.emitExternCall("malloc");
    emitter.emit("; deallocating leftlength %s", leftLengthReg);
    resolver.deallocate(leftLengthReg);
    // 4. put string into dest
    registerState.condPop(); // this will pop leftlengthreg but it doesn't matter.
    if (!dest.equals(RAX.name64())) {
      emitter.emit("mov %s, RAX  ; destination from rax", dest);
    }

    // 5. strcpy from left to dest
    emitter.emit0("");
    registerState = RegisterState.condPush(emitter, resolver, Register.VOLATILE_REGISTERS);
    String leftName = resolver.resolve(left);
    emitter.emit("; strcpy from %s to %s", leftName, dest);
    if (resolver.isInRegister(left, RCX) && dest.equals(RDX.name64())) {
      // just swap rdx & rcx
      emitter.emit("; left wants to be in rcx dest in rdx, just swap");
      emitter.emit("xchg RDX, RCX");
    } else if (resolver.isInRegister(left, RCX)) {
      emitter.emit("; opposite order, so that we don't munge rcx");
      emitter.emit("mov RDX, %s", leftName);
      emitter.emit("mov RCX, %s", dest);
    } else {
      emitter.emit("mov RCX, %s", dest);
      emitter.emit("mov RDX, %s", leftName);
    }
    emitter.emitExternCall("strcpy");
    registerState.condPop();

    // 6. strcat dest, right
    emitter.emit0("");
    registerState = RegisterState.condPush(emitter, resolver, Register.VOLATILE_REGISTERS);
    String rightName = resolver.resolve(right);
    emitter.emit("; strcat from %s to %s", rightName, dest);
    if (resolver.isInRegister(right, RCX) && dest.equals(RDX.name64())) {
      // just swap rdx & rcx
      emitter.emit("; right wants to be in rcx dest in rdx, just swap");
      emitter.emit("xchg RDX, RCX");
    } else if (resolver.isInRegister(right, RCX)) {
      emitter.emit("; opposite order, so that we don't munge rcx");
      resolver.mov(right, RDX);
      emitter.emit("mov RCX, %s", dest);
    } else {
      emitter.emit("mov RCX, %s", dest);
      resolver.mov(right, RDX);
    }
    emitter.emitExternCall("strcat");
    registerState.condPop();
  }

  /** Generate destination = length(source) */
  void generateStringLength(Position position, Location destination, Operand source) {
    npeCheckGenerator.generateNullPointerCheck(position, source);
    // We're doing something special with RAX & RCX
    RegisterState raxRcxState =
        RegisterState.condPush(emitter, resolver, ImmutableList.of(RAX, RCX));
    RegisterState registerState =
        RegisterState.condPush(emitter, resolver, ImmutableList.of(RDX, R8, R9, R10, R11));

    String sourceName = resolver.resolve(source);
    String destinationName = resolver.resolve(destination);
    if (resolver.isInRegister(source, RCX)) {
      emitter.emit("; RCX already has address of string");
    } else {
      resolver.mov(source, RCX);
    }
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
      emitter.emit("; pseudo-pop; destination was already %s", destination);
      if (raxRcxState.wasPushed(RAX)) {
        emitter.emit("add RSP, 8");
      }
    } else {
      // NOTE: eax not rax, because lengths are always ints (32 bits)
      emitter.emit("mov %s, EAX  ; %s = strlen(%s)", destinationName, destinationName, sourceName);
      raxRcxState.condPop(RAX);
    }
  }

  /** Generate destName = chr(sourceName), where sourceName is a number */
  // TODO: change to ResolvedOperator, ResolvedOperator
  void generateChr(String sourceName, String destName) {
    // realistically, nothing is ever kept in RAX because it's the return value register...
    RegisterState raxState = RegisterState.condPush(emitter, resolver, ImmutableList.of(RAX));
    RegisterState registerState =
        RegisterState.condPush(emitter, resolver, Register.VOLATILE_REGISTERS);

    // 1. allocate a new 2-char string
    emitter.emit("mov RCX, 2");
    emitter.emitExternCall("malloc");
    registerState.condPop();
    // 2. set destName to allocated string
    if (!destName.equals(RAX.name64())) {
      emitter.emit("mov %s, RAX  ; copy string location from RAX", destName);
    }

    Register charReg = resolver.allocate(VarType.INT);
    // 3. get source char as character
    emitter.emit(
        "mov DWORD %s, %s  ; get the character int into %s", charReg.name32(), sourceName, charReg);
    // 4. write source char in first location
    emitter.emit(
        "mov BYTE [RAX], %s  ; move the character into the first location", charReg.name8());
    // 5. clear second location.
    emitter.emit("mov BYTE [RAX+1], 0  ; clear the 2nd location");
    raxState.condPop();
    resolver.deallocate(charReg);
  }
}
