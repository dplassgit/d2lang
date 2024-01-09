package com.plasstech.lang.d2.codegen.t100;

import static com.plasstech.lang.d2.codegen.Codegen.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.escape.Escaper;
import com.google.common.net.PercentEscaper;
import com.plasstech.lang.d2.codegen.ConstEntry;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Emitter;
import com.plasstech.lang.d2.codegen.Labels;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.StringFinder;
import com.plasstech.lang.d2.codegen.StringTable;
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
import com.plasstech.lang.d2.codegen.t100.Subroutine.Name;
import com.plasstech.lang.d2.common.D2RuntimeException;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.phase.Phase;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.type.SymbolTable;
import com.plasstech.lang.d2.type.VarType;

public class T100CodeGenerator extends DefaultOpcodeVisitor implements Phase {
  private static final int BASE = 0xd000;

  private static final Escaper ESCAPER =
      new PercentEscaper("`-=[];',./~!@#$%^&*()_+{}|:\"<>?\\ ", false);

  private static final Map<TokenType, String> IMMEDIATE_BINARY_OPCODE =
      ImmutableMap.<TokenType, String>builder()
          .put(TokenType.PLUS, "adi") // for bytes
          .put(TokenType.MINUS, "sui") // for bytes
          .put(TokenType.AND, "ani") // for booleans
          .put(TokenType.OR, "ori") // for booleans
          .put(TokenType.XOR, "xri") // for booleans
          .put(TokenType.BIT_AND, "ani") // for bytes
          .put(TokenType.BIT_OR, "ori") // for bytes
          .put(TokenType.BIT_XOR, "xri") // for bytes
          .build();

  private static final Map<TokenType, String> BINARY_OPCODE =
      ImmutableMap.<TokenType, String>builder()
          .put(TokenType.PLUS, "add") // for bytes
          .put(TokenType.MINUS, "sub") // for bytes
          .put(TokenType.AND, "ana") // for boolean
          .put(TokenType.OR, "ora") // for boolean
          .put(TokenType.XOR, "xra") // for boolean
          .put(TokenType.BIT_AND, "ana") // for bytes
          .put(TokenType.BIT_OR, "ora") // for bytes
          .put(TokenType.BIT_XOR, "xra") // for bytes
          .build();

  private static final Map<TokenType, String> COMPARISON_OPCODE =
      ImmutableMap.<TokenType, String>builder()
          .put(TokenType.EQEQ, "jz")
          .put(TokenType.NEQ, "jz")
          .put(TokenType.LT, "jnc")
          .put(TokenType.GT, "jc")
          .put(TokenType.LEQ, "jc")
          .put(TokenType.GEQ, "jnc")
          .build();
  private static final Map<TokenType, String> COMPARISON_OPCODE2 =
      ImmutableMap.<TokenType, String>builder()
          .put(TokenType.LT, "jz")
          .put(TokenType.GT, "jz")
          .put(TokenType.LEQ, "jz")
          .put(TokenType.GEQ, "jz")
          .build();

  private static final Map<TokenType, String> COMPARISON_PRESET =
      ImmutableMap.<TokenType, String>builder()
          .put(
              TokenType.LT,
              "mvi A, 0x00  ; preset to false (cannot use xra because it affects flags)")
          .put(
              TokenType.GT,
              "mvi A, 0x00  ; preset to false (cannot use xra because it affects flags)")
          .put(TokenType.LEQ, "mvi A, 0x01  ; preset to true")
          .put(TokenType.GEQ, "mvi A, 0x01  ; preset to true")
          .put(TokenType.EQEQ, "mvi A, 0x01  ; preset to true")
          .put(TokenType.NEQ, "mvi A, 0x00  ; preset to false")
          .build();
  private static final Map<TokenType, String> COMPARISON_POSTSET =
      ImmutableMap.<TokenType, String>builder()
          .put(TokenType.LT, "inr A  ; success, set to true")
          .put(TokenType.GT, "inr A  ; success, set to true")
          .put(TokenType.NEQ, "inr A  ; success, set to true")
          .put(TokenType.LEQ, "dcr A  ; failure, set to false")
          .put(TokenType.GEQ, "dcr A  ; failure, set to false")
          .put(TokenType.EQEQ, "dcr A  ; failure, set to false")
          .build();

  private static final T100StringData ERROR_MESSAGE = new T100StringData("ERROR_MSG", "ERROR: ");
  private static final T100StringData TRUE_MESSAGE = new T100StringData("TRUE_MSG", "true");
  private static final T100StringData FALSE_MESSAGE = new T100StringData("FALSE_MSG", "false");

  private final List<String> prelude = new ArrayList<>();
  private final Registers registers;
  private final Emitter emitter;
  private SymbolTable symTab;
  private Resolver resolver;
  private StringTable stringTable;
  // private DoubleTable doubleTable;

  // don't add the same subroutine multiple times.
  private final Map<String, List<String>> subroutines = new HashMap<>();

  public T100CodeGenerator() {
    this(new T100Emitter(), new Registers());
  }

  T100CodeGenerator(Emitter emitter, Registers registers) {
    this.emitter = emitter;
    this.registers = registers;
  }

  @Override
  public State execute(State input) {
    stringTable = new StringFinder().execute(input.lastIlCode());
    //  doubleTable = new DoubleFinder().execute(input.lastIlCode());

    resolver = new Resolver(stringTable, registers, emitter);

    ImmutableList<Op> code = input.lastIlCode();
    String f = "dcode";
    if (input.filename() != null) {
      f = input.filename();
    }

    prelude.add(String.format("; To assemble: python [path]/assembler.py %s.as -t", f));
    prelude.add(String.format("; in BASIC, call 'clear 256,%d' before loading", BASE));
    prelude.add(String.format("  org 0x%x", BASE));
    prelude.add("  jmp main");
    prelude.add("");

    symTab = input.symbolTable();

    // emit all globals
    input.programNode().accept(new GlobalVariableEmitter(symTab, emitter));

    // emit all string constants
    for (ConstEntry<String> entry : stringTable.entries()) {
      // convert to a type we can deal with. FTR I hate this. Instead of making a new entry
      // there probably should be an entry creator adapter, or maybe even not have an
      // abstract method on ConstEntry called "dataEntry" at all - it should be up to the
      // code generator -slash-assembler to decide how to create a data entry
      T100StringData t100entry = new T100StringData(entry);
      String unescapedString = entry.value();
      String escaped = ESCAPER.escape(unescapedString);
      emitter.emit("; %s=\"%s\"", entry.name(), escaped);
      emitter.emit(t100entry.dataEntry());
    }
    //    for (ConstEntry<Double> entry : doubleTable.entries()) {
    //      emitter.addData(entry.dataEntry());
    //    }

    emitter.emit0("");
    emitter.emitLabel("main");
    try {
      for (Op opcode : code) {
        String opcodeString = opcode.toString();
        String escaped = ESCAPER.escape(opcodeString);
        emitter.emit0("");
        if (opcode.position() != null) {
          emitter.emit("; SOURCE LINE %d: %s", opcode.position().line(), escaped);
        } else {
          emitter.emit("; SOURCE: %s", escaped);
        }
        opcode.accept(this);
      }
    } catch (D2RuntimeException e) {
      ImmutableList<String> allCode =
          ImmutableList.<String>builder()
              .add("PARTIAL ASSEMBLY\n\n")
              .add("================\n\n")
              .addAll(prelude)
              .addAll(emitter.data().stream().map(s -> "  " + s).iterator())
              .addAll(emitter.all())
              .build();
      input = input.addAsmCode(allCode).addException(e);
      throw e;
      //      return input;
    }

    // for each subroutine, add to the emitter.
    subroutines.values().stream().flatMap(Collection::stream)
        .forEach(line -> emitter.emit0(line));
    ImmutableList<String> allCode =
        ImmutableList.<String>builder()
            .addAll(prelude)
            .addAll(emitter.data().stream().map(s -> "  " + s).iterator())
            .addAll(emitter.all())
            .build();

    return input.addAsmCode(allCode);
  }

  private void addSubroutine(Name nameEnum) {
    String name = nameEnum.name();
    if (!subroutines.containsKey(name)) {
      Subroutine sub = Subroutines.get(name);
      if (sub == null) {
        fail(null, "No code for subroutine %s", name);
      }
      subroutines.put(name, sub.code());
      // ALSO add its deps
      for (Name dep : sub.dependencies()) {
        addSubroutine(dep);
      }
    }
  }

  private void callSubroutine(Name name) {
    addSubroutine(name);
    emitter.emit("call %s", name.name());
  }

  @Override
  public void visit(Label op) {
    emitter.emitLabel(op.label());
  }

  @Override
  public void visit(Stop op) {
    emitter.emitExit(0);
  }

  @Override
  public void visit(Goto op) {
    emitter.emit("jmp %s", op.label());
  }

  @Override
  public void visit(Transfer op) {
    Operand source = op.source();
    Location destination = op.destination();
    resolver.mov(source, destination);
    resolver.deallocate(source);
  }

  @Override
  public void visit(SysCall op) {
    // See
    // http://bitchin100.com/wiki/index.php?title=Model_100_System_Map_Part_2_(0DAB-290F)
    // for system calls.
    switch (op.call()) {
      case MESSAGE:
        emitter.addData(ERROR_MESSAGE.dataEntry());
        emitter.emit("lxi H, %s", ERROR_MESSAGE.name());
        emitter.emit("call 0x11A2  ; print HL (destroys HL, A)");
        // fall through:
      case PRINT:
      case PRINTLN:
        Operand arg = op.arg();
        String name = resolver.resolve(arg);
        if (arg.type() == VarType.STRING) {
          // TODO: If null, output "null".
          if (arg.isConstant()) {
            // load the value of the constant string
            emitter.emit("lxi H, %s  ; prepare to print HL", name);
          } else {
            // load the location where the string is.
            emitter.emit("lhld %s  ; prepare to print HL", name);
          }
          emitter.emit("call 0x11A2  ; print HL (destroys HL, A)");
        } else if (arg.type() == VarType.BYTE) {
          // use a call to print a byte. clear H.
          if (arg.isConstant()) {
            byte value = ((ConstantOperand<Byte>) arg).value();
            if (value < 0) {
              // print '-' and then convert to positive
              emitter.emit("mvi A, 0x2d");
              emitter.emit("call 0x0020  ; print a negative sign before the negative byte");
              value = (byte) ((-value) + 1);
            }
            emitter.emit("lxi H, 0x00%02x", value);
            emitter.emit("call 0x39D4  ; print the ASCII value of the number in HL (destroys all)");
          } else {
            emitter.emit("lda %s", name);
            callSubroutine(Name.D_print8);
          }
        } else if (arg.type() == VarType.BOOL) {
          if (arg.equals(ConstantOperand.TRUE)) {
            emitter.addData(TRUE_MESSAGE.dataEntry());
            emitter.emit("lxi H, %s", TRUE_MESSAGE.name());
          } else if (arg.equals(ConstantOperand.FALSE)) {
            emitter.addData(FALSE_MESSAGE.dataEntry());
            emitter.emit("lxi H, %s", FALSE_MESSAGE.name());
          } else {
            emitter.addData(TRUE_MESSAGE.dataEntry());
            emitter.addData(FALSE_MESSAGE.dataEntry());
            // compare to 0
            resolver.mov(arg, Register.A);
            // pre-set to false
            emitter.emit("lxi H, %s  ; preset to false string", FALSE_MESSAGE.name());
            emitter.emit("cpi 0x00  ; see if in fact false");
            String falseLabel = Labels.nextLabel("false");
            emitter.emit("jz %s  ; is in fact false", falseLabel); // it's indeed false.
            emitter.emit("lxi H, %s  ; set to true string", TRUE_MESSAGE.name());
            emitter.emitLabel(falseLabel);
          }
          emitter.emit("call 0x11A2  ; print HL (destroys HL, A)");
        } else if (arg.type() == VarType.INT) {
          if (arg.isConstant()) {
            int value = ((ConstantOperand<Integer>) arg).value();
            if (Math.abs(value) < 32768) {
              if (value < 0) {
                // I'm just too lazy.
                fail(op.position(), "Cannot print negative int constant %s of type %s", arg,
                    arg.type());
              } else {
                emitter.emit("lxi H, 0x%04x", value);
              }
            } else {
              fail(op.position(), "Cannot print 32-bit constant int %s", arg);
            }
            emitter.emit("call 0x39D4  ; print the ASCII value of the number in HL (destroys all)");
          } else {
            printInt(arg);
          }
        } else {
          fail(op.position(), "Cannot print %s of type %s", arg, arg.type());
          return;
        }

        if (op.call() == SysCall.Call.PRINTLN || op.call() == SysCall.Call.MESSAGE) {
          // print a CRLF, destroys A
          emitter.emit("call 0x4222");
        }
        resolver.deallocate(op.arg());

        break;

      case INPUT:
        // fall through:

      default:
        fail(op.position(), "Cannot generate %s", op);
        break;
    }
  }

  private void printInt(Operand arg) {
    resolver.mov(arg, Register.BC);
    emitter.emit("; prints the int that BC points at");
    // emit the code that prints the int
    callSubroutine(Name.D_print32);
  }

  @Override
  public void visit(Dec op) {
    if (op.target().type() == VarType.BYTE) {
      resolver.mov(op.target(), Register.M);
      emitter.emit("dcr M");
    } else if (op.target().type() == VarType.INT) {
      resolver.mov(op.target(), Register.BC);
      callSubroutine(Name.D_dec32);
    } else {
      emitter.emit("Cannot decrement %s", op.target().type());
    }
  }

  @Override
  public void visit(Inc op) {
    if (op.target().type() == VarType.BYTE) {
      // maybe do this with B&C?
      resolver.mov(op.target(), Register.M);
      emitter.emit("inr M");
    } else if (op.target().type() == VarType.INT) {
      resolver.mov(op.target(), Register.BC);
      callSubroutine(Name.D_inc32);
    } else {
      emitter.emit("Cannot increment %s", op.target().type());
    }
  }

  @Override
  public void visit(IfOp op) {
    if (op.condition().isConstant()) {
      // should not get here, shrug.
      fail(op.position(), "Do not want to build if constant");
    }
    // op.condition is always a boolean type
    resolver.mov(op.condition(), Register.M);
    emitter.emit("xra A  ; instead of mvi A, 0");
    // compare
    emitter.emit("cmp M  ; compare %s (now in [HL]) against 0", op.condition());
    if (op.isNot()) {
      emitter.emit("jz %s", op.destination());
    } else {
      emitter.emit("jnz %s", op.destination());
    }
    resolver.deallocate(op.condition());
  }

  @Override
  public void visit(BinOp op) {
    // dest = left <op> right
    Operand left = op.left();
    Operand right = op.right();
    Location destination = op.destination();

    if (left.type() == VarType.BOOL) {
      // boolean op
      switch (op.operator()) {
        case AND:
        case OR:
        case XOR:
          generateByteBinOp(destination, left, op.operator(), right);
          break;

        case EQEQ:
        case NEQ:
          generateByteCmp(destination, left, op.operator(), right);
          break;

        default:
          fail(op.position(), "Cannot generate %s yet", op);
          break;
      }
    } else if (left.type() == VarType.BYTE) {
      switch (op.operator()) {
        case BIT_AND:
        case BIT_OR:
        case BIT_XOR:
        case PLUS:
        case MINUS:
          generateByteBinOp(destination, left, op.operator(), right);
          break;

        case MULT:
          generateByteMult(destination, left, right);
          break;

        case DIV:
          generateByteDiv(destination, left, right);
          break;

        case SHIFT_LEFT:
        case SHIFT_RIGHT:
          generateByteShift(destination, left, op.operator(), right);
          break;

        case EQEQ:
        case NEQ:
        case LT:
        case GT:
        case LEQ:
        case GEQ:
          generateByteCmp(destination, left, op.operator(), right);
          break;

        default:
          fail(op.position(), "Cannot generate %s yet", op);
          break;
      }
    } else if (left.type() == VarType.INT) {
      switch (op.operator()) {
        case PLUS:
          generateIntAdd(destination, left, right);
          break;

        case MINUS:
          generateIntSub(destination, left, right);
          break;

        case BIT_AND:
        case BIT_OR:
        case BIT_XOR:
          generateIntBitOp(destination, left, op.operator(), right);
          break;

        case MULT:
          generateIntMult(destination, left, right);
          break;

        case DIV:
          generateIntDiv(destination, left, right);
          break;

        case SHIFT_LEFT:
        case SHIFT_RIGHT:
          generateIntShift(destination, left, op.operator(), right);
          break;

        case EQEQ:
        case NEQ:
        case LT:
        case GT:
        case LEQ:
        case GEQ:
          generateIntCmp(destination, left, op.operator(), right);
          break;

        default:
          fail(op.position(), "Cannot generate %s yet", op);
          break;
      }
    } else {
      fail(op.position(), "Cannot generate %s yet", op);
    }
    resolver.deallocate(left);
    resolver.deallocate(right);
  }

  private void generateIntDiv(Location destination, Operand left, Operand right) {
    // transform dest = left / right
    // into:
    // dest = left
    // dest = dest / right
    emitter.emit("; copy left to dest");
    resolver.mov(left, Register.BC);
    resolver.mov(destination, Register.M);
    callSubroutine(Name.D_copy32);

    // now we need to copy dest to numerator and right to denominator. This is different
    // than usual t100 32-bit routines, but I might like it.
    resolver.mov(destination, "DIV32_PARAM_num");
    resolver.mov(right, "DIV32_PARAM_denom");
    callSubroutine(Name.D_div32);
    resolver.mov("DIV32_RETURN_SLOT", destination);
  }

  private void generateIntBitOp(Location destination, Operand left, TokenType operator,
      Operand right) {
    // transform dest = left op right into
    // dest = left
    // dest = dest op right
    emitter.emit("; copy left to dest");
    resolver.mov(left, Register.BC);
    resolver.mov(destination, Register.M);
    // Copies 4 bytes from BC to HL (from left to destination)
    callSubroutine(Name.D_copy32);
    emitter.emit("; dest=dest&right");
    // do dest=dest(left)&right: BC=BC&HL (dest=dest&hl)
    resolver.mov(destination, Register.BC);
    resolver.mov(right, Register.M);
    callSubroutine(Subroutines.lookupSimple(operator));
  }

  private void generateIntSub(Location destination, Operand left, Operand right) {
    // transform dest = left - right into
    // dest = left
    // dest = dest - right
    emitter.emit("; copy left to dest");
    resolver.mov(left, Register.BC);
    resolver.mov(destination, Register.M);
    // Copies 4 bytes from BC to HL (from left to destination)
    callSubroutine(Name.D_copy32);
    emitter.emit("; dest=dest-right");
    // do dest=dest(left)-right: BC=BC-HL (dest=dest-hl)
    resolver.mov(destination, Register.BC);
    resolver.mov(right, Register.M);
    callSubroutine(Name.D_sub32);
  }

  private void generateIntAdd(Location destination, Operand left, Operand right) {
    // transform dest = left + right into
    // dest = left
    // dest = dest + right
    emitter.emit("; copy left to dest");
    // this fails when "left" is a constant
    resolver.mov(left, Register.BC);
    resolver.mov(destination, Register.M);
    // Copies 4 bytes from BC to HL (from left to destination)
    callSubroutine(Name.D_copy32);
    emitter.emit("; dest=dest+right");
    // do dest=dest(left)+right: BC=BC+HL
    resolver.mov(destination, Register.BC);
    // this fails when "right" is a constant
    resolver.mov(right, Register.M);
    callSubroutine(Name.D_add32);
  }

  private void generateByteDiv(Location destination, Operand left, Operand right) {
    if (left.equals(right)) {
      // done!
      emitter.emit("mvi A, 0x01");
    } else {
      resolver.mov(left, Register.C);
      resolver.mov(right, Register.D);
      // a=c/d
      callSubroutine(Name.D_div8);
    }
    resolver.mov(Register.A, destination);
  }

  private void generateIntMult(Location destination, Operand left, Operand right) {
    // transform dest = left * right
    // into:
    // dest = left
    // dest = dest * right
    emitter.emit("; copy left to dest");
    resolver.mov(left, Register.BC);
    resolver.mov(destination, Register.M);
    // Copies 4 bytes from BC to HL (from left to destination)
    callSubroutine(Name.D_copy32);

    emitter.emit("; dest=dest*right");
    // do dest=dest(left)*right: BC=BC*HL
    resolver.mov(destination, Register.BC);
    resolver.mov(right, Register.M);

    callSubroutine(Name.D_mult32);
  }

  private void generateByteMult(Location destination, Operand left, Operand right) {
    resolver.mov(left, Register.C);
    if (left.equals(right)) {
      emitter.emit("mov D, C");
    } else {
      resolver.mov(right, Register.D);
    }
    // a=c*d
    callSubroutine(Name.D_mult8);
    resolver.mov(Register.A, destination);
  }

  /**
   * Generate a (byte) binary op:
   *
   * <pre>
   *  lda left (or mvi a, left)
   *  [opcode] right
   *  sta dest
   * </pre>
   */
  private void generateByteBinOp(
      Location destination, Operand left, TokenType operation, Operand right) {
    // lda left OR mvi a, left
    resolver.mov(left, Register.A);
    if (right.isConstant()) {
      // TODO: this is broken if right is a param?
      String rightVal = resolver.resolve(right);
      emitter.emit("%s %s", IMMEDIATE_BINARY_OPCODE.get(operation), rightVal);
    } else {
      // put into M first to retrieve from memory
      resolver.mov(right, Register.M);
      emitter.emit("%s M", BINARY_OPCODE.get(operation));
    }
    resolver.mov(Register.A, destination);
  }

  /**
   * Generate a shift left or right. For shift left, always shift in zero. For shift right, always
   * shifts in the high bit of the source.
   */
  private void generateByteShift(
      Location destination, Operand left, TokenType operation, Operand count) {
    // lda left OR mvi a, left
    resolver.mov(left, Register.A);
    // set the count in D
    resolver.mov(count, Register.D);
    if (operation == TokenType.SHIFT_LEFT) {
      callSubroutine(Name.D_shift_left8);
    } else {
      callSubroutine(Name.D_shift_right8);
    }
    resolver.mov(Register.A, destination);
  }

  private void generateIntShift(
      Location destination, Operand left, TokenType operator, Operand right) {
    if (!right.isConstant()) {
      fail(null, "Cannot shift by a non-constant count");
      return;
    }
    if (left.isConstant()) {
      fail(null, "Cannot shift a constant yet");
      return;
    }

    // transform dest = left << right into
    // dest = left
    // dest = dest << right or dest >> right

    emitter.emit("; copy left to dest");
    resolver.mov(left, Register.BC);
    resolver.mov(destination, Register.M);
    // Copies 4 bytes from BC to HL (from left to destination)
    callSubroutine(Name.D_copy32);

    // repeat the shift "D times."
    ConstantOperand<Integer> rightConstant = (ConstantOperand<Integer>) right;
    int count = rightConstant.value() % 32;
    String loopLabel = null;
    emitter.emit("; dest=dest %s right (in D)", operator);
    if (count != 1) {
      resolver.mov(ConstantOperand.of((byte) count), Register.D);
      loopLabel = Labels.nextLabel("shift_loop");
      emitter.emitLabel(loopLabel);
    }
    emitter.emit("stc");
    emitter.emit("cmc  ; clear carry");
    if (operator == TokenType.SHIFT_LEFT) {
      callSubroutine(Name.D_shift_left32);
    } else {
      callSubroutine(Name.D_shift_right32);
    }
    if (count != 1) {
      emitter.emit("dcr D");
      emitter.emit("jnz %s", loopLabel);
    }

    return;
  }

  /** Generate a compare for a byte or bool. */
  private void generateByteCmp(
      Location destination, Operand left, TokenType operation, Operand right) {
    // lda left OR mvi a, left
    resolver.mov(left, Register.A);
    if (right.isConstant()) {
      // TODO: this will fail if right is a param or local
      emitter.emit("cpi %s", resolver.resolve(right));
    } else {
      // put into M first to retrieve "right" from memory
      resolver.mov(right, Register.M);
      emitter.emit("cmp M");
    }
    String continueLabel = Labels.nextLabel("continue_compare");
    /*
     * If A less than 8-bit data, the CY flag is set AND Zero flag is reset. < jc AND jz If A equals
     * to 8-bit data, the Zero flag is set AND CY flag is reset. == jz If A greater than 8-bit data,
     * the CY AND Zero flag are reset. > jnc AND jz
     */
    // * 1. set first
    emitter.emit(COMPARISON_PRESET.get(operation));
    // * 2. do first test
    emitter.emit(COMPARISON_OPCODE.get(operation) + " %s", continueLabel);
    // * 3. do second test
    if (COMPARISON_OPCODE2.get(operation) != null) {
      emitter.emit(COMPARISON_OPCODE2.get(operation) + " %s", continueLabel);
    }
    // * 4. set second
    emitter.emit(COMPARISON_POSTSET.get(operation));
    // * 5: continue label
    emitter.emitLabel(continueLabel);
    resolver.mov(Register.A, destination);
  }

  private void generateIntCmp(
      Location destination, Operand left, TokenType operation, Operand right) {

    // Compare BC and H
    resolver.mov(left, Register.BC);
    resolver.mov(right, Register.M);
    callSubroutine(Name.D_comp32);
    String continueLabel = Labels.nextLabel("continue_compare");
    /*
     * If A less than 8-bit data, the CY flag is set AND Zero flag is reset. < jc AND jz If A equals
     * to 8-bit data, the Zero flag is set AND CY flag is reset. == jz If A greater than 8-bit data,
     * the CY AND Zero flag are reset. > jnc AND jz
     */

    // * 1. set first
    emitter.emit(COMPARISON_PRESET.get(operation));
    // * 2. do first test
    emitter.emit(COMPARISON_OPCODE.get(operation) + " %s", continueLabel);
    // * 3. do second test
    if (COMPARISON_OPCODE2.get(operation) != null) {
      emitter.emit(COMPARISON_OPCODE2.get(operation) + " %s", continueLabel);
    }
    // * 4. set second
    emitter.emit(COMPARISON_POSTSET.get(operation));
    // * 5: continue label
    emitter.emitLabel(continueLabel);
    resolver.mov(Register.A, destination);
  }

  @Override
  public void visit(UnaryOp op) {
    // dest = <op> operand
    Location destination = op.destination();
    Operand operand = op.operand();

    switch (op.operator()) {
      case MINUS:
        if (operand.type() == VarType.BYTE) {
          //  0-a
          if (operand.isConstant()) {
            // hard-coded negation
            ConstantOperand<Byte> byteOp = (ConstantOperand<Byte>) operand;
            byte newValue = (byte) (0 - byteOp.value());
            emitter.emit("mvi A, 0x%02x", newValue);
          } else {
            // negate via twos-complement
            resolver.mov(operand, Register.A);
            emitter.emit("cma  ; negate A");
            emitter.emit("inr A");
          }
          // store 0-operand
          resolver.mov(Register.A, destination);
        } else if (operand.type() == VarType.INT) {
          if (operand.isConstant()) {
            // hard-coded negation
            ConstantOperand<Integer> integerOp = (ConstantOperand<Integer>) operand;
            ConstantOperand<Integer> neg = ConstantOperand.of(-integerOp.value());
            // will this work?
            resolver.mov(neg, destination);
          } else {
            // set destination to 0, then do dest-operand
            emitter.emit("lxi H, 0x0000");
            String destName = resolver.resolve(destination);
            emitter.emit("shld %s ; store low word (LSByte first)", destName);
            emitter.emit("shld %s + 0x02  ; store high word", destName);
            // sub = bc=bc-hl
            resolver.mov(op.operand(), Register.M);
            resolver.mov(op.destination(), Register.BC);
            callSubroutine(Name.D_sub32);
          }
        } else {
          fail(op.position(), "Cannot generate %s yet", op);
        }
        break;

      case BIT_NOT:
        if (operand.type() == VarType.BYTE) {
          if (operand.isConstant()) {
            // hard-coded bit not
            ConstantOperand<Byte> byteOp = (ConstantOperand<Byte>) operand;
            byte newValue = (byte) (~byteOp.value());
            emitter.emit("mvi A, 0x%02x", newValue);
          } else {
            resolver.mov(operand, Register.A);
            emitter.emit("cma  ; bit not A");
          }
          // store 1-operand
          resolver.mov(Register.A, destination);
        } else {
          // m=source
          resolver.mov(op.operand(), Register.M);
          // bc=dest
          resolver.mov(op.destination(), Register.BC);
          // bc=~hl
          callSubroutine(Name.D_bitnot32);
        }
        break;

      case NOT:
        if (operand.type() == VarType.BOOL) {
          // not a
          if (operand.isConstant()) {
            // hard-coded negation
            if (operand.equals(ConstantOperand.TRUE)) {
              emitter.emit("mvi A, 0x00");
            } else {
              emitter.emit("mvi A, 0x01");
            }
          } else {
            resolver.mov(operand, Register.A);
            // toggle the low bit via xor
            emitter.emit("xri 0x01  ; not A");
          }
          resolver.mov(Register.A, destination);
        }
        break;

      default:
        fail(op.position(), "Cannot generate %s yet", op);
        break;
    }
    resolver.deallocate(op.operand());
  }

  @Override
  public void visit(ProcEntry op) {
    emitter.emit("; entering %s", op.name());
  }

  @Override
  public void visit(ProcExit op) {
    emitter.emitLabel(String.format("__exit_of_%s", op.procName()));
    emitter.emit("ret");
  }

  @Override
  public void visit(Return op) {
    op.returnValueLocation().ifPresent(source -> {
      // Copy the source to the return slot.
      String name = T100Locations.returnSlot(op.procName());
      resolver.mov(source, name);
      resolver.deallocate(source);
    });
    emitter.emit("jmp __exit_of_%s", op.procName());
  }

  @Override
  public void visit(Call op) {
    emitter.emit("; set arguments");
    for (int i = 0; i < op.actuals().size(); ++i) {
      Location formal = op.formals().get(i);
      Operand actual = op.actuals().get(i);
      resolver.mov(actual, formal);
      resolver.deallocate(actual);
    }
    List<PseudoReg> temps = resolver.temps();
    for (PseudoReg preg : temps) {
      if (preg.type().size() == 1) {
        emitter.emit("lda %s", preg.name());
        emitter.emit("push PSW");
      } else {
        for (int i = 0; i < preg.type().size(); i += 2) {
          if (i > 0) {
            emitter.emit("lhld %s + 0x%02x", preg.name(), i);
          } else {
            emitter.emit("lhld %s", preg.name());
          }
          emitter.emit("push H");
        }
      }
    }
    emitter.emit("call _%s", op.procSym().name());
    for (PseudoReg preg : temps) {
      if (preg.type().size() == 1) {
        emitter.emit("pop PSW");
        emitter.emit("sta %s", preg.name());
      } else {
        for (int i = preg.type().size() - 2; i >= 0; i -= 2) {
          emitter.emit("pop H");
          if (i > 0) {
            emitter.emit("shld %s + 0x%02x", preg.name(), i);
          } else {
            emitter.emit("shld %s", preg.name());
          }
        }
      }
    }
    op.destination().ifPresent(destination -> {
      // Copy the return slot to the destination.
      String name = T100Locations.returnSlot(op.procSym().name());
      resolver.mov(name, destination);
    });
  }
}
