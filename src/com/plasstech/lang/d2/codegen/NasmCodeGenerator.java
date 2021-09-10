package com.plasstech.lang.d2.codegen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Dec;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.Goto;
import com.plasstech.lang.d2.codegen.il.IfOp;
import com.plasstech.lang.d2.codegen.il.Inc;
import com.plasstech.lang.d2.codegen.il.Label;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Stop;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.phase.Phase;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.type.SymTab;
import com.plasstech.lang.d2.type.Symbol;
import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VarType;

public class NasmCodeGenerator extends DefaultOpcodeVisitor implements Phase {

  private static FluentLogger logger = FluentLogger.forEnclosingClass();

  private final List<String> asm = new ArrayList<>();
  private final Registers registers = new Registers();
  // map from name to register
  private final BiMap<String, Register> aliases = HashBiMap.create(16);
  private final List<Register> lruRegs = new ArrayList<>();
  // it's possible that we've run out of registers. Push the name on the stack and deallocate
  // its register. (Maybe put this feature into Registers? Maybe make an "Aliases" object?)
  private final Stack<String> tempStack = new Stack<>();

  private StringTable stringTable;

  @Override
  public State execute(State input) {
    stringTable = new StringFinder().execute(input.lastIlCode());

    ImmutableList<Op> code = input.lastIlCode();
    String f = "dcode";
    if (input.filename() != null) {
      f = input.filename();
    }

    emit0("; To execute:");
    // -Ox = optimize
    emit0("; nasm -fwin64 -Ox %s.asm && gcc %s.obj -o %s && ./%s\n", f, f, f, f);

    emit0("global main"); // required

    emit0("\nextern printf"); // optional
    emit0("extern strlen"); // optional
    emit0("extern strcat"); // optional
    emit0("extern strcpy"); // optional
    emit0("extern malloc"); // optional
    emit0("extern exit"); // required

    // Probably what we should do is:
    // 1. emit all globals OK
    // 2. emit all string constants OK
    // 3. emit all array constants (?)
    emit0("\nsection .data");
    SymTab globals = input.symbolTable();
    for (Map.Entry<String, Symbol> entry : globals.entries().entrySet()) {
      Symbol symbol = entry.getValue();
      if (symbol.storage() == SymbolStorage.GLOBAL) {
        // temporarily reserve (& clear) 1 byte for bool, 4 bytes per int, 8 bytes for string
        if (symbol.varType() == VarType.INT) {
          emit("__%s: dd 0", entry.getKey());
        } else if (symbol.varType() == VarType.BOOL) {
          emit("__%s: db 0", entry.getKey());
        } else if (symbol.varType() == VarType.STRING) {
          emit("__%s: dq 0", entry.getKey());
        }
      }
    }

    // TODO: only emit these if we need to. Probably can do this in the StringFinder
    emit("PRINTF_NUMBER_FMT: db \"%%d\", 0");
    emit("CONST_TRUE: db \"true\", 0");
    emit("CONST_FALSE: db \"false\", 0");
    emit("EXIT_MSG: db \"ERROR: %%s\", 0");
    for (StringEntry entry : stringTable.orderedEntries()) {
      emit(entry.dataEntry());
    }

    emit0("\nsection .text\n");

    // TODO: convert command-line arguments to ??? and send to __main
    emit0("main:");
    for (Op opcode : code) {
      emit(opcode);
      opcode.accept(this);
    }

    return input.addAsmCode(ImmutableList.copyOf(asm));
  }

  @Override
  public void visit(Label op) {
    emit0("%s:", op.label());
  }

  @Override
  public void visit(Stop op) {
    emit("mov RCX, %d", op.exitCode());
    emit("call exit");
  }

  @Override
  public void visit(Goto op) {
    emit("jmp %s", op.label());
  }

  @Override
  public void visit(Transfer op) {
    Operand source = op.source();
    String sourceLoc = resolve(source);
    Location destination = op.destination();
    String destLoc = resolve(destination);
    String size = dataSize(op.source().type());

    if (source.type() == VarType.STRING) {
      // move from sourceLoc to temp
      // then from temp to dest
      Register tempReg = registers.allocate();
      // TODO: deal with out-of-registers
      String tempName = registerNameSized(tempReg, op.source().type());
      emit("mov %s, %s", tempName, sourceLoc);
      emit("mov %s, %s", destLoc, tempName);
      registers.deallocate(tempReg);
      deallocate(source);
      return;
    }

    if (source.isConstant()) {
      emit("mov %s %s, %s", size, destLoc, sourceLoc);
    } else {
      if (!sourceLoc.startsWith("[")) {
        // register. Just move it.
        emit("mov %s %s, %s", size, destLoc, sourceLoc);
      } else {
        Register tempReg = registers.allocate();
        // TODO: deal with out-of-registers
        String tempName = registerNameSized(tempReg, op.source().type());
        emit("mov %s %s, %s", size, tempName, sourceLoc);
        emit("mov %s %s, %s", size, destLoc, tempName);
        registers.deallocate(tempReg);
      }
    }
    deallocate(source);
  }

  @Override
  public void visit(SysCall op) {
    Operand arg = op.arg();
    switch (op.call()) {
      case MESSAGE:
      case PRINT:
        String argVal = resolve(arg);
        emit("sub RSP, 0x28  ; Reserve the shadow space");
        boolean pushedRcx = condPush(Register.RCX);
        boolean pushedRdx = false;
        if (arg.type() == VarType.INT) {
          pushedRdx = condPush(Register.RDX);
          emit("mov RCX, PRINTF_NUMBER_FMT  ; First argument is address of pattern");
          emit("mov DWORD EDX, %s  ; Second argument is parameter", argVal);
          emit("call printf  ; printf(message)");
          emit("add RSP, 0x28  ; Remove shadow space");
        } else if (arg.type() == VarType.BOOL) {
          if (argVal.equals("1")) {
            emit("mov RCX, CONST_TRUE");
          } else if (argVal.equals("0")) {
            emit("mov RCX, CONST_FALSE");
          } else {
            // translate dynamically from 0/1 to false/true
            emit("mov RCX, CONST_FALSE");
            emit("cmp byte %s, 1", argVal);
            pushedRdx = condPush(Register.RDX);
            emit("mov RDX, CONST_TRUE");
            emit("cmovz RCX, RDX");
          }
          emit("call printf  ; printf(message)");
          emit("add RSP, 0x28  ; Remove shadow space");
        } else if (arg.type() == VarType.STRING) {
          if (op.call() == SysCall.Call.MESSAGE) {
            pushedRdx = condPush(Register.RDX);
            emit("mov RCX, EXIT_MSG  ; First argument is address of pattern");
            emit("mov RDX, %s  ; Second argument is parameter/string to print", argVal);
          } else {
            emit("mov RCX, %s  ; String to print", argVal);
          }
          // String
          emit("call printf  ; printf(%s)", argVal);
          emit("add RSP, 0x28  ; Remove shadow space");
        } else {
          fail("Cannot print %s yet", arg);
        }
        condPop(Register.RDX, pushedRdx);
        condPop(Register.RCX, pushedRcx);
        break;
      default:
        fail("Cannot generate %s yet", op);
        break;
    }
    deallocate(op.arg());
  }

  @Override
  public void visit(Dec op) {
    emit("dec dword %s", resolve(op.target()));
  }

  @Override
  public void visit(Inc op) {
    emit("inc dword %s", resolve(op.target()));
  }

  @Override
  public void visit(IfOp op) {
    String condName = resolve(op.condition());
    emit("cmp byte %s, 0", condName);
    emit("jne %s", op.destination());
    // deallocate
    Operand operand = op.condition();
    deallocate(operand);
  }

  @Override
  public void visit(BinOp op) {
    // 1. get left
    String leftName = resolve(op.left());
    // 2. get right
    String rightName = resolve(op.right());

    if (op.operator() == TokenType.DIV || op.operator() == TokenType.MOD) {
      generateDivMod(op, leftName, rightName);
      deallocate(op.left());
      deallocate(op.right());
      return;
    }
    VarType sourceType = op.left().type();
    String destName = resolve(op.destination());
    if (op.operator() == TokenType.PLUS && sourceType == VarType.STRING) {
      generateStringAdd(leftName, rightName, destName);
      deallocate(op.left());
      deallocate(op.right());
      return;
    }

    // 3. determine dest location
    VarType destType = op.destination().type();
    String size = dataSize(destType);
    if (op.operator() != TokenType.LBRACKET) {
      // 4. mov dest, left
      emit("mov %s %s, %s ; binary setup", size, destName, leftName);
    }

    boolean pushedRcx = false;
    // 5. [op] dest, right
    switch (op.operator()) {
      case PLUS:
        if (sourceType == VarType.INT) {
          // TODO: optimize adding 1
          emit("add %s, %s ; binary %s", destName, rightName, op.operator());
        } else {
          fail("Cannot do %s on %ss", op.operator(), destType);
        }
        break;
      case MINUS:
        // TODO: optimize subtracting 1
        emit("sub %s, %s ; binary %s", destName, rightName, op.operator());
        break;
      case MULT:
        emit("imul %s, %s ; binary %s", destName, rightName, op.operator());
        break;

      case SHIFT_LEFT:
        pushedRcx = condPush(Register.RCX);
        // TODO: this is a problem if rightname is CL
        emit("mov CL, %s ; shift left prep", rightName);
        emit("shl %s, CL ; binary %s", destName, op.operator());
        condPop(Register.RCX, pushedRcx);
        break;
      case SHIFT_RIGHT:
        pushedRcx = condPush(Register.RCX);
        // TODO: this is a problem if rightname is CL
        emit("mov CL, %s ; shift left prep", rightName);
        emit("sar %s, CL ; binary %s", destName, op.operator());
        condPop(Register.RCX, pushedRcx);
        break;
      case AND:
      case BIT_AND:
        emit("and %s, %s ; binary and", destName, rightName);
        break;
      case OR:
      case BIT_OR:
        emit("or %s, %s ; binary or", destName, rightName);
        break;
      case XOR:
      case BIT_XOR:
        emit("xor %s, %s ; binary xor", destName, rightName);
        break;

      case EQEQ:
        if (sourceType != VarType.INT) {
          fail("Cannot do %s on %ss", op.operator(), destType);
        }
        // TODO: THIS IS WRONG. it's not comparing with the right size - it's using the
        // *destination* size instead of the *source* size.
        emit("cmp %s, %s ; binary %s", destName, rightName, op.operator());
        emit("setz %s", destName);
        break;
      case NEQ:
        if (sourceType != VarType.INT) {
          fail("Cannot do %s on %ss", op.operator(), destType);
        }
        emit("cmp %s, %s ; binary %s", destName, rightName, op.operator());
        emit("setnz %s", destName);
        break;
      case GT:
        if (sourceType != VarType.INT) {
          fail("Cannot do %s on %ss", op.operator(), destType);
        }
        emit("cmp %s, %s ; binary %s", destName, rightName, op.operator());
        emit("setg %s", destName);
        break;
      case GEQ:
        if (sourceType != VarType.INT) {
          fail("Cannot do %s on %ss", op.operator(), destType);
        }
        emit("cmp %s, %s ; binary %s", destName, rightName, op.operator());
        emit("setge %s", destName);
        break;
      case LT:
        if (sourceType != VarType.INT) {
          fail("Cannot do %s on %ss", op.operator(), destType);
        }
        emit("cmp %s, %s ; binary %s", destName, rightName, op.operator());
        emit("setl %s", destName);
        break;
      case LEQ:
        if (sourceType != VarType.INT) {
          fail("Cannot do %s on %ss", op.operator(), destType);
        }
        emit("cmp %s, %s ; binary %s", destName, rightName, op.operator());
        emit("setle %s", destName);
        break;

      case LBRACKET:
        if (sourceType != VarType.STRING) {
          fail("Cannot do %s on %ss", op.operator(), destType);
        }
        generateStringIndex(leftName, rightName, destName);
        break;

      default:
        fail("Cannot generate %s yet", op);
    }
    deallocate(op.left());
    deallocate(op.right());
  }

  private void generateStringIndex(String leftName, String rightName, String destName) {
    boolean pushedRcx = condPush(Register.RCX);
    // 1. allocate a new 2-char string
    emit("sub RSP, 0x28  ; Reserve the shadow space");
    emit("mov RCX, 2");
    emit("call malloc  ; malloc(2)");
    emit("add RSP, 0x28  ; Remove shadow space");
    condPop(Register.RCX, pushedRcx);
    // 2. copy the location to the dest
    emit("mov %s, RAX  ; destination from rax", destName);

    // TODO: deal with out-of-registers
    Register indexReg = registers.allocate();
    Register charReg = registers.allocate();
    // 3. get the string
    emit("mov %s, %s  ; get the string into %s", charReg, leftName, charReg);
    // 4. get the index
    emit("mov %s, %s  ; put index value into %s", indexReg.name32, rightName, indexReg.name32);
    // 5. get the actual character
    emit("mov %s, [%s+%s]  ; get the character", charReg, charReg, indexReg);
    registers.deallocate(indexReg);
    // 6. copy the character to the first location
    emit("mov byte [RAX], %s  ; move the character into the first location", charReg.name8);
    registers.deallocate(charReg);
    // 7. clear the 2nd location
    emit("mov byte [RAX+1], 0  ; clear the 2nd location");
  }

  private void generateStringAdd(String left, String right, String dest) {
    // 1. get left length
    Register leftReg = registers.allocate();
    emit0("");
    emit("; Get left length:");
    generateStringLength(left, leftReg.name32);
    // 2. get right length
    Register rightReg = registers.allocate();
    // TODO: if leftReg is volatile, push it first
    emit0("");
    emit("; Get right length:");
    generateStringLength(right, rightReg.name32);
    emit("add %s, %s  ; Total new string length", leftReg.name32, rightReg.name32);
    emit("inc %s  ; Plus 1 for end of string", leftReg.name32);
    registers.deallocate(rightReg);

    // 3. allocate string of length left+right + 1
    emit0("");
    emit("; Allocate string of length %s", leftReg.name32);
    boolean pushedRcx = condPush(Register.RCX);
    boolean pushedRdx = condPush(Register.RDX);
    emit("sub RSP, 0x28  ; Reserve the shadow space");
    emit("mov ECX, %s", leftReg.name32);
    emit("call malloc  ; malloc(%s)", leftReg.name32);
    registers.deallocate(leftReg);
    // 4. put string into dest
    if (!dest.equals(Register.RAX.name64)) {
      emit("mov %s, RAX  ; destination from rax", dest);
    }

    // 5. strcpy from left to dest
    emit0("");
    emit("; strcpy from %s to %s", left, dest);
    emit("mov RCX, %s", dest);
    emit("mov RDX, %s", left);
    emit("call strcpy");
    // 6. strcat dest, right
    emit0("");
    emit("; strcat from %s to %s", right, dest);
    emit("mov RCX, %s", dest);
    emit("mov RDX, %s", right);
    emit("call strcat");
    emit("add RSP, 0x28  ; Remove shadow space");
    condPop(Register.RDX, pushedRdx);
    condPop(Register.RCX, pushedRcx);
  }

  private void generateDivMod(BinOp op, String leftName, String rightName) {
    // 3. determine dest location
    String destName = resolve(op.destination());
    // 4. set up left in EDX:EAX
    // 5. idiv by right, result in eax
    // 6. mov destName, eax
    boolean pushedRax = condPush(Register.RAX);
    boolean pushedRdx = condPush(Register.RDX);
    registers.reserve(Register.RAX);
    registers.reserve(Register.RDX);
    Register temp = registers.allocate();
    emit("mov %s, %s  ; denominator", temp.name32, rightName);
    if (!leftName.equals(Register.RAX.name32)) {
      emit("mov EAX, %s  ; numerator", leftName);
    } else {
      emit("; denominator already in EAX");
    }
    // sign extend eax to edx
    emit("cdq ; sign extend eax to edx");
    emit("idiv %s  ; %s / %s", temp.name32, leftName, rightName);
    registers.deallocate(temp);
    if (!pushedRdx) {
      registers.deallocate(Register.RDX);
    }
    if (!pushedRax) {
      registers.deallocate(Register.RAX);
    }
    // not required if it's already supposed to be in eax
    if (op.operator() == TokenType.DIV && !destName.equals(Register.RAX.name32)) {
      // eax has dividend
      emit("mov %s, EAX ; dividend", destName);
    }
    if (op.operator() == TokenType.MOD && !destName.equals(Register.RDX.name32)) {
      // edx has remainder
      emit("mov %s, EDX ; remainder", destName);
    }
    if (!destName.equals(Register.RDX.name32)) {
      condPop(Register.RDX, pushedRdx);
    } else {
      // pseudo pop
      emit("add RSP, 8");
    }
    if (!destName.equals(Register.RAX.name32)) {
      condPop(Register.RAX, pushedRax);
    } else {
      // pseudo pop
      emit("add RSP, 8");
    }
    deallocate(op.left());
    deallocate(op.right());
  }

  @Override
  public void visit(UnaryOp op) {
    // 1. get source location name
    Operand source = op.operand();
    String sourceName = resolve(source);
    // 2. apply op
    // 3. store in destination
    Location destination = op.destination();
    String destName = resolve(destination);
    // apply op
    switch (op.operator()) {
      case BIT_NOT:
        // NOTE: NOT TWOS COMPLEMENT NOT, it's 1-s complement not.
        emit("mov dword %s, %s  ; unary setup", destName, sourceName);
        emit("not %s  ; unary not", destName);
        break;
      case NOT:
        // binary not
        // 1. compare to 0
        emit("cmp byte %s, 0", sourceName);
        // 2. setz %s
        emit("setz %s", destName);
        break;
      case MINUS:
        emit("mov dword %s, %s  ; unary setup", destName, sourceName);
        emit("neg %s  ; unary negation", destName);
        break;
      case LENGTH:
        if (source.type() == VarType.STRING) {
          generateStringLength(sourceName, destName);
        } else {
          // array length
          fail("Cannot generate %s yet", op);
        }
        break;
      case ASC:
        // move from sourceLoc to temp
        // then from temp to dest
        Register tempReg = registers.allocate();
        // TODO: deal with out-of-registers
        // Just read one byte
        if (source.isConstant()) {
          emit("mov byte %s, %s ; copy one byte / first character", tempReg.name8, sourceName);
          emit("mov byte %s, %s ; store a full int. shrug", destName, tempReg.name32);
        } else {
          // need to do two indirects
          emit("mov %s, %s  ; %s has address of string", tempReg, sourceName, tempReg);
          emit("mov byte %s, [%s] ; copy a byte", destName, tempReg);
        }
        emit("and %s, 0x000000ff", destName);
        registers.deallocate(tempReg);
        break;
      case CHR:
        generateChr(sourceName, destName);
        break;
      default:
        fail("Cannot generate %s yet", op);
        break;
    }
    deallocate(op.operand());
  }

  private void generateStringLength(String sourceName, String destName) {
    boolean pushedRcx = condPush(Register.RCX);
    boolean pushedRax = condPush(Register.RAX);
    emit("sub RSP, 0x28  ; Reserve the shadow space");
    emit("mov RCX, %s  ; Address of string", sourceName);
    emit("call strlen  ; strlen(%s)", sourceName);
    emit("add RSP, 0x28  ; Remove shadow space");
    if (destName.equals(Register.RAX.name32)) {
      // pseudo pop; eax already has the length.
      emit("add RSP, 8");
    } else {
      // NOTE: eax not rax, because lengths are always ints (32 bits)
      emit("mov %s, EAX  ; %s = strlen(%s)", destName, destName, sourceName);
      condPop(Register.RAX, pushedRax);
    }

    if (destName.equals(Register.RCX.name32)) {
      // pseudo pop
      emit("add RSP, 8");
    } else {
      condPop(Register.RCX, pushedRcx);
    }
  }

  private void generateChr(String sourceName, String destName) {
    boolean pushedRax = condPush(Register.RAX);
    boolean pushedRcx = condPush(Register.RCX);
    // 1. allocate a new 2-char string
    emit("sub RSP, 0x28  ; Reserve the shadow space");
    emit("mov RCX, 2");
    emit("call malloc  ; malloc(2)");
    emit("add RSP, 0x28  ; Remove shadow space");
    condPop(Register.RCX, pushedRcx);
    // 2. set destName to allocated string
    if (!destName.equals(Register.RAX.name64)) {
      emit("mov %s, RAX  ; copy string location from RAX", destName);
    }

    Register charReg = registers.allocate();
    // 3. get source char as character
    emit("mov DWORD %s, %s  ; get the character int into %s", charReg.name32, sourceName, charReg);
    // 4. write source char in first location
    emit("mov byte [RAX], %s  ; move the character into the first location", charReg.name8);
    // 5. clear second location.
    emit("mov byte [RAX+1], 0  ; clear the 2nd location");
    condPop(Register.RAX, pushedRax);
    registers.deallocate(charReg);
  }

  /** Conditionally push the register, if it's already in use. */
  private boolean condPush(Register reg) {
    if (registers.isAllocated(reg)) {
      emit("push %s", reg.name64);
      return true;
    }
    return false;
  }

  /** Conditionally pop the register, if it's been pushed. */
  private void condPop(Register reg, boolean pushed) {
    if (pushed) {
      emit("pop %s", reg.name64);
    }
  }

  private void deallocate(Operand operand) {
    if (operand.storage() == SymbolStorage.TEMP) {
      // now that we used the temp, unallocate it
      String operandName = operand.toString();
      Register reg = aliases.get(operandName);
      if (reg != null) {
        emit("; Deallocating %s from %s", operand, reg);
        aliases.remove(operandName);
        registers.deallocate(reg);
      }
    }
  }

  private String resolve(Operand operand) {
    if (operand.isConstant()) {
      if (operand.type() == VarType.INT) {
        return operand.toString();
      } else if (operand.type() == VarType.BOOL) {
        ConstantOperand<Boolean> boolConst = (ConstantOperand<Boolean>) operand;
        if (boolConst.value() == Boolean.TRUE) {
          return "1";
        }
        return "0";
      } else if (operand.type() == VarType.STRING) {
        // look it up in the string table.
        ConstantOperand<String> stringConst = (ConstantOperand<String>) operand;
        StringEntry entry = stringTable.lookup(stringConst.value());
        return entry.name();
      }
      fail("Cannot generate %s operand %s yet", operand.storage(), operand);
      return null;
    }

    Location location = (Location) operand;
    Register reg = aliases.get(location.toString());
    if (reg != null) {
      // Found it in a register.
      return registerNameSized(reg, location.type());
    }
    switch (location.storage()) {
      case TEMP:
        reg = registers.allocate();
        // if (reg == null) {
        //   // deal with out-of-registers situation
        //   // pick the least recently used one (say, the first one)
        //   reg = lruRegs.remove(0);
        //   // don't have to deallocate, since we're just re-using the register out from
        // underneath
        //   // the registers object.
        //   // TODO: push the value onto the stack
        //   // remove the alias too.
        //   aliases.inverse().remove(reg);
        // }
        // lruRegs.add(reg); // add to the end.
        aliases.put(location.name(), reg);
        emit("; Allocating %s to %s", location, reg);
        return registerNameSized(reg, location.type());
      case GLOBAL:
        // TODO: deal with array
        return "[__" + location.name() + "]";
      default:
        fail("Cannot generate %s operand %s yet", location.storage(), location);
        return null;
    }
  }

  private static String dataSize(VarType type) {
    String size = "";
    if (type == VarType.INT) {
      size = "dword";
    } else if (type == VarType.BOOL) {
      size = "byte";
    }
    return size;
  }

  private static String registerNameSized(Register reg, VarType type) {
    if (type == VarType.INT) {
      return reg.name32;
    } else if (type == VarType.BOOL) {
      return reg.name8;
    }
    return reg.name64;
  }

  // Emit at column 2
  private void emit(String format, Object... values) {
    emit0("  " + format, values);
  }

  private void emit(Op op) {
    emit0("\n  ; %s", op.toString());
  }

  // Emit at column 0
  private void emit0(String format, Object... values) {
    asm.add(String.format(format, values));
    logger.atFine().log(String.format(format, values));
  }

  private void fail(String format, Object... values) {
    throw new UnsupportedOperationException(String.format(format, values));
  }
}
