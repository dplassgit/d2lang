package com.plasstech.lang.d2.codegen;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VarType;

public class Resolver {
  private final BiMap<String, Register> aliases = HashBiMap.create(16);
  private Registers registers;
  private StringTable stringTable;
  private Emitter emitter;

  public Resolver(Registers registers, StringTable stringTable, Emitter emitter) {
    this.registers = registers;
    this.stringTable = stringTable;
    this.emitter = emitter;
  }

  public String resolve(Operand operand) {
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

      emitter.fail("Cannot generate %s operand %s yet", operand.type().name(), operand);
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
        reg = registers.allocate();
        aliases.put(location.name(), reg);
        emitter.emit("; Allocating %s to %s", location, reg);
        return reg.sizeByType(location.type());
      case GLOBAL:
        return "[__" + location.name() + "]";
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

  public String generateParamLocationName(int index, VarType varType) {
    Register reg = Register.paramRegister(index);
    if (reg == null) {
      // TODO: implement > 4 params.
      emitter.fail("Cannot generate more than 4 params yet");
      return null;
    }
    return reg.sizeByType(varType);
  }

  public Register allocate() {
    return registers.allocate();
  }

  public void deallocate(Register register) {
    registers.deallocate(register);
  }

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
   * @param source
   * @return the equivalent register, or null if none.
   */
  public Register toRegister(Operand source) {
    if (source.isConstant()) {
      return null;
    }

    Location location = (Location) source;
    if (source.isRegister()) {
      return ((RegisterLocation) location).register();
    }
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

  public boolean isInAnyRegister(Operand arg) {
    return toRegister(arg) != null;
  }

  public boolean isInRegister(Operand arg, Register register) {
    if (arg.isConstant()) {
      return false;
    }

    if (arg.isRegister()) {
      Register actualReg = ((RegisterLocation) arg).register();
      return register == actualReg;
    }
    Location location = (Location) arg;
    Register aliasReg = aliases.get(location.toString());
    if (aliasReg != null) {
      return aliasReg == register;
    }
    switch (location.storage()) {
      case TEMP:
        return false; // we would have found its alias already.
      case GLOBAL:
        return false;
      case PARAM:
        ParamLocation paramLoc = (ParamLocation) location;
        Register actualReg = Register.paramRegister(paramLoc.index());
        return actualReg == register;
      case LOCAL:
        return false;
      default:
        emitter.fail("Cannot determine if %s is in a reg", location);
        return false;
    }
  }
}
