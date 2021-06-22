package com.plasstech.lang.d2.codegen;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Call;
import com.plasstech.lang.d2.codegen.il.Goto;
import com.plasstech.lang.d2.codegen.il.IfOp;
import com.plasstech.lang.d2.codegen.il.Label;
import com.plasstech.lang.d2.codegen.il.ProcEntry;
import com.plasstech.lang.d2.codegen.il.ProcExit;
import com.plasstech.lang.d2.codegen.il.Return;
import com.plasstech.lang.d2.codegen.il.Stop;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.type.SymbolStorage;

public class DeadAssignmentOptimizer extends LineOptimizer {
  private final FluentLogger logger = FluentLogger.forEnclosingClass();

  // Map from name to line number
  private final Map<String, Integer> assignments = new HashMap<>();

  private void recordAssignment(Location destination) {
    if (destination.storage() != SymbolStorage.GLOBAL) {
      assignments.put(destination.name(), ip);
    }
  }

  private boolean deleteRecentlyAssigned(Location destination) {
    if (destination.storage() != SymbolStorage.GLOBAL) {
      Integer loc = assignments.get(destination.name());
      if (loc != null) {
        assignments.remove(destination.name());
        deleteAt(loc);
        return true;
      }
    }
    return false;
  }

  private void markAsUsed(Operand source) {
    if (!source.isConstant()) {
      Location sourceLocation = (Location) source;
      assignments.remove(sourceLocation.name());
    }
  }

  @Override
  public void visit(Stop op) {
    assignments.clear();
  }

  @Override
  public void visit(ProcEntry op) {
    // start of scope.
    assignments.clear();
  }

  @Override
  public void visit(ProcExit op) {
    // End of scope. Kill all assigned-unused.
    for (int theIp : assignments.values()) {
      logger.atInfo().log("Killing all unused variables at end of proc: " + assignments);
      deleteAt(theIp);
    }
    assignments.clear();
  }

  @Override
  public void visit(Label op) {
    // a label means potentially a loop and we can't rely on unused settings
    assignments.clear();
  }

  @Override
  public void visit(Goto op) {
    // a goto means potentially a loop and we can't rely on unused settings
    assignments.clear();
  }

  // a=123 - add a to "assignedUnused" map at line IP
  // a=234 - a already in "assignedUnused" map - delete at old IP
  // if a goto 4 - clear map
  // c=a+temp - remove a and b from "assignedUnused" map
  // b=a  - remove a from assignedunused map
  // end of proc - Nop all from assignedunused map
  // label: clear map
  // goto: clear map
  // call: for each param, remove from assignedunused map
  @Override
  public void visit(Transfer op) {
    Location dest = op.destination();
    markAsUsed(op.source());
    deleteRecentlyAssigned(dest);
    recordAssignment(dest);
  }

  @Override
  public void visit(UnaryOp op) {
    recordAssignment(op.destination());
    markAsUsed(op.operand());
  }

  @Override
  public void visit(IfOp op) {
    assignments.clear();
  }

  @Override
  public void visit(SysCall op) {
    Operand operand = op.arg();
    markAsUsed(operand);
  }

  @Override
  public void visit(Call op) {
    ImmutableList<Operand> actualParams = op.actualLocations();
    for (Operand actual : actualParams) {
      markAsUsed(actual);
    }
  }

  @Override
  public void visit(Return op) {
    if (op.returnValueLocation().isPresent()) {
      Operand returnValue = op.returnValueLocation().get();
      markAsUsed(returnValue);
    }
  }

  @Override
  public void visit(BinOp op) {
    markAsUsed(op.left());
    markAsUsed(op.right());
    deleteRecentlyAssigned(op.destination());
  }
}
