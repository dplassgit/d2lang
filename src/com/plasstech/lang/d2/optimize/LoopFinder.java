package com.plasstech.lang.d2.optimize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.IfOp;
import com.plasstech.lang.d2.codegen.il.Label;
import com.plasstech.lang.d2.codegen.il.Op;

/** Finds loops in a codebase. */

// the (new) order is:
//   if (!condition) goto loop_end
//   ...
//   loop_begin
//   ...
//   if condition goto loop_begin // we can ignore this one.
//   loop_end

// Algorithm:
// 1. find if goto __loop_end, store name as "most recent end"
// 2. find the next __loop_begin, store in map from "most recent end" to current (start) IP
// 3. find the __loop_ends - look up in map, loop is from map IP to current (end) IP
class LoopFinder extends DefaultOpcodeVisitor {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final List<Op> code;
  private int ip;
  private String mostRecentEnd;
  // Map from loop end label to start ip
  private Map<String, Integer> endLabelToStartIp = new HashMap<>();
  private List<Block> loops = new ArrayList<>();

  public LoopFinder(List<Op> code) {
    this.code = code;
  }

  List<Block> findLoops() {
    mostRecentEnd = null;
    endLabelToStartIp.clear();
    loops.clear();
    for (ip = 0; ip < code.size(); ++ip) {
      code.get(ip).accept(this);
    }
    return loops;
  }

  @Override
  public void visit(Label op) {
    if (op.label().startsWith("__" + Label.LOOP_BEGIN_PREFIX)) {
      if (mostRecentEnd == null) {
        logger.atFine().log("Found loop start %s without loop end", op.label());
      } else {
        endLabelToStartIp.put(mostRecentEnd, ip);
      }
    } else if (op.label().startsWith("__" + Label.LOOP_END_PREFIX)) {
      Integer start = endLabelToStartIp.get(op.label());
      if (start != null) {
        // matched up this end, with the start.
        loops.add(new Block(start, ip));
      } else {
        logger.atFine().log("Could not find start to loop %s", op.label());
      }
    }
  }

  @Override
  public void visit(IfOp op) {
    if (op.destination().startsWith("__" + Label.LOOP_END_PREFIX)) {
      mostRecentEnd = op.destination();
    }
  }
}