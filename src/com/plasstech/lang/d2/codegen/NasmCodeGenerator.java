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
import com.plasstech.lang.d2.codegen.Resolver.ResolvedOperand;
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
import com.plasstech.lang.d2.phase.Phase;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.type.ParamSymbol;
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
  private final Registers registers;
  private final DelegatingEmitter emitter;

  public NasmCodeGenerator() {
    this(new DelegatingEmitter(new ListEmitter()), new Registers());
  }

  NasmCodeGenerator(Emitter emitter, Registers registers) {
    this.emitter = new DelegatingEmitter(emitter);
    this.registers = registers;
  }

  private StringTable stringTable;
  private DoubleTable doubleTable;
  private Resolver resolver;

  private CallCodeGenerator callGenerator;
  private RecordCodeGenerator recordGenerator;
  private StringCodeGenerator stringGenerator;
  private NullPointerCheckGenerator npeCheckGenerator;
  private ArrayCodeGenerator arrayGenerator;
  private InputCodeGenerator inputGenerator;
  private PrintCodeGenerator printGenerator;
  private DoubleCodeGenerator doubleGenerator;
  private ArgsCodeGenerator argsGenerator;

  @Override
  public State execute(State input) {
    stringTable = new StringFinder().execute(input.lastIlCode());
    doubleTable = new DoubleFinder().execute(input.lastIlCode());
    resolver = new Resolver(registers, stringTable, doubleTable, emitter);
    callGenerator = new CallCodeGenerator(resolver, emitter);
    recordGenerator = new RecordCodeGenerator(resolver, input.symbolTable(), emitter);
    npeCheckGenerator = new NullPointerCheckGenerator(resolver, emitter);
    stringGenerator = new StringCodeGenerator(resolver, emitter);
    arrayGenerator = new ArrayCodeGenerator(resolver, emitter);
    inputGenerator = new InputCodeGenerator(resolver, registers, emitter);
    printGenerator = new PrintCodeGenerator(resolver, emitter);
    doubleGenerator = new DoubleCodeGenerator(resolver, emitter);
    argsGenerator = new ArgsCodeGenerator(emitter, input.symbolTable());

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

    emit0("main:");
    // Convert command-line arguments to a D-style array of strings
    argsGenerator.generate();

    try {
      for (Op opcode : code) {
        // need to escape this!
        String opcodeString = opcode.toString();
        String escaped = ESCAPER.escape(opcodeString);
        emit0("");
        if (opcode.position() != null) {
          emit("; SOURCE LINE %d: %s", opcode.position().line(), escaped);
        } else {
          emit("; SOURCE: %s", escaped);
        }
        opcode.accept(this);
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
    arrayGenerator.visit(op);
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
    recordGenerator.visit(op);
  }

  @Override
  public void visit(FieldSetOp op) {
    recordGenerator.visit(op);
  }

  @Override
  public void visit(ArrayAlloc op) {
    arrayGenerator.visit(op);
  }

  @Override
  public void visit(SysCall op) {
    switch (op.call()) {
      case MESSAGE:
      case PRINT:
        printGenerator.visit(op);
        break;
      case INPUT:
        inputGenerator.visit(op);
        break;
      default:
        fail("Cannot generate %s yet", op);
        break;
    }
  }

  @Override
  public void visit(Dec op) {
    String target = resolver.resolve(op.target());
    String size = Size.of(op.target().type()).asmType;
    emit("dec %s %s", size, target);
    resolver.deallocate(op.target());
  }

  @Override
  public void visit(Inc op) {
    String target = resolver.resolve(op.target());
    String size = Size.of(op.target().type()).asmType;
    emit("inc %s %s", size, target);
    resolver.deallocate(op.target());
  }

  @Override
  public void visit(IfOp op) {
    String condName = resolver.resolve(op.condition());
    emit("cmp BYTE %s, 0", condName);
    if (op.isNot()) {
      emit("je %s", op.destination());
    } else {
      emit("jne %s", op.destination());
    }
    resolver.deallocate(op.condition());
  }

  @Override
  public void visit(BinOp op) {
    // 1. get left
    ResolvedOperand leftRo = resolver.resolveFully(op.left());
    String leftName = leftRo.name();
    VarType leftType = op.left().type();

    // 2. get right
    ResolvedOperand rightRo = resolver.resolveFully(op.right());

    Location dest = op.destination();
    boolean reuse = false;
    if (op.left() instanceof TempLocation
        && op.destination() instanceof TempLocation
        && (leftType == VarType.BOOL
            || leftType == VarType.BYTE
            || leftType == VarType.DOUBLE
            || leftType == VarType.INT
            || leftType == VarType.LONG)
        // Only do this for int=int (op) int, because bool=int (relop) int has a weird set of
        // register sizes for now
        && (leftType.equals(op.destination().type()))) {

      // reuse left. left = left (op) right.
      dest = (Location) op.left();
      reuse = true;
      resolver.addAlias(op.destination(), op.left());
    }
    // 3. determine dest location
    ResolvedOperand destRo = resolver.resolveFully(dest);
    String destName = destRo.name();

    Register tempReg = null;

    // 5. [op] dest, right
    TokenType operator = op.operator();
    if (leftType == VarType.STRING) {
      switch (operator) {
        case PLUS:
          stringGenerator.generateStringAdd(op.destination(), op.left(), op.right(), op.position());
          break;
        case LBRACKET:
          stringGenerator.generateStringIndex(
              op.destination(), op.left(), op.right(), op.position());
          break;
        case EQEQ:
        case NEQ:
        case GT:
        case GEQ:
        case LT:
        case LEQ:
          stringGenerator.generateStringCompare(op);
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
          if (!reuse) {
            resolver.mov(op.left(), dest);
          }
          emit("%s %s, %s", BINARY_OPCODE.get(operator), destName, rightRo.name());
          break;

        case EQEQ:
        case NEQ:
        case GT:
        case GEQ:
        case LT:
        case LEQ:
          tempReg = generateCmp(leftRo, rightRo, operator, destName);
          break;

        default:
          fail("Cannot do %s on %ss (yet?)", operator, leftType);
          break;
      }
    } else if (leftType == VarType.BYTE || leftType == VarType.INT || leftType == VarType.LONG) {
      String size = Size.of(leftType).asmType;
      switch (operator) {
        case MULT:
          if (leftType == VarType.BYTE) {
            // This might be able to be simplified, but, shrug.
            // 1. move left to a (new) reg
            tempReg = resolver.allocate(leftType);
            resolver.mov(op.left(), tempReg);

            // 2. move right to a reg if it's not in one already
            Register rightReg = rightRo.register();
            boolean rightAllocated = false;
            if (rightReg == null) {
              rightReg = resolver.allocate(leftType);
              resolver.mov(op.right(), rightReg);
              rightAllocated = true;
            }

            // 3. temp = left * right
            emit("imul %s, %s", tempReg.name16(), rightReg.name16());

            // 4. mov dest, temp
            resolver.mov(tempReg, dest);
            if (rightAllocated) {
              resolver.deallocate(rightReg);
            }
            break;
          } // else: fall through
        case BIT_AND:
        case BIT_OR:
        case BIT_XOR:
        case MINUS:
        case PLUS:
          if (!reuse) {
            resolver.mov(op.left(), dest);
          }
          emit("%s %s, %s", BINARY_OPCODE.get(operator), destName, rightRo.name());
          break;

        case SHIFT_LEFT:
        case SHIFT_RIGHT:
          // mov dest, left, then mov cl, amount to shirt, then shift.
          if (op.right().isConstant()) {
            // easy. left << right or left >> right
            // TODO this will fail if dest and left are in memory
            if (!reuse) {
              resolver.mov(op.left(), dest);
            }
            emit("%s %s, %s", BINARY_OPCODE.get(operator), destName, rightRo.name());
          } else {
            // TODO: this is stupidly complex.
            Register rightReg = null;
            String rightName = rightRo.name();
            if (rightRo.register() == RCX) {
              // Have to move it
              rightReg = resolver.allocate(VarType.INT);
              Operand rightOp = new RegisterLocation(op.right().toString(), rightReg, leftType);
              emit(
                  "mov %s, %s  ; save right to a different register",
                  rightReg.sizeByType(leftType), rightName);
              // NOTE: rightName IS OVERWRITTEN
              rightName = resolver.resolve(rightOp);
            }
            // TODO: maybe use tempReg for this
            Register destReg = null;
            if (resolver.isInRegister(dest, RCX)) {
              destReg = resolver.allocate(VarType.INT);
              Location destRegLocation = new RegisterLocation(dest.name(), destReg, dest.type());
              resolver.mov(dest, destRegLocation);
              // NOTE: destName IS OVERWRITTEN
              destName = resolver.resolve(destRegLocation);
            }

            RegisterState registerState = null;
            if (destReg == null) {
              // it wasn't in rcx, so we have to push now
              registerState = condPush(ImmutableList.of(RCX));
            }
            if (!reuse) {
              // Start with dest = left. CANNOT use destRo.operand because destName might have been
              // overwritten
              // MUST use "size" because it might be memory
              emit("mov %s %s, %s ; shift setup (source)", size, destName, leftName);
            }
            // NOTE: rightName was overwritten (though, it is in both rightRo.operand AND rightName)
            // move right (amount to shift) to RCX
            emit(
                "mov %s %s, %s ; get amount to shift into rcx",
                size, RCX.sizeByType(leftType), rightName);
            // NOTE: destName may have been overwritten
            emit("%s %s, CL ; shift %s", BINARY_OPCODE.get(operator), destName, operator);
            if (rightReg != null) {
              resolver.deallocate(rightReg);
            }
            if (destReg != null) {
              // destReg was rcx, copy it out now.
              resolver.mov(dest, RCX);
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
          tempReg = generateCmp(leftRo, rightRo, operator, destName);
          break;

        case DIV:
        case MOD:
          generateDivMod(op, dest);
          break;

        default:
          fail("Cannot do %s on %ss (yet?)", operator, leftType);
          break;
      }
    } else if (leftType.isArray()) {
      arrayGenerator.visit(op);
    } else if (leftType.isRecord()) {
      recordGenerator.visit(op);
    } else if (leftType == VarType.DOUBLE) {
      doubleGenerator.generate(op);
    } else if (leftType == VarType.NULL) {
      switch (operator) {
        case EQEQ:
        case NEQ:
          tempReg = generateCmp(leftRo, rightRo, operator, destName);
          break;
        default:
          fail("Cannot do %s on %ss (yet?)", operator, leftType);
          break;
      }
    } else {
      fail("Cannot do %s on %ss (yet?)", operator, leftType);
    }

    if (tempReg != null) {
      resolver.deallocate(tempReg);
    }
    if (!leftType.isArray() && !reuse) {
      // don't deallocate yet so that array literal assignments work.
      resolver.deallocate(op.left());
    }
    resolver.deallocate(op.right());
  }

  private Register generateCmp(
      ResolvedOperand leftRo, ResolvedOperand rightRo, TokenType operator, String destName) {
    Register tempReg = null;
    if (directCompare(leftRo, rightRo)) {
      // Direct comparison: reg/anything, mem/reg, mem/imm
      emit(
          "cmp %s %s, %s  ; direct comparison",
          Size.of(leftRo.type()).asmType, leftRo.name(), rightRo.name());
    } else {
      // imm/imm, imm/reg, imm/mem, mem/mem
      // TODO: Switch imm/reg & imm/mem to be reg/imm & mem/imm in the code generator
      tempReg = resolver.allocate(VarType.INT);
      String tempRegName = tempReg.sizeByType(leftRo.type());
      resolver.mov(leftRo.operand(), tempReg);
      emit("cmp %s, %s  ; indirect comparison", tempRegName, rightRo.name());
    }
    emit("%s %s", BINARY_OPCODE.get(operator), destName);
    return tempReg;
  }

  /** returns true if we can directly compare left and right. */
  private static boolean directCompare(ResolvedOperand leftRo, ResolvedOperand rightRo) {
    if (leftRo.isRegister()) {
      // reg/anything
      return true;
    }
    if (leftRo.isConstant()) {
      // cannot do imm/anything
      return false;
    }
    // can do mem/reg or mem/imm
    return rightRo.isRegister() || rightRo.isConstant();
  }

  private void generateDivMod(BinOp op, Location dest) {
    Operand rightOperand = op.right();
    String rightName = resolver.resolve(rightOperand);
    Operand leftOperand = op.left();
    VarType operandType = leftOperand.type();
    String size = Size.of(operandType).asmType;

    if (rightOperand.isConstant()) {
      if (rightOperand.type() == VarType.INT) {
        ConstantOperand<Integer> rightConstOperand = (ConstantOperand<Integer>) rightOperand;
        int rightValue = rightConstOperand.value();
        if (rightValue == 0) {
          throw new D2RuntimeException("Division by 0", op.position(), "Arithmetic");
        }
      } else {
        ConstantOperand<Byte> rightConstOperand = (ConstantOperand<Byte>) rightOperand;
        byte rightValue = rightConstOperand.value();
        if (rightValue == 0) {
          throw new D2RuntimeException("Division by 0", op.position(), "Arithmetic");
        }
      }
    } else {
      emit("cmp %s %s, 0  ; detect division by 0", size, rightName);
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
    // 4. set up left in EDX:EAX
    // 5. idiv by right, result in eax
    // 6. mov destName, eax
    RegisterState registerState = condPush(ImmutableList.of(RAX, RDX));
    Register temp = resolver.allocate(VarType.INT);
    String leftName = resolver.resolve(leftOperand);
    emit("xor RAX, RAX");
    emit("; numerator:");
    resolver.mov(leftOperand, RAX);
    emit("; denominator:");
    resolver.mov(rightOperand, temp);

    if (operandType == VarType.BYTE) {
      emit("cbw  ; sign extend al to ax");
    } else {
      emit("cdq  ; sign extend eax to edx");
    }
    emit(
        "idiv %s  ; %s = %s / %s",
        temp.sizeByType(operandType), RAX.sizeByType(operandType), leftName, rightName);

    resolver.deallocate(temp);
    if (op.operator() == TokenType.DIV) {
      // EAX (or AL) has quotient
      emit("; quotient:");
      resolver.mov(RAX, dest);
    } else if (op.operator() == TokenType.MOD) {
      if (operandType == VarType.BYTE) {
        // remainder is in AH
        String destName = resolver.resolve(dest);
        emit("mov %s %s, AH  ; remainder", size, destName);
      } else {
        // remainder is in EDX
        resolver.mov(RDX, dest);
      }
    }

    if (!resolver.isInRegister(op.destination(), RDX)) {
      registerState.condPop(RDX);
    } else {
      // pseudo pop
      emit("add RSP, 8  ; pseudo pop RDX");
    }
    registerState.condPop(RAX);
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
        resolver.mov(source, destination);
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
        if (source.type() == VarType.INT || source.type() == VarType.BYTE) {
          resolver.mov(source, destination);
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
        // Just read one byte
        if (source.isConstant()) {
          // This can't really happen, because asc(constant) is optimized out;
          // if we turn off optimizations, asc('hi') generates
          // _temp1='hi' _temp2=asc(_temp1) so it's not really asc(constant)...
          ConstantOperand<String> stringConst = (ConstantOperand<String>) source;
          String value = stringConst.value();
          emit("mov %s, %d ; store a full int (anded to 0xff)", destName, (value.charAt(0)) & 0xff);
        } else {

          if (resolver.isInAnyRegister(source) && resolver.isInAnyRegister(destination)) {
            // register to register, don't need extra temp
            Register sourceReg = resolver.toRegister(source);
            Register destReg = resolver.toRegister(destination);
            emit("mov BYTE %s, [%s] ; copy a byte", destReg.name8(), sourceReg);
          } else {

            // Source or dest is in memory; use a temp register.
            // In reality only source can be in memory; destinations are
            // typically temps which are always in registers.

            Register tempReg = resolver.allocate(VarType.INT);
            resolver.mov(source, tempReg);
            if (resolver.isInAnyRegister(destination)) {
              // two regs, good.
              Register destReg = resolver.toRegister(destination);
              emit("mov BYTE %s, [%s] ; copy a byte", destReg.name8(), tempReg);
            } else {
              // This can't really happen, probably, because destinations
              // are typically temps, which are stored in registers.
              emit("mov BYTE %s, [%s] ; copy a byte", destName, tempReg);
            }
            resolver.deallocate(tempReg);
          }
          emit("and %s, 0xff", destName);
        }
        break;
      case CHR:
        stringGenerator.generateChr(op.operand(), op.destination());
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
    emit("push RBP");
    emit("mov RBP, RSP");
    if (op.localBytes() > 0) {
      // this may over-allocate, but /shrug.
      int bytes = 16 * (op.localBytes() / 16 + 1);
      emit("sub RSP, %d  ; space for locals", bytes);
    }
    resolver.procEntry();

    int i = 0;
    for (ParamSymbol formal : op.formals()) {
      if (i < 4) {
        resolver.reserve(Register.paramRegister(formal.varType(), i));
      } else {
        break;
      }
      i++;
    }
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
    for (Register reg : IntRegister.values()) {
      if (reg != RAX && resolver.isAllocated(reg)) {
        resolver.deallocate(reg);
      }
    }
    for (Register reg : XmmRegister.values()) {
      if (resolver.isAllocated(reg)) {
        resolver.deallocate(reg);
      }
    }

    emit0("__exit_of_%s:", op.procName());
    resolver.procEnd();

    emit("mov RSP, RBP");
    emit("pop RBP");
    emit("ret  ; return from procedure");
  }

  @Override
  public void visit(Call op) {
    RegisterState registerState = condPush(Register.VOLATILE_REGISTERS);

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
    int numArgs = op.actuals().size();
    if (numArgs > 3) {
      // # of bytes we have to adjust the stack (pseudo-pop)
      int bytes = 8 * (numArgs - 4);
      emit("add RSP, %d  ; adjust for 5th-nth parameters pushed into stack", bytes);
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
