package com.plasstech.lang.d2.codegen.x64;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.plasstech.lang.d2.codegen.Emitter;
import com.plasstech.lang.d2.codegen.Labels;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.il.Call;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.x64.Resolver.ResolvedOperand;
import com.plasstech.lang.d2.type.ProcSymbol;
import com.plasstech.lang.d2.type.VarType;

/** Generates nasm code for calling a procedure. Figures out how to best map actuals to formals. */
class CallCodeGenerator extends DefaultOpcodeVisitor {

  private final Emitter emitter;
  private final Resolver resolver;

  public CallCodeGenerator(Resolver resolver, Emitter emitter) {
    this.resolver = resolver;
    this.emitter = emitter;
  }

  @Override
  public void visit(Call op) {
    RegisterState registerState =
        RegisterState.condPush(emitter, resolver, Register.VOLATILE_REGISTERS);

    if (op.actuals().size() > 0) {
      /**
       * We ONLY have to do gymnastics when a param register needs to be copied to a LATER param
       * register, e.g., RCX needs to be in RDX or RDX needs to be in R9
       */
      Map<Register, Register> sourceToAlias = new HashMap<>();
      for (int i = 0; i < Math.min(op.actuals().size(), 4); ++i) {
        Location formalLocation = op.formals().get(i);
        Register reg = Register.paramRegister(formalLocation.type(), i);
        for (int j = i + 1; j < Math.min(op.actuals().size(), 4); ++j) {
          Operand actual = op.actuals().get(j);
          ResolvedOperand resolved = resolver.resolveFully(actual);
          if (reg == resolved.register()) {
            // it means we're being copied to a later register.
            Register alias = resolver.allocate(formalLocation.type());
            sourceToAlias.put(reg, alias);
            emitter.emit("; aliasing %s to %s", reg, alias);
            resolver.mov(formalLocation.type(), reg, alias);
            break;
          }
        }
      }

      if (sourceToAlias.isEmpty()) {
        // Simple case, no conflicts or all locals.
        emitter.emit("; no sources conflict with dests, doing simple version");
        // Push from right to left.
        for (int index = op.actuals().size() - 1; index >= 0; index--) {
          Operand actual = op.actuals().get(index);
          ResolvedOperand actualOp = resolver.resolveFully(actual);
          if (index <= 3) {
            // write into register.
            Location formal = op.formals().get(index);
            String formalLocation = resolver.resolve(formal);
            if (formalLocation.equals(actualOp.name())) {
              emitter.emit(
                  "; parameter #%d (formal %s) already in %s (%s)",
                  index, formal.name(), actualOp.name(), actual);
            } else {
              resolver.mov(actual, formal);
            }
          } else {
            // Push it, push it good.
            resolver.push(actualOp);
          }
        }
      } else {
        emitter.emit("; at least one conflict; doing complicated case");
        if (op.actuals().size() > 3) {
          // Push from right to left.
          for (int i = op.actuals().size() - 1; i >= 4; i--) {
            Operand actual = op.actuals().get(i);
            ResolvedOperand actualOp = resolver.resolveFully(actual);
            resolver.push(actualOp);
          }
        }
        for (int i = 0; i < Math.min(op.actuals().size(), 4); ++i) {
          Operand actual = op.actuals().get(i);
          VarType type = actual.type();
          Register sourceReg = resolver.toRegister(actual);
          if (sourceReg != null) {
            // it's in a register. See if it's aliased
            Register alias = sourceToAlias.get(sourceReg);
            if (alias != null) {
              // overwrite; the original register may not be in the aliases, which is fine.
              sourceReg = alias;
            }
          }
          Register formalReg = Register.paramRegister(type, i);
          if (sourceReg != null) {
            if (formalReg != sourceReg) {
              // it's not already in this register.
              resolver.mov(type, sourceReg, formalReg);
            } else {
              emitter.emit("; %dth param already in %s", i, sourceReg);
            }
          } else {
            resolver.mov(actual, formalReg);
          }
        }

        for (Entry<Register, Register> pair : sourceToAlias.entrySet()) {
          if (pair.getKey() != pair.getValue() && resolver.isAllocated(pair.getValue())) {
            emitter.emit("; deallocating alias %s", pair.getValue());
            resolver.deallocate(pair.getValue());
          }
        }
      }
    }

    ProcSymbol procSym = op.procSym();
    if (procSym.isExtern()) {
      String alignedLabel = Labels.nextLabel("aligned");
      String afterLabel = Labels.nextLabel("afterExternCall");
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
      emitter.emit("call %s", procSym.mungedName());
    }
    int numArgs = op.actuals().size();
    if (numArgs > 3) {
      // # of bytes we have to adjust the stack (pseudo-pop)
      int bytes = 8 * (numArgs - 4);
      emitter.emit("add RSP, %d  ; adjust for 5th-nth parameters pushed into stack", bytes);
    }
    Register tempReg = null;
    if (op.destination().isPresent()) {
      Location destination = op.destination().get();
      Register returnReg = Registers.returnRegister(destination.type());
      if (resolver.isAllocated(returnReg)) {
        // it will be overwritten. let's stash it in a temp reg
        tempReg = resolver.allocate(destination.type());
        emitter.emit("; temporarily putting %s in %s", returnReg, tempReg);
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
}
