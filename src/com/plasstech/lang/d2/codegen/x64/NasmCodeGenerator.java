package com.plasstech.lang.d2.codegen.x64;

import static com.plasstech.lang.d2.codegen.Codegen.fail;
import static com.plasstech.lang.d2.codegen.x64.IntRegister.RAX;
import static com.plasstech.lang.d2.codegen.x64.IntRegister.RCX;
import static com.plasstech.lang.d2.codegen.x64.IntRegister.RDX;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.escape.Escaper;
import com.google.common.net.PercentEscaper;
import com.plasstech.lang.d2.codegen.ConstEntry;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.DelegatingEmitter;
import com.plasstech.lang.d2.codegen.DoubleFinder;
import com.plasstech.lang.d2.codegen.DoubleTable;
import com.plasstech.lang.d2.codegen.Emitter;
import com.plasstech.lang.d2.codegen.Labels;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.StringFinder;
import com.plasstech.lang.d2.codegen.StringTable;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Dec;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.IfOp;
import com.plasstech.lang.d2.codegen.il.Inc;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.OpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.ProcEntry;
import com.plasstech.lang.d2.codegen.il.ProcExit;
import com.plasstech.lang.d2.codegen.il.Return;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.codegen.x64.Resolver.ResolvedOperand;
import com.plasstech.lang.d2.common.D2RuntimeException;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.parse.node.BlockNode;
import com.plasstech.lang.d2.parse.node.DefaultNodeVisitor;
import com.plasstech.lang.d2.phase.Phase;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.type.BlockSymbol;
import com.plasstech.lang.d2.type.ParamSymbol;
import com.plasstech.lang.d2.type.Symbol;
import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.SymbolTable;
import com.plasstech.lang.d2.type.VarType;

public class NasmCodeGenerator extends DefaultOpcodeVisitor implements Phase {
  private static final Escaper ESCAPER = new PercentEscaper("`-=[];',./~!@#$%^&*()_+{}|:\"<>?\\ ",
      false);

  static final Map<TokenType, String> COMPARISON_OPCODE =
      ImmutableMap.<TokenType, String>builder()
          .put(TokenType.EQEQ, "setz")
          .put(TokenType.NEQ, "setnz")
          .put(TokenType.GT, "setg")
          .put(TokenType.GEQ, "setge")
          .put(TokenType.LT, "setl")
          .put(TokenType.LEQ, "setle")
          .build();

  private static final Map<TokenType, String> BINARY_OPCODE = ImmutableMap
      .<TokenType, String>builder()
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
      .putAll(COMPARISON_OPCODE)
      .build();

  private final List<String> prelude = new ArrayList<>();
  private final Registers registers;
  private final DelegatingEmitter emitter;

  public NasmCodeGenerator() {
    this(new DelegatingEmitter(new X64Emitter()), new Registers());
  }

  NasmCodeGenerator(Emitter emitter, Registers registers) {
    this.emitter = new DelegatingEmitter(emitter);
    this.registers = registers;
  }

  private StringTable stringTable;
  private DoubleTable doubleTable;
  private Resolver resolver;

  private NullPointerCheckGenerator npeCheckGenerator;
  private OpcodeVisitor doubleGenerator;

  @Override
  public State execute(State input) {
    SymbolTable globals = input.symbolTable();

    stringTable = new StringFinder().execute(input.lastIlCode());
    doubleTable = new DoubleFinder().execute(input.lastIlCode());
    resolver = new Resolver(registers, stringTable, doubleTable, emitter);

    npeCheckGenerator = new NullPointerCheckGenerator(resolver, emitter);
    doubleGenerator = new DoubleCodeGenerator(resolver, emitter);

    OpcodeVisitor arrayGenerator = new ArrayCodeGenerator(resolver, emitter);
    OpcodeVisitor stringGenerator = new StringCodeGenerator(resolver, emitter);
    OpcodeVisitor recordGenerator = new RecordCodeGenerator(resolver, globals, emitter);
    OpcodeVisitor callGenerator = new CallCodeGenerator(resolver, emitter);
    OpcodeVisitor inputGenerator = new InputCodeGenerator(resolver, registers, emitter);
    OpcodeVisitor printGenerator = new PrintCodeGenerator(resolver, emitter);
    OpcodeVisitor labelCodeGenerator = new LabelCodeGenerator(emitter);
    List<OpcodeVisitor> visitors = ImmutableList.of(
        labelCodeGenerator,
        inputGenerator,
        printGenerator,
        callGenerator,
        stringGenerator,
        arrayGenerator,
        recordGenerator,
        this);

    ImmutableList<Op> code = input.lastIlCode();
    String f = "dcode";
    if (input.filename() != null) {
      f = input.filename();
    }

    prelude.add("; To execute:");
    // -Ox = optimize
    prelude.add(String.format("; nasm -fwin64 %s.asm && gcc %s.obj -o %s && ./%s\n", f, f, f, f));

    prelude.add("global main\n");

    // emit all string constants
    input.programNode().accept(new GlobalVariableEmitter(globals));
    for (ConstEntry<String> entry : stringTable.entries()) {
      emitter.addData(entry.dataEntry());
    }
    for (ConstEntry<Double> entry : doubleTable.entries()) {
      emitter.addData(entry.dataEntry());
    }

    emitter.emit0("main:");
    // Convert command-line arguments to a D-style array of strings
    ArgsCodeGenerator argsGenerator = new ArgsCodeGenerator(emitter, globals);
    argsGenerator.generate();

    try {
      for (Op opcode : code) {
        // need to escape this!
        String opcodeString = opcode.toString();
        String escaped = ESCAPER.escape(opcodeString);
        emitter.emit0("");
        if (opcode.position() != null) {
          emit("; SOURCE LINE %d: %s", opcode.position().line(), escaped);
        } else {
          emit("; SOURCE: %s", escaped);
        }

        int length = emitter.all().size();
        for (OpcodeVisitor visitor : visitors) {
          opcode.accept(visitor);
          if (emitter.all().size() > length) {
            // This visitor handled the opcode, so we're done with this opcode.
            break;
          }
        }
      }
    } catch (D2RuntimeException e) {
      ImmutableList<String> allCode = ImmutableList.<String>builder().add("PARTIAL ASSEMBLY\n\n")
          .add("================\n\n").addAll(prelude)
          .addAll(emitter.externs().stream().map(s -> "extern " + s).iterator())
          .add("\nsection .data").addAll(emitter.data().stream().map(s -> "  " + s).iterator())
          .add("\nsection .text").addAll(emitter.all()).build();
      input = input.addAsmCode(allCode).addException(e);
      return input;
    }

    ImmutableList<String> allCode = ImmutableList.<String>builder().addAll(prelude)
        .addAll(emitter.externs().stream().map(s -> "extern " + s).iterator())
        .add("\nsection .data").addAll(emitter.data().stream().map(s -> "  " + s).iterator())
        .add("\nsection .text").addAll(emitter.all()).build();

    return input.addAsmCode(allCode);
  }

  private class GlobalVariableEmitter extends DefaultNodeVisitor {
    private SymbolTable mySymbolTable;

    private GlobalVariableEmitter(SymbolTable mySymbolTable) {
      this.mySymbolTable = mySymbolTable;
      emitGlobals(mySymbolTable);
    }

    @Override
    public void visit(BlockNode node) {
      BlockSymbol blockSymbol = mySymbolTable.enterBlock(node);
      if (blockSymbol.storage() == SymbolStorage.GLOBAL) {
        SymbolTable globals = blockSymbol.symTab();
        emitGlobals(globals);
        mySymbolTable = globals;
        // recurse
        super.visit(node);
        mySymbolTable = globals.parent();
      }
    }

    private void emitGlobals(SymbolTable symbolTable) {
      // emit all string constants
      for (Map.Entry<String, Symbol> entry : symbolTable.variables().entrySet()) {
        Symbol symbol = entry.getValue();
        if (symbol.storage() == SymbolStorage.GLOBAL) {
          // reserve (& clear) 1 byte for bool, 4 bytes per int, 8 bytes for string
          Size size = Size.of(symbol.varType());
          emitter.addData(String.format("_%s: %s 0", entry.getKey(), size.dataSizeName));
        }
      }
    }
  }

  @Override
  public void visit(Transfer op) {
    Operand source = op.source();
    Location destination = op.destination();

    resolver.mov(source, destination);
    resolver.deallocate(source);
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
    Operand condition = op.condition();
    String condAsString = resolver.resolve(condition);
    String destination = op.destination();
    if (condition.isConstant()) {
      int condAsNumber = Integer.parseInt(condAsString);
      if (!((condAsNumber == 0) ^ op.isNot())) {
        // If both true or both false, unconditionally jump to destination. 
        emit("jmp %s", destination);
      }
    } else {
      emit("cmp BYTE %s, 0", condAsString);
      if (op.isNot()) {
        // We want to jump to the destination if cond IS zero, so we use 'je'.
        emit("je %s", destination);
      } else {
        // We want to jump to the destination if cond is NOT zero, so we use 'jne'.
        emit("jne %s", destination);
      }
    }
    resolver.deallocate(condition);
  }

  @Override
  public void visit(BinOp op) {
    // 1. get left
    VarType leftType = op.left().type();

    ResolvedOperand leftRo = resolver.resolveFully(op.left());
    String leftName = leftRo.name();
    // 2. get right
    ResolvedOperand rightRo = resolver.resolveFully(op.right());

    Location dest = op.destination();
    boolean reuse = false;
    if (op.left().isTemp() && op.destination().isTemp()
        && (leftType.isNumeric() || leftType == VarType.BOOL)
        // Only do this for int=int (op) int, because bool=int (relop) int has a weird set of
        // register sizes for now
        && leftType.equals(op.destination().type())) {

      // reuse left. left = left (op) right.
      Register maybeAlias = resolver.toRegister(op.destination());
      if (maybeAlias != null) {
        // if dest is already allocated to a register, so don't re-allocate it, and don't alias it.
        emitter.emit("; dest %s already in a register %s", op.destination(), maybeAlias);
      } else {
        dest = (Location) op.left();
        reuse = true;
        resolver.addAlias(op.destination(), op.left());
      }
    }
    // 3. determine dest location
    ResolvedOperand destRo = resolver.resolveFully(dest);
    String destName = destRo.name();

    Register tempReg = null;

    // 5. [op] dest, right
    TokenType operator = op.operator();
    if (leftType == VarType.BOOL) {
      switch (operator) {
        case AND:
        case OR:
        case XOR:
          if (!reuse) {
            resolver.mov(op.left(), dest);
          }
          generateBinOp(rightRo, destRo, operator);
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
          fail(op.position(), "Cannot do %s on %ss (yet?)", operator, leftType);
          break;
      }
    } else if (leftType == VarType.DOUBLE) {
      op.accept(doubleGenerator);
    } else if (leftType.isIntegral()) {
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
          generateBinOp(rightRo, destRo, operator);
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
            generateBinOp(rightRo, destRo, operator);
          } else {
            // TODO: bug https://github.com/dplassgit/d2lang/issues/177 - this is stupidly complex.
            Register rightReg = null;
            String rightName = rightRo.name();
            if (rightRo.register() == RCX) {
              // Have to move it
              rightReg = resolver.allocate(VarType.INT);
              Operand rightOp = new RegisterLocation(op.right().toString(), rightReg, leftType);
              emit("mov %s, %s  ; save right to a different register",
                  rightReg.sizeByType(leftType),
                  rightName);
              // NOTE: rightName IS OVERWRITTEN
              rightName = resolver.resolve(rightOp);
            }
            // TODO: maybe use tempReg for this
            Register destReg = null;
            if (resolver.isInRegister(dest, RCX)) {
              destReg = resolver.allocate(VarType.INT);
              Location destRegLocation = new RegisterLocation(dest.name(), destReg, dest.type());
              emit("; dest is RCX, have to do an extra mov:");
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
            emit("mov %s %s, %s ; get amount to shift into CL",
                size,
                RCX.sizeByType(leftType),
                rightName);
            // NOTE: destName may have been overwritten
            emit("%s %s, CL ; shift %s", BINARY_OPCODE.get(operator), destName, operator);
            if (rightReg != null) {
              resolver.deallocate(rightReg);
            }
            if (destReg != null) {
              emit("; destreg was set, copy it out now");
              resolver.mov(VarType.INT, destReg, RCX);
              resolver.deallocate(destReg);
            } else {
              // it wasn't in RCX; we already put it in the right place
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
          fail(op.position(), "Cannot do %s on %ss (yet?)", operator, leftType);
          break;
      }
    } else if (leftType == VarType.NULL) {
      switch (operator) {
        case EQEQ:
        case NEQ:
          tempReg = generateCmp(leftRo, rightRo, operator, destName);
          break;

        case DOT:
          fail("Null pointer", op.position(),
              "Cannot retrieve field %s of null object", op.right());
          break;

        default:
          fail(op.position(), "Cannot do %s on %ss (yet?)", operator, leftType);
          break;
      }
    } else {
      fail(op.position(), "Cannot do %s on %ss (yet?)", operator, leftType);
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

  private void generateBinOp(ResolvedOperand source, ResolvedOperand dest, TokenType operator) {
    if (ConstantOperand.isImm64(source.operand())) {
      // adjust for OPCODE REG, imm64 if the constant is too big.
      emitter.emit("; constant is larger than 32 bits, must use intermediary");
      Register tempReg = resolver.allocate(source.type());
      resolver.mov(source.operand(), tempReg);
      emit("%s %s %s, %s",
          BINARY_OPCODE.get(operator),
          Size.of(source.type()).asmType,
          dest.name(),
          tempReg.name());
      resolver.deallocate(tempReg);
      // NOTE RETURN
      return;
    }
    emit("%s %s %s, %s",
        BINARY_OPCODE.get(operator),
        Size.of(source.type()).asmType,
        dest.name(),
        source.name());
  }

  private Register generateCmp(ResolvedOperand leftRo, ResolvedOperand rightRo, TokenType operator,
      String destName) {
    Register tempReg = null;
    if (directCompare(leftRo, rightRo)) {
      // Direct comparison: reg/anything, mem/reg, mem/imm8, mem/imm32
      emit("cmp %s %s, %s  ; direct comparison",
          Size.of(leftRo.type()).asmType,
          leftRo.name(),
          rightRo.name());
    } else if (rightRo.isConstant()) {
      // Normally we'd do a direct comparison, but the RHS was too big. Need to do even worse
      // indirect comparison.
      tempReg = resolver.allocate(VarType.INT);
      String tempRegName = tempReg.sizeByType(leftRo.type());
      resolver.mov(leftRo.operand(), tempReg);
      Register rightReg = resolver.allocate(VarType.INT);
      resolver.mov(rightRo.operand(), rightReg);
      emit("cmp %s, %s  ; imm comparison", tempRegName, rightReg.sizeByType(rightRo.type()));
      resolver.deallocate(rightReg);
    } else {
      // imm/imm, imm/reg, imm/mem, mem/mem
      // TODO: Switch imm/reg & imm/mem to be reg/imm & mem/imm in the ILCodeGenerator
      // (doesn't it do this?!)
      tempReg = resolver.allocate(VarType.INT);
      String tempRegName = tempReg.sizeByType(leftRo.type());
      resolver.mov(leftRo.operand(), tempReg);
      emit("cmp %s, %s  ; indirect comparison", tempRegName, rightRo.name());
    }
    emit("%s %s", BINARY_OPCODE.get(operator), destName);
    return tempReg;
  }

  /** returns true if we can directly compare left and right. */
  private boolean directCompare(ResolvedOperand leftRo, ResolvedOperand rightRo) {
    // anything vs imm64: false
    if (ConstantOperand.isImm64(rightRo.operand())) {
      return false;
    }

    if (leftRo.isRegister()) {
      // reg/anything
      return true;
    }
    if (leftRo.isConstant()) {
      // cannot do imm/anything
      return false;
    }
    // left is mem so it's either mem/reg or mem/imm
    return rightRo.isConstant() || rightRo.isRegister();
  }

  private void generateDivMod(BinOp op, Location dest) {
    Operand rightOperand = op.right();
    String rightName = resolver.resolve(rightOperand);
    Operand leftOperand = op.left();
    VarType operandType = leftOperand.type();
    String size = Size.of(operandType).asmType;

    if (ConstantOperand.isAnyZero(rightOperand)) {
      fail("Arithmetic", op.position(), "Division by 0");
    }
    if (!rightOperand.isConstant()) {
      emit("cmp %s %s, 0  ; detect division by 0", size, rightName);
      String continueLabel = Labels.nextLabel("not_div_by_zero");
      emit("jne %s", continueLabel);

      emitter.emit0("\n  ; division by zero. print error and stop");
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

    if (operandType == VarType.INT) {
      emit("cdq  ; sign extend eax to edx");
    } else if (operandType == VarType.LONG) {
      emit("cqo  ; sign extend rax to rdx");
    } else if (operandType == VarType.BYTE) {
      emit("cbw  ; sign extend al to ax");
    }

    emit("idiv %s  ; %s = %s / %s",
        temp.sizeByType(operandType),
        RAX.sizeByType(operandType),
        leftName,
        rightName);

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
    // 2. apply op
    // 3. store in destination
    Location destination = op.destination();
    String destName = resolver.resolve(destination);
    // apply op
    switch (op.operator()) {
      case BIT_NOT:
        // NOTE: NOT TWOS COMPLEMENT NOT, it's 1-s complement not.
        if (!source.type().isIntegral()) {
          fail("Code generation", op.position(),
              "Cannot apply %s to %s expression; must be BYTE, INT or LONG", op.operator(),
              source.type());
        }
        resolver.mov(source, destination);
        emit("not %s  ; bit not", destName);
        break;

      case NOT:
        // boolean not
        if (source.type() != VarType.BOOL) {
          fail("Code generation", op.position(),
              "Cannot apply %s to %s expression; must be BOOL", op.operator(),
              source.type());
        }
        resolver.mov(source, destination);
        emit("xor %s, 0x01  ; boolean not", destName);
        break;

      case MINUS:
        if (source.type() == VarType.DOUBLE) {
          op.accept(doubleGenerator);
        } else {
          if (!source.type().isIntegral()) {
            fail("Code generation", op.position(),
                "Cannot apply %s to %s expression; must be BYTE, INT or LONG", op.operator(),
                source.type());
          }
          resolver.mov(source, destination);
          emit("neg %s  ; unary minus", destName);
        }
        break;

      case LENGTH:
        fail("Null pointer", op.position(),
            "Cannot apply LENGTH function to %s expression; must be ARRAY or STRING",
            source.type());
        break;

      case ASC:
        if (source.type() != VarType.STRING) {
          fail("Code generation", op.position(),
              "Cannot apply %s to %s expression; must be STRING", op.operator(),
              source.type());
        }
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

      default:
        fail(op.position(), "Cannot generate %s yet", op);
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

    emitter.emit0("__exit_of_%s:", op.procName());
    resolver.procEnd();

    emit("mov RSP, RBP");
    emit("pop RBP");
    emit("ret  ; return from procedure");
  }

  /** Conditionally push all allocated registers in the list */
  private RegisterState condPush(ImmutableList<Register> registerList) {
    return RegisterState.condPush(emitter, resolver, registerList);
  }

  private void emit(String format, Object... values) {
    emitter.emit(format, values);
  }
}
