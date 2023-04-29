package com.plasstech.lang.d2.codegen.x64;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.ConstEntry;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.DelegatingEmitter;
import com.plasstech.lang.d2.codegen.DoubleTable;
import com.plasstech.lang.d2.codegen.Emitter;
import com.plasstech.lang.d2.codegen.ListEmitter;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.ParamLocation;
import com.plasstech.lang.d2.codegen.StackLocation;
import com.plasstech.lang.d2.codegen.StringTable;
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

  private final DelegatingEmitter emitter;
  private final Deque<Emitter> emitters = new ArrayDeque<>();

  private static final Comparator<Register> REGISTER_NAME_COMPARATOR = new Comparator<Register>() {
    @Override
    public int compare(Register arg0, Register arg1) {
      // This doesn't sort in enum order but it doesn't matter.
      return arg0.name64().compareTo(arg1.name64());
    }
  };
  private final Set<Register> usedRegisters = new TreeSet<>(REGISTER_NAME_COMPARATOR);
  private boolean inProc;

  Resolver(
      Registers registers,
      StringTable stringTable,
      DoubleTable doubleTable,
      // I don't love this. The alternative would have been to accept an Emitter
      // and wrap it with our own delegating emitter, but then all the other objects would be
      // using the wrong emitter. We need all objects to use the *same* **delegating** emitter
      // so the resolver can modify the delegate as needed.
      DelegatingEmitter emitter) {
    this.registers = registers;
    this.stringTable = stringTable;
    this.doubleTable = doubleTable;
    this.emitter = emitter;
  }

  /**
   * Resolves the given operand "fully" to a ResolvedOperand object. Sets the operand, name, and
   * register (nullable)
   */
  ResolvedOperand resolveFully(Operand operand) {
    String name = resolve(operand);
    return ResolvedOperand.create(operand, name).setRegister(toRegister(operand));
  }

  /**
   * Given an operand returns a string representation of where it can be accessed. It also allocates
   * a register for a temp if it's not already allocated.
   */
  String resolve(Operand operand) {
    if (operand.isConstant()) {
      if (operand.type() == VarType.BYTE
          || operand.type() == VarType.INT
          || operand.type() == VarType.LONG) {
        // TODO: this is weird - should be operand.value.toString?
        return operand.toString();
      } else if (operand.type() == VarType.BOOL) {
        if (operand.equals(ConstantOperand.TRUE)) {
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
        return generateParamLocationName(paramLoc);

      case LOCAL:
        StackLocation stackLoc = (StackLocation) location;
        return "[RBP - " + stackLoc.offset() + "]";

      default:
        emitter.fail("Cannot generate %s operand %s yet", location.storage(), location);
        return null;
    }
  }

  /** Given a parameter index and type, returns a string representation of that parameter. */
  private String generateParamLocationName(ParamLocation param) {
    Register reg = Register.paramRegister(param.type(), param.index());
    if (reg == null) {
      // No register; must be an overflow parameter
      emitter.emit("; param location for %s (%d)", param.name(), param.offset());
      return "[RBP + " + param.offset() + "]";
    }
    return reg.sizeByType(param.type());
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

  /** Allocate and return a register. */
  @Override
  public Register reserve(Register r) {
    if (inProc) {
      usedRegisters.add(r);
    }
    emitter.emit("; reserving %s", r);
    return registers.reserve(r);
  }

  @Override
  public Register allocate(VarType varType) {
    Register r = registers.allocate(varType);
    if (inProc) {
      usedRegisters.add(r);
    }
    return r;
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
          // Fixed Issue #170: if register to register, don't need "size"
          emitter.emit("mov %s, %s", destReg.sizeByType(type), sourceReg.sizeByType(type));
        } else {
          // if source is a constant and it's bigger than a 32-bit int, AND we're moving to
          // memory, we need to use an intermediary
          if (type == VarType.LONG && source.isConstant() && destReg == null) {
            ConstantOperand<Long> longOp = (ConstantOperand<Long>) source.operand();
            long value = longOp.value();
            if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
              emitter.emit("; constant is larger than 32 bits, must use intermediary");
              Register tempReg = allocate(VarType.INT);
              mov(source.operand(), tempReg);
              mov(tempReg, dest.location());
              deallocate(tempReg);

              // NOTE RETURN
              return;
            }
          }

          // mov register, constant
          // mov qword dest, register
          emitter.emit("mov %s %s, %s", size, destName, sourceName);
        }
      }
    } else {
      // Memory to memory:
      // Move from sourceName to tempReg, then from tempReg to destName
      Register tempReg = allocate(VarType.INT);
      mov(source.operand(), tempReg);
      mov(tempReg, dest.location());
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
      mov(source.operand(), tempReg);
      mov(tempReg, dest.location());
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
    mov(source.operand(), tempReg);
    mov(tempReg, dest.location());
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

  void procEntry() {
    inProc = true;
    Emitter original = emitter.getDelegate();
    emitters.push(original);

    emitter.setDelegate(new ListEmitter());
  }

  void procEnd() {
    inProc = false;

    Emitter original = emitters.pop();
    // now, push all nonvolatile registers on the *original* emitter
    RegisterState rs = new RegisterState(original);
    usedRegisters.retainAll(Register.NONVOLATILE_REGISTERS);
    ImmutableList<Register> registersToSave = ImmutableList.copyOf(usedRegisters);
    for (Register r : registersToSave) {
      rs.push(r);
    }

    // then copy everything from the child to the *original* emitter
    Emitter child = emitter.getDelegate();
    // Copy emitted lines, externs and data
    for (String line : child.all()) {
      original.emit0("%s", line);
    }
    for (String extern : child.externs()) {
      original.addExtern(extern);
    }
    for (String datum : child.data()) {
      original.addData(datum);
    }
    // Then pop all the registers to the *original* emitter
    for (Register r : registersToSave.reverse()) {
      rs.pop(r);
    }

    emitter.setDelegate(original);
    usedRegisters.clear();
  }

  void push(ResolvedOperand operand) {
    Register register = operand.register();
    if (register != null) {
      // This is a little wasteful, shrug.
      RegisterState rs = new RegisterState(emitter);
      rs.push(register);
    } else {
      emitter.emit("push QWORD %s", operand.name());
    }
  }

  @AutoValue
  abstract static class ResolvedOperand implements Operand {
    abstract Operand operand();

    @Nullable
    abstract Location location();

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
      return register() != null;
    }

    @Override
    public SymbolStorage storage() {
      return operand().storage();
    }

    @Override
    public String toString() {
      return name();
    }

    public ResolvedOperand setRegister(Register reg) {
      if (reg == null) {
        return this;
      }
      return this.toBuilder().setRegister(reg).build();
    }

    public static ResolvedOperand create(Operand operand, String name) {
      Builder builder = new AutoValue_Resolver_ResolvedOperand.Builder().setOperand(operand)
          .setName(name);
      if (operand instanceof Location) {
        // I wish this was easier
        builder.setLocation((Location) operand);
      }
      return builder.build();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    abstract static class Builder {
      abstract Builder setOperand(Operand operand);

      abstract Builder setLocation(Location location);

      abstract Builder setName(String name);

      abstract Builder setRegister(Register register);

      abstract ResolvedOperand build();
    }
  }
}
