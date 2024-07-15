package com.plasstech.lang.d2.codegen.il;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.SymbolStorage;

/**
 * Indicates the code generator should "deallocate" this (long) temp, meaning it is no longer used.
 */
public class DeallocateTemp extends Op {

  private final Location temp;

  public DeallocateTemp(Location temp, Position position) {
    super(position);
    this.temp = temp;
  }

  public Location temp() {
    return temp;
  }

  @Override
  public String toString() {
    return String.format("dealloc(%s)", temp.toString());
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }

  /**
   * "Fixes" the long temp deallocate ops in the given program. Returns a new program where only the
   * last "read" of a long temp is followed by a DeallocateTemp op.
   */
  public static List<Op> fixDeallocateTemps(List<Op> program) {
    // 1. remove all deallocates
    List<Op> code = Op.removeMatchingOps(program, DeallocateTemp.class);

    // 2. add the correct ones.
    Set<Operand> longTempsRead = new HashSet<>();
    // Go from end to start, because we want to find the *last* entry
    for (int ip = code.size() - 1; ip >= 0; ip--) {
      Op opcode = code.get(ip);
      List<Operand> sources = opcode.getSources();
      for (Operand operand : sources) {
        if (operand.storage() == SymbolStorage.LONG_TEMP) {
          if (!longTempsRead.contains(operand)) {
            // This is the first time we've seen this long temp (thus the last one in the code)
            // so add a deallocate AFTER this opcode.
            code.add(ip + 1, new DeallocateTemp((Location) operand, opcode.position()));
          }
          longTempsRead.add(operand);
        }
      }
      // Even though this is also done in the DeadLongTempAssignmentOptimizer, it won't be
      // run if optimizations are off.
      Operand dest = opcode.getDestination();
      if (dest != null && dest.storage() == SymbolStorage.LONG_TEMP
          && !longTempsRead.contains(dest)) {
        // 3. We are writing to this long temp but have never read from it. Delete it.
        code.set(ip, new Nop(opcode));
      }
    }

    return code;
  }
}
