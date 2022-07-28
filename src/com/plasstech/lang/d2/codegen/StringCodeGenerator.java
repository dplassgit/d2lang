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
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.VarType;

/** Generate nasm code for string operations. */
class StringCodeGenerator {

  private static final Map<TokenType, String> BINARY_OPCODE =
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
  private final RegistersInterface registers;
  private final NullPointerCheckGenerator npeCheckGenerator;

  public StringCodeGenerator(Resolver resolver, Emitter emitter) {
    this.resolver = resolver;
    this.registers = resolver;
    this.emitter = emitter;
    this.npeCheckGenerator = new NullPointerCheckGenerator(resolver, emitter);
  }

  /** Generate destName = left operator right */
  void generateStringCompare(
      String destName, Operand left, Operand right, TokenType operator) {

    // TODO: if the operator is == or != we can short-circuit the call to strcmp if one is null

    RegisterState registerState =
        RegisterState.condPush(emitter, registers, Register.VOLATILE_REGISTERS);

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
        if (resolver.isInRegister(right, RDX)) {
          emitter.emit("; right already in RDX");
        } else {
          emitter.emit("mov RDX, %s  ; Address of right string", resolver.resolve(right));
        }
        if (resolver.isInRegister(left, RCX)) {
          emitter.emit("; left already in RCX");
        } else {
          emitter.emit("mov RCX, %s  ; Address of left string", resolver.resolve(left));
        }
      } else {
        if (resolver.isInRegister(left, RCX)) {
          emitter.emit("; left already in RCX");
        } else {
          emitter.emit("mov RCX, %s  ; Address of left string", resolver.resolve(left));
        }
        if (resolver.isInRegister(right, RDX)) {
          emitter.emit("; right already in RDX");
        } else {
          emitter.emit("mov RDX, %s  ; Address of right string", resolver.resolve(right));
        }
      }
    }
    emitter.emitExternCall("strcmp");
    emitter.emit("cmp RAX, 0");
    emitter.emit("%s %s  ; string %s", BINARY_OPCODE.get(operator), destName, operator);
    registerState.condPop();
  }

  /** generate dest = stringOperand[index] */
  void generateStringIndex(Op op, Location destination, Operand stringOperand, Operand index) {

    npeCheckGenerator.generateNullPointerCheck(op, stringOperand);

    String stringName = resolver.resolve(stringOperand);
    String indexName = resolver.resolve(index);
    String destName = resolver.resolve(destination);
    RegisterState registerState =
        RegisterState.condPush(emitter, registers, Register.VOLATILE_REGISTERS);

    // Issue #112:
    // if indexname == stringname's length-1, just return stringname-1
    if (resolver.isInRegister(stringOperand, RCX)) {
      emitter.emit("; string already in rcx");
    } else {
      emitter.emit("mov RCX, %s  ; get string", stringName);
    }
    emitter.emitExternCall("strlen");
    registerState.condPop();
    emitter.emit("dec RAX");
    if (!indexName.equals("0")) {
      // if index is 0, we're comparing to 0, and the ZF is set by dec.
      emitter.emit(
          "cmp %s, %s  ; see if index == length - 1", RAX.sizeByType(index.type()), indexName);
    }
    String allocateLabel = resolver.nextLabel("allocate_1_char_string");
    emitter.emit("jne _%s", allocateLabel);

    emitter.emit("; %s = %s[%s]", destName, stringName, indexName);
    if (indexName.equals("0")) {
      // Simplest case - first character of 1-character string
      if (resolver.isInAnyRegister(destination) || resolver.isInAnyRegister(stringOperand)) {
        emitter.emit("; index is 0, at least one is in a register - easiest case");
        emitter.emit("mov %s, %s", destName, stringName);
      } else {
        // both are not in registers - use one to transfer.
        Register tempReg = registers.allocate(VarType.INT);
        emitter.emit("; index is 0, neither in a register; allocated %s", tempReg);
        emitter.emit("mov %s, %s", tempReg, stringName);
        emitter.emit("mov %s, %s", destName, tempReg);
        registers.deallocate(tempReg);
      }
    } else {
      if (resolver.isInAnyRegister(destination)) {
        Register destReg = resolver.toRegister(destination);
        emitter.emit("; dest in %s", destReg);
        emitter.emit("mov %s, %s", destReg.sizeByType(index.type()), indexName);
        emitter.emit("add %s, %s", destReg, stringName);
      } else {
        Register tempReg = registers.allocate(VarType.INT);
        emitter.emit("; dest not in reg; allocated %s as tempReg", tempReg);
        emitter.emit("mov %s, %s", tempReg.sizeByType(index.type()), indexName);
        emitter.emit("add %s, %s", tempReg, stringName);
        emitter.emit("mov %s, %s", destName, tempReg);
        registers.deallocate(tempReg);
      }
    }
    String afterLabel = resolver.nextLabel("after_string_index");
    emitter.emit("jmp _%s", afterLabel);

    // 1. allocate a new 2-char string
    emitter.emitLabel(allocateLabel);
    registerState = RegisterState.condPush(emitter, registers, Register.VOLATILE_REGISTERS);
    emitter.emit("mov RCX, 2");
    emitter.emitExternCall("malloc");
    registerState.condPop();
    // 2. copy the location to the dest
    emitter.emit("mov %s, RAX  ; destination from rax", destName);

    Register indexReg = registers.allocate(VarType.INT);
    emitter.emit("; allocated indexReg to %s", indexReg);
    Register charReg = registers.allocate(VarType.INT);
    emitter.emit("; allocated charReg to %s", charReg);
    // 3. get the string
    emitter.emit("mov %s, %s  ; get the string into %s", charReg, stringName, charReg);
    // 4. get the index
    emitter.emit(
        "mov %s, %s  ; put index value into %s", indexReg.name32(), indexName, indexReg.name32());
    // 5. get the actual character
    emitter.emit("mov %s, [%s + %s]  ; get the character", charReg, charReg, indexReg);
    registers.deallocate(indexReg);
    emitter.emit("; deallocated indexReg from %s", indexReg);
    // 6. copy the character to the first location
    emitter.emit(
        "mov BYTE [RAX], %s  ; move the character into the first location", charReg.name8());
    registers.deallocate(charReg);
    emitter.emit("; deallocated charReg from %s", charReg);
    // 7. clear the 2nd location
    emitter.emit("mov BYTE [RAX + 1], 0  ; clear the 2nd location");

    //  * after:
    emitter.emitLabel(afterLabel);
  }

  /** Generate dest = leftOperand + rightOperand */
  void generateStringAdd(Op op, String dest, Operand left, Operand right) {
    npeCheckGenerator.generateNullPointerCheck(op, left);
    npeCheckGenerator.generateNullPointerCheck(op, right);

    // 1. get left length
    Register leftLengthReg = registers.allocate(VarType.INT);
    emitter.emit0("");
    emitter.emit("; Get left length into %s:", leftLengthReg);
    generateStringLength(new RegisterLocation("__leftLengthReg", leftLengthReg, VarType.INT), left);
    // 2. get right length
    Register rightLengthReg = registers.allocate(VarType.INT);
    // TODO: if leftLengthReg is volatile, push it first (?!)
    emitter.emit0("");
    emitter.emit("; Get right length into %s:", rightLengthReg);
    generateStringLength(
        new RegisterLocation("__rightLengthReg", rightLengthReg, VarType.INT), right);
    emitter.emit0("");
    emitter.emit(
        "add %s, %s  ; Total new string length", leftLengthReg.name32(), rightLengthReg.name32());
    emitter.emit("inc %s  ; Plus 1 for end of string", leftLengthReg.name32());
    emitter.emit("; deallocating right length %s", rightLengthReg);
    registers.deallocate(rightLengthReg);

    // 3. allocate string of length left+right + 1
    emitter.emit0("");
    emitter.emit("; Allocate string of length %s", leftLengthReg.name32());

    RegisterState registerState =
        RegisterState.condPush(emitter, registers, Register.VOLATILE_REGISTERS);
    emitter.emit("mov ECX, %s", leftLengthReg.name32());
    // change to calloc?
    emitter.emitExternCall("malloc");
    emitter.emit("; deallocating leftlength %s", leftLengthReg);
    registers.deallocate(leftLengthReg);
    // 4. put string into dest
    registerState.condPop(); // this will pop leftlengthreg but it doesn't matter.
    if (!dest.equals(RAX.name64())) {
      emitter.emit("mov %s, RAX  ; destination from rax", dest);
    }

    // 5. strcpy from left to dest
    emitter.emit0("");
    registerState = RegisterState.condPush(emitter, registers, Register.VOLATILE_REGISTERS);
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
    registerState = RegisterState.condPush(emitter, registers, Register.VOLATILE_REGISTERS);
    String rightName = resolver.resolve(right);
    emitter.emit("; strcat from %s to %s", rightName, dest);
    if (resolver.isInRegister(right, RCX) && dest.equals(RDX.name64())) {
      // just swap rdx & rcx
      emitter.emit("; right wants to be in rcx dest in rdx, just swap");
      emitter.emit("xchg RDX, RCX");
    } else if (resolver.isInRegister(right, RCX)) {
      emitter.emit("; opposite order, so that we don't munge rcx");
      emitter.emit("mov RDX, %s", rightName);
      emitter.emit("mov RCX, %s", dest);
    } else {
      emitter.emit("mov RCX, %s", dest);
      emitter.emit("mov RDX, %s", rightName);
    }
    emitter.emitExternCall("strcat");
    registerState.condPop();
  }

  /** Generate destination = length(source) */
  void generateStringLength(Location destination, Operand source) {
    // We're doing something special with RAX & RCX
    RegisterState raxRcxState =
        RegisterState.condPush(emitter, registers, ImmutableList.of(RAX, RCX));
    RegisterState registerState =
        RegisterState.condPush(emitter, registers, ImmutableList.of(RDX, R8, R9, R10, R11));

    String sourceName = resolver.resolve(source);
    String destinationName = resolver.resolve(destination);
    if (resolver.isInRegister(source, RCX)) {
      emitter.emit("; RCX already has address of string");
    } else {
      emitter.emit("mov RCX, %s  ; Address of string", sourceName);
    }
    emitter.emitExternCall("strlen");
    registerState.condPop();

    if (resolver.isInRegister(destination, RCX)) {
      // pseudo pop
      emitter.emit("; pseudo-pop; destination was already %s", destination);
      if (raxRcxState.pushed(RCX)) {
        emitter.emit("add RSP, 8");
      }
    } else {
      raxRcxState.condPop(RCX);
    }
    if (resolver.isInRegister(destination, RAX)) {
      // pseudo pop; eax already has the length.
      emitter.emit("; pseudo-pop; destination was already %s", destination);
      if (raxRcxState.pushed(RAX)) {
        emitter.emit("add RSP, 8");
      }
    } else {
      // NOTE: eax not rax, because lengths are always ints (32 bits)
      emitter.emit("mov %s, EAX  ; %s = strlen(%s)", destinationName, destinationName, sourceName);
      raxRcxState.condPop(RAX);
    }
  }

  /** Generate destName = chr(sourceName), where sourceName is a number */
  void generateChr(String sourceName, String destName) {
    // realistically, nothing is ever kept in RAX because it's the return value register...
    RegisterState raxState = RegisterState.condPush(emitter, registers, ImmutableList.of(RAX));
    RegisterState registerState =
        RegisterState.condPush(emitter, registers, Register.VOLATILE_REGISTERS);

    // 1. allocate a new 2-char string
    emitter.emit("mov RCX, 2");
    emitter.emitExternCall("malloc");
    registerState.condPop();
    // 2. set destName to allocated string
    if (!destName.equals(RAX.name64())) {
      emitter.emit("mov %s, RAX  ; copy string location from RAX", destName);
    }

    Register charReg = registers.allocate(VarType.INT);
    // 3. get source char as character
    emitter.emit(
        "mov DWORD %s, %s  ; get the character int into %s", charReg.name32(), sourceName, charReg);
    // 4. write source char in first location
    emitter.emit(
        "mov BYTE [RAX], %s  ; move the character into the first location", charReg.name8());
    // 5. clear second location.
    emitter.emit("mov BYTE [RAX+1], 0  ; clear the 2nd location");
    raxState.condPop();
    registers.deallocate(charReg);
  }
}
