package com.plasstech.lang.d2.codegen.t100;

import static com.plasstech.lang.d2.codegen.Codegen.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.ConstEntry;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Emitter;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.StringTable;
import com.plasstech.lang.d2.codegen.TempLocation;
import com.plasstech.lang.d2.codegen.VariableLocation;
import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VarType;
import com.plasstech.lang.d2.type.VariableSymbol;

public class Resolver {
  private final StringTable stringTable;
  private final Registers registers;
  private final Emitter emitter;
  private final Map<String, PseudoReg> aliases = new HashMap<>();
  private final Map<VarType, Set<PseudoReg>> regsByType = new HashMap<>();

  public Resolver(StringTable stringTable, Registers registers, Emitter emitter) {
    this.stringTable = stringTable;
    this.registers = registers;
    this.emitter = emitter;
  }

  void debug() {
    emitter.emit("; aliases: %s", aliases.toString());
    emitter.emit("; regs: %s", regsByType.toString());
  }

  // Returns the current set of temp names
  public List<PseudoReg> temps() {
    List<PseudoReg> all = new ArrayList<>();
    for (Set<PseudoReg> value : regsByType.values()) {
      all.addAll(value);
    }
    return all;
  }

  public void mov(Operand source, Location destination) {
    String destName = resolve(destination);
    if (source.isConstant()) {
      movConstant(source, destName);
    } else {
      // source is not a constant.
      // have to copy 1,2, or 4 bytes
      String sourceName = resolve(source);
      int sourceSize = source.type().size();
      if (sourceSize == 1) {

        // load one byte from source
        emitter.emit("lda %s  ; read byte at source", sourceName);
        // write one byte to dest
        emitter.emit("sta %s  ; write byte to dest", destName);

      } else if (sourceSize == 2 || sourceSize == 4) {

        // load 2 bytes
        RegisterState state =
            RegisterState.condPush(emitter, registers, ImmutableList.of(Register.M));

        // Transfer low word
        emitter.emit("lhld %s  ; read word at source", sourceName);
        emitter.emit("shld %s  ; write word to dest", destName);
        if (sourceSize == 4) {
          // Transfer high word
          emitter.emit("lhld %s + 0x02  ; read high word at source", sourceName);
          emitter.emit("shld %s + 0x02  ; write high word to dest", destName);
        }
        state.condPop();

      }
    }
  }

  private void movConstant(Operand source, String dest) {
    RegisterState state = null;

    if (source.type() == VarType.BYTE) {

      ConstantOperand<Byte> byteOp = (ConstantOperand<Byte>) source;
      if (registers.isAllocated(Register.M)) {
        emitter.emit("lxi B, %s  ; location to store literal byte", dest);
        emitter.emit("mvi A, 0x%02x  ; A <- literal byte", byteOp.value());
        emitter.emit("stax B  ; [BC] <- literal byte");
      } else {
        emitter.emit("lxi H, %s  ; location to store literal byte", dest);
        emitter.emit("mvi M, 0x%02x  ; [HL] <- literal byte", byteOp.value());
      }

    } else if (source.type() == VarType.INT) {

      ConstantOperand<Integer> intOp = (ConstantOperand<Integer>) source;
      int value = intOp.value();
      emitter.emit("; 32-bit value 0x%08x (NOTE LITTLE ENDIAN-NESS)", value);
      int low = value & 0x0000ffff;
      state =
          RegisterState.condPush(emitter, registers, ImmutableList.of(Register.M));
      emitter.emit("lxi H, 0x%04x  ; put low word of 32-bit literal into HL", low);
      emitter.emit("shld %s  ; store low word (LSByte first)", dest);

      int high = (value >> 16) & 0x0000ffff;
      // do a little optimization
      if (low == high) {
        emitter.emit("; no need to set high because high==low");
      } else if (low - 1 == high) {
        emitter.emit("dcx H  ; high word is 0x%04x", high);
      } else if (low + 1 == high) {
        emitter.emit("inx H  ; high word is 0x%04x", high);
      } else {
        emitter.emit("lxi H, 0x%04x  ; put high word into HL", high);
      }
      emitter.emit("shld %s + 0x02  ; store high word", dest);

    } else if (source.type() == VarType.BOOL) {

      String value = source.equals(ConstantOperand.TRUE) ? "1" : "0";
      if (registers.isAllocated(Register.M)) {
        // Don't use H if it's reserved
        emitter.emit("lxi B, %s  ; location to store literal byte", dest);
        emitter.emit("mvi A, 0x0%s  ; A <- literal boolean", value);
        emitter.emit("stax B  ; [BC] <- literal byte");
      } else {
        emitter.emit("lxi H, %s  ; location to store boolean", dest);
        emitter.emit("mvi M, 0x0%s  ; [HL] <- literal boolean", value);
      }

    } else if (source.type() == VarType.STRING) {

      ConstantOperand<String> stringOp = (ConstantOperand<String>) source;
      String value = stringOp.value();
      // look it up
      ConstEntry<String> entry = stringTable.lookup(value);
      state = RegisterState.condPush(emitter, registers, ImmutableList.of(Register.M));
      emitter.emit("lxi H, %s  ; location to store literal string", entry.name());
      emitter.emit("shld %s  ; [HL] <- literal string", dest);
    }

    if (state != null) {
      state.condPop();
    }
  }

  // TODO: Fold this into mov(source, dest)
  public void mov(Operand source, String globalDestination) {
    String destName = globalDestination;
    if (source.isConstant()) {
      // easy case: copy constant to global or temp
      // for byte: just store it
      if (source.type() == VarType.BYTE) {
        ConstantOperand<Byte> byteOp = (ConstantOperand<Byte>) source;
        if (registers.isAllocated(Register.M)) {
          emitter.emit("lxi B, %s  ; location to store literal byte", destName);
          emitter.emit("mvi A, 0x%02x  ; A <- literal byte", byteOp.value());
          emitter.emit("stax B  ; [BC] <- literal byte");
        } else {
          emitter.emit("lxi H, %s  ; location to store literal byte", destName);
          emitter.emit("mvi M, 0x%02x  ; [HL] <- literal byte", byteOp.value());
        }
        return;
      } else if (source.type() == VarType.INT) {
        ConstantOperand<Integer> intOp = (ConstantOperand<Integer>) source;
        int value = intOp.value();
        emitter.emit("; 32-bit value 0x%08x (NOTE LITTLE ENDIAN-NESS)", value);
        int low = value & 0x0000ffff;
        RegisterState state =
            RegisterState.condPush(emitter, registers, ImmutableList.of(Register.M));
        emitter.emit("lxi H, 0x%04x  ; get low word of 32-bit literal into HL", low);
        emitter.emit("shld %s  ; store low word (LSByte first)", destName);
        int high = (value >> 16) & 0x0000ffff;
        if (low == high) {
          emitter.emit("; no need to set high because high==low");
        } else if (low - 1 == high) {
          emitter.emit("dcx H  ; high word is 0x%04x", high);
        } else if (low + 1 == high) {
          emitter.emit("inx H  ; high word is 0x%04x", high);
        } else {
          emitter.emit("lxi H, 0x%04x  ; put high word into HL", high);
        }
        emitter.emit("shld %s + 0x02  ; store high word", destName);
        state.condPop();
        return;
      } else if (source.type() == VarType.STRING) {
        // look it up
        ConstantOperand<String> stringOp = (ConstantOperand<String>) source;
        String value = stringOp.value();
        ConstEntry<String> entry = stringTable.lookup(value);
        RegisterState state =
            RegisterState.condPush(emitter, registers, ImmutableList.of(Register.M));
        emitter.emit("lxi H, %s  ; location to store literal string", entry.name());
        emitter.emit("shld %s  ; [HL] <- literal string", destName);
        state.condPop();
        return;
      } else if (source.type() == VarType.BOOL) {
        boolean set = source.equals(ConstantOperand.TRUE);
        if (registers.isAllocated(Register.M)) {
          // Don't use H if it's reserved
          emitter.emit("lxi B, %s  ; location to store literal byte", destName);
          emitter.emit("mvi A, 0x0%s  ; A <- literal boolean", set ? "1" : "0");
          emitter.emit("stax B  ; [BC] <- literal byte");
        } else {
          emitter.emit("lxi H, %s  ; location to store boolean", destName);
          emitter.emit("mvi M, 0x0%s  ; [HL] <- literal boolean", set ? "1" : "0");
        }
        return;
      }
    } else {
      // source is not a constant.
      // have to copy 1,2, or 4 bytes
      String sourceName = resolve(source);
      int sourceSize = source.type().size();
      if (sourceSize == 1) {
        // load one byte from source
        emitter.emit("lda %s  ; read byte at source", sourceName);
        // write one byte to dest
        emitter.emit("sta %s  ; write byte to dest", destName);
        return;
      } else if (sourceSize == 2 || sourceSize == 4) {
        // load 2 bytes
        RegisterState state =
            RegisterState.condPush(emitter, registers, ImmutableList.of(Register.M));
        // Transfer low word
        emitter.emit("lhld %s  ; read word at source", sourceName);
        emitter.emit("shld %s  ; write word to dest", destName);
        if (sourceSize == 4) {
          // Transfer high word
          emitter.emit("lhld %s + 0x02  ; read high word at source", sourceName);
          emitter.emit("shld %s + 0x02  ; write high word to dest", destName);
        }
        state.condPop();
        return;
      }
    }
    fail(null, "Cannot generate mov from %s to %s", source, destName);
  }

  // TODO: Fold this into mov(source, dest)
  public void mov(String sourceName, Location destination) {
    String destName = resolve(destination);
    // have to copy 1,2, or 4 bytes
    int sourceSize = destination.type().size();
    if (sourceSize == 1) {
      // load one byte from source
      emitter.emit("lda %s  ; read byte at source", sourceName);
      // write one byte to dest
      emitter.emit("sta %s  ; write byte to dest", destName);
      return;
    } else if (sourceSize == 2 || sourceSize == 4) {
      // load 2 bytes
      RegisterState state =
          RegisterState.condPush(emitter, registers, ImmutableList.of(Register.M));
      // Transfer low word
      emitter.emit("lhld %s  ; read word at source", sourceName);
      emitter.emit("shld %s  ; write word to dest", destName);
      if (sourceSize == 4) {
        // Transfer high word
        emitter.emit("lhld %s + 0x02  ; read high word at source", sourceName);
        emitter.emit("shld %s + 0x02  ; write high word to dest", destName);
      }
      state.condPop();
      return;
    }
    fail(null, "Cannot generate mov from %s to %s", sourceName, destination);
  }

  public String resolve(Operand arg) {
    if (arg.isConstant()) {
      return resolveConstant(arg);
    }

    if (arg instanceof VariableLocation) {
      VariableLocation location = (VariableLocation) arg;
      if (location.storage() == SymbolStorage.TEMP) {
        return resolveTemp(location);
      }

      // global, local or param
      return T100Locations.locationName(location.symbol());
    }

    // Cannot deal with ??? 
    fail(null, "Cannot resolve %s", arg);
    return null;
  }

  private String resolveTemp(VariableLocation location) {
    PseudoReg tempDest = aliases.get(location.name());
    if (tempDest != null) {
      return tempDest.name();
    }

    tempDest = allocate(location.type());
    // repeat the 0x00s based on the size
    String zeros = T100Locations.zeros(location.type());
    emitter.addData(String.format("%s:   db %s", tempDest.name(), zeros));
    aliases.put(location.name(), tempDest);
    emitter.emit("; Allocating %s to %s", location, tempDest);
    return tempDest.name();
  }

  private PseudoReg allocate(VarType varType) {
    int i = 0;
    Set<PseudoReg> regs = regsByType.get(varType);
    if (regs == null) {
      regs = new HashSet<>();
      regsByType.put(varType, regs);
    }
    for (; i < regs.size(); ++i) {
      PseudoReg reg = makePseudoReg(varType, i);
      if (!regs.contains(reg)) {
        // see if we have an empty slot. if not, create a new one
        regs.add(reg);
        return reg;
      }
    }
    // If we got here there were no registers left. add one. :)
    PseudoReg reg = makePseudoReg(varType, i);
    regs.add(reg);
    return reg;
  }

  private PseudoReg makePseudoReg(VarType varType, int i) {
    return PseudoReg.create(String.format("_TEMP_%s_%d", varType.name().toUpperCase(), i), varType);
  }

  public void deallocate(Operand operand) {
    if (operand.storage() == SymbolStorage.TEMP) {
      // now that we used the temp, unallocate it
      String operandName = operand.toString();
      PseudoReg reg = aliases.get(operandName);
      if (reg != null) {
        emitter.emit("; Deallocating %s from %s", operand, reg);
        aliases.remove(operandName);
        Set<PseudoReg> regs = regsByType.get(reg.type());
        if (regs != null) {
          regs.remove(reg);
        }
      }
    }
  }

  private String resolveConstant(Operand arg) {
    if (arg.equals(ConstantOperand.TRUE)) {
      return "0x01";
    }
    if (arg.equals(ConstantOperand.FALSE)) {
      return "0x00";
    }
    if (arg.type() == VarType.STRING) {
      ConstantOperand<String> stringOperand = (ConstantOperand<String>) arg;
      ConstEntry<String> constEntry = stringTable.lookup(stringOperand.value());
      return constEntry.name();
    }
    if (arg.type() == VarType.BYTE) {
      ConstantOperand<Byte> byteOperand = (ConstantOperand<Byte>) arg;
      return String.format("0x%02x", byteOperand.value());
    }
    if (arg.type() == VarType.INT) {
      ConstantOperand<Integer> intOperand = (ConstantOperand<Integer>) arg;
      int value = intOperand.value();
      if (Math.abs(value) < 32768 || value == -32768) {
        // 16-bit value
        return String.format("0x%04x", value);
      } else {
        fail(null, "Cannot resolve large constant %d", value);
      }
    }
    fail(null, "Cannot resolve constant %s yet", arg);
    return null;
  }

  public void mov(Operand source, Register destination) {
    String sourceName = resolve(source);
    if (source.isConstant()) {
      if (source.equals(ConstantOperand.FALSE) || source.equals(ConstantOperand.ZERO_BYTE)) {
        emitter.emit("mvi %s, 0x00", destination.name());
        return;
      } else if (source.equals(ConstantOperand.TRUE) || source.equals(ConstantOperand.ONE_BYTE)) {
        emitter.emit("mvi %s, 0x01", destination.name());
        return;
      } else if (source.type() == VarType.BYTE) {
        ConstantOperand<Byte> byteOperand = (ConstantOperand<Byte>) source;
        emitter.emit("mvi %s, 0x%02x", destination, byteOperand.value());
        return;
      } else if (source.type() == VarType.INT) {
        // use a temp
        VariableSymbol tempSymbol = new VariableSymbol("TEMP", SymbolStorage.TEMP);
        tempSymbol.setVarType(VarType.INT);
        Location temp = new TempLocation(tempSymbol);
        // move a const to TEMP
        mov(source, temp);
        // then move TEMP to destination.
        mov(temp, destination);
        deallocate(temp);
        return;
      }
    } else {
      // source is memory.
      if (destination.isPair()) {
        // use the 1-letter alias
        emitter.emit("lxi %s, %s", destination.alias, sourceName);
        return;
      }
      if (destination == Register.A) {
        emitter.emit("lda %s", sourceName);
        return;
      }
      // for other registers we need to use an intermediary:
      // this may be broken if source is a pair
      RegisterState state =
          RegisterState.condPush(emitter, registers, ImmutableList.of(Register.M));
      emitter.emit("lxi H, %s", sourceName);
      emitter.emit("mov %s, M", destination);
      state.condPop();
      return;
    }
    fail(null, "Cannot mov %s to %s", source, destination);
  }

  public void mov(Register source, Location destination) {
    if (source == Register.A) {
      emitter.emit("sta %s", resolve(destination));
      return;
    } else if (source.isPair()) {
      if (destination.type() == VarType.BYTE || destination.type() == VarType.BOOL) {
        // the register pair points to a byte in memory.
        // get value into A, then write it to destination.
        emitter.emit(source.lda);
        mov(Register.A, destination);
        return;
      } else if (destination.type() == VarType.INT) {
        // copy 4 bytes to global
      }
    }
    fail(null, "Cannot mov %s to %s yet", source, destination);
  }
}
