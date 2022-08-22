package com.plasstech.lang.d2.codegen;

import static com.plasstech.lang.d2.codegen.IntRegister.RAX;
import static com.plasstech.lang.d2.codegen.IntRegister.RCX;
import static com.plasstech.lang.d2.codegen.IntRegister.RDX;

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
import com.plasstech.lang.d2.type.ProcSymbol;
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

  private final List<String> prelude = new ArrayList<>();
  private final Emitter emitter = new ListEmitter();

  private StringTable stringTable;
  private DoubleTable doubleTable;
  private Resolver resolver;

  private CallGenerator callGenerator;
  private RecordGenerator recordGenerator;
  private StringCodeGenerator stringGenerator;
  private NullPointerCheckGenerator npeCheckGenerator;
  private ArrayCodeGenerator arrayGenerator;
  private InputGenerator inputGenerator;
  private PrintGenerator printGenerator;
  private DoubleGenerator doubleGenerator;

  @Override
  public State execute(State input) {
    stringTable = new StringFinder().execute(input.lastIlCode());
    doubleTable = new DoubleFinder().execute(input.lastIlCode());
    Registers registers = new Registers();
    resolver = new Resolver(registers, stringTable, doubleTable, emitter);
    callGenerator = new CallGenerator(resolver, emitter);
    recordGenerator = new RecordGenerator(resolver, input.symbolTable(), emitter);
    npeCheckGenerator = new NullPointerCheckGenerator(resolver, emitter);
    stringGenerator = new StringCodeGenerator(resolver, emitter);
    arrayGenerator = new ArrayCodeGenerator(resolver, emitter);
    inputGenerator = new InputGenerator(resolver, registers, emitter);
    printGenerator = new PrintGenerator(resolver, emitter);
    doubleGenerator = new DoubleGenerator(resolver, emitter);

    ImmutableList<Op> code = input.lastIlCode();
    String f = "dcode";
    if (input.filename() != null) {
      f = input.filename();
    }

    prelude.add("; To execute:");
    // -Ox = optimize
    prelude.add(String.format("; nasm -fwin64 %s.asm && gcc %s.obj -o %s && ./%s\n", f, f, f, f));

    prelude.add("global main\n");

    SymTab globals = input.symbolTable();
    // emit all string constants
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
    emit("jmp %s", op.label());
  }

  @Override
  public void visit(ArraySet op) {
    arrayGenerator.generateArraySet(op);
  }

  @Override
  public void visit(Transfer op) {
    Operand source = op.source();
    Location destination = op.destination();

    resolver.mov(source, destination);
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
        printGenerator.generate(op);
        break;
      case INPUT:
        inputGenerator.generate(arg);
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
    emit("jne %s", op.destination());
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
          stringGenerator.generateStringIndex(op);
          break;
        case EQEQ:
        case NEQ:
        case GT:
        case GEQ:
        case LT:
        case LEQ:
          stringGenerator.generateStringCompare(op, destName);
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
          tempReg = resolver.allocate(VarType.INT);
          emit("mov BYTE %s, %s ; compare setup", tempReg.name8(), leftName);
          emit("cmp %s, %s", tempReg.name8(), rightName);
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
              rightReg = resolver.allocate(VarType.INT);
              Operand rightOp = new RegisterLocation(op.right().toString(), rightReg, rightType);
              emit(
                  "mov %s, %s  ; saving right to a different register",
                  rightReg.sizeByType(rightType), rightName);
              rightName = resolver.resolve(rightOp);
            }
            Register destReg = null;
            if (resolver.isInRegister(op.destination(), RCX)) {
              destReg = resolver.allocate(VarType.INT);
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
          tempReg = resolver.allocate(VarType.INT);
          emit("mov %s %s, %s ; int compare setup", size, tempReg.name32(), leftName);
          emit("cmp %s, %s", tempReg.name32(), rightName);
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
          if (arrayType.baseType() == VarType.DOUBLE) {
            emit("movq %s, [%s]", destName, fullIndex);
          } else {
            emit("mov %s %s, [%s]", Size.of(arrayType.baseType()).asmType, destName, fullIndex);
          }
          resolver.deallocate(fullIndex);
          break;

        default:
          fail("Cannot do %s on %ss (yet?)", operator, leftType);
          break;
      }
    } else if (leftType.isRecord()) {
      recordGenerator.generate(op);
    } else if (leftType == VarType.DOUBLE) {
      doubleGenerator.generate(op);
    } else {
      fail("Cannot do %s on %ss (yet?)", operator, leftType);
    }

    if (tempReg != null) {
      resolver.deallocate(tempReg);
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
      emit("jne %s", continueLabel);

      emit0("\n  ; division by zero. print error and stop");
      emitter.addData(Messages.DIV_BY_ZERO_ERR);
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
    Register temp = resolver.allocate(VarType.INT);
    if (!leftName.equals(RAX.name32())) { // TODO: don't use .equals
      emit("mov EAX, %s  ; numerator", leftName);
    } else {
      emit("; numerator already in EAX");
    }
    emit("mov %s, %s  ; denominator", temp.name32(), right);

    // sign extend eax to edx
    emit("cdq  ; sign extend eax to edx");
    emit("idiv %s  ; EAX = %s / %s", temp.name32(), leftName, right);
    resolver.deallocate(temp);
    if (op.operator() == TokenType.DIV) {
      // eax has dividend
      if (!destName.equals(RAX.name32())) {
        emit("mov %s, EAX  ; dividend", destName);
      } else {
        // not required if it's already supposed to be in eax
        emit("; dividend in EAX, where we wanted it to be");
      }
    } else if (op.operator() == TokenType.MOD) {
      // edx has remainder
      if (!destName.equals(RDX.name32())) {
        emit("mov %s, EDX  ; remainder", destName);
      } else {
        emit("; remainder in EDX, where we wanted it to be");
      }
    }

    if (!destName.equals(RDX.name32())) {
      registerState.condPop(RDX);
    } else {
      // pseudo pop
      emit("add RSP, 8  ; pseudo pop RDX");
    }
    if (!destName.equals(RAX.name32())) {
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
        if (source.type() == VarType.INT) {
          emit("mov DWORD %s, %s  ; unary setup", destName, sourceName);
          emit("neg %s  ; unary minus", destName);
        } else {
          doubleGenerator.generate(op, sourceName);
        }
        break;
      case LENGTH:
        if (source.type() == VarType.STRING) {
          stringGenerator.generateStringLength(op.position(), destination, source);
        } else if (source.type().isArray()) {
          arrayGenerator.generateArrayLength(destination, source);
        } else {
          throw new D2RuntimeException(
              String.format(
                  "Cannot apply LENGTH function to %s expression; must be ARRAY or STRING",
                  source.type()),
              op.position(),
              "Null pointer");
        }
        break;
      case ASC:
        npeCheckGenerator.generateNullPointerCheck(op.position(), source);
        // move from sourceLoc to temp
        // then from temp to dest
        Register tempReg = resolver.allocate(VarType.INT);
        // Just read one byte
        if (source.isConstant()) {
          emit("mov BYTE %s, %s ; copy one byte / first character", tempReg.name8(), sourceName);
          emit("mov BYTE %s, %s ; store a full int. shrug", destName, tempReg.name32());
        } else {
          // need to do two indirects
          emit("mov %s, %s  ; %s has address of string", tempReg, sourceName, tempReg);
          emit("mov BYTE %s, [%s] ; copy a byte", destName, tempReg);
        }
        emit("and %s, 0x000000ff", destName);
        resolver.deallocate(tempReg);
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
      Register reg = Register.paramRegister(formal.varType(), i);
      if (reg != null) {
        location = new RegisterLocation(formal.name(), reg, formal.varType());
        resolver.reserve(reg);
      } else {
        // TODO: implement this
        // use the vartype to decide how much space to allocate
        // location = new StackLocation(formal.name(), -i * 8, formal.varType());
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
      // this may over-allocate, but /shrug.
      int bytes = 16 * (op.localBytes() / 16 + 1);
      emit("sub RSP, %d", bytes);
    }
    // MUST PUSH XMMs FIRST
    RegisterState rs = new RegisterState(emitter);
    rs.push(Register.NONVOLATILE_REGISTERS);
  }

  @Override
  public void visit(Return op) {
    // we can't just "ret" here because there's cleanup we need to do first.
    if (op.returnValueLocation().isPresent()) {
      // transfer from return value to XMM0/RAX
      Operand returnValue = op.returnValueLocation().get();
      resolver.mov(returnValue, Registers.returnRegister(returnValue.type()));
      resolver.deallocate(returnValue);
    }
    // NOTYPO
    emit("jmp __exit_of_%s", op.procName());
  }

  @Override
  public void visit(ProcExit op) {
    // I hate this
    for (Register reg : IntRegister.values()) {
      if (reg != RAX && resolver.isAllocated(reg)) {
        resolver.deallocate(reg);
      }
    }
    for (Register reg : MmxRegister.values()) {
      if (resolver.isAllocated(reg)) {
        resolver.deallocate(reg);
      }
    }

    emit0("__exit_of_%s:", op.procName());
    // restore registers
    RegisterState rs = new RegisterState(emitter);
    rs.pop(Register.NONVOLATILE_REGISTERS.reverse());
    if (op.localBytes() > 0) {
      // this adjusts rbp, rsp
      emit("leave");
    }
    emit("ret  ; return from procedure");
  }

  @Override
  public void visit(Call op) {
    RegisterState registerState = condPush(Register.VOLATILE_REGISTERS);

    emit("; set up actuals");
    callGenerator.generate(op);
    ProcSymbol procSym = op.procSym();
    if (procSym.isExtern()) {
      String alignedLabel = resolver.nextLabel("aligned");
      String afterLabel = resolver.nextLabel("afterExternCall");
      String externName = procSym.name();
      emitter.addExtern(externName);
      emitter.emit("test rsp, 8");
      emitter.emit("je %s ; not a multiple of 8, it's aligned", alignedLabel);
      emitter.emit("sub RSP, 0x28");
      emitter.emit("call %s", externName);
      emitter.emit("add RSP, 0x28");
      emitter.emit("jmp %s", afterLabel);
      emitter.emitLabel(alignedLabel);
      emitter.emitExternCall(externName);
      emitter.emitLabel(afterLabel);
    } else {
      emit("call %s", procSym.mungedName());
    }
    Register tempReg = null;
    if (op.destination().isPresent()) {
      Location destination = op.destination().get();
      Register returnReg = Registers.returnRegister(destination.type());
      if (resolver.isAllocated(returnReg)) {
        // it will be overwritten. let's stash it in a temp reg
        tempReg = resolver.allocate(destination.type());
        emit("; temporarily putting %s in %s", returnReg, tempReg);
        resolver.mov(destination.type(), returnReg, tempReg);
      }
    }
    registerState.condPop();
    if (op.destination().isPresent()) {
      emit("; get return value");
      Location destination = op.destination().get();
      Register returnReg = Registers.returnRegister(destination.type());
      if (tempReg == null) {
        // Didn't have to stash it, just copy.
        resolver.mov(returnReg, destination);
      } else {
        resolver.mov(tempReg, destination);
        resolver.deallocate(tempReg);
      }
    }
    for (int i = 0; i < op.actuals().size(); ++i) {
      Operand actual = op.actuals().get(i);
      resolver.deallocate(actual);
    }
  }

  /** Conditionally push all allocated registers in the list */
  private RegisterState condPush(ImmutableList<Register> registerList) {
    return RegisterState.condPush(emitter, resolver, registerList);
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
