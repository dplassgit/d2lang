package com.plasstech.lang.d2.codegen;

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

// Algorithm:
// 1. find all the __loop_begin s
// 2. find the if after the __loop_begin, this goes to the _loop_end of the most recent begin
// 3. find the __loop_ends
// 4. match up the starts & the ends
class LoopFinder extends DefaultOpcodeVisitor {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final List<Op> code;
  private int ip;
  private int mostRecentBegin = -1;
  // Map from loop end label to start ip
  private Map<String, Integer> loopStarts = new HashMap<>();
  private List<Block> loops = new ArrayList<>();

  public LoopFinder(List<Op> code) {
    this.code = code;
  }

  List<Block> findLoops() {
    mostRecentBegin = -1;
    loopStarts.clear();
    loops.clear();
    for (ip = 0; ip < code.size(); ++ip) {
      code.get(ip).accept(this);
    }
    return loops;
  }

  @Override
  public void visit(Label op) {
    if (op.label().startsWith("__" + Label.LOOP_BEGIN_PREFIX)) {
      mostRecentBegin = ip;
    } else if (op.label().startsWith("__" + Label.LOOP_END_PREFIX)) {
      Integer start = loopStarts.get(op.label());
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
      if (mostRecentBegin != -1) {
        loopStarts.put(op.destination(), mostRecentBegin);
      } else {
        logger.atFine().log("Found 'if' at %d without preceding begin loop", ip);
      }
      mostRecentBegin = -1;
    }
  }
}