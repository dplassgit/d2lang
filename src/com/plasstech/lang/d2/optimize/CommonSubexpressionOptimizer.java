package com.plasstech.lang.d2.optimize;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.LongTempLocation;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.il.ArrayAlloc;
import com.plasstech.lang.d2.codegen.il.ArraySet;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.DeallocateTemp;
import com.plasstech.lang.d2.codegen.il.Dec;
import com.plasstech.lang.d2.codegen.il.FieldSetOp;
import com.plasstech.lang.d2.codegen.il.Inc;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;

/**
 * Replace common subexpressions.
 * 
 * TODO: this is N^2. It could be linear.
 */
public class CommonSubexpressionOptimizer extends LineOptimizer {

  private static final List<Class<? extends Op>> ASSIGNMENT_OPS =
      ImmutableList.of(
          Transfer.class,
          UnaryOp.class,
          ArrayAlloc.class,
          ArraySet.class,
          FieldSetOp.class,
          Dec.class,
          Inc.class,
          SysCall.class);

  CommonSubexpressionOptimizer(int debugLevel) {
    super(debugLevel);
  }

  @Override
  protected void preProcess() {
    // Remove all DeallocateTemp ops.
    code = removeMatchingOps(code, DeallocateTemp.class);
  }

  @Override
  protected void postProcess() {
    // re-add DeallocateTemp operands in the "right" places.
    LongTempDeallocator deallocator = new LongTempDeallocator();
    code = deallocator.optimize(ImmutableList.copyOf(code), null);
    setChanged(isChanged() || deallocator.isChanged());
  }

  @Override
  public void visit(UnaryOp op) {
    Operand source = op.operand();
    if (source.isTemp()) {
      // temps are never re-used so they can't be "common"
      return;
    }

    Location replacement = op.destination();
    // find subsequent ops that have the same source and operator unless the source changes
    // (or there's a stopper). When found, replace the RHS with this op's dest.
    for (int ip = ip() + 1; ip < code.size(); ++ip) {
      Op nextOp = code.get(ip);
      // stop if the dest of this op is the source
      Location dest = nextOp.getDestination(); // THIS MAY BE NULL
      if (source.equals(dest) || replacement.equals(dest)) {
        // we changed a source or our dest, stop.
        break;
      }

      UnaryOp next = getOpAt(ip, UnaryOp.class);
      if (next != null) {
        // does it match? replace it.
        if (next.operand().equals(source) && next.operator() == op.operator()) {
          replacement = replaceIt(replacement, next, ip, op);
        }
        // Replaced, or wrong unary op, just keep going.
      } else if (!isAssignment(nextOp)) {
        // Stop, Goto, Label, Proc Entry, etc.
        break;
      } // else an assignment, which is fine.
    }
  }

  @Override
  public void visit(BinOp op) {
    Operand left = op.left();
    Operand right = op.right();
    if (left.isTemp() || right.isTemp()) {
      // temps are never re-used so they can't be "common"
      return;
    }

    Location replacement = op.destination();
    // Find subsequent ops that have the same left and operator and right unless left or right change
    // (or there's a stopper). when found, replace the RHS with this op's dest.
    for (int ip = ip() + 1; ip < code.size(); ++ip) {
      Op nextOp = code.get(ip);
      // stop if the dest of this op is left or right
      Location dest = nextOp.getDestination(); // THIS MAY BE NULL
      if (left.equals(dest) || right.equals(dest) || replacement.equals(dest)) {
        // we changed a source or our dest, stop.
        break;
      }

      BinOp next = getOpAt(ip, BinOp.class);
      if (next != null) {
        // does it match? replace it.
        if (next.left().equals(left) && next.right().equals(right)
            && next.operator() == op.operator()) {
          replacement = replaceIt(replacement, next, ip, op);
        }
        // Replaced, or non-matching op, just keep going.
      } else if (!isAssignment(nextOp)) {
        // Stop, Goto, Label, Proc Entry, etc.
        break;
      } // else an assignment, which is fine.
    }
  }

  /**
   * Replace the opcode at "ip" (which is "next") with "transfer from replacement to existing
   * destination". If the replacement is a temp, change its "originalOp" to be assigned to a long
   * temp, and then replace its next use with the long temp.
   * 
   * Returns the possibly changed replacement location.
   */
  private Location replaceIt(Location replacement, Op next, int ip, Op originalOp) {
    // We have a candidate.
    logger.at(loggingLevel).log("FOUND MATCH: %s", next);

    if (replacement.isTemp()) {
      // if this op's dest is a temp, make this op's dest a longtemp
      Location longTemp =
          LongTempLocation.create(String.format("_longtemp_%s", replacement.name()),
              replacement.type());
      replaceAt(ip(), originalOp.setDestination(longTemp));

      // find where it's used and replace it. Since it's a temp it can only be used once.
      int tempUsedIp = findTempUse(replacement, ip() + 1);
      Op tempUsedOp = code.get(tempUsedIp);
      Op remappedOp = tempUsedOp.setSource(replacement, longTemp);
      replaceAt(tempUsedIp, remappedOp);

      // overwrite for later re-use
      replacement = longTemp;
    }

    Location destination = next.getDestination();
    replaceAt(ip, new Transfer(destination, replacement, next.position()));
    return replacement;
  }

  private int findTempUse(Location target, int ip) {
    while (ip < code.size()) {
      Op op = code.get(ip);
      List<Operand> sources = op.getSources();
      if (sources.contains(target)) {
        // found it.
        return ip;
      }
      ip++;
    }
    throw new IllegalStateException("Could not find use of temp " + target.name());
  }

  private static boolean isAssignment(Op nextOp) {
    Class<? extends Op> clazz = nextOp.getClass();
    return ASSIGNMENT_OPS.contains(clazz);
  }
}
