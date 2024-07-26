package com.plasstech.lang.d2.codegen.x64;

import static com.plasstech.lang.d2.codegen.Codegen.fail;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.plasstech.lang.d2.codegen.ConstEntry;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.DelegatingEmitter;
import com.plasstech.lang.d2.codegen.DoubleTable;
import com.plasstech.lang.d2.codegen.Emitter;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.ParamLocation;
import com.plasstech.lang.d2.codegen.StackLocation;
import com.plasstech.lang.d2.codegen.StringTable;
import com.plasstech.lang.d2.codegen.il.ProcExit;
import com.plasstech.lang.d2.common.D2RuntimeException;
import com.plasstech.lang.d2.common.Range;
import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VarType;

/**
 * Resolves temp and other variables and keeps track if they're in registers or not. TODO: rename
 * this to something better.
 */
class Resolver implements RegistersInterface {
  // map from temp name to register
  private final Map<String, Register> aliases = new HashMap<>();
  // map from register to names
  private final Multimap<Register, String> reverseAllocations = HashMultimap.create();
  // map from temp name to offset
  private final HashMap<String, Integer> offsets = new HashMap<>();
  private final Registers registers;
  private final StringTable stringTable;
  private final DoubleTable doubleTable;

  private final DelegatingEmitter emitter;
  private final Deque<Emitter> emitters = new ArrayDeque<>();

  private static final Comparator<Register> REGISTER_NAME_COMPARATOR = new Comparator<Register>() {
    @Override
    public int compare(Register arg0, Register arg1) {
      // This doesn't sort in enum order but it doesn't matter.
      return arg0.name().compareTo(arg1.name());
    }
  };
  private final Set<Register> usedRegisters = new TreeSet<>(REGISTER_NAME_COMPARATOR);
  private boolean inProc;
  private int localBytes;

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
    if (operand instanceof ResolvedOperand) {
      return (ResolvedOperand) operand;
    }
    String name = resolve(operand);
    return ResolvedOperand.create(operand, name).setRegister(toRegister(operand));
  }

  /**
   * Given an operand returns a string representation of where it can be accessed. It also allocates
   * a register for a temp if it's not already allocated.
   */
  String resolve(Operand operand) {
    if (operand.isConstant()) {
      if (operand.isNull()) {
        return "0";
      }
      if (operand.type().isIntegral()) {
        Number value = ConstantOperand.valueFromConstOperand(operand);
        return value.toString();
      }
      if (operand.type() == VarType.BOOL) {
        if (operand.equals(ConstantOperand.TRUE)) {
          return "1";
        }
        return "0";
      }
      if (operand.type() == VarType.STRING) {
        // look it up in the string table.
        String value = ConstantOperand.stringValueFromConstOperand(operand);
        ConstEntry<String> entry = stringTable.lookup(value);
        return entry.name();
      }
      if (operand.type() == VarType.DOUBLE) {
        // look it up in the double table.
        double doubleValue = ConstantOperand.valueFromConstOperand(operand).doubleValue();
        ConstEntry<Double> entry = doubleTable.lookup(doubleValue);
        return String.format("[%s]", entry.name());
      }
      if (operand.type() == VarType.RANGE) {
        ConstantOperand<Range> rangeOperand = (ConstantOperand<Range>) operand;
        Range range = rangeOperand.value();
        return Long.toString(range.value());
      }

      fail(null, "Cannot generate %s constant %s yet", operand.type().name(), operand);
      return null;
    }

    Location location = (Location) operand;
    // maybe look up the location in the symbol table?
    if (location.isRegister()) {
      Register reg = ((RegisterLocation) location).register();
      return reg.nameByType(location.type());
    }
    Register reg = aliases.get(location.toString());
    if (reg != null) {
      // Found it in a register.
      return reg.nameByType(location.type());
    }
    switch (location.storage()) {
      case TEMP:
      case LONG_TEMP:
        // look up in offsets table
        Integer offset = offsets.get(location.toString());
        if (offset != null) {
          return "[RBP - " + offset + "]";
        }
        reg = allocate(location.type());
        aliases.put(location.name(), reg);
        reverseAllocations.put(reg, location.name());
        emitter.emit("; Allocating %s (%s) to %s", location, location.storage(), reg);
        return reg.nameByType(location.type());

      case GLOBAL:
        return "[_" + location.name() + "]";

      case PARAM:
        ParamLocation paramLoc = (ParamLocation) location;
        return generateParamLocationName(paramLoc);

      case LOCAL:
        StackLocation stackLoc = (StackLocation) location;
        return "[RBP - " + stackLoc.offset() + "]";

      default:
        fail(null, "Cannot generate %s operand %s yet", location.storage(), location);
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
    return reg.nameByType(param.type());
  }

  /** If the operand is a temp and was allocated, deallocate its register. */
  void deallocate(Operand operand) {
    if (operand instanceof ResolvedOperand) {
      // why is this bad?!
      throw new IllegalStateException(
          "trying to deallocate a fully resovled operand " + operand.toString());
    }
    if (operand.isTemp()) {
      unconditionallyDeallocate(operand);
    } else if (operand.storage() == SymbolStorage.REGISTER) {
      RegisterLocation regLoc = (RegisterLocation) operand;
      deallocate(regLoc.register());
    }
  }

  /** Now that we used the temp, deallocate it */
  void unconditionallyDeallocate(Operand operand) {
    String operandName = operand.toString();
    Register reg = aliases.get(operandName);
    if (reg != null) {
      emitter.emit("; Deallocating %s from %s", operand, reg);
      aliases.remove(operandName);
      emitter.emit("; removing reverse allocation of %s (%s) to %s", reg,
          reverseAllocations.get(reg),
          operandName);
      reverseAllocations.remove(reg, operandName);
      // This is broken; there may still be a forward allocation
      if (!reverseAllocations.containsKey(reg)) {
        registers.deallocate(reg);
      } else {
        emitter.emit("; NOT deallocating %s because it's still used: %s", reg,
            reverseAllocations.get(reg));
      }
    }
    offsets.remove(operandName);
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
      case LONG_TEMP:
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
        fail(null, "Cannot generate %s operand %s yet", location.storage(), location);
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

  boolean isInRegister(ResolvedOperand arg, Register register) {
    return arg.isRegister() && arg.register().equals(register);
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
    if (r != null) {
      if (inProc) {
        usedRegisters.add(r);
      }
      return r;
    }

    return spillOver(varType);
  }

  // Spillover: find the LRU register, and put it onto the stack instead.
  private Register spillOver(VarType varType) {
    Register r = registers.lru(varType);
    // reset its location in the LRU cache.
    registers.touch(r);
    if (inProc) {
      usedRegisters.add(r);
    }

    // find the allocations (aliases) and update them all to the offset
    Collection<String> tempNames = ImmutableSet.copyOf(reverseAllocations.get(r));

    if (tempNames.isEmpty()) {
      throw new D2RuntimeException("Spilling but no temp is using " + r, null, "Internal");
    }
    localBytes += 8;
    int offset = localBytes;
    for (String name : tempNames) {
      offsets.put(name, offset);
      aliases.remove(name);
      reverseAllocations.remove(r, name);
      emitter.emit("; spilling %s from %s to RBP - %d", name, r, offset);
    }

    // Move from register to [RBP-offset]
    if (varType == VarType.DOUBLE) {
      emitter.emit("movq [RBP - %d], %s", offset, r.name());
    } else {
      // Always move all 8 bytes.
      emitter.emit("mov [RBP - %d], %s", offset, r.name());
    }
    return r;
  }

  @Override
  public Register lru(VarType varType) {
    return registers.lru(varType);
  }

  /** Deallocate the given register. */
  @Override
  public void deallocate(Register r) {
    if (registers.isAllocated(r)) {
      // This may mask errors
      registers.deallocate(r);

      // remove all the aliases, etc
      reverseAllocations.removeAll(r);
      for (var e : ImmutableSet.copyOf(aliases.entrySet())) {
        if (e.getValue().equals(r)) {
          aliases.remove(e.getKey());
        }
      }
    }
  }

  @Override
  public boolean isAllocated(Register r) {
    return registers.isAllocated(r);
  }

  void mov(Operand source, Register dest) {
    mov(source, new RegisterLocation("_destRegister", dest, source.type()));
  }

  public void mov(Register source, Operand destination) {
    mov(new RegisterLocation("_sourceRegister", source, destination.type()), destination);
  }

  void mov(VarType type, Register source, Register destination) {
    mov(new RegisterLocation("_sourceRegister", source, type),
        new RegisterLocation("_destRegister", destination, type));
  }

  void mov(Register source, Location destination) {
    mov(new RegisterLocation("_sourceRegister", source, destination.type()), destination);
  }

  void mov(Operand source, Operand destination) {
    ResolvedOperand destRo = resolveFully(destination);
    ResolvedOperand sourceRo = resolveFully(source); // this may put it in a register
    if (sourceRo.name().equals(destRo.name())) {
      // do nothing!
      emitter.emit("; mov %s, %s is a nop", destination, source);
      emitter.emit("; source name %s dest name %s", sourceRo.name(), destRo.name());
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
      // reg to reg or const to reg
      if (source.isConstant() && sourceName.equals("0") && destReg != null) {
        emitter.emit("xor %s, %s", destReg.name(), destReg.name());
      } else {
        if (sourceReg != null && destReg != null) {
          // Fixed Issue #170: if register to register, don't need "size"
          emitter.emit("mov %s, %s", destReg.nameByType(type), sourceReg.nameByType(type));
        } else {
          // if source is a constant and it's bigger than a 32-bit int, AND we're moving to
          // memory, we need to use an intermediary
          if (type == VarType.LONG && source.isConstant() && destReg == null) {
            long value = ConstantOperand.valueFromConstOperand(source.operand()).longValue();
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
        emitter.emit("xor %s, %s", destReg.name(), destReg.name());
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
      double sourceDub = ConstantOperand.valueFromConstOperand(source.operand()).doubleValue();
      if (sourceDub == 0.0) {
        // Constant zero to register
        emitter.emit("xorpd %s, %s", destReg, destReg);
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
    emitter.emit("; allocated tempreg %s during mov", tempReg);
    mov(source.operand(), tempReg);
    mov(tempReg, dest.location());
    deallocate(tempReg);
  }

  void addAlias(Location newAlias, Operand oldAlias) {
    String oldAliasName = oldAlias.toString();
    Register reg = aliases.get(oldAliasName);
    String newAliasName = newAlias.name();
    if (reg == null) {
      Integer offset = offsets.get(oldAliasName);
      if (offset != null) {
        emitter.emit("; Aliasing %s to %s at [RBP - %d]", newAliasName, oldAliasName, offset);
        offsets.put(newAliasName, offset);
        return;
      }
      throw new IllegalStateException("No alias or offset for temp: " + oldAlias);
    }
    emitter.emit("; Aliasing %s (%s) to %s (%s)", newAliasName, newAlias.storage(), reg,
        oldAliasName);
    aliases.put(newAliasName, reg);
    reverseAllocations.put(reg, newAliasName);
  }

  void procEntry(int localBytes) {
    this.localBytes = localBytes;
    inProc = true;

    Emitter original = emitter.getDelegate();
    emitters.push(original);

    emitter.setDelegate(new X64Emitter());
    reverseAllocations.clear();
  }

  void procExit(ProcExit op) {
    inProc = false;

    Emitter original = emitters.pop();
    if (localBytes > 0 || op.numFormals() > 4) {
      original.emit("push RBP");
      original.emit("mov RBP, RSP");
    }
    // this over-allocates, but /shrug.
    if (localBytes > 0) {
      int bytes = 16 * (localBytes / 16 + 1);
      original.emit("sub RSP, 0x%02x  ; space for locals or spillover", bytes);
    }
    // now, push all nonvolatile registers on the *original* emitter
    RegisterState rs = new RegisterState(original);
    original.emit("; used registers %s", usedRegisters);
    usedRegisters.retainAll(Register.NONVOLATILE_REGISTERS);
    original.emit("; needed to save used registers %s", usedRegisters);
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

    if (localBytes > 0 || op.numFormals() > 4) {
      original.emit("mov RSP, RBP");
      original.emit("pop RBP");
    }

    emitter.setDelegate(original);
    usedRegisters.clear();

    aliases.clear();
    reverseAllocations.clear();
    localBytes = 0;
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

  @Override
  public void touch(Register r) {
    registers.touch(r);
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
