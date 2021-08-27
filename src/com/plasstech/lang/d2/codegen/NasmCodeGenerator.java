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

  @Override
  public State execute(State input) {
    ImmutableList<Op> code = input.lastIlCode();
    String f = "dcode";
    if (input.filename() != null) {
      f = input.filename();
    }

    emit0("; To execute:");
    // -Ox = optimize
    emit0("; nasm -fwin64 -Ox %s.asm && gcc %s.obj -o %s && ./%s", f, f, f, f);

    emit0("%%use altreg"); // lets us use r01 instead of rax, etc.
    emit0("global main"); // required
    emit0("extern puts");
    emit0("extern printf");
    emit0("extern exit");

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
    // TODO: only emit these if we need to.
    emit("__PRINTF_NUMBER_FMT: db \"%%d\", 0");
    emit("__TRUE: db \"true\", 0");
    emit("__FALSE: db \"false\", 0");
    emit("__EXIT_MSG: db \"ERROR: \", 0");

    emit0("\nsection .text");

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
    emit("mov rcx, %d", op.exitCode());
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
    String destLoc = resolveDestination(destination);
    String size = dataSize(op.source().type());
    if (source.isConstant()) {
      // TODO: fails for strings.
      emit("mov %s %s, %s", size, destLoc, sourceLoc);
    } else {
      // TODO: deal with out-of-registers
      Register tempReg = registers.allocate();
      String suffix = registerSuffix(op.source().type());
      String tempName = tempReg.name() + suffix;
      // TODO: This can be optimized if source and dest are both registers.
      emit("mov %s %s, %s", size, tempName, sourceLoc);
      emit("mov %s %s, %s", size, destLoc, tempName);
      registers.deallocate(tempReg);
      deallocate(source);
    }
  }

  @Override
  public void visit(SysCall op) {
    Operand arg = op.arg();
    switch (op.call()) {
      case PRINT:
        String argVal = resolve(arg);
        emit("sub rsp, 0x28         ; Reserve the shadow space");
        boolean pushedRcx = condPush(Registers.RCX);
        boolean pushedRdx = false;
        if (arg.type() == VarType.INT) {
          pushedRdx = condPush(Registers.RDX);
          emit("mov rcx, __PRINTF_NUMBER_FMT ; First argument is address of message");
          emit("mov rdx, %s           ; Second argument is parameter", argVal);
          emit("call printf           ; printf(message)");
          emit("add rsp, 0x28         ; Remove shadow space");
        } else if (arg.type() == VarType.BOOL) {
          if (argVal.equals("1")) {
            emit("mov rcx, __TRUE");
          } else if (argVal.equals("0")) {
            emit("mov rcx, __FALSE");
          } else {
            // translate from 0/1 to false/true
            emit("mov rcx, __FALSE");
            emit("cmp byte %s, 1", argVal);
            pushedRdx = condPush(Registers.RDX);
            emit("lea rdx, __TRUE");
            emit("cmovz rcx, rdx");
          }
          emit("call printf           ; printf(message)");
          emit("add rsp, 0x28         ; Remove shadow space");
        } else {
          fail("Cannot print string yet: %s", arg);
        }
        condPop(Registers.RDX, pushedRdx);
        condPop(Registers.RCX, pushedRcx);
        break;
      default:
        fail("Cannot generate %s yet", op);
        break;
    }
  }

  /** Conditionally push the register, if it's already in use. */
  private boolean condPush(Register reg) {
    if (registers.isAllocated(reg)) {
      emit("push %s", reg);
      return true;
    }
    return false;
  }

  /** Conditionally pop the register, if it's been pushed. */
  private void condPop(Register reg, boolean pushed) {
    if (pushed) {
      emit("pop %s", reg);
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
        emit("; Deallocating %s (at %s)", operand, reg);
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

    if (op.operator() == TokenType.DIV) {
      // 3. determine dest location
      String destName = resolveDestination(op.destination());
      // 4. set up left in EDX:EAX
      // 5. idiv by right, result in eax
      // 6. mov destName, eax
      boolean raxUsed = condPush(Registers.RAX);
      boolean rdxUsed = condPush(Registers.RDX);
      //      if (op.right().isConstant()) {
      registers.reserve(Registers.RAX);
      registers.reserve(Registers.RDX);
      Register temp = registers.allocate();
      emit("mov %sd, %s", temp.name(), rightName);
      emit("mov eax, %s", leftName);
      // sign extend eax to edx
      emit("cdq");
      emit("idiv %sd", temp.name());
      registers.deallocate(temp);
      if (!rdxUsed) {
        registers.deallocate(Registers.RDX);
      }
      if (!raxUsed) {
        registers.deallocate(Registers.RAX);
      }
      // TODO: this might not be required if it's already in eax
      emit("mov %s, eax", destName);
      if (!destName.equals("R2d")) {
        condPop(Registers.RDX, rdxUsed);
      } else {
        // pseudo pop
        emit("add rsp, 8");
      }
      if (!destName.equals("R0d")) {
        condPop(Registers.RAX, raxUsed);
      } else {
        // pseudo pop
        emit("add rsp, 8");
      }
      deallocate(op.left());
      deallocate(op.right());
      return;
    }
    // 3. determine dest location
    String destName = resolveDestination(op.destination());
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

  private String dataSize(VarType type) {
    String size = "";
    if (type == VarType.INT) {
      size = "dword";
    } else if (type == VarType.BOOL) {
      size = "byte";
    }
    return size;
  }

  private String resolveDestination(Location destination) {
    Register reg = this.aliases.get(destination.name());
    if (reg != null) {
      return reg.toString();
    }
    switch (destination.storage()) {
      case TEMP:
        reg = registers.allocate();
        //        if (reg == null) {
        //          // deal with out-of-registers situation
        //          // pick the least recently used one (say, the first one)
        //          reg = lruRegs.remove(0);
        //          // don't have to deallocate, since we're just re-using the register out from
        // underneath
        //          // the registers object.
        //          // TODO: push the value onto the stack
        //          // remove the alias too.
        //          aliases.inverse().remove(reg);
        //        }
        //        lruRegs.add(reg); // add to the end.
        aliases.put(destination.name(), reg);
        emit("; Allocating %s to %s", destination, reg);
        return reg.name() + registerSuffix(destination.type());
      case GLOBAL:
        return "[" + destination.name() + "]";
      default:
        fail("Cannot generate %s destination %s yet", destination.storage(), destination);
        return null;
    }
  }

  private static String registerSuffix(VarType type) {
    String suffix = "";
    if (type == VarType.INT) {
      suffix = "d";
    } else if (type == VarType.BOOL) {
      suffix = "b";
    }
    return suffix;
  }

  @Override
  public void visit(UnaryOp op) {
    // 1. get source location name
    String sourceName = resolve(op.operand());
    // 2. apply op
    // 3. store in destination
    Location destination = op.destination();
    String destName = resolveDestination(destination);
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
        emit("test %s, %s", sourceName, sourceName);
        // 2. setz %s
        // TODO: this will fail for non-registers
        emit("setz %s", destName);
        break;
      case MINUS:
        emit("mov dword %s, %s  ; unary setup", destName, sourceName);
        emit("neg %s  ; unary negation", destName);
        break;
      case LENGTH:
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
    if (operand.storage() == SymbolStorage.IMMEDIATE) {
      if (operand.type() == VarType.INT) {
        return operand.toString();
      } else if (operand.type() == VarType.BOOL) {
        ConstantOperand<Boolean> boolConst = (ConstantOperand<Boolean>) operand;
        if (boolConst.value() == Boolean.TRUE) {
          return "1";
        }
        return "0";
      }
      // TODO: fails for string constants.
    }

    Register sourceReg = aliases.get(operand.toString());
    if (sourceReg != null) {
      return sourceReg.name() + registerSuffix(operand.type());
    }
    switch (operand.storage()) {
      case GLOBAL:
        return "[" + operand.toString() + "]";
      default:
        fail("Cannot generate %s operand %s yet", operand.storage(), operand);
        return operand.toString();
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
