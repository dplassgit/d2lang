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
    emit0("; nasm -fwin64 -Ox %s.asm && gcc %s.obj -o %s && ./%s", f, f, f, f);

    emit0("global main"); // required
    emit0("extern printf"); // optional
    emit0("extern strlen"); // optional
    emit0("extern exit"); // required

    // Probably what we should do is:
    // 1. emit all globals OK
    // 2. emit all string constants not done - if string constants are propagated, ???
    // maybe we shouldn't constant propagate string constants except if it's "foo"[3]
    // 3. emit all array constants (?)
    emit0("\nsection .data");
    SymTab globals = input.symbolTable();
    for (Map.Entry<String, Symbol> entry : globals.entries().entrySet()) {
      Symbol symbol = entry.getValue();
      if (symbol.storage() == SymbolStorage.GLOBAL) {
        // temporarily reserve (& clear) 1 byte for bool, 4 bytes per int, 8 bytes for string
        if (symbol.varType() == VarType.INT) {
          emit("%s: dd 0", entry.getKey());
        } else if (symbol.varType() == VarType.BOOL) {
          emit("%s: db 0", entry.getKey());
        } else if (symbol.varType() == VarType.STRING) {
          emit("%s: dq 0", entry.getKey());
        }
      }
    }

    // TODO: only emit these if we need to. Probably can do this in the StringFinder
    emit("__PRINTF_NUMBER_FMT: db \"%%d\", 0");
    emit("__TRUE: db \"true\", 0");
    emit("__FALSE: db \"false\", 0");
    emit("__EXIT_MSG: db \"ERROR: \", 0");
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
      emit("mov %s, [%s]", tempName, sourceLoc);
      emit("mov [%s], %s", destLoc, tempName);
      registers.deallocate(tempReg);
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
      deallocate(source);
    }
  }

  @Override
  public void visit(SysCall op) {
    Operand arg = op.arg();
    switch (op.call()) {
      case PRINT:
        String argVal = resolve(arg);
        emit("sub RSP, 0x28         ; Reserve the shadow space");
        boolean pushedRcx = condPush(Register.RCX);
        boolean pushedRdx = false;
        if (arg.type() == VarType.INT) {
          pushedRdx = condPush(Register.RDX);
          emit("mov RCX, __PRINTF_NUMBER_FMT ; First argument is address of message");
          emit("mov RDX, %s           ; Second argument is parameter", argVal);
          emit("call printf           ; printf(message)");
          emit("add RSP, 0x28         ; Remove shadow space");
        } else if (arg.type() == VarType.BOOL) {
          if (argVal.equals("1")) {
            emit("mov RCX, __TRUE");
          } else if (argVal.equals("0")) {
            emit("mov RCX, __FALSE");
          } else {
            // translate from 0/1 to false/true
            emit("mov RCX, __FALSE");
            emit("cmp byte %s, 1", argVal);
            pushedRdx = condPush(Register.RDX);
            emit("lea RDX, __TRUE");
            emit("cmovz RCX, RDX");
          }
          emit("call printf           ; printf(message)");
          emit("add RSP, 0x28         ; Remove shadow space");
        } else if (arg.type() == VarType.STRING) {
          // String, hopefully.
          emit("mov RCX, %s ; First argument is address of message", argVal);
          emit("call printf           ; printf(message)");
          emit("add RSP, 0x28         ; Remove shadow space");
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

  private void deallocate(Operand operand) {
    if (operand.storage() == SymbolStorage.TEMP) {
      // now that we used the temp, unallocate it (?)
      String operandName = operand.toString();
      Register reg = aliases.get(operandName);
      if (reg != null) {
        emit("; Deallocating %s from %s", operand, reg);
        aliases.remove(operandName);
        registers.deallocate(reg);
      }
    }
  }

  @Override
  public void visit(BinOp op) {
    VarType type = op.destination().type();
    String size = dataSize(type);

    // 1. get left
    String leftName = resolve(op.left());
    // 2. get right
    String rightName = resolve(op.right());

    if (op.operator() == TokenType.DIV || op.operator() == TokenType.MOD) {
      generateDivMod(op, leftName, rightName);
      return;
    }
    // 3. determine dest location
    String destName = resolve(op.destination());
    // 4. mov dest, left
    emit("mov %s %s, %s ; binary setup", size, destName, leftName);

    // 5. {op] dest, right
    switch (op.operator()) {
      case PLUS:
        emit("add %s, %s ; binary %s", destName, rightName, op.operator());
        break;
      case MINUS:
        emit("sub %s, %s ; binary %s", destName, rightName, op.operator());
        break;
      case MULT:
        emit("imul %s, %s ; binary %s", destName, rightName, op.operator());
        break;

      case SHIFT_LEFT:
        {
          boolean used = registers.isAllocated(Register.RCX);
          if (used) {
            emit("push RCX");
          }
          // this is a problem if rightname is CL
          emit("mov CL, %s ; shift left prep", rightName);
          // this is a problem if dest is CL
          emit("shl %s, CL ; binary %s", destName, op.operator());
          if (used) {
            // this is a problem if dest is CL
            emit("pop RCX");
          }
        }
        break;
      case SHIFT_RIGHT:
        {
          boolean used = registers.isAllocated(Register.RCX);
          if (used) {
            emit("push RCX");
          }
          emit("mov CL, %s ; shift left prep", rightName);
          emit("sar %s, CL ; binary %s", destName, op.operator());
          if (used) {
            emit("pop RCX");
          }
        }
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
        // TODO: THIS IS WRONG. it's not comparing with the right size - it's using the
        // *destination* size instead of the *source* size.
        emit("cmp %s, %s ; binary %s", destName, rightName, op.operator());
        emit("setz %s", destName);
        break;
      case NEQ:
        emit("cmp %s, %s ; binary %s", destName, rightName, op.operator());
        emit("setnz %s", destName);
        break;
      case GT:
        emit("cmp %s, %s ; binary %s", destName, rightName, op.operator());
        emit("setg %s", destName);
        break;
      case GEQ:
        emit("cmp %s, %s ; binary %s", destName, rightName, op.operator());
        emit("setge %s", destName);
        break;
      case LT:
        emit("cmp %s, %s ; binary %s", destName, rightName, op.operator());
        emit("setl %s", destName);
        break;
      case LEQ:
        emit("cmp %s, %s ; binary %s", destName, rightName, op.operator());
        emit("setle %s", destName);
        break;

      default:
        fail("Cannot generate %s yet", op);
    }
    deallocate(op.left());
    deallocate(op.right());
  }

  private void generateDivMod(BinOp op, String leftName, String rightName) {
    // 3. determine dest location
    String destName = resolve(op.destination());
    // 4. set up left in EDX:EAX
    // 5. idiv by right, result in eax
    // 6. mov destName, eax
    boolean raxUsed = condPush(Register.RAX);
    boolean rdxUsed = condPush(Register.RDX);
    registers.reserve(Register.RAX);
    registers.reserve(Register.RDX);
    Register temp = registers.allocate();
    emit("mov %s, %s ; right", temp.name32, rightName);
    if (!leftName.equals(Register.RAX.name32)) {
      emit("mov EAX, %s ; left", leftName);
    }
    // sign extend eax to edx
    emit("cdq ; sign extend eax to edx");
    emit("idiv %s ; %s / %s", temp.name32, leftName, rightName);
    registers.deallocate(temp);
    if (!rdxUsed) {
      registers.deallocate(Register.RDX);
    }
    if (!raxUsed) {
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
      condPop(Register.RDX, rdxUsed);
    } else {
      // pseudo pop
      emit("add RSP, 8");
    }
    if (!destName.equals(Register.RAX.name32)) {
      condPop(Register.RAX, raxUsed);
    } else {
      // pseudo pop
      emit("add RSP, 8");
    }
    deallocate(op.left());
    deallocate(op.right());
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

  @Override
  public void visit(UnaryOp op) {
    // 1. get source location name
    String sourceName = resolve(op.operand());
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
        boolean pushedRcx = condPush(Register.RCX);
        boolean pushedRax = condPush(Register.RAX);
        emit("sub RSP, 0x28         ; Reserve the shadow space");
        emit("mov RCX, %s    ; argument is address of string", sourceName);
        emit("call strlen    ; strlen(message)");
        emit("add RSP, 0x28  ; Remove shadow space");
        if (destName.equals(Register.RAX.name32)) {
          // pseudo pop; eax already has the length.
          emit("add RSP, 8");
        } else {
          // NOTE: eax not rax, because lengths are always ints (32 bits)
          emit("mov %s, EAX    ; %s = length(%s)", destName, destName, sourceName);
          condPop(Register.RAX, pushedRax);
        }

        if (destName.equals(Register.RCX.name32)) {
          // pseudo pop
          emit("add RSP, 8");
        } else {
          condPop(Register.RCX, pushedRcx);
        }
        break;
      case ASC:
      case CHR:
      default:
        fail("Cannot generate %s yet", op);
        break;
    }
  }

  private void fail(String format, Object... values) {
    throw new UnsupportedOperationException(String.format(format, values));
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
        if (location.type() == VarType.BOOL || location.type() == VarType.INT) {
          return "[" + location.name() + "]";
        } else {
          // string or record or array
          return location.name();
        }
      default:
        fail("Cannot generate %s operand %s yet", location.storage(), location);
        return null;
    }
  }

  // Emit at column 0
  private void emit0(String format, Object... values) {
    asm.add(String.format(format, values));
    logger.atFine().log(String.format(format, values));
  }

  // Emit at column 2
  private void emit(String format, Object... values) {
    emit0("  " + format, values);
  }

  private void emit(Op op) {
    emit0("\n  ; %s", op.toString());
  }
}
