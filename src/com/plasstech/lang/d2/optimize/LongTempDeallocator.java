package com.plasstech.lang.d2.optimize;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.il.DeallocateTemp;
import com.plasstech.lang.d2.codegen.il.Nop;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.SymbolTable;

/** Adds a "deallocate" op the last time a long temp is seen. */
public class LongTempDeallocator implements Optimizer {
  private boolean changed;

  @Override
  public ImmutableList<Op> optimize(ImmutableList<Op> program, SymbolTable symtab) {
    changed = false;

    // 1. remove all deallocations
    List<Op> code = new ArrayList<>(program);
    // TODO: capture the location of all deallocates
    code = removeMatchingOps(code, DeallocateTemp.class);

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
      Operand dest = opcode.getDestination();
      if (dest != null && dest.storage() == SymbolStorage.LONG_TEMP
          && !longTempsRead.contains(dest)) {
        //        System.err.printf("I think long temp %s is unread, written at %s\n", dest, opcode);
        // 3. We are writing to this long temp but have never read from it. Delete it.
        code.set(ip, new Nop(opcode));
        changed = true;
      }
    }
    // TODO: capture the location of all deallocates and if they're different than before,
    // set isChanged.
    return ImmutableList.copyOf(code);
  }

  @Override
  public boolean isChanged() {
    return changed;
  }
}
