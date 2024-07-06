package com.plasstech.lang.d2.codegen.x64;

import static com.plasstech.lang.d2.codegen.Codegen.fail;
import static com.plasstech.lang.d2.codegen.x64.IntRegister.RAX;
import static com.plasstech.lang.d2.codegen.x64.IntRegister.RCX;
import static com.plasstech.lang.d2.codegen.x64.IntRegister.RDX;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;
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
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.StringFinder;
import com.plasstech.lang.d2.codegen.StringTable;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.DeallocateTemp;
import com.plasstech.lang.d2.codegen.il.Dec;
import com.plasstech.lang.d2.codegen.il.IfOp;
import com.plasstech.lang.d2.codegen.il.ImplementedOnlyOpcodeVisitor;
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

public class NasmCodeGenerator extends ImplementedOnlyOpcodeVisitor implements Phase {
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

  private OpcodeVisitor doubleGenerator;

  @Override
  public State execute(State input) {
    SymbolTable globals = input.symbolTable();

    stringTable = new StringFinder().execute(input.lastIlCode());
    doubleTable = new DoubleFinder().execute(input.lastIlCode());
    resolver = new Resolver(registers, stringTable, doubleTable, emitter);

    doubleGenerator = new DoubleCodeGenerator(resolver, emitter);

    OpcodeVisitor arrayGenerator = new ArrayCodeGenerator(resolver, emitter);
    OpcodeVisitor stringGenerator = new StringCodeGenerator(resolver, emitter);
    OpcodeVisitor recordGenerator = new RecordCodeGenerator(resolver, globals, emitter);
    OpcodeVisitor callGenerator = new CallCodeGenerator(resolver, emitter);
    OpcodeVisitor inputGenerator = new InputCodeGenerator(resolver, registers, emitter);
    OpcodeVisitor printGenerator = new PrintCodeGenerator(resolver, stringTable, emitter);
    OpcodeVisitor labelCodeGenerator = new LabelCodeGenerator(emitter);
    OpcodeVisitor rangeGenerator = new RangeCodeGenerator(resolver, emitter);
    List<OpcodeVisitor> visitors = ImmutableList.of(
        labelCodeGenerator,
        inputGenerator,
        printGenerator,
        callGenerator,
        stringGenerator,
        arrayGenerator,
        recordGenerator,
        rangeGenerator,
        this);

    ImmutableList<Op> code = input.lastIlCode();
    String f = "dcode";
    if (input.filename() != null) {
      f = input.filename();
    }

    prelude.add("; To execute:");
    // -Ox = optimize
    prelude.add(String.format("; nasm -fwin64 %s.asm && gcc %s.obj -o %s && ./%s", f, f, f, f));
    prelude.add("");

    prelude.add("global main");
    prelude.add("");

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

    Op latest = null;
    try {
      for (Op opcode : code) {
        latest = opcode;
        // need to escape this!
        String opcodeString = opcode.toString();
        String escaped = ESCAPER.escape(opcodeString);
        emitter.emit0("");
        if (opcode.position() != null) {
          emitter.emit("; SOURCE LINE %d: %s", opcode.position().line(), escaped);
        } else {
          emitter.emit("; SOURCE: %s", escaped);
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
      System.err.println(Joiner.on('\n').join(allCode));
      input = input.addAsmCode(allCode).addException(e);
      return input;
    } catch (RuntimeException e) {
      e.printStackTrace();
      System.err.println("Latest opcode: " + latest);
      ImmutableList<String> allCode = ImmutableList.<String>builder().add("PARTIAL ASSEMBLY\n\n")
          .add("================\n\n").addAll(prelude)
          .addAll(emitter.externs().stream().map(s -> "extern " + s).iterator())
          .add("\nsection .data").addAll(emitter.data().stream().map(s -> "  " + s).iterator())
          .add("\nsection .text").addAll(emitter.all()).build();
      input =
          input.addAsmCode(allCode).addException(new D2RuntimeException("oops", null, "Internal"));
      return input;
    }

    ImmutableList<String> allCode = ImmutableList.<String>builder().addAll(prelude)
        .addAll(emitter.externs().stream().map(s -> "extern " + s).iterator())
        .add("")
        .add("section .data").addAll(emitter.data().stream().map(s -> "  " + s).iterator())
        .add("")
        .add("section .text").addAll(emitter.all()).build();

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
    if (source.isTemp() && destination.storage() == SymbolStorage.LONG_TEMP) {
      // Just alias the long temp to the temp's register; the long temp's register will be
      // deallocated eventually.
      resolver.addAlias(destination, source);
      return;
    }

    resolver.mov(source, destination);
    resolver.deallocate(source);
  }

  @Override
  public void visit(Dec op) {
    String target = resolver.resolve(op.target());
    String size = Size.of(op.target().type()).asmType;
    emitter.emit("dec %s %s", size, target);
    resolver.deallocate(op.target());
  }

  @Override
  public void visit(Inc op) {
    String target = resolver.resolve(op.target());
    String size = Size.of(op.target().type()).asmType;
    emitter.emit("inc %s %s", size, target);
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
        emitter.emit("jmp %s", destination);
      }
    } else {
      emitter.emit("cmp BYTE %s, 0", condAsString);
      if (op.isNot()) {
        // We want to jump to the destination if cond IS zero, so we use 'je'.
        emitter.emit("je %s", destination);
      } else {
        // We want to jump to the destination if cond is NOT zero, so we use 'jne'.
        emitter.emit("jne %s", destination);
      }
    }
    resolver.deallocate(condition);
  }

  @Override
  public void visit(BinOp op) {
    VarType leftType = op.left().type();

    Location dest = op.destination();
    // TODO: I think it's possible we don't need this anymore, because resolveFully should
    // probably do the right thign.
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
    ResolvedOperand leftRo = resolver.resolveFully(op.left());
    ResolvedOperand rightRo = resolver.resolveFully(op.right());
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
            emitter.emit("imul %s, %s", tempReg.nameByType(VarType.SHORT),
                rightReg.nameByType(VarType.SHORT));

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
          generateShift(leftRo, rightRo, reuse, destRo, operator);
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
              "Cannot retrieve field %s of NULL record", op.right());
          break;

        default:
          fail(op.position(), "Cannot do %s on %ss (yet?)", operator, leftType);
          break;
      }
    } else {
      fail(op.position(), "Cannot do anything (%s) on %ss (yet?)", operator, leftType);
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

  private void generateShift(ResolvedOperand leftRo,
      ResolvedOperand rightRo, boolean reuxse, ResolvedOperand destRo,
      TokenType operator) {

    if (rightRo.isConstant()) {
      // Easy, because shl and sar can take a constant
      // mov dest, left
      // mov cl, right (amount to shift)
      // shl dest, cl
      // easy. left << right or left >> right

      // dest = left. it does this smartly.
      resolver.mov(leftRo, destRo);

      if (!ConstantOperand.isAnyZero(rightRo.operand())) {
        // if rightRo is 0, skip.
        // dest = dest << right or dest = dest >> right
        generateBinOp(rightRo, destRo, operator);
      }
      return;
    }

    if (resolver.isInRegister(destRo, RCX)) {
      // allocate temp reg
      // temp = left
      // rcx = right ;; mov is smart enough to not do this if not necessary
      // temp = temp << cl   ; can ONLY do temp = temp
      // dest = temp
      Register tempReg = resolver.allocate(VarType.INT);
      emitter.emit("; dest in rcx - go through temp reg %s", tempReg);
      resolver.mov(leftRo, tempReg);
      resolver.mov(rightRo, RCX);
      emitter.emit("%s %s, CL",
          BINARY_OPCODE.get(operator),
          tempReg.nameByType(leftRo.type()));
      resolver.mov(tempReg, destRo);
      resolver.deallocate(tempReg);
      return;
    }

    // destination is not in rcx.
    // dest = left ;; mov is smart enough to not do this if not necessary
    // rcx = right ;; mov is smart enough to not do this if not necessary
    // dest = dest << cl
    emitter.emit("; dest not in rcx");
    resolver.mov(leftRo, destRo);
    resolver.mov(rightRo, RCX);
    emitter.emit("%s %s, CL",
        BINARY_OPCODE.get(operator),
        destRo.name());
  }

  // Generate dest=dest (operator) source
  private void generateBinOp(ResolvedOperand source, ResolvedOperand dest, TokenType operator) {
    if (ConstantOperand.isImm64(source.operand())) {
      // adjust for OPCODE REG, imm64 if the constant is too big.
      emitter.emit("; constant is larger than 32 bits, must use intermediary");
      Register tempReg = resolver.allocate(source.type());
      resolver.mov(source.operand(), tempReg);
      emitter.emit("%s %s %s, %s",
          BINARY_OPCODE.get(operator),
          Size.of(source.type()).asmType,
          dest.name(),
          tempReg.name());
      resolver.deallocate(tempReg);
      // NOTE RETURN
      return;
    }
    // This will fail if both source and dest have offsets (i.e, [RBP-8], [RBP+16])
    emitter.emit("%s %s %s, %s",
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
      emitter.emit("cmp %s %s, %s  ; direct comparison",
          Size.of(leftRo.type()).asmType,
          leftRo.name(),
          rightRo.name());
    } else if (rightRo.isConstant()) {
      emitter.emit("; both are constants: %s vs %s", leftRo, rightRo);
      // Normally we'd do a direct comparison, but the RHS was too big. Need to do even worse
      // indirect comparison.
      tempReg = resolver.allocate(VarType.INT);
      String tempRegName = tempReg.nameByType(leftRo.type());
      resolver.mov(leftRo.operand(), tempReg);
      Register rightReg = resolver.allocate(VarType.INT);
      resolver.mov(rightRo.operand(), rightReg);
      emitter.emit("cmp %s, %s  ; imm comparison", tempRegName,
          rightReg.nameByType(rightRo.type()));
      resolver.deallocate(rightReg);
    } else {
      // imm/imm, imm/reg, imm/mem, mem/mem
      // TODO: Switch imm/reg & imm/mem to be reg/imm & mem/imm in the ILCodeGenerator
      // (doesn't it do this?!)
      tempReg = resolver.allocate(VarType.INT);
      String tempRegName = tempReg.nameByType(leftRo.type());
      resolver.mov(leftRo.operand(), tempReg);
      emitter.emit("cmp %s, %s  ; indirect comparison", tempRegName, rightRo.name());
    }
    emitter.emit("%s %s", BINARY_OPCODE.get(operator), destName);
    return tempReg;
  }

  /** returns true if we can directly compare left and right. */
  private static boolean directCompare(ResolvedOperand leftRo, ResolvedOperand rightRo) {
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

  // NOTE: `dest` might NOT be op.destination() because of register reuse.
  private void generateDivMod(BinOp op, Location dest) {
    Operand right = op.right();
    Operand left = op.left();
    VarType operandType = left.type();

    // Note: division by 0 checks are done in IL code now.
    // 1. determine dest location
    // 2. set up left in EDX:EAX
    // 3. idiv by right, result in eax
    // 4. mov destName, eax
    RegisterState registerState =
        RegisterState.condPush(emitter, resolver, ImmutableList.of(RAX, RDX));
    emitter.emit("; numerator:");
    resolver.mov(left, RAX);
    emitter.emit("; denominator:");
    Register temp = resolver.allocate(VarType.INT);
    resolver.mov(right, temp);

    if (operandType == VarType.BYTE) {
      emitter.emit("; sign extend AL to AX");
      emitter.emit("cbw");
    } else if (operandType == VarType.INT) {
      emitter.emit("; sign extend EAX to EDX");
      emitter.emit("cdq");
    } else if (operandType == VarType.LONG) {
      emitter.emit("; sign extend RAX to RDX");
      emitter.emit("cqo");
    }

    emitter.emit("; %s = %s / %s", RAX.nameByType(operandType), left, right);
    emitter.emit("idiv %s", temp.nameByType(operandType));

    resolver.deallocate(temp);
    if (op.operator() == TokenType.DIV) {
      // EAX (or AL) has quotient
      emitter.emit("; quotient:");
      resolver.mov(RAX, dest);
    } else if (op.operator() == TokenType.MOD) {
      if (operandType == VarType.BYTE) {
        // Remainder is in AH, but we can only transfer from AH to certain other registers.
        // Prevent it by always just using AL.
        emitter.emit("; prevent using AH for mod");
        emitter.emit("xchg AL, AH");
        resolver.mov(RAX, dest);
      } else {
        // remainder is in EDX
        resolver.mov(RDX, dest);
      }
    }

    if (!resolver.isInRegister(op.destination(), RDX)) {
      registerState.condPop(RDX);
    } else {
      // pseudo pop
      emitter.emit("add RSP, 0x08  ; adjust stack instead of popping RDX");
    }
    registerState.condPop(RAX);
  }

  @Override
  public void visit(UnaryOp op) {
    // 1. get source location name
    // 2. apply op
    // 3. store in destination
    Operand source = op.operand();
    Location destination = op.destination();
    String destName = resolver.resolve(destination);

    switch (op.operator()) {
      case BIT_NOT:
        // NOTE: NOT TWOS COMPLEMENT NOT, it's 1-s complement not.
        if (!source.type().isIntegral()) {
          fail("Code generation", op.position(),
              "Cannot apply %s to %s expression; must be BYTE, INT or LONG", op.operator(),
              source.type());
        }
        resolver.mov(source, destination);
        emitter.emit("not %s  ; bit not", destName);
        break;

      case NOT:
        // boolean not
        if (source.type() != VarType.BOOL) {
          fail("Code generation", op.position(),
              "Cannot apply %s to %s expression; must be BOOL", op.operator(),
              source.type());
        }
        resolver.mov(source, destination);
        emitter.emit("xor %s, 0x01  ; boolean not", destName);
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
          emitter.emit("neg %s  ; unary minus", destName);
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
        // Just read one byte
        if (source.isConstant()) {
          // This can't really happen, because asc(constant) is optimized out;
          // if we turn off optimizations, asc('hi') generates
          // _temp1='hi' _temp2=asc(_temp1) so it's not really asc(constant)...
          String value = ConstantOperand.stringValueFromConstOperand(source);
          emitter.emit("mov %s, %d ; store a full int (anded to 0xff)", destName,
              (value.charAt(0)) & 0xff);
        } else {
          if (resolver.isInAnyRegister(source) && resolver.isInAnyRegister(destination)) {
            // register to register, don't need extra temp
            Register sourceReg = resolver.toRegister(source);
            Register destReg = resolver.toRegister(destination);
            emitter.emit("mov BYTE %s, [%s] ; copy a byte", destReg.nameByType(VarType.BYTE),
                sourceReg);
          } else {

            // Source or dest is in memory; use a temp register.
            // In reality only source can be in memory; destinations are
            // typically temps which are always in registers.

            Register tempReg = resolver.allocate(VarType.INT);
            resolver.mov(source, tempReg);
            if (resolver.isInAnyRegister(destination)) {
              // two regs, good.
              Register destReg = resolver.toRegister(destination);
              emitter.emit("mov BYTE %s, [%s] ; copy a byte", destReg.nameByType(VarType.BYTE),
                  tempReg);
            } else {
              // This can't really happen, probably, because destinations
              // are typically temps, which are stored in registers.
              emitter.emit("mov BYTE %s, [%s] ; copy a byte", destName, tempReg);
            }
            resolver.deallocate(tempReg);
          }
          emitter.emit("and %s, 0xff", destName);
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
    if (op.localBytes() > 0 || op.formals().size() > 4) {
      emitter.emit("push RBP");
      emitter.emit("mov RBP, RSP");
    }
    // this over-allocates, but /shrug.
    if (op.localBytes() > 0) {
      int bytes = 16 * (op.localBytes() / 16 + 1);
      emitter.emit("sub RSP, 0x%02x  ; space for locals", bytes);
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

    if (op.localBytes() > 0 || op.numFormals() > 4) {
      emitter.emit("mov RSP, RBP");
      emitter.emit("pop RBP");
    }
    emitter.emit("ret");
  }

  @Override
  public void visit(Return op) {
    // we can't just "ret" here because there's cleanup we need to do first.
    op.returnValueLocation().ifPresent(returnValue -> {
      // transfer from return value to XMM0/RAX
      resolver.mov(returnValue, Registers.returnRegister(returnValue.type()));
      resolver.deallocate(returnValue);
    });
    // NOTYPO
    emitter.emit("jmp __exit_of_%s", op.procName());
  }

  @Override
  public void visit(DeallocateTemp op) {
    resolver.unconditionallyDeallocate(op.temp());
  }
}
