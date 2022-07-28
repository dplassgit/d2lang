package com.plasstech.lang.d2.codegen;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.plasstech.lang.d2.codegen.il.Call;
import com.plasstech.lang.d2.type.VarType;

/** Generates nasm code for calling a procedure. Figures out how to best map actuals to formals. */
class CallGenerator {

  private final Emitter emitter;
  private final Resolver resolver;

  public CallGenerator(Resolver resolver, Emitter emitter) {
    this.resolver = resolver;
    this.emitter = emitter;
  }

  /** Generate nasm code for the given call */
  public void generate(Call op) {
    /**
     * We ONLY have to do gymnastics when a param register needs to be copied to a LATER param
     * register, e.g., RCX needs to be in RDX or RDX needs to be in R9
     */
    Map<Register, Register> sourceToAlias = new HashMap<>();
    for (int i = 0; i < Math.min(op.actuals().size(), Register.INT_PARAM_REGISTERS.size()); ++i) {
      Register reg = Register.INT_PARAM_REGISTERS.get(i);
      for (int j = i + 1; j < op.actuals().size(); ++j) {
        // this allocates a register to it, but... it shouldn't be needed here
        resolver.resolve(op.actuals().get(j));
        Register sourceReg = resolver.toRegister(op.actuals().get(j));
        if (reg == sourceReg) {
          // it means we're being copied to a later register.
          Register alias = resolver.allocate(VarType.INT);
          sourceToAlias.put(reg, alias);
          emitter.emit("mov %s, %s  ; stash in alias", alias, reg);
          break;
        }
      }
    }

    if (sourceToAlias.isEmpty()) {
      // simple case, no conflicts or all locals.
      emitter.emit("; no sources conflict with dests, doing simple version");
      int index = 0;
      for (Operand actual : op.actuals()) {
        Location formal = op.formals().get(index);
        String formalLocation = resolver.resolve(formal);
        String actualLocation = resolver.resolve(actual);
        if (formalLocation.equals(actualLocation)) {
          emitter.emit(
              "; parameter #%d (formal %s) already in %s (%s)",
              index, formal.name(), actualLocation, actual);
        } else {
          Size size = Size.of(formal.type());
          emitter.emit("mov %s %s, %s", size, formalLocation, actualLocation);
        }
        index++;
        resolver.deallocate(actual);
      }
    } else {
      emitter.emit("; at least one conflict; doing complicated case");
      for (int i = 0; i < op.actuals().size(); ++i) {
        Operand actual = op.actuals().get(i);
        VarType type = actual.type();
        Size size = Size.of(actual.type());
        String source = resolver.resolve(actual);
        Register sourceReg = resolver.toRegister(actual);
        if (sourceReg != null) {
          // it's in a register. See if it's aliased
          Register alias = sourceToAlias.get(sourceReg);
          if (alias != null) {
            // overwrite; the original register may not be in the aliases, which is fine.
            sourceReg = alias;
          }
          source = sourceReg.sizeByType(actual.type());
        }
        Register formalReg = Register.INT_PARAM_REGISTERS.get(i);
        if (formalReg != sourceReg) {
          // it's not already in this register.
          emitter.emit(
              "mov %s %s, %s  ; set %dth param",
              size.asmType, formalReg.sizeByType(type), source, i);
        } else {
          emitter.emit("; %dth param already in %s", i, sourceReg);
        }
      }

      for (Entry<Register, Register> pair : sourceToAlias.entrySet()) {
        if (pair.getKey() != pair.getValue() && resolver.isAllocated(pair.getValue())) {
          emitter.emit("; deallocating alias %s", pair.getValue());
          resolver.deallocate(pair.getValue());
        }
      }
    }
    for (int i = 0; i < op.actuals().size(); ++i) {
      Operand actual = op.actuals().get(i);
      resolver.deallocate(actual);
    }
  }
}
