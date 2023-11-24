package com.plasstech.lang.d2.optimize;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.il.AllocateOp;
import com.plasstech.lang.d2.codegen.il.ArrayAlloc;
import com.plasstech.lang.d2.codegen.il.ArraySet;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Call;
import com.plasstech.lang.d2.codegen.il.Dec;
import com.plasstech.lang.d2.codegen.il.FieldSetOp;
import com.plasstech.lang.d2.codegen.il.Goto;
import com.plasstech.lang.d2.codegen.il.IdentityOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.IfOp;
import com.plasstech.lang.d2.codegen.il.Inc;
import com.plasstech.lang.d2.codegen.il.Label;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.ProcEntry;
import com.plasstech.lang.d2.codegen.il.ProcExit;
import com.plasstech.lang.d2.codegen.il.Return;
import com.plasstech.lang.d2.codegen.il.Stop;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;

class DeadAssignmentOptimizer extends LineOptimizer {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  // Map from object to line number
  private final Map<Location, Integer> assignments = new HashMap<>();
  private final Map<Location, Integer> tempAssignments = new HashMap<>();

  DeadAssignmentOptimizer(int debugLevel) {
    super(debugLevel);
  }

  @Override
  protected void preProcess() {
    assignments.clear();
    tempAssignments.clear();
  }

  private void recordAssignment(Location destination) {
    switch (destination.storage()) {
      case TEMP:
        tempAssignments.put(destination.baseLocation(), ip());
        break;

      case LOCAL:
      case PARAM:
      case REGISTER:
        assignments.put(destination.baseLocation(), ip());
        break;

      default:
        break;
    }
  }

  private class CallReplacer extends IdentityOpcodeVisitor {
    private int loc;

    public CallReplacer(int loc) {
      super((op) -> {
        // run this for all ops
        deleteAt(loc);
      });
      this.loc = loc;
    }

    @Override
    public void visit(Call op) {
      // change to a call without a return value
      if (op.destination().isPresent()) {
        Call newCall = new Call(op.procSym(), op.actuals(), op.formals(), op.position());
        replaceAt(loc, newCall);
      }
    }
  }

  // This can never be called with a temp, because temps aren't reassigned.
  private boolean killIfReassigned(Location destination) {
    Integer loc = assignments.get(destination.baseLocation());
    if (loc != null) {
      assignments.remove(destination.baseLocation());
      smartDeleteAt(loc);
      return true;
    }
    return false;
  }

  private void smartDeleteAt(Integer loc) {
    Op theOp = code.get(loc);
    theOp.accept(new CallReplacer(loc));
  }

  private void markRead(Operand source) {
    if (!source.isConstant()) {
      Location sourceLocation = (Location) source;
      assignments.remove(sourceLocation.baseLocation());
      // this might have ot change for long lived temps
      tempAssignments.remove(sourceLocation.baseLocation());
    }
  }

  @Override
  public void visit(Stop op) {
    if (!assignments.isEmpty()) {
      logger.at(loggingLevel).log("Killing all unused variables at stop: %s", assignments);
      for (int theIp : assignments.values()) {
        smartDeleteAt(theIp);
      }
      assignments.clear();
    }
    if (!tempAssignments.isEmpty()) {
      logger.at(loggingLevel).log("Killing all unused temps at stop: %s", tempAssignments);
      for (int theIp : tempAssignments.values()) {
        smartDeleteAt(theIp);
      }
      tempAssignments.clear();
    }
  }

  @Override
  public void visit(ProcEntry op) {
    // start of scope.
    assignments.clear();
    tempAssignments.clear();
  }

  @Override
  public void visit(ProcExit op) {
    // End of scope. Kill all assigned-unused.
    if (!assignments.isEmpty()) {
      logger.at(loggingLevel).log("Killing all unused variables at end of proc: %s", assignments);
      for (int theIp : assignments.values()) {
        smartDeleteAt(theIp);
      }
      assignments.clear();
    }
    if (!tempAssignments.isEmpty()) {
      logger.at(loggingLevel).log("Killing all unused temps at end of proc: %s", tempAssignments);
      for (int theIp : tempAssignments.values()) {
        smartDeleteAt(theIp);
      }
      tempAssignments.clear();
    }
  }

  @Override
  public void visit(Label op) {
    // a label means potentially a loop and we can't rely on unused non-temps
    // TODO: this is too aggressive; if a variable isn't used the rest of the procedure,
    // it can probably be killed.
    assignments.clear();
  }

  @Override
  public void visit(Goto op) {
    // a goto means potentially a loop and we can't rely on unused non-temps
    assignments.clear();
  }

  @Override
  public void visit(ArrayAlloc op) {
    markRead(op.sizeLocation());
    Location dest = op.destination();
    killIfReassigned(dest);
    recordAssignment(dest);
  }

  @Override
  public void visit(AllocateOp op) {
    Location dest = op.destination();
    killIfReassigned(dest);
    recordAssignment(dest);
  }

  @Override
  public void visit(ArraySet op) {
    markRead(op.source());
    markRead(op.index());
    // Mark the array read here, so it's not killed.
    markRead(op.array());
  }

  @Override
  public void visit(FieldSetOp op) {
    markRead(op.source());
    // Mark the record read here, since it kind of was.
    markRead(op.recordLocation());
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
    markRead(op.source());
    killIfReassigned(dest);
    recordAssignment(dest);
  }

  @Override
  public void visit(Inc op) {
    Location dest = op.target();
    markRead(dest);
    killIfReassigned(dest);
    recordAssignment(dest);
  }

  @Override
  public void visit(Dec op) {
    Location dest = op.target();
    markRead(dest);
    killIfReassigned(dest);
    recordAssignment(dest);
  }

  @Override
  public void visit(UnaryOp op) {
    markRead(op.operand());
    killIfReassigned(op.destination());
    recordAssignment(op.destination());
  }

  @Override
  public void visit(IfOp op) {
    assignments.clear();
    markRead(op.condition());
  }

  @Override
  public void visit(SysCall op) {
    markRead(op.arg());
  }

  @Override
  public void visit(Call op) {
    ImmutableList<Operand> actualParams = op.actuals();
    for (Operand actual : actualParams) {
      markRead(actual);
    }
    if (op.destination().isPresent()) {
      Location dest = op.destination().get();
      killIfReassigned(dest);
      recordAssignment(dest);
    }
  }

  @Override
  public void visit(Return op) {
    op.returnValueLocation().ifPresent(returnValue -> markRead(returnValue));
  }

  @Override
  public void visit(BinOp op) {
    // Ignore records???
    markRead(op.left());
    markRead(op.right());
    killIfReassigned(op.destination());
    recordAssignment(op.destination());
  }
}
