package com.plasstech.lang.d2.codegen;

import static com.plasstech.lang.d2.codegen.Register.R10;
import static com.plasstech.lang.d2.codegen.Register.R11;
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
import java.util.TreeSet;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.il.AllocateOp;
import com.plasstech.lang.d2.codegen.il.ArrayAlloc;
import com.plasstech.lang.d2.codegen.il.ArraySet;
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
import com.plasstech.lang.d2.common.D2RuntimeException;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.parse.node.ProcedureNode.Parameter;
import com.plasstech.lang.d2.phase.Phase;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.type.ArrayType;
import com.plasstech.lang.d2.type.SymTab;
import com.plasstech.lang.d2.type.Symbol;
import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VarType;

public class NasmCodeGenerator extends DefaultOpcodeVisitor implements Phase {
  private static FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final ImmutableList<Register> PARAM_REGISTERS = ImmutableList.of(RCX, RDX, R8, R9);

  private final List<String> prelude = new ArrayList<>();
  private final Set<String> externs = new HashSet<>();
  private final Set<String> data = new TreeSet<>();
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

  private Emitter emitter = new ListEmitter();

  @Override
  public State execute(State input) {
    stringTable = new StringFinder().execute(input.lastIlCode());

    ImmutableList<Op> code = input.lastIlCode();
    String f = "dcode";
    if (input.filename() != null) {
      f = input.filename();
    }

    prelude.add("; To execute:");
    // -Ox = optimize
    prelude.add(
        String.format("; nasm -fwin64 -Ox %s.asm && gcc %s.obj -o %s && ./%s\n", f, f, f, f));

    prelude.add("global main\n"); // required
    externs.add("extern exit"); // required

    // Probably what we should do is:
    // 1. emit all globals OK
    // 2. emit all string constants OK
    // 3. emit all array constants (?)
    SymTab globals = input.symbolTable();
    for (Map.Entry<String, Symbol> entry : globals.entries().entrySet()) {
      Symbol symbol = entry.getValue();
      if (symbol.storage() == SymbolStorage.GLOBAL) {
        // reserve (& clear) 1 byte for bool, 4 bytes per int, 8 bytes for string
        if (symbol.varType() == VarType.INT) {
          data.add(String.format("  __%s: dd 0", entry.getKey()));
        } else if (symbol.varType() == VarType.BOOL) {
          data.add(String.format("  __%s: db 0", entry.getKey()));
        } else if (symbol.varType() == VarType.STRING || symbol.varType().isArray()) {
          data.add(String.format("  __%s: dq 0", entry.getKey()));
        }
      }
    }
    for (StringEntry entry : stringTable.orderedEntries()) {
      data.add("  " + entry.dataEntry());
    }

    // TODO: convert command-line arguments to ??? and send to __main
    emit0("main:");
    try {
      for (Op opcode : code) {
        emit0("\n  ; %s", opcode.toString());
        opcode.accept(this);
      }
    } catch (D2RuntimeException e) {
      ImmutableList<String> allCode =
          ImmutableList.<String>builder()
              .add("PARTIAL ASSEMBLY\n\n")
              .add("================\n\n")
              .addAll(prelude)
              .addAll(externs)
              .add("\nsection .data")
              .addAll(data)
              .add("\nsection .text")
              .addAll(emitter.all())
              .build();
      input = input.addAsmCode(allCode).addException(e);
      return input;
    }

    ImmutableList<String> allCode =
        ImmutableList.<String>builder()
            .addAll(prelude)
            .addAll(externs)
            .add("\nsection .data")
            .addAll(data)
            .add("\nsection .text")
            .addAll(emitter.all())
            .build();

    return input.addAsmCode(allCode);
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
  public void visit(ArraySet op) {
    Operand indexLoc = op.index();
    ArrayType arrayType = op.arrayType();

    // TODO: check dimension size against index

    // calculate full index: indexName*basetype.size() + 1+4*dimensions+arrayLoc
    Register fullIndex = generateArrayIndex(indexLoc, arrayType, resolve(op.array()));

    Operand sourceLoc = op.source();
    String sourceName = resolve(sourceLoc);
    Register sourceReg = null;
    String baseTypeSize = Size.of(arrayType.baseType()).asmName;
    if (!(isInAnyRegister(sourceLoc) || sourceLoc.isConstant())) {
      // not a constant and not in a registers; put it in a register
      sourceReg = registers.allocate();
      emit("; source (%s) is not in a register; putting it into %s:", sourceLoc, sourceReg);
      String sourceRegisterSized = registerNameSized(sourceReg, arrayType.baseType());
      emit("mov %s %s, %s", baseTypeSize, sourceRegisterSized, sourceName);
      sourceName = sourceRegisterSized;
    }
    emit("mov %s [%s], %s  ; store it!", baseTypeSize, fullIndex, sourceName);
    if (sourceReg != null) {
      emit("; deallocating %s", sourceReg);
      registers.deallocate(sourceReg);
    }
    registers.deallocate(fullIndex);
    emit("; deallocating %s", fullIndex);

    deallocate(sourceLoc);
    deallocate(indexLoc);
  }

  /**
   * Generate code that puts the location of the given index into the given array into the register
   * returned.
   */
  private Register generateArrayIndex(Operand indexLoc, ArrayType arrayType, String arrayLoc) {
    Register fullIndex = registers.allocate();
    emit("; allocated %s for calculations", fullIndex);
    if (indexLoc.isConstant()) {
      // if index is constant, can skip some of this calculation.
      ConstantOperand<Integer> indexConst = (ConstantOperand<Integer>) indexLoc;
      int index = indexConst.value();
      if (index < 0) {
        throw new D2RuntimeException(
            String.format("ARRAY index must be non-negative; was %d", index), null, "Type");
      }

      // index is always a dword/int because I said so.
      emit(
          "mov %s, %d  ; const index; full index=1+dims*4+index*base size",
          fullIndex, 1 + 4 * arrayType.dimensions() + arrayType.baseType().size() * index);
    } else {
      // index is always a dword/int because I said so.
      emit("; calculate index*base size+1+dims*4");
      emit("mov DWORD %s, %s  ; index...", fullIndex.name32, resolve(indexLoc));
      emit("imul %s, %s  ; ...*base size ...", fullIndex, arrayType.baseType().size());
      emit("add %s, %d  ; ... +1+dims*4", fullIndex, 1 + arrayType.dimensions() * 4);
    }
    emit("add %s, %s  ; actual location", fullIndex, arrayLoc);
    return fullIndex;
  }

  @Override
  public void visit(Transfer op) {
    Operand source = op.source();
    Location destination = op.destination();

    if (source.type().isArray()) {
      // for now, all arrays contain constant values.
      fail("Cannot generate transfer from array yet (source type is %s)", source.type());
      return;
    }

    Register fromReg = toRegister(source); // if this is *already* a register
    Register toReg = toRegister(destination); // if this is *already* a register
    String destLoc = resolve(destination);
    String sourceLoc = resolve(source);
    if (source.type() == VarType.STRING) {
      if (toReg != null || fromReg != null) {
        // go right from source to dest
        emit("; short circuit string");
        emit("mov %s, %s", destLoc, sourceLoc);
      } else {
        // move from sourceLoc to temp
        // then from temp to dest
        Register tempReg = registers.allocate();
        String tempName = registerNameSized(tempReg, op.source().type());
        // TODO: this can be short-circuited too if dest is a register
        emit("mov %s, %s", tempName, sourceLoc);
        emit("mov %s, %s", destLoc, tempName);
        registers.deallocate(tempReg);
      }
      deallocate(source);
      return;
    }

    String size = Size.of(op.source().type()).asmName;
    if (source.isConstant() || source.isRegister()) {
      emit("mov %s %s, %s", size, destLoc, sourceLoc);
    } else {
      if (toReg != null || fromReg != null) {
        // go right from source to dest
        emit("; short circuit int");
        emit("mov %s %s, %s", size, destLoc, sourceLoc);
      } else {
        Register tempReg = registers.allocate();
        String tempName = registerNameSized(tempReg, op.source().type());
        emit("mov %s %s, %s", size, tempName, sourceLoc);
        emit("mov %s %s, %s", size, destLoc, tempName);
        registers.deallocate(tempReg);
      }
    }
    deallocate(source);
  }

  @Override
  public void visit(AllocateOp op) {
    fail("Cannot generate AllocateOp yet");
  }

  @Override
  public void visit(ArrayAlloc op) {
    RegisterState registerState = condPush(Register.VOLATILE_REGISTERS);
    Operand numEntriesLoc = op.sizeLocation();
    String numEntriesLocName = resolve(numEntriesLoc);

    // 1. calculate # of bytes to allocate:
    //    size * entrySize +
    //    1 byte (# of dimensions) + 4 * # dimensions
    Register allocSizeBytesRegister = Register.RDX;
    int dimensions = op.arrayType().dimensions();
    int entrySize = op.arrayType().baseType().size();
    if (numEntriesLoc.isConstant()) {
      // if numEntriesLocName is a constant, pre-calculate storage requireds.
      ConstantOperand<Integer> numEntriesOp = (ConstantOperand<Integer>) numEntriesLoc;
      int numEntries = numEntriesOp.value();
      if (numEntries <= 0) {
        throw new D2RuntimeException(
            String.format("ARRAY size must be positive; was %d", numEntries), null, "Type");
      }
      emit(
          "mov %s, %s  ; storage for # dimensions (1), dimension values (%d), actual storage (%d)",
          allocSizeBytesRegister,
          1 + 4 * dimensions + numEntries * entrySize,
          4 * dimensions,
          numEntries * entrySize);
    } else {
      emit("mov %s, %s  ; number of entries", allocSizeBytesRegister.name32, numEntriesLocName);
      if (entrySize > 1) {
        emit("imul %s, %s ; total size of entries", allocSizeBytesRegister.name32, entrySize);
      }
      emit(
          "add %s, %s  ; add storage for # of dimensions, and %d dimension value(s)",
          allocSizeBytesRegister.name32, 1 + 4 * dimensions, dimensions);
    }

    emit("mov RCX, 1  ; # of 'entries' for calloc");
    emit("sub RSP, 0x20  ; Reserve the shadow space");
    externs.add("extern calloc");
    emit("call calloc; calloc(%s)", allocSizeBytesRegister.name32);
    emit("add RSP, 0x20  ; Remove shadow space");
    String dest = resolve(op.destination());
    emit("mov %s, RAX  ; RAX has location of allocated memory", dest);
    registerState.condPop();
    emit("mov BYTE [RAX], %s  ; store # of dimensions", dimensions);

    Register numEntriesReg = null;
    if (!(isInAnyRegister(numEntriesLoc) || numEntriesLoc.isConstant())) {
      // not a constant and not in a registers; put it in a register
      numEntriesReg = registers.allocate();
      emit(
          "; numEntriesLoc (%s) is not in a register; putting it into %s",
          numEntriesLoc, numEntriesReg);
      emit("mov DWORD %s, %s", numEntriesReg.name32, numEntriesLocName);
      numEntriesLocName = numEntriesReg.name32;
    }
    // TODO: iterate over dimensions.
    emit("mov DWORD [RAX+1], %s  ; store size of the first dimension", numEntriesLocName);
    if (numEntriesReg != null) {
      emit("; deallocating numEntriesLoc from %s", numEntriesReg);
      registers.deallocate(numEntriesReg);
    }
    deallocate(op.sizeLocation());
  }

  @Override
  public void visit(SysCall op) {
    Operand arg = op.arg();
    // need to preserve used registers!
    RegisterState registerState = condPush(Register.VOLATILE_REGISTERS);

    switch (op.call()) {
      case MESSAGE:
      case PRINT:
        String argVal = resolve(arg);
        if (arg.type() == VarType.INT) {
          // move with sign extend. intentionally set rdx first, in case the arg is in ecx

          emit("movsx RDX, DWORD %s  ; Second argument is parameter", argVal);
          data.add("  PRINTF_NUMBER_FMT: db \"%d\", 0");
          emit("mov RCX, PRINTF_NUMBER_FMT  ; First argument is address of pattern");
          emit("sub RSP, 0x20  ; Reserve the shadow space");
          externs.add("extern printf");
          emit("call printf    ; printf(message)");
          emit("add RSP, 0x20  ; Remove shadow space");
        } else if (arg.type() == VarType.BOOL) {
          if (argVal.equals("1")) {
            data.add("  CONST_TRUE: db \"true\", 0");
            emit("mov RCX, CONST_TRUE");
          } else if (argVal.equals("0")) {
            data.add("  CONST_FALSE: db \"false\", 0");
            emit("mov RCX, CONST_FALSE");
          } else {
            // translate dynamically from 0/1 to false/true
            // Intentionally do the comp first, in case the arg is in dl or cl
            emit("cmp BYTE %s, 1", argVal);
            data.add("  CONST_FALSE: db \"false\", 0");
            emit("mov RCX, CONST_FALSE");
            data.add("  CONST_TRUE: db \"true\", 0");
            emit("mov RDX, CONST_TRUE");
            // Conditional move
            emit("cmovz RCX, RDX");
          }
          emit("sub RSP, 0x20  ; Reserve the shadow space");
          externs.add("extern printf");
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
            data.add("  EXIT_MSG: db \"ERROR: %s\", 0");
            emit("mov RCX, EXIT_MSG  ; First argument is address of pattern");
          } else if (!isInRegister(arg, RCX)) {
            // arg is not in rcx yet
            emit("mov RCX, %s  ; String to print", argVal);
          }
          emit("sub RSP, 0x20  ; Reserve the shadow space");
          externs.add("extern printf");
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
    registerState.condPop();
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

  private boolean isInAnyRegister(Operand arg) {
    if (arg.isConstant()) {
      return false;
    }
    if (arg.isRegister()) {
      return true;
    }

    Location location = (Location) arg;
    if (aliases.get(location.toString()) != null) {
      return true;
    }
    switch (location.storage()) {
      case TEMP:
        return false; // we would have found its alias already.
      case GLOBAL:
        return false;
      case PARAM:
        ParamLocation paramLoc = (ParamLocation) location;
        return paramRegister(paramLoc.index()) != null;
      case LOCAL:
        return false;
      default:
        fail("Cannot determine if %s is in a reg", location);
        return false;
    }
  }

  private boolean isInRegister(Operand arg, Register register) {
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
        fail("Cannot determine if %s is in a reg", location);
        return false;
    }
  }

  @Override
  public void visit(Dec op) {
    String target = resolve(op.target());
    emit("dec DWORD %s", target);
    deallocate(op.target());
  }

  @Override
  public void visit(Inc op) {
    String target = resolve(op.target());
    emit("inc DWORD %s", target);
    deallocate(op.target());
  }

  @Override
  public void visit(IfOp op) {
    String condName = resolve(op.condition());
    emit("cmp BYTE %s, 0", condName);
    emit("jne __%s", op.destination());
    deallocate(op.condition());
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
          generateStringAdd(op.left(), op.right(), destName);
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
            RegisterState pushedRcx = condPush(ImmutableList.of(RCX));
            // TODO: this is a problem if rightname is CL
            emit("mov CL, %s ; shift prep", rightName);
            emit("%s %s, CL ; shift %s", BINARY_OPCODE.get(operator), destName, operator);
            pushedRcx.condPop();
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
    } else if (leftType.isArray()) {
      switch (operator) {
        case LBRACKET:
          ArrayType arrayType = (ArrayType) leftType;
          generateArrayGet(arrayType, destName, leftName, op.right());
          break;
        default:
          fail("Cannot do %s on %ss yet", operator, leftType);
          break;
      }
    } else {
      fail("No idea how to do %s on %ss", operator, leftType);
    }
    if (tempReg != null) {
      registers.deallocate(tempReg);
    }
    deallocate(op.left());
    deallocate(op.right());
  }

  private void generateArrayGet(
      ArrayType arrayType, String destName, String arrayLoc, Operand indexLoc) {

    // TODO: get dimension size using arrayLoc
    // TODO: check dimension size against index

    // calculate full index: indexName*basetype.size() + 1+4*dimensions+arrayLoc
    Register fullIndex = generateArrayIndex(indexLoc, arrayType, arrayLoc);

    emit("mov %s %s, [%s]", Size.of(arrayType.baseType()).asmName, destName, fullIndex);

    registers.deallocate(fullIndex);
    emit("; deallocating %s", fullIndex);
  }

  private void generateStringCompare(
      Operand left, Operand right, String destName, TokenType operator) {
    RegisterState state = condPush(Register.VOLATILE_REGISTERS);

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
    externs.add("extern strcmp");
    emit("call strcmp");
    emit("add RSP, 0x20  ; Remove shadow space");
    emit("cmp RAX, 0");
    emit("%s %s  ; string %s", BINARY_OPCODE.get(operator), destName, operator);
    state.condPop();
  }

  private void generateStringIndex(String leftName, String rightName, String destName) {
    RegisterState registerState = condPush(Register.VOLATILE_REGISTERS);
    // 1. allocate a new 2-char string
    emit("mov RCX, 2");
    emit("sub RSP, 0x20  ; Reserve the shadow space");
    externs.add("extern malloc");
    emit("call malloc  ; malloc(2)");
    emit("add RSP, 0x20  ; Remove shadow space");
    registerState.condPop();
    // 2. copy the location to the dest
    emit("mov %s, RAX  ; destination from rax", destName);

    Register indexReg = registers.allocate();
    emit("; allocated indexReg to %s", indexReg);
    Register charReg = registers.allocate();
    emit("; allocated charReg to %s", charReg);
    // 3. get the string
    emit("mov %s, %s  ; get the string into %s", charReg, leftName, charReg);
    // 4. get the index
    emit("mov %s, %s  ; put index value into %s", indexReg.name32, rightName, indexReg.name32);
    // 5. get the actual character
    emit("mov %s, [%s + %s]  ; get the character", charReg, charReg, indexReg);
    registers.deallocate(indexReg);
    emit("; deallocated indexReg from %s", indexReg);
    // 6. copy the character to the first location
    emit("mov BYTE [RAX], %s  ; move the character into the first location", charReg.name8);
    registers.deallocate(charReg);
    emit("; deallocated charReg from %s", charReg);
    // 7. clear the 2nd location
    emit("mov BYTE [RAX + 1], 0  ; clear the 2nd location");
  }

  private void generateStringAdd(Operand leftOperand, Operand rightOperand, String dest) {
    // 1. get left length
    Register leftLengthReg = registers.allocate();
    emit0("");
    emit("; Get left length into %s:", leftLengthReg);
    generateStringLength(
        leftOperand, new RegisterLocation("____leftLengthReg", leftLengthReg, VarType.INT));
    // 2. get right length
    Register rightLengthReg = registers.allocate();
    // TODO: if leftLengthReg is volatile, push it first (?!)
    emit0("");
    emit("; Get right length into %s:", rightLengthReg);
    generateStringLength(
        rightOperand, new RegisterLocation("____rightLengthReg", rightLengthReg, VarType.INT));
    emit0("");
    emit("add %s, %s  ; Total new string length", leftLengthReg.name32, rightLengthReg.name32);
    emit("inc %s  ; Plus 1 for end of string", leftLengthReg.name32);
    emit("; deallocating right length %s", rightLengthReg);
    registers.deallocate(rightLengthReg);

    // 3. allocate string of length left+right + 1
    emit0("");
    emit("; Allocate string of length %s", leftLengthReg.name32);

    RegisterState registerState = condPush(Register.VOLATILE_REGISTERS);
    emit("mov ECX, %s", leftLengthReg.name32);
    emit("sub RSP, 0x20  ; Reserve the shadow space");
    externs.add("extern malloc");
    emit("call malloc  ; malloc(%s)", leftLengthReg.name32);
    emit("add RSP, 0x20  ; Remove shadow space");
    emit("; deallocating leftlength %s", leftLengthReg);
    registers.deallocate(leftLengthReg);
    // 4. put string into dest
    registerState.condPop(); // this will pop leftlengthreg but it doesn't matter.
    if (!dest.equals(RAX.name64)) {
      emit("mov %s, RAX  ; destination from rax", dest);
    }

    // 5. strcpy from left to dest
    emit0("");
    registerState = condPush(Register.VOLATILE_REGISTERS);
    String left = resolve(leftOperand);
    emit("; strcpy from %s to %s", left, dest);
    if (left.equals(RCX.name64) && dest.equals(RDX.name64)) {
      // just swap rdx & rcx
      emit("; left wants to be in rcx dest in rdx, just swap");
      emit("xchg RDX, RCX");
    } else if (left.equals(RCX.name64)) {
      emit("; opposite order, so that we don't munge rcx");
      emit("mov RDX, %s", left);
      emit("mov RCX, %s", dest);
    } else {
      emit("mov RCX, %s", dest);
      emit("mov RDX, %s", left);
    }
    emit("sub RSP, 0x20  ; Reserve the shadow space");
    externs.add("extern strcpy");
    emit("call strcpy");
    emit("add RSP, 0x20  ; Remove shadow space");
    registerState.condPop();

    // 6. strcat dest, right
    emit0("");
    registerState = condPush(Register.VOLATILE_REGISTERS);
    String right = resolve(rightOperand);
    emit("; strcat from %s to %s", right, dest);
    if (right.equals(RCX.name64) && dest.equals(RDX.name64)) {
      // just swap rdx & rcx
      emit("; right wants to be in rcx dest in rdx, just swap");
      emit("xchg RDX, RCX");
    } else if (right.equals(RCX.name64)) {
      emit("; opposite order, so that we don't munge rcx");
      emit("mov RDX, %s", right);
      emit("mov RCX, %s", dest);
    } else {
      emit("mov RCX, %s", dest);
      emit("mov RDX, %s", right);
    }
    externs.add("extern strcat");
    emit("sub RSP, 0x20  ; Reserve the shadow space");
    emit("call strcat");
    emit("add RSP, 0x20  ; Remove shadow space");
    registerState.condPop();
  }

  private void generateDivMod(BinOp op, String leftName, String rightName) {
    // 3. determine dest location
    String destName = resolve(op.destination());
    // 4. set up left in EDX:EAX
    // 5. idiv by right, result in eax
    // 6. mov destName, eax
    RegisterState registerState = condPush(ImmutableList.of(RAX, RDX));
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
    if (op.operator() == TokenType.DIV) {
      // eax has dividend
      if (!destName.equals(RAX.name32)) {
        emit("mov %s, EAX  ; dividend", destName);
      } else {
        // not required if it's already supposed to be in eax
        emit("; dividend in EAX, where we wanted it to be");
      }
    }
    if (op.operator() == TokenType.MOD) {
      // edx has remainder
      if (!destName.equals(RDX.name32)) {
        emit("mov %s, EDX  ; remainder", destName);
      } else {
        emit("; remainder in EDX, where we wanted it to be");
      }
    }

    if (!registerState.pushed(RDX)) {
      registers.deallocate(RDX);
    }
    if (!registerState.pushed(RAX)) {
      registers.deallocate(RAX);
    }
    if (!destName.equals(RDX.name32)) {
      registerState.condPop(RDX);
    } else {
      // pseudo pop
      emit("add RSP, 8  ; pseudo pop RDX");
    }
    if (!destName.equals(RAX.name32)) {
      registerState.condPop(RAX);
    } else {
      // pseudo pop
      emit("add RSP, 8  ; pseudo pop RAX");
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
        emit("mov DWORD %s, %s  ; unary setup", destName, sourceName);
        emit("not %s  ; bit not", destName);
        break;
      case NOT:
        // boolean not
        // 1. compare to 0
        emit("cmp BYTE %s, 0  ; unary not", sourceName);
        // 2. setz %s
        emit("setz %s  ; boolean not", destName);
        break;
      case MINUS:
        emit("mov DWORD %s, %s  ; unary setup", destName, sourceName);
        emit("neg %s  ; unary minus", destName);
        break;
      case LENGTH:
        if (source.type() == VarType.STRING) {
          generateStringLength(source, destination);
        } else if (source.type().isArray()) {
          generateArrayLength(source, destination);
        } else {
          fail("Cannot generate length of %s", source.type());
        }
        break;
      case ASC:
        // move from sourceLoc to temp
        // then from temp to dest
        Register tempReg = registers.allocate();
        // Just read one byte
        if (source.isConstant()) {
          emit("mov BYTE %s, %s ; copy one byte / first character", tempReg.name8, sourceName);
          emit("mov BYTE %s, %s ; store a full int. shrug", destName, tempReg.name32);
        } else {
          // need to do two indirects
          emit("mov %s, %s  ; %s has address of string", tempReg, sourceName, tempReg);
          emit("mov BYTE %s, [%s] ; copy a byte", destName, tempReg);
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

  private void generateArrayLength(Operand source, Location destination) {
    String sourceName = resolve(source);
    String destName = resolve(destination);
    if (isInAnyRegister(source)) {
      emit("mov %s, [%s + 1]  ; get length from first dimension", destName, sourceName);
    } else {
      // if source is not a register we have to allocate a register first
      Register tempReg = registers.allocate();
      emit("mov %s, %s  ; get array location into reg", tempReg, sourceName);
      emit("mov %s, [%s + 1]  ; get length from first dimension", destName, tempReg);
      registers.deallocate(tempReg);
    }
  }

  private void generateStringLength(Operand source, Location destination) {
    // We're doing something special with RAX & RCX
    RegisterState raxRcxState = condPush(ImmutableList.of(RAX, RCX));
    RegisterState registerState = condPush(ImmutableList.of(RDX, R8, R9, R10, R11));
    String sourceName = resolve(source);
    String destinationName = resolve(destination);
    if (isInRegister(source, RCX)) {
      emit("; RCX already has address of string");
    } else {
      emit("mov RCX, %s  ; Address of string", sourceName);
    }
    emit("sub RSP, 0x20  ; Reserve the shadow space");
    externs.add("extern strlen");
    emit("call strlen    ; strlen(%s)", sourceName);
    emit("add RSP, 0x20  ; Remove shadow space");
    registerState.condPop();

    if (isInRegister(destination, RCX)) {
      // pseudo pop
      emit("; pseudo-pop; destination was already %s", destination);
      if (raxRcxState.pushed(RCX)) {
        emit("add RSP, 8");
      }
    } else {
      raxRcxState.condPop(RCX);
    }
    if (isInRegister(destination, RAX)) {
      // pseudo pop; eax already has the length.
      emit("; pseudo-pop; destination was already %s", destination);
      if (raxRcxState.pushed(RAX)) {
        emit("add RSP, 8");
      }
    } else {
      // NOTE: eax not rax, because lengths are always ints (32 bits)
      emit("mov %s, EAX  ; %s = strlen(%s)", destinationName, destinationName, sourceName);
      raxRcxState.condPop(RAX);
    }
  }

  private void generateChr(String sourceName, String destName) {
    RegisterState raxState = condPush(ImmutableList.of(RAX));
    RegisterState registerState = condPush(Register.VOLATILE_REGISTERS);

    // 1. allocate a new 2-char string
    emit("mov RCX, 2");
    emit("sub RSP, 0x20  ; Reserve the shadow space");
    externs.add("extern malloc");
    emit("call malloc  ; malloc(2)");
    emit("add RSP, 0x20  ; Remove shadow space");
    registerState.condPop();
    // 2. set destName to allocated string
    if (!destName.equals(RAX.name64)) {
      emit("mov %s, RAX  ; copy string location from RAX", destName);
    }

    Register charReg = registers.allocate();
    // 3. get source char as character
    emit("mov DWORD %s, %s  ; get the character int into %s", charReg.name32, sourceName, charReg);
    // 4. write source char in first location
    emit("mov BYTE [RAX], %s  ; move the character into the first location", charReg.name8);
    // 5. clear second location.
    emit("mov BYTE [RAX+1], 0  ; clear the 2nd location");
    raxState.condPop();
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
        // use the vartype to decide how much space to allocate
        // location = new StackLocation(formal.name(), -i * 8, formal.varType());
        // TODO: implement this
        fail("Cannot generate more than 4 params yet");
        return;
      }
      i++;
      // Is this even ever used?!
      formal.setLocation(location);
    }

    // Save nonvolatile registers:
    emit("; entry to %s", op.name());
    // TODO: if no locals, don't need to muck with rbp, rsp.
    // NOTE: procend doesn't know about localbytes so we can't remove this completely.
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
    for (Register reg : Register.VOLATILE_REGISTERS) {
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
    RegisterState registerState = condPush(Register.VOLATILE_REGISTERS);

    int index = 0;

    for (Operand actual : op.actuals()) {
      String formalLocation = resolve(op.formals().get(index++));
      String actualLocation = resolve(actual);
      Size size = Size.of(actual.type());
      emit("mov %s %s, %s", size, formalLocation, actualLocation);
      // is this right?!
      deallocate(actual);
    }
    emit("call __%s", op.procName());
    registerState.condPop();
    if (op.destination().isPresent()) {
      Location destination = op.destination().get();
      String destName = resolve(destination);
      Size size = Size.of(destination.type());
      emit("mov %s %s, %s", size, destName, registerNameSized(RAX, destination.type()));
    }
  }

  /** Conditionally push all allocated registers in the list */
  private RegisterState condPush(ImmutableList<Register> registerList) {
    return RegisterState.condPush(emitter, registers, registerList);
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

      fail("Cannot generate %s operand %s yet", operand.type().name(), operand);
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
        // TODO: deal with out-of-registers
        reg = registers.allocate();
        aliases.put(location.name(), reg);
        emit("; Allocating %s to %s", location, reg);
        return registerNameSized(reg, location.type());
      case GLOBAL:
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
      // TODO: implement > 4 params.
      fail("Cannot generate more than 4 params yet");
      return null;
    }
    return registerNameSized(reg, varType);
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

  // Emit at column 0
  private void emit0(String format, Object... values) {
    emitter.emit(format, values);
    logger.atFine().logVarargs(format, values);
  }

  private static void fail(String format, Object... values) {
    throw new D2RuntimeException("UnsupportedOperation", null, String.format(format, values));
  }

  private enum Size {
    _1BYTE("BYTE"),
    _32BITS("DWORD"),
    _64BITS("QWORD");

    public final String asmName;

    Size(String asmName) {
      this.asmName = asmName;
    }

    @Override
    public String toString() {
      return asmName;
    }

    static Size of(VarType type) {
      if (type == VarType.INT) {
        return Size._32BITS;
      } else if (type == VarType.BOOL) {
        return Size._1BYTE;
      } else if (type == VarType.STRING) {
        return Size._64BITS;
      }
      throw new D2RuntimeException("IllegalState", null, "Cannot get type of " + type);
    }
  }
}
