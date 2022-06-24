package com.plasstech.lang.d2.codegen;

import static com.plasstech.lang.d2.codegen.Register.R8;
import static com.plasstech.lang.d2.codegen.Register.R9;
import static com.plasstech.lang.d2.codegen.Register.RAX;
import static com.plasstech.lang.d2.codegen.Register.RCX;
import static com.plasstech.lang.d2.codegen.Register.RDX;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Call;
import com.plasstech.lang.d2.codegen.il.Dec;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.Goto;
import com.plasstech.lang.d2.codegen.il.IfOp;
import com.plasstech.lang.d2.codegen.il.Inc;
import com.plasstech.lang.d2.codegen.il.Label;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.ProcEntry;
import com.plasstech.lang.d2.codegen.il.ProcExit;
import com.plasstech.lang.d2.codegen.il.Return;
import com.plasstech.lang.d2.codegen.il.Stop;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.parse.node.ProcedureNode.Parameter;
import com.plasstech.lang.d2.phase.Phase;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.type.SymTab;
import com.plasstech.lang.d2.type.Symbol;
import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VarType;

public class NasmCodeGenerator extends DefaultOpcodeVisitor implements Phase {

  public enum Size {
    _1BYTE(1, "byte"),
    _32BITS(32, "dword"),
    _64BITS(64, "");

    public final int bytes;
    public final String asmName;

    Size(int bytes, String asmName) {
      this.bytes = bytes;
      this.asmName = asmName;
    }
  }

  private static final ImmutableList<Register> PARAM_REGISTERS = ImmutableList.of(RCX, RDX, R8, R9);
  private static final ImmutableList<Register> PARAM_REGISTERS_REVERSED = PARAM_REGISTERS.reverse();
  private static FluentLogger logger = FluentLogger.forEnclosingClass();

  private final List<String> asm = new ArrayList<>();
  private final Registers registers = new Registers();
  // map from name to register
  private final BiMap<String, Register> aliases = HashBiMap.create(16);

  private StringTable stringTable;

  private Map<TokenType, String> BINARY_OPCODE =
      ImmutableMap.<TokenType, String>builder()
          .put(TokenType.PLUS, "add")
          .put(TokenType.MINUS, "sub")
          .put(TokenType.MULT, "imul")
          .put(TokenType.AND, "and") // for boolean
          .put(TokenType.OR, "or") // for boolean
          .put(TokenType.XOR, "xor") // for boolean
          .put(TokenType.BIT_AND, "and") // for ints
          .put(TokenType.BIT_OR, "or") // for ints
          .put(TokenType.BIT_XOR, "xor") // for ints
          .put(TokenType.SHIFT_LEFT, "shl")
          .put(TokenType.SHIFT_RIGHT, "sar")
          .put(TokenType.EQEQ, "setz") // for both int and boolean
          .put(TokenType.NEQ, "setnz") // for both int and boolean
          .put(TokenType.GT, "setg") // for both int and boolean
          .put(TokenType.GEQ, "setge") // for both int and boolean
          .put(TokenType.LT, "setl") // for both int and boolean
          .put(TokenType.LEQ, "setle") // for both intand boolean
          .build();

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
    emit0("extern strcmp"); // optional
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
      emitComment(opcode);
      opcode.accept(this);
    }

    return input.addAsmCode(ImmutableList.copyOf(asm));
  }

  @Override
  public void visit(Label op) {
    emit0("__%s:", op.label());
  }

  @Override
  public void visit(Stop op) {
    emit("mov RCX, %d", op.exitCode());
    emit("call exit");
  }

  @Override
  public void visit(Goto op) {
    emit("jmp __%s", op.label());
  }

  @Override
  public void visit(Transfer op) {
    Operand source = op.source();
    String sourceLoc = resolve(source);
    Location destination = op.destination();
    Register fromReg = toRegister(source);
    Register toReg = toRegister(destination);
    String destLoc = resolve(destination);
    String size = size(op.source().type()).asmName;

    if (source.type() == VarType.STRING) {
      if (toReg != null || fromReg != null) {
        // go right from source to dest
        emit("; short circuit string");
        emit("mov %s, %s", destLoc, sourceLoc);
      } else {
        // move from sourceLoc to temp
        // then from temp to dest
        Register tempReg = registers.allocate();
        // TODO: deal with out-of-registers
        String tempName = registerNameSized(tempReg, op.source().type());
        emit("mov %s, %s", tempName, sourceLoc);
        emit("mov %s, %s", destLoc, tempName);
        registers.deallocate(tempReg);
        deallocate(source);
      }

      return;
    }

    if (source.isConstant() || source.isRegister()) {
      emit("mov %s %s, %s", size, destLoc, sourceLoc);
    } else {
      if (toReg != null || fromReg != null) {
        // go right from source to dest
        emit("; short circuit int");
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
    // need to preserve used registers!
    boolean pushedRax = condPush(RAX);
    boolean pushedR8 = condPush(R8);
    boolean pushedR9 = condPush(R9);
    boolean pushedRcx = condPush(RCX);
    boolean pushedRdx = condPush(RDX);

    switch (op.call()) {
      case MESSAGE:
      case PRINT:
        String argVal = resolve(arg);
        if (arg.type() == VarType.INT) {
          // move with sign extend. intentionally set rdx first, in case the arg is in ecx

          emit("movsx RDX, DWORD %s  ; Second argument is parameter", argVal);
          emit("mov RCX, PRINTF_NUMBER_FMT  ; First argument is address of pattern");
          emit("sub RSP, 0x20  ; Reserve the shadow space");
          emit("call printf    ; printf(message)");
          emit("add RSP, 0x20  ; Remove shadow space");
        } else if (arg.type() == VarType.BOOL) {
          if (argVal.equals("1")) {
            emit("mov RCX, CONST_TRUE");
          } else if (argVal.equals("0")) {
            emit("mov RCX, CONST_FALSE");
          } else {
            // translate dynamically from 0/1 to false/true
            // Intentionally do the comp first, in case the arg is in dl or cl
            emit("cmp byte %s, 1", argVal);
            emit("mov RCX, CONST_FALSE");
            emit("mov RDX, CONST_TRUE");
            // Conditional move
            emit("cmovz RCX, RDX");
          }
          emit("sub RSP, 0x20  ; Reserve the shadow space");
          emit("call printf    ; printf(%s)", argVal);
          emit("add RSP, 0x20  ; Remove shadow space");
        } else if (arg.type() == VarType.STRING) {
          // String
          if (op.call() == SysCall.Call.MESSAGE) {
            // Intentionally set rdx first in case the arg is in rcx
            if (!isInRegister(arg, RDX)) {
              // arg is not in rdx yet
              emit("mov RDX, %s  ; Second argument is parameter/string to print", argVal);
            }
            emit("mov RCX, EXIT_MSG  ; First argument is address of pattern");
          } else if (!isInRegister(arg, RCX)) {
            // arg is not in rcx yet
            emit("mov RCX, %s  ; String to print", argVal);
          }
          emit("sub RSP, 0x20  ; Reserve the shadow space");
          emit("call printf    ; printf(%s)", argVal);
          emit("add RSP, 0x20  ; Remove shadow space");
        } else {
          fail("Cannot print %s yet", arg);
        }
        break;
      default:
        fail("Cannot generate %s yet", op);
        break;
    }
    condPop(RDX, pushedRdx);
    condPop(RCX, pushedRcx);
    condPop(R9, pushedR9);
    condPop(R8, pushedR8);
    condPop(RAX, pushedRax);
    deallocate(op.arg());
  }

  /**
   * @param source
   * @return the equivalent register, or null if none.
   */
  private Register toRegister(Operand source) {
    if (source.isConstant()) {
      return null;
    }

    Location location = (Location) source;
    if (source.isRegister()) {
      return ((RegisterLocation) location).register();
    }
    Register aliasReg = aliases.get(location.toString());
    if (aliasReg != null) {
      return aliasReg;
    }
    switch (location.storage()) {
      case TEMP:
        return null; // we would have found its alias already.
      case GLOBAL:
        return null;
      case PARAM:
        ParamLocation paramLoc = (ParamLocation) location;
        Register actualReg = paramRegister(paramLoc.index());
        return actualReg;
      case LOCAL:
        return null;
      default:
        fail("Cannot generate %s operand %s yet", location.storage(), location);
        return null;
    }
  }

  private boolean isInRegister(Operand arg, Register register) {
    return isInRegister2(arg, register);
  }

  private boolean isInRegister2(Operand arg, Register register) {
    if (arg.isConstant()) {
      return false;
    }

    if (arg.isRegister()) {
      Register actualReg = ((RegisterLocation) arg).register();
      return register == actualReg;
    }
    Location location = (Location) arg;
    Register aliasReg = aliases.get(location.toString());
    if (aliasReg != null) {
      return aliasReg == register;
    }
    switch (location.storage()) {
      case TEMP:
        return false; // we would have found its alias already.
      case GLOBAL:
        return false;
      case PARAM:
        ParamLocation paramLoc = (ParamLocation) location;
        Register actualReg = paramRegister(paramLoc.index());
        return actualReg == register;
      case LOCAL:
        return false;
      default:
        fail("Cannot generate %s operand %s yet", location.storage(), location);
        return false;
    }
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
    emit("jne __%s", op.destination());
    // deallocate
    Operand operand = op.condition();
    deallocate(operand);
  }

  @Override
  public void visit(BinOp op) {
    // 1. get left
    String leftName = resolve(op.left());
    VarType leftType = op.left().type();
    // 2. get right
    String rightName = resolve(op.right());

    // 3. determine dest location
    String destName = resolve(op.destination());

    Register tempReg = null;

    // 5. [op] dest, right
    TokenType operator = op.operator();
    if (leftType == VarType.STRING) {
      switch (operator) {
        case PLUS:
          generateStringAdd(leftName, rightName, destName);
          break;
        case LBRACKET:
          generateStringIndex(leftName, rightName, destName);
          break;
        case EQEQ:
        case NEQ:
        case GT:
        case GEQ:
        case LT:
        case LEQ:
          generateStringCompare(op.left(), op.right(), destName, operator);
          break;

        default:
          fail("Cannot do %s on %ss", operator, leftType);
          break;
      }
    } else if (leftType == VarType.BOOL) {
      switch (operator) {
        case AND:
        case OR:
        case XOR:
          emit("mov BYTE %s, %s ; boolean setup", destName, leftName);
          emit(
              "%s %s, %s ; boolean %s", BINARY_OPCODE.get(operator), destName, rightName, operator);
          break;

        case EQEQ:
        case NEQ:
        case GT:
        case GEQ:
        case LT:
        case LEQ:
          tempReg = registers.allocate();
          emit("mov BYTE %s, %s ; compare setup", tempReg.name8, leftName);
          emit("cmp %s, %s", tempReg.name8, rightName, operator);
          emit("%s %s ; bool compare %s", BINARY_OPCODE.get(operator), destName, operator);
          break;

        default:
          fail("Cannot do %s on %ss", operator, leftType);
          break;
      }
    } else if (leftType == VarType.INT) {
      String size = "DWORD";
      switch (operator) {
        case PLUS:
        case MINUS:
        case MULT:
        case BIT_AND:
        case BIT_OR:
        case BIT_XOR:
          emit("mov %s %s, %s ; int setup", size, destName, leftName);
          emit("%s %s, %s ; int %s", BINARY_OPCODE.get(operator), destName, rightName, operator);
          break;

        case SHIFT_LEFT:
        case SHIFT_RIGHT:
          {
            emit("mov %s %s, %s ; shift setup", size, destName, leftName);
            boolean pushedRcx = condPush(RCX);
            // TODO: this is a problem if rightname is CL
            emit("mov CL, %s ; shift prep", rightName);
            emit("%s %s, CL ; shift %s", BINARY_OPCODE.get(operator), destName, operator);
            condPop(RCX, pushedRcx);
          }
          break;

        case EQEQ:
        case NEQ:
        case GT:
        case GEQ:
        case LT:
        case LEQ:
          tempReg = registers.allocate();
          emit("mov DWORD %s, %s ; int compare setup", tempReg.name32, leftName);
          emit("cmp %s, %s", tempReg.name32, rightName);
          emit("%s %s  ; int compare %s", BINARY_OPCODE.get(operator), destName, operator);
          break;

        case DIV:
        case MOD:
          generateDivMod(op, leftName, rightName);
          break;

        default:
          fail("Cannot do %s on %ss", operator, leftType);
          break;
      }
    } else {
      fail("Cannot do %s on %ss", operator, leftType);
    }
    if (tempReg != null) {
      registers.deallocate(tempReg);
    }
    deallocate(op.left());
    deallocate(op.right());
  }

  private void generateStringCompare(
      Operand left, Operand right, String destName, TokenType operator) {
    boolean pushedRax = condPush(RAX);
    boolean pushedRcx = condPush(RCX);
    boolean pushedRdx = condPush(RDX);
    // TODO: might need to save r8 & r9 too

    emit("; strcmp: %s = %s %s %s", destName, resolve(left), operator, resolve(right));

    if (isInRegister(left, RDX) && isInRegister(right, RCX)) {
      if (operator == TokenType.EQEQ || operator == TokenType.NEQ) {
        emit("; no need to set up RCX, RDX for %s", operator);
      } else {
        // not an equality comparison, need to swap either the operator or the operands.
        emit("xchg RCX, RDX  ; left was rdx, right was rcx, so swap them");
      }
    } else {

      if (isInRegister(left, RCX)) {
        emit("; left already in RCX");
      } else {
        emit("mov RCX, %s  ; Address of left string", resolve(left));
      }

      if (isInRegister(right, RDX)) {
        emit("; right already in RDX");
      } else {
        emit("mov RDX, %s  ; Address of right string", resolve(right));
      }
    }
    emit("sub RSP, 0x20  ; Reserve the shadow space");
    emit("call strcmp");
    emit("add RSP, 0x20  ; Remove shadow space");
    emit("cmp RAX, 0");
    emit("%s %s  ; string %s", BINARY_OPCODE.get(operator), destName, operator);
    condPop(RDX, pushedRdx);
    condPop(RCX, pushedRcx);
    condPop(RAX, pushedRax);
  }

  private void generateStringIndex(String leftName, String rightName, String destName) {
    boolean pushedRcx = condPush(RCX);
    // 1. allocate a new 2-char string
    emit("mov RCX, 2");
    emit("sub RSP, 0x20  ; Reserve the shadow space");
    emit("call malloc  ; malloc(2)");
    emit("add RSP, 0x20  ; Remove shadow space");
    condPop(RCX, pushedRcx);
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
    boolean pushedRcx = condPush(RCX);
    boolean pushedRdx = condPush(RDX);
    emit("mov ECX, %s", leftReg.name32);
    emit("sub RSP, 0x20  ; Reserve the shadow space");
    emit("call malloc  ; malloc(%s)", leftReg.name32);
    registers.deallocate(leftReg);
    // 4. put string into dest
    if (!dest.equals(RAX.name64)) {
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
    emit("add RSP, 0x20  ; Remove shadow space");
    condPop(RDX, pushedRdx);
    condPop(RCX, pushedRcx);
  }

  private void generateDivMod(BinOp op, String leftName, String rightName) {
    // 3. determine dest location
    String destName = resolve(op.destination());
    // 4. set up left in EDX:EAX
    // 5. idiv by right, result in eax
    // 6. mov destName, eax
    boolean pushedRax = condPush(RAX);
    boolean pushedRdx = condPush(RDX);
    registers.reserve(RAX);
    registers.reserve(RDX);
    Register temp = registers.allocate();
    if (!leftName.equals(RAX.name32)) {
      emit("mov EAX, %s  ; numerator", leftName);
    } else {
      emit("; numerator already in EAX");
    }
    emit("mov %s, %s  ; denominator", temp.name32, rightName);

    // sign extend eax to edx
    emit("cdq  ; sign extend eax to edx");
    emit("idiv %s  ; EAX = %s / %s", temp.name32, leftName, rightName);
    registers.deallocate(temp);
    // not required if it's already supposed to be in eax
    if (op.operator() == TokenType.DIV && !destName.equals(RAX.name32)) {
      // eax has dividend
      emit("mov %s, EAX  ; dividend", destName);
    } else {
      emit("; dividend in EAX, where we wanted it to be");
    }
    if (op.operator() == TokenType.MOD && !destName.equals(RDX.name32)) {
      // edx has remainder
      emit("mov %s, EDX  ; remainder", destName);
    } else {
      emit("; remainder in EDX, where we wanted it to be");
    }

    if (!pushedRdx) {
      registers.deallocate(RDX);
    }
    if (!pushedRax) {
      registers.deallocate(RAX);
    }
    if (!destName.equals(RDX.name32)) {
      condPop(RDX, pushedRdx);
    } else {
      // pseudo pop
      emit("add RSP, 8");
    }
    if (!destName.equals(RAX.name32)) {
      condPop(RAX, pushedRax);
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
        emit("not %s  ; bit not", destName);
        break;
      case NOT:
        // boolean not
        // 1. compare to 0
        emit("cmp byte %s, 0  ; unary not", sourceName);
        // 2. setz %s
        emit("setz %s  ; boolean not", destName);
        break;
      case MINUS:
        emit("mov dword %s, %s  ; unary setup", destName, sourceName);
        emit("neg %s  ; unary minus", destName);
        break;
      case LENGTH:
        if (source.type() == VarType.STRING) {
          generateStringLength(sourceName, destName);
        } else {
          // array length
          fail("Cannot generate array %s yet", op);
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
    boolean pushedRcx = condPush(RCX);
    boolean pushedRax = condPush(RAX);
    emit("mov RCX, %s  ; Address of string", sourceName);
    emit("sub RSP, 0x20  ; Reserve the shadow space");
    emit("call strlen  ; strlen(%s)", sourceName);
    emit("add RSP, 0x20  ; Remove shadow space");
    if (destName.equals(RAX.name32)) {
      // pseudo pop; eax already has the length.
      emit("add RSP, 8");
    } else {
      // NOTE: eax not rax, because lengths are always ints (32 bits)
      emit("mov %s, EAX  ; %s = strlen(%s)", destName, destName, sourceName);
      condPop(RAX, pushedRax);
    }

    if (destName.equals(RCX.name32)) {
      // pseudo pop
      emit("add RSP, 8");
    } else {
      condPop(RCX, pushedRcx);
    }
  }

  private void generateChr(String sourceName, String destName) {
    boolean pushedRax = condPush(RAX);
    boolean pushedRcx = condPush(RCX);
    // 1. allocate a new 2-char string
    emit("mov RCX, 2");
    emit("sub RSP, 0x20  ; Reserve the shadow space");
    emit("call malloc  ; malloc(2)");
    emit("add RSP, 0x20  ; Remove shadow space");
    condPop(RCX, pushedRcx);
    // 2. set destName to allocated string
    if (!destName.equals(RAX.name64)) {
      emit("mov %s, RAX  ; copy string location from RAX", destName);
    }

    Register charReg = registers.allocate();
    // 3. get source char as character
    emit("mov DWORD %s, %s  ; get the character int into %s", charReg.name32, sourceName, charReg);
    // 4. write source char in first location
    emit("mov byte [RAX], %s  ; move the character into the first location", charReg.name8);
    // 5. clear second location.
    emit("mov byte [RAX+1], 0  ; clear the 2nd location");
    condPop(RAX, pushedRax);
    registers.deallocate(charReg);
  }

  @Override
  public void visit(ProcEntry op) {
    // Assign locations of each parameter
    int i = 0;
    // I hate this. the param should already know its location, as a ParamLocation
    for (Parameter formal : op.formals()) {
      Location location;
      Register reg = paramRegister(i);
      if (reg != null) {
        location = new RegisterLocation(formal.name(), reg, formal.varType());
        registers.reserve(reg);
      } else {
        fail("Cannot generate more than 4 params yet");
        return;
        // maybe use the vartype to decide how much space to allocate?
        //          location = new StackLocation(formal.name(), -i * 8, formal.varType());
      }
      i++;
      // Is this even ever used?!
      formal.setLocation(location);
    }

    // Save nonvolatile registers:
    emit("; entry to %s", op.name());
    // TODO: if no locals, don't need to muck with rbp, rsp
    emit("push RBP");
    emit("mov RBP, RSP");
    if (op.localBytes() > 0) {
      emit("sub RSP, %d", op.localBytes());
    }
    emit("push RBX");
    emit("push R12");
    emit("push R13");
    emit("push R14");
    emit("push R15");
    emit("push RDI");
    emit("push RSI");
  }

  @Override
  public void visit(Return op) {
    // we can't just "ret" here because there's cleanup we need to do first.
    if (op.returnValueLocation().isPresent()) {
      // transfer from return value to RAX
      Operand returnValue = op.returnValueLocation().get();
      // I hate this. why can't we emit the "mov"?
      visit(new Transfer(new RegisterLocation("RAX", RAX, returnValue.type()), returnValue));
    }
    emit("jmp __exit_of_%s", op.procName());
  }

  @Override
  public void visit(ProcExit op) {
    for (Register reg : PARAM_REGISTERS) {
      if (registers.isAllocated(reg)) {
        registers.deallocate(reg);
      }
    }

    emit0("__exit_of_%s:", op.procName());
    // restore registers
    emit("pop RSI");
    emit("pop RDI");
    emit("pop R15");
    emit("pop R14");
    emit("pop R13");
    emit("pop R12");
    emit("pop RBX");
    emit("leave");
    emit("ret  ; return from procedure");
  }

  @Override
  public void visit(Call op) {
    emit("; set up actuals, mapped to RCX, RDX, etc.");
    Set<Register> pushedRegs = new HashSet<>();
    for (Register reg : PARAM_REGISTERS) {
      if (registers.isAllocated(reg)) {
        emit("push %s", reg.name64);
        pushedRegs.add(reg);
      }
    }

    int index = 0;
    for (Operand actual : op.actuals()) {
      String formalLocation = resolve(op.formals().get(index++));
      String actualLocation = resolve(actual);
      Size size = size(actual.type());
      emit("mov %s %s, %s", size.asmName, formalLocation, actualLocation);
    }
    emit("call __%s", op.procName());
    for (Register reg : PARAM_REGISTERS_REVERSED) {
      if (pushedRegs.contains(reg)) {
        emit("pop %s", reg.name64);
      }
    }
    if (op.destination().isPresent()) {
      Location destination = op.destination().get();
      String destName = resolve(destination);
      String size = size(destination.type()).asmName;
      emit("mov %s %s, %s", size, destName, registerNameSized(RAX, destination.type()));
    }
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
      fail("Cannot generate %s operand %s yet", operand.type(), operand);
      return null;
    }

    Location location = (Location) operand;
    // maybe look up the location in the symbol table?
    if (location.isRegister()) {
      Register reg = ((RegisterLocation) location).register();
      return registerNameSized(reg, location.type());
    }
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
      case PARAM:
        ParamLocation paramLoc = (ParamLocation) location;
        return generateParamLocationName(paramLoc.index(), location.type());
      case LOCAL:
        StackLocation stackLoc = (StackLocation) location;
        return "[RBP - " + stackLoc.offset() + "]";
      default:
        fail("Cannot generate %s operand %s yet", location.storage(), location);
        return null;
    }
  }

  private Register paramRegister(int index) {
    if (index > 3) {
      return null;
    }
    return PARAM_REGISTERS.get(index);
  }

  private String generateParamLocationName(int index, VarType varType) {
    Register reg = paramRegister(index);
    if (reg == null) {
      fail("Cannot generate more than 4 params yet");
      return null;
    }
    return registerNameSized(reg, varType);
  }

  private static Size size(VarType type) {
    if (type == VarType.INT) {
      return Size._32BITS;
    } else if (type == VarType.BOOL) {
      return Size._1BYTE;
    } else if (type == VarType.STRING) {
      return Size._64BITS;
    }
    throw new IllegalStateException("Cannot get type of " + type);
  }

  private static String registerNameSized(Register reg, VarType type) {
    // TODO: figure out how to generalize this.
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

  private void emitComment(Op op) {
    emit0("\n  ; %s", op.toString());
  }

  // Emit at column 0
  private void emit0(String format, Object... values) {
    asm.add(String.format(format, values));
    logger.atFine().logVarargs(format, values);
  }

  private void fail(String format, Object... values) {
    throw new UnsupportedOperationException(String.format(format, values));
  }
}
