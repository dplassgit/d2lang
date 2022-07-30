package com.plasstech.lang.d2.codegen;

import java.util.HashMap;
import java.util.Map;

import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VarType;

/**
 * Resolves temp and other variables and keeps track if they're in registers or not. TODO: rename
 * this to something better.
 */
public class Resolver implements RegistersInterface {
  // map from name to register
  private final Map<String, Register> aliases = new HashMap<>();
  private final Registers registers;
  private final StringTable stringTable;
  private final DoubleTable doubleTable;
  private final Emitter emitter;

  public Resolver(
      Registers registers, StringTable stringTable, DoubleTable doubleTable, Emitter emitter) {
    this.registers = registers;
    this.stringTable = stringTable;
    this.doubleTable = doubleTable;
    this.emitter = emitter;
  }

  /**
   * Given an operand returns a string representation of where it can be accessed. It also allocates
   * a register for a temp if it's not already allocated.
   */
  public String resolve(Operand operand) {
    if (operand.isConstant()) {
      if (operand.type() == VarType.INT || operand.type() == VarType.BYTE) {
        return operand.toString();
      } else if (operand.type() == VarType.BOOL) {
        ConstantOperand<Boolean> boolConst = (ConstantOperand<Boolean>) operand;
        if (boolConst.value().booleanValue()) {
          return "1";
        }
        return "0";
      } else if (operand.type() == VarType.STRING) {
        // look it up in the string table.
        ConstantOperand<String> stringConst = (ConstantOperand<String>) operand;
        ConstEntry<String> entry = stringTable.lookup(stringConst.value());
        return entry.name();
      } else if (operand.type() == VarType.DOUBLE) {
        // look it up in the double table.
        ConstantOperand<Double> doubleConst = (ConstantOperand<Double>) operand;
        ConstEntry<Double> entry = doubleTable.lookup(doubleConst.value());
        return String.format("[%s]", entry.name());
      } else if (operand.type().isNull()) {
        return "0";
      }

      emitter.fail("Cannot generate %s constant %s yet", operand.type().name(), operand);
      return null;
    }

    Location location = (Location) operand;
    // maybe look up the location in the symbol table?
    if (location.isRegister()) {
      Register reg = ((RegisterLocation) location).register();
      return reg.sizeByType(location.type());
    }
    Register reg = aliases.get(location.toString());
    if (reg != null) {
      // Found it in a register.
      return reg.sizeByType(location.type());
    }
    switch (location.storage()) {
      case TEMP:
        // TODO: deal with out-of-registers
        reg = registers.allocate(location.type());
        aliases.put(location.name(), reg);
        emitter.emit("; Allocating %s to %s", location, reg);
        return reg.sizeByType(location.type());
      case GLOBAL:
        return "[_" + location.name() + "]";
      case PARAM:
        ParamLocation paramLoc = (ParamLocation) location;
        return generateParamLocationName(paramLoc.index(), location.type());
      case LOCAL:
        StackLocation stackLoc = (StackLocation) location;
        return "[RBP - " + stackLoc.offset() + "]";
      default:
        emitter.fail("Cannot generate %s operand %s yet", location.storage(), location);
        return null;
    }
  }

  /** Given a parameter index and type, returns a string representation of that parameter. */
  private String generateParamLocationName(int index, VarType varType) {
    Register reg = Register.paramRegister(index);
    if (reg == null) {
      // TODO: implement > 4 params.
      emitter.fail("Cannot generate more than 4 params yet");
      return null;
    }
    return reg.sizeByType(varType);
  }

  /** If the operand is a temp and was allocated, deallocate its register. */
  public void deallocate(Operand operand) {
    if (operand.storage() == SymbolStorage.TEMP) {
      // now that we used the temp, unallocate it
      String operandName = operand.toString();
      Register reg = aliases.get(operandName);
      if (reg != null) {
        emitter.emit("; Deallocating %s from %s", operand, reg);
        aliases.remove(operandName);
        deallocate(reg);
      }
    }
  }

  /**
   * @return the equivalent register, or null if none.
   */
  public Register toRegister(Operand source) {
    if (source.isConstant()) {
      return null;
    }

    if (source.isRegister()) {
      return ((RegisterLocation) source).register();
    }
    Location location = (Location) source;
    Register aliasReg = aliases.get(location.toString());
    if (aliasReg != null) {
      return aliasReg;
    }
    switch (location.storage()) {
      case TEMP:
        return null; // we would have found its alias already.
      case GLOBAL:
        return null;
      case PARAM:
        ParamLocation paramLoc = (ParamLocation) location;
        Register actualReg = Register.paramRegister(paramLoc.index());
        return actualReg;
      case LOCAL:
        return null;
      default:
        emitter.fail("Cannot generate %s operand %s yet", location.storage(), location);
        return null;
    }
  }

  /** Returns true if the given operand is in any register. */
  public boolean isInAnyRegister(Operand arg) {
    return toRegister(arg) != null;
  }

  /** Returns true if the given operand is in the given register. */
  public boolean isInRegister(Operand arg, Register register) {
    return toRegister(arg) == register;
  }

  private static int id;

  public String nextLabel(String prefix) {
    return String.format("_%s_%d", prefix, id++);
  }

  /** Allocate and return a register. */
  @Override
  public Register reserve(Register r) {
    return registers.reserve(r);
  }

  @Override
  public Register allocate(VarType varType) {
    return registers.allocate(varType);
  }

  /** Deallocate the given register. */
  @Override
  public void deallocate(Register register) {
    registers.deallocate(register);
  }

  @Override
  public boolean isAllocated(Register r) {
    return registers.isAllocated(r);
  }
}
