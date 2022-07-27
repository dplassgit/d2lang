package com.plasstech.lang.d2.codegen;

import static com.plasstech.lang.d2.codegen.Register.RAX;
import static com.plasstech.lang.d2.codegen.Register.RCX;
import static com.plasstech.lang.d2.codegen.Register.RDX;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.escape.Escaper;
import com.google.common.net.PercentEscaper;
import com.plasstech.lang.d2.codegen.il.AllocateOp;
import com.plasstech.lang.d2.codegen.il.ArrayAlloc;
import com.plasstech.lang.d2.codegen.il.ArraySet;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Call;
import com.plasstech.lang.d2.codegen.il.Dec;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.FieldSetOp;
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
  private static final Escaper ESCAPER =
      new PercentEscaper("`-=[];',./~!@#$%^&*()_+{}|:\"<>?\\ ", false);

  private static final Map<TokenType, String> BINARY_OPCODE =
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

  private static final String DIV_BY_ZERO_ERR =
      "DIV_BY_ZERO_ERR: db \"Arithmentic error at line %d: Division by 0\", 10, 0";

  private static final String EXIT_MSG = "EXIT_MSG: db \"ERROR: %s\", 0";
  private static final String PRINTF_NUMBER_FMT = "PRINTF_NUMBER_FMT: db \"%d\", 0";
  private static final String CONST_FALSE = "CONST_FALSE: db \"false\", 0";
  private static final String CONST_TRUE = "CONST_TRUE: db \"true\", 0";
  private static final String CONST_NULL = "CONST_NULL: db \"null\", 0";

  private final List<String> prelude = new ArrayList<>();
  private final Registers registers = new Registers();
  private final Emitter emitter = new ListEmitter();

  private StringTable stringTable;
  private Resolver resolver;
  private CallGenerator callGenerator;
  private RecordGenerator recordGenerator;
  private StringCodeGenerator stringGenerator;
  private NullPointerCheckGenerator npeCheckGenerator;
  private ArrayCodeGenerator arrayGenerator;

  private DoubleTable doubleTable;

  @Override
  public State execute(State input) {
    stringTable = new StringFinder().execute(input.lastIlCode());
    doubleTable = new DoubleFinder().execute(input.lastIlCode());
    resolver = new Resolver(registers, stringTable, doubleTable, emitter);
    callGenerator = new CallGenerator(resolver, emitter);
    recordGenerator = new RecordGenerator(resolver, input.symbolTable(), emitter);
    npeCheckGenerator = new NullPointerCheckGenerator(resolver, emitter);
    stringGenerator = new StringCodeGenerator(resolver, emitter);
    arrayGenerator = new ArrayCodeGenerator(resolver, emitter);

    ImmutableList<Op> code = input.lastIlCode();
    String f = "dcode";
    if (input.filename() != null) {
      f = input.filename();
    }

    prelude.add("; To execute:");
    // -Ox = optimize
    prelude.add(
        String.format("; nasm -fwin64 -Ox %s.asm && gcc %s.obj -o %s && ./%s\n", f, f, f, f));

    prelude.add("global main\n");

    // 1. emit all globals
    // 2. emit all string constants
    SymTab globals = input.symbolTable();
    for (Map.Entry<String, Symbol> entry : globals.variables().entrySet()) {
      Symbol symbol = entry.getValue();
      if (symbol.storage() == SymbolStorage.GLOBAL) {
        // reserve (& clear) 1 byte for bool, 4 bytes per int, 8 bytes for string
        Size size = Size.of(symbol.varType());
        emitter.addData(String.format("_%s: %s 0", entry.getKey(), size.dataSizeName));
      }
    }
    for (ConstEntry<String> entry : stringTable.entries()) {
      emitter.addData(entry.dataEntry());
    }
    for (ConstEntry<Double> entry : doubleTable.entries()) {
      emitter.addData(entry.dataEntry());
    }

    // TODO: convert command-line arguments to ??? and send to _main
    emit0("main:");
    try {
      for (Op opcode : code) {
        // need to escape this!
        String opcodeString = opcode.toString();
        String escaped = ESCAPER.escape(opcodeString);
        emit0("\n  ; START %s", escaped);
        opcode.accept(this);
        emit("; END %s", escaped);
      }
    } catch (D2RuntimeException e) {
      ImmutableList<String> allCode =
          ImmutableList.<String>builder()
              .add("PARTIAL ASSEMBLY\n\n")
              .add("================\n\n")
              .addAll(prelude)
              .addAll(emitter.externs().stream().map(s -> "extern " + s).iterator())
              .add("\nsection .data")
              .addAll(emitter.data().stream().map(s -> "  " + s).iterator())
              .add("\nsection .text")
              .addAll(emitter.all())
              .build();
      input = input.addAsmCode(allCode).addException(e);
      return input;
    }

    ImmutableList<String> allCode =
        ImmutableList.<String>builder()
            .addAll(prelude)
            .addAll(emitter.externs().stream().map(s -> "extern " + s).iterator())
            .add("\nsection .data")
            .addAll(emitter.data().stream().map(s -> "  " + s).iterator())
            .add("\nsection .text")
            .addAll(emitter.all())
            .build();

    return input.addAsmCode(allCode);
  }

  @Override
  public void visit(Label op) {
    emitter.emitLabel(op.label());
  }

  @Override
  public void visit(Stop op) {
    emitter.emitExit(op.exitCode());
  }

  @Override
  public void visit(Goto op) {
    emit("jmp _%s", op.label());
  }

  @Override
  public void visit(ArraySet op) {
    arrayGenerator.generateArraySet(op);
  }

  @Override
  public void visit(Transfer op) {
    Operand source = op.source();
    Location destination = op.destination();

    Register fromReg = resolver.toRegister(source); // if this is *already* a register
    Register toReg = resolver.toRegister(destination); // if this is *already* a register
    String destLoc = resolver.resolve(destination);
    String sourceLoc = resolver.resolve(source);
    if (source.type() == VarType.STRING || source.type().isArray()) {
      if (toReg != null || fromReg != null) {
        // go right from source to dest
        emit("mov %s, %s", destLoc, sourceLoc);
      } else {
        // move from sourceLoc to temp
        // then from temp to dest
        Register tempReg = registers.allocate();
        String tempName = tempReg.sizeByType(op.source().type());
        // TODO: this can be short-circuited too if dest is a register
        emit("mov %s, %s", tempName, sourceLoc);
        emit("mov %s, %s", destLoc, tempName);
        registers.deallocate(tempReg);
      }
      resolver.deallocate(source);
      return;
    }

    String size = Size.of(op.source().type()).asmType;
    if (source.isConstant() || source.isRegister()) {
      emit("mov %s %s, %s", size, destLoc, sourceLoc);
    } else {
      if (toReg != null || fromReg != null) {
        // go right from source to dest
        emit("mov %s %s, %s", size, destLoc, sourceLoc);
      } else {
        Register tempReg = registers.allocate();
        String tempName = tempReg.sizeByType(op.source().type());
        emit("mov %s %s, %s", size, tempName, sourceLoc);
        emit("mov %s %s, %s", size, destLoc, tempName);
        registers.deallocate(tempReg);
      }
    }
    resolver.deallocate(source);
  }

  @Override
  public void visit(AllocateOp op) {
    recordGenerator.generate(op);
  }

  @Override
  public void visit(FieldSetOp op) {
    recordGenerator.generate(op);
  }

  @Override
  public void visit(ArrayAlloc op) {
    arrayGenerator.generate(op);
  }

  @Override
  public void visit(SysCall op) {
    Operand arg = op.arg();
    // need to preserve used registers!
    RegisterState registerState = condPush(Register.VOLATILE_REGISTERS);

    switch (op.call()) {
      case MESSAGE:
      case PRINT:
        String argVal = resolver.resolve(arg);
        if (arg.type() == VarType.INT) {
          // move with sign extend. intentionally set rdx first, in case the arg is in ecx
          emit("movsx RDX, DWORD %s  ; Second argument is parameter", argVal);
          emitter.addData(PRINTF_NUMBER_FMT);
          emit("mov RCX, PRINTF_NUMBER_FMT  ; First argument is address of pattern");
          emitter.emitExternCall("printf");
        } else if (arg.type() == VarType.BOOL) {
          if (argVal.equals("1")) {
            emitter.addData(CONST_TRUE);
            emit("mov RCX, CONST_TRUE");
          } else if (argVal.equals("0")) {
            emitter.addData(CONST_FALSE);
            emit("mov RCX, CONST_FALSE");
          } else {
            // translate dynamically from 0/1 to false/true
            // Intentionally do the comp first, in case the arg is in dl or cl
            emit("cmp BYTE %s, 1", argVal);
            emitter.addData(CONST_FALSE);
            emit("mov RCX, CONST_FALSE");
            emitter.addData(CONST_TRUE);
            emit("mov RDX, CONST_TRUE");
            // Conditional move
            emit("cmovz RCX, RDX");
          }
          emitter.emitExternCall("printf");
        } else if (arg.type() == VarType.STRING) {
          // String
          if (op.call() == SysCall.Call.MESSAGE) {
            // Intentionally set rdx first in case the arg is in rcx
            if (!resolver.isInRegister(arg, RDX)) {
              // arg is not in rdx yet
              emit("mov RDX, %s  ; Second argument is parameter/string to print", argVal);
            }
            emitter.addData(EXIT_MSG);
            emit("mov RCX, EXIT_MSG  ; First argument is address of pattern");
          } else if (!resolver.isInRegister(arg, RCX)) {
            // arg is not in rcx yet
            emit("mov RCX, %s  ; String to print", argVal);
          }
          if (!arg.isConstant()) {
            // if null, print null
            emit("cmp QWORD RCX, 0");
            String notNullLabel = resolver.nextLabel("not_null");
            emit("jne _%s", notNullLabel);
            emitter.addData(CONST_NULL);
            emit("mov RCX, CONST_NULL  ; constant 'null'");
            emit0("_%s:", notNullLabel);
          }
          emitter.emitExternCall("printf");
        } else if (arg.type() == VarType.NULL) {
          emitter.addData(CONST_NULL);
          emit("mov RCX, CONST_NULL  ; constant 'null'");
          emitter.emitExternCall("printf");
        } else {
          fail("Cannot print %s yet", arg);
        }
        break;
      case INPUT:
        InputGenerator generator = new InputGenerator(resolver, registers, emitter);
        generator.generate(arg);
        break;
      default:
        fail("Cannot generate %s yet", op);
        break;
    }
    registerState.condPop();
    resolver.deallocate(arg);
  }

  @Override
  public void visit(Dec op) {
    String target = resolver.resolve(op.target());
    emit("dec DWORD %s", target);
    resolver.deallocate(op.target());
  }

  @Override
  public void visit(Inc op) {
    String target = resolver.resolve(op.target());
    emit("inc DWORD %s", target);
    resolver.deallocate(op.target());
  }

  @Override
  public void visit(IfOp op) {
    String condName = resolver.resolve(op.condition());
    emit("cmp BYTE %s, 0", condName);
    emit("jne _%s", op.destination());
    resolver.deallocate(op.condition());
  }

  @Override
  public void visit(BinOp op) {
    // 1. get left
    String leftName = resolver.resolve(op.left());
    VarType leftType = op.left().type();
    // 2. get right
    String rightName = resolver.resolve(op.right());

    // 3. determine dest location
    String destName = resolver.resolve(op.destination());

    Register tempReg = null;

    // 5. [op] dest, right
    TokenType operator = op.operator();
    if (leftType == VarType.STRING) {
      switch (operator) {
        case PLUS:
          stringGenerator.generateStringAdd(op, destName, op.left(), op.right());
          break;
        case LBRACKET:
          stringGenerator.generateStringIndex(op, op.destination(), op.left(), op.right());
          break;
        case EQEQ:
        case NEQ:
        case GT:
        case GEQ:
        case LT:
        case LEQ:
          stringGenerator.generateStringCompare(destName, op.left(), op.right(), operator);
          break;

        default:
          fail("Cannot do %s on %ss (yet?)", operator, leftType);
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
          emit("cmp %s, %s", tempReg.name8, rightName);
          emit("%s %s ; bool compare %s", BINARY_OPCODE.get(operator), destName, operator);
          break;

        default:
          fail("Cannot do %s on %ss (yet?)", operator, leftType);
          break;
      }
    } else if (leftType == VarType.INT) {
      String size = Size.of(leftType).asmType;
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
          // mov dest, left, then mov cl, amount to shirt, then shift.
          if (op.right().isConstant()) {
            // easy. left << right or left >> right
            // TODO this will fail if dest and left are in memory
            emit("mov %s %s, %s ; shift setup", size, destName, leftName);
            emit("%s %s, %s  ; %s", BINARY_OPCODE.get(operator), destName, rightName, operator);
          } else {

            Register rightReg = null;
            VarType rightType = op.right().type();
            if (resolver.isInRegister(op.right(), RCX)) {
              rightReg = resolver.allocate();
              Operand rightOp = new RegisterLocation(op.right().toString(), rightReg, rightType);
              emit(
                  "mov %s, %s  ; saving right to a different register",
                  rightReg.sizeByType(rightType), rightName);
              rightName = resolver.resolve(rightOp);
            }
            Register destReg = null;
            if (resolver.isInRegister(op.destination(), RCX)) {
              destReg = resolver.allocate();
              Location destOp =
                  new RegisterLocation(op.destination().name(), destReg, op.destination().type());
              emit("mov %s, %s  ; saving dest to a different register", destReg, destName);
              destName = resolver.resolve(destOp);
            }

            RegisterState registerState = null;
            if (destReg == null) {
              // it wasn't in rcx, so we have to push now
              registerState = condPush(ImmutableList.of(RCX));
            }
            emit("mov %s %s, %s ; shift setup", size, destName, leftName);
            emit("mov %s, %s ; shift prep", RCX.sizeByType(rightType), rightName);
            emit("%s %s, CL ; shift %s", BINARY_OPCODE.get(operator), destName, operator);
            if (rightReg != null) {
              resolver.deallocate(rightReg);
            }
            if (destReg != null) {
              // destReg was rcx, copy it out now.
              emit("mov ECX, %s  ; copy to dest", destName);
              resolver.deallocate(destReg);
            } else {
              // it wasn't in RCX, we already put it in the right place
              registerState.condPop();
            }
          }
          break;

        case EQEQ:
        case NEQ:
        case GT:
        case GEQ:
        case LT:
        case LEQ:
          tempReg = registers.allocate();
          emit("mov %s %s, %s ; int compare setup", size, tempReg.name32, leftName);
          emit("cmp %s, %s", tempReg.name32, rightName);
          emit("%s %s  ; int compare %s", BINARY_OPCODE.get(operator), destName, operator);
          break;

        case DIV:
        case MOD:
          generateDivMod(op, leftName);
          break;

        default:
          fail("Cannot do %s on %ss (yet?)", operator, leftType);
          break;
      }
    } else if (leftType.isArray()) {
      switch (operator) {
        case LBRACKET:
          ArrayType arrayType = (ArrayType) leftType;
          Register fullIndex =
              arrayGenerator.generateArrayIndex(
                  op.right(), arrayType, leftName, false, op.position());
          emit("mov %s %s, [%s]", Size.of(arrayType.baseType()).asmType, destName, fullIndex);
          registers.deallocate(fullIndex);
          emit("; deallocating %s", fullIndex);
          break;

        default:
          fail("Cannot do %s on %ss (yet?)", operator, leftType);
          break;
      }
    } else if (leftType.isRecord()) {
      recordGenerator.generate(op);
    } else {
      fail("Cannot do %s on %ss (yet?)", operator, leftType);
    }

    if (tempReg != null) {
      registers.deallocate(tempReg);
    }
    if (!op.left().type().isArray()) {
      // don't deallocate yet so that array literal assignments work.
      resolver.deallocate(op.left());
    }
    resolver.deallocate(op.right());
  }

  private void generateDivMod(BinOp op, String leftName) {
    Operand rightOperand = op.right();
    String right = resolver.resolve(rightOperand);
    if (rightOperand.isConstant()) {
      ConstantOperand<Integer> rightConstOperand = (ConstantOperand<Integer>) rightOperand;
      int rightValue = rightConstOperand.value();
      if (rightValue == 0) {
        throw new D2RuntimeException("Division by 0", op.position(), "Arithmetic");
      }
    } else {
      emit("cmp DWORD %s, 0  ; detect division by 0", right);
      String continueLabel = resolver.nextLabel("not_div_by_zero");
      emit("jne _%s", continueLabel);

      emit0("\n  ; division by zero. print error and stop");
      emitter.addData(DIV_BY_ZERO_ERR);
      emit("mov EDX, %d  ; line number", op.position().line());
      emit("mov RCX, DIV_BY_ZERO_ERR");
      emitter.emitExternCall("printf");
      emitter.emitExit(-1);

      emitter.emitLabel(continueLabel);
    }

    // 3. determine dest location
    String destName = resolver.resolve(op.destination());
    // 4. set up left in EDX:EAX
    // 5. idiv by right, result in eax
    // 6. mov destName, eax
    RegisterState registerState = condPush(ImmutableList.of(RAX, RDX));
    Register temp = registers.allocate();
    if (!leftName.equals(RAX.name32)) { // TODO: don't use .equals
      emit("mov EAX, %s  ; numerator", leftName);
    } else {
      emit("; numerator already in EAX");
    }
    emit("mov %s, %s  ; denominator", temp.name32, right);

    // sign extend eax to edx
    emit("cdq  ; sign extend eax to edx");
    emit("idiv %s  ; EAX = %s / %s", temp.name32, leftName, right);
    registers.deallocate(temp);
    if (op.operator() == TokenType.DIV) {
      // eax has dividend
      if (!destName.equals(RAX.name32)) {
        emit("mov %s, EAX  ; dividend", destName);
      } else {
        // not required if it's already supposed to be in eax
        emit("; dividend in EAX, where we wanted it to be");
      }
    } else if (op.operator() == TokenType.MOD) {
      // edx has remainder
      if (!destName.equals(RDX.name32)) {
        emit("mov %s, EDX  ; remainder", destName);
      } else {
        emit("; remainder in EDX, where we wanted it to be");
      }
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
    resolver.deallocate(op.left());
    resolver.deallocate(op.right());
  }

  @Override
  public void visit(UnaryOp op) {
    // 1. get source location name
    Operand source = op.operand();
    String sourceName = resolver.resolve(source);
    // 2. apply op
    // 3. store in destination
    Location destination = op.destination();
    String destName = resolver.resolve(destination);
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
          stringGenerator.generateStringLength(destination, source);
        } else if (source.type().isArray()) {
          arrayGenerator.generateArrayLength(destination, source);
        } else {
          fail("Cannot generate length of %s", source.type());
        }
        break;
      case ASC:
        npeCheckGenerator.generateNullPointerCheck(op, source);
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
        stringGenerator.generateChr(sourceName, destName);
        break;
      default:
        fail("Cannot generate %s yet", op);
        break;
    }
    // this affects print arrays.
    if (!op.operand().type().isArray()) {
      resolver.deallocate(op.operand());
    }
  }

  @Override
  public void visit(ProcEntry op) {
    // Assign locations of each parameter
    int i = 0;
    // I hate this. the param should already know its location, as a ParamLocation
    for (Parameter formal : op.formals()) {
      Location location;
      Register reg = Register.paramRegister(i);
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
    if (op.localBytes() > 0) {
      emit("push RBP");
      emit("mov RBP, RSP");
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
      String returnValueName = resolver.resolve(returnValue);
      emit("mov %s, %s", RAX.sizeByType(returnValue.type()), returnValueName);
      resolver.deallocate(returnValue);
    }
    emit("jmp __exit_of_%s", op.procName());
  }

  @Override
  public void visit(ProcExit op) {
    for (Register reg : Register.values()) {
      if (reg != RAX && registers.isAllocated(reg)) {
        emit("; deallocating %s", reg);
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
    if (op.localBytes() > 0) {
      // this adjusts rbp, rsp
      emit("leave");
    }
    emit("ret  ; return from procedure");
  }

  @Override
  public void visit(Call op) {
    emit("; set up actuals, mapped to RCX, RDX, etc.");
    RegisterState registerState = condPush(Register.VOLATILE_REGISTERS);

    /**
     * We ONLY have to do gymnastics when a param register needs to be copied to a LATER param
     * register, e.g., RCX needs to be in RDX or RDX needs to be in R9
     */
    callGenerator.generate(op);
    emit0("\n  call _%s\n", op.procName());
    registerState.condPop();
    if (op.destination().isPresent()) {
      Location destination = op.destination().get();
      String destName = resolver.resolve(destination);
      Size size = Size.of(destination.type());
      emit("mov %s %s, %s", size, destName, RAX.sizeByType(destination.type()));
    }
  }

  /** Conditionally push all allocated registers in the list */
  private RegisterState condPush(ImmutableList<Register> registerList) {
    return RegisterState.condPush(emitter, registers, registerList);
  }

  private void emit(String format, Object... values) {
    emitter.emit(format, values);
  }

  private void emit0(String format, Object... values) {
    emitter.emit0(format, values);
  }

  private void fail(String format, Object... values) {
    emitter.fail(format, values);
  }
}
