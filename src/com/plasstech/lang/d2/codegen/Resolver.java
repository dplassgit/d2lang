package com.plasstech.lang.d2.codegen;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VarType;

/**
 * Resolves temp and other variables and keeps track if they're in registers or not. TODO: rename
 * this to something better.
 */
class Resolver implements RegistersInterface {
  // map from name to register
  private final Map<String, Register> aliases = new HashMap<>();
  private final Registers registers;
  private final StringTable stringTable;
  private final DoubleTable doubleTable;
  private final Emitter emitter;

  Resolver(Registers registers, StringTable stringTable, DoubleTable doubleTable, Emitter emitter) {
    this.registers = registers;
    this.stringTable = stringTable;
    this.doubleTable = doubleTable;
    this.emitter = emitter;
  }

  /**
   * Resolves the given operand "fully" to a ResolvedOperand object. Sets the operand, name, and
   * register (nullably)
   */
  ResolvedOperand resolveFully(Operand operand) {
    String name = resolve(operand);
    ResolvedOperand ro = ResolvedOperand.create(operand, name);
    ro = ro.setRegister(toRegister(operand));
    return ro;
  }

  /**
   * Given an operand returns a string representation of where it can be accessed. It also allocates
   * a register for a temp if it's not already allocated.
   */
  String resolve(Operand operand) {
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
        reg = allocate(location.type());
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
    Register reg = Register.paramRegister(varType, index);
    if (reg == null) {
      // TODO: implement > 4 params.
      emitter.fail("Cannot generate more than 4 params yet");
      return null;
    }
    return reg.sizeByType(varType);
  }

  /** If the operand is a temp and was allocated, deallocate its register. */
  void deallocate(Operand operand) {
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

  /** @return the equivalent register, or null if none. */
  Register toRegister(Operand source) {
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
        Register actualReg = Register.paramRegister(paramLoc.type(), paramLoc.index());
        return actualReg;
      case LOCAL:
        return null;
      default:
        emitter.fail("Cannot generate %s operand %s yet", location.storage(), location);
        return null;
    }
  }

  /** Returns true if the given operand is in any register. */
  boolean isInAnyRegister(Operand arg) {
    return toRegister(arg) != null;
  }

  /** Returns true if the given operand is in the given register. */
  boolean isInRegister(Operand arg, Register register) {
    return toRegister(arg) == register;
  }

  private static int id;

  String nextLabel(String prefix) {
    return String.format("__%s_%d", prefix, id++);
  }

  /** Allocate and return a register. */
  @Override
  public Register reserve(Register r) {
    emitter.emit("; reserving %s", r);
    return registers.reserve(r);
  }

  @Override
  public Register allocate(VarType varType) {
    return registers.allocate(varType);
  }

  /** Deallocate the given register. */
  @Override
  public void deallocate(Register r) {
    registers.deallocate(r);
  }

  @Override
  public boolean isAllocated(Register r) {
    return registers.isAllocated(r);
  }

  void mov(Operand source, Register dest) {
    mov(source, new RegisterLocation("dest", dest, source.type()));
  }

  void mov(VarType type, Register source, Register destination) {
    mov(
        new RegisterLocation("source", source, type),
        new RegisterLocation("dest", destination, type));
  }

  void mov(Register source, Location destination) {
    mov(new RegisterLocation("source", source, destination.type()), destination);
  }

  void mov(Operand source, Operand destination) {
    ResolvedOperand destRo = resolveFully(destination);
    ResolvedOperand sourceRo = resolveFully(source); // this may put it in a register
    if (sourceRo.name().equals(destRo.name())) {
      // do nothing!
      emitter.emit("; mov %s, %s is a nop", destination, source);
      return;
    }

    if (source.type() == VarType.STRING || source.type().isArray()) {
      movPointer(sourceRo, destRo);
    } else if (source.type() == VarType.DOUBLE) {
      movDouble(sourceRo, destRo);
    } else {
      movInt(sourceRo, destRo);
    }
  }

  private void movInt(ResolvedOperand source, ResolvedOperand dest) {
    Register sourceReg = source.register();
    Register destReg = dest.register();
    String destName = dest.name();
    String sourceName = source.name();

    VarType type = source.type();
    String size = Size.of(type).asmType;
    if (source.isConstant() || source.isRegister() || destReg != null || sourceReg != null) {
      if (source.isConstant() && sourceName.equals("0") && destReg != null) {
        emitter.emit("xor %s, %s  ; instead of mov reg, 0", destReg.name64(), destReg.name64());
      } else {
        if (sourceReg != null && destReg != null) {
          // Issue #170: if register to register, don't need "size"
          emitter.emit("mov %s, %s", destReg.sizeByType(type), sourceReg.sizeByType(type));
        } else {
          emitter.emit("mov %s %s, %s", size, destName, sourceName);
        }
      }
    } else {
      // Memory to memory:
      // Move from sourceName to tempReg, then from tempReg to destName
      Register tempReg = allocate(VarType.INT);
      String tempName = tempReg.sizeByType(type);
      emitter.emit("mov %s %s, %s", size, tempName, sourceName);
      emitter.emit("mov %s %s, %s", size, destName, tempName);
      deallocate(tempReg);
    }
  }

  private void movPointer(ResolvedOperand source, ResolvedOperand dest) {
    Register sourceReg = source.register();
    Register destReg = dest.register();
    String destName = dest.name();
    String sourceName = source.name();

    if (destReg != null || sourceReg != null) {
      if (source.isConstant() && sourceName.equals("0") && destReg != null) {
        emitter.emit("xor %s, %s  ; instead of mov reg, 0", destReg.name64(), destReg.name64());
      } else {
        // go right from source to dest
        emitter.emit("mov %s, %s", destName, sourceName);
      }
    } else {
      // Memory to memory:
      // Move from sourceName to tempReg, then from tempReg to destName
      Register tempReg = allocate(VarType.INT);
      // TODO: This (source.type) is probably not needed, since we're talking pointers here
      String tempRegName = tempReg.sizeByType(source.type());
      emitter.emit("mov %s, %s", tempRegName, sourceName);
      emitter.emit("mov %s, %s", destName, tempRegName);
      deallocate(tempReg);
    }
  }

  private void movDouble(ResolvedOperand source, ResolvedOperand dest) {
    Register sourceReg = source.register();
    Register destReg = dest.register();
    String destName = dest.name();
    String sourceName = source.name();

    if (source.isConstant() && destReg != null) {
      ConstantOperand<Double> doubleOp = (ConstantOperand<Double>) source.operand();
      double sourceDub = doubleOp.value();
      if (sourceDub == 0.0) {
        // Constant zero to register
        emitter.emit("xorpd %s, %s  ; instead of mov reg, 0", destReg, destReg);
        return;
      }
    }

    if (destReg != null || sourceReg != null) {
      // To or from a register. The other may be a register or memory,
      // so we can go right from source to dest.
      if (sourceReg == null) {
        // source is not a register (it's memory)
        emitter.emit("movsd %s, %s", destName, sourceName);
      } else {
        // source is a register - either int or XMM register
        emitter.emit("movq %s, %s", destName, sourceName);
      }
      return;
    }

    // Memory to memory.
    // Move from sourceName to tempReg, then from tempReg to destName
    Register tempReg = allocate(VarType.DOUBLE);
    // whoa, this works.
    mov(source.operand(), tempReg);
    mov(new RegisterLocation("tempReg", tempReg, VarType.DOUBLE), dest.operand());
    deallocate(tempReg);
  }

  void addAlias(Location newAlias, Operand oldAlias) {
    Register reg = aliases.get(oldAlias.toString());
    if (reg == null) {
      throw new IllegalStateException("No alias for temp " + oldAlias);
    }
    emitter.emit("; aliasing %s to %s (%s)", newAlias.name(), reg, oldAlias.toString());
    aliases.put(newAlias.name(), reg);
  }

  @AutoValue
  abstract static class ResolvedOperand implements Operand {
    abstract Operand operand();

    abstract String name();

    @Nullable
    abstract Register register();

    @Override
    public VarType type() {
      return operand().type();
    }

    @Override
    public boolean isConstant() {
      return operand().isConstant();
    }

    @Override
    public boolean isRegister() {
      return operand().isRegister();
    }

    @Override
    public SymbolStorage storage() {
      return operand().storage();
    }

    public ResolvedOperand setRegister(Register reg) {
      if (reg == null) {
        return this;
      }
      return this.toBuilder().setRegister(reg).build();
    }

    public static ResolvedOperand create(Operand operand, String name) {
      return new AutoValue_Resolver_ResolvedOperand.Builder()
          .setOperand(operand)
          .setName(name)
          .build();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    abstract static class Builder {
      abstract Builder setOperand(Operand operand);

      abstract Builder setName(String name);

      abstract Builder setRegister(Register register);

      abstract ResolvedOperand build();
    }
  }
}
