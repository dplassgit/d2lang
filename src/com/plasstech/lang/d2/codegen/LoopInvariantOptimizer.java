package com.plasstech.lang.d2.codegen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Call;
import com.plasstech.lang.d2.codegen.il.Dec;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.IfOp;
import com.plasstech.lang.d2.codegen.il.Inc;
import com.plasstech.lang.d2.codegen.il.Label;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Return;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.type.SymbolStorage;

/*
* If a variable is set in the loop but none of its **non-temp** dependencies are modified
* in the loop -> it's an invariant

* Can push it above the loop, including all its dependencies.
*
* General idea:
*  1. find the next loop start & end. OK
*     Start = "__loop_begin".
*     end = the __loop_end in the next if.
*     This should work for nested loops.
*  2. Find all vars that are set in the loop (transfers, inc/dec, binop, unary) OK
*  3. Find all vars that are read in the loop (transfers, inc/dec, calls, if, return, syscall, binop, unary) OK
*  4. for each transfer to a **temp**, if source is not set in the loop, the statement can be moved outside the loop
*  5. move "above" the __loop_begin
*
* Similarly, for temps:
*  if a temp is set to a value that is not itself set in the loop, it's invariant <<< EASY CASE.
*/
class LoopInvariantOptimizer implements Optimizer {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final Level loggingLevel;
  private boolean changed;
  private ArrayList<Op> code;

  LoopInvariantOptimizer(int debugLevel) {
    loggingLevel = toLoggingLevel(debugLevel);
  }

  private static class Loop {
    int start;
    int end;

    Loop(int start, int end) {
      this.start = start;
      this.end = end;
    }

    @Override
    public String toString() {
      return String.format("Loop %d to %d", start, end);
    }
  }

  @Override
  public ImmutableList<Op> optimize(ImmutableList<Op> program) {
    code = new ArrayList<>(program);
    changed = false;

    // Find loop starts & ends
    LoopFinder finder = new LoopFinder();
    List<Loop> loops = finder.findLoops();
    for (Loop loop : loops) {
      while (optimizeLoop(loop)) {}
      // OH NO the starts and ends may have moved...do we just give up? or re-start?
    }

    return ImmutableList.copyOf(code);
  }

  private boolean optimizeLoop(Loop loop) {
    logger.at(loggingLevel).log("Optimizing %s", loop);

    SetterGetterFinder finder = new SetterGetterFinder();
    for (int ip = loop.start; ip < loop.end; ++ip) {
      code.get(ip).accept(finder);
    }
    logger.at(loggingLevel).log("Setters = %s", finder.setters);
    logger.at(loggingLevel).log("Getters = %s", finder.getters);

    TransferMover mover = new TransferMover(finder);
    boolean optimizedLoop = false;
    for (int ip = loop.start; ip < loop.end; ++ip) {
      mover.reset();
      Op op = code.get(ip);
      op.accept(mover);
      if (mover.liftedLast()) {
        optimizedLoop = true;
        code.remove(ip);
        code.add(loop.start, op);
        loop.start++;
      }
    }

    changed |= optimizedLoop;
    return optimizedLoop;
  }

  private class TransferMover extends DefaultOpcodeVisitor {
    private SetterGetterFinder finder;
    private boolean lifted;

    private TransferMover(SetterGetterFinder finder) {
      this.finder = finder;
    }

    public boolean liftedLast() {
      return lifted;
    }

    public void reset() {
      lifted = false;
    }

    @Override
    public void visit(Transfer op) {
      if (op.destination() instanceof TempLocation) {
        // Transferring to a temp
        if (op.source().isConstant()) {
          // can lift
          logger.at(loggingLevel).log("Lifting to temp of const: %s", op);
          lifted = true;
        } else if (!finder.setters.contains(op.source())) {
          Location source = (Location) op.source();
          if (source.storage() != SymbolStorage.GLOBAL) {
            // can lift, maybe. but if it's being copied from a *global* it's impossible to know
            // if the global was modified in the loop
            logger.at(loggingLevel).log(
                "Lifting assignment to temp of non-global invariant: %s", op);
            lifted = true;
          }
        }
      }
    }
  }

  // Finds all the uses of an operand.
  private class SetterGetterFinder extends DefaultOpcodeVisitor {
    private Set<Operand> setters = new HashSet<>();
    // These aren't really used:
    private Set<Operand> getters = new HashSet<>();

    @Override
    public void visit(Call op) {
      if (op.destination().isPresent()) {
        setters.add(op.destination().get());
      }
      for (Operand actual : op.actualLocations()) {
        getters.add(actual);
      }
    }

    @Override
    public void visit(Dec op) {
      getters.add(op.target());
      setters.add(op.target());
    }

    @Override
    public void visit(Inc op) {
      getters.add(op.target());
      setters.add(op.target());
    }

    @Override
    public void visit(IfOp op) {
      if (!op.condition().isConstant()) {
        getters.add(op.condition());
      }
    }

    @Override
    public void visit(Return op) {
      if (op.returnValueLocation().isPresent()) {
        if (!op.returnValueLocation().get().isConstant()) {
          getters.add(op.returnValueLocation().get());
        }
      }
    }

    @Override
    public void visit(SysCall op) {
      switch (op.call()) {
        case INPUT:
          setters.add(op.arg()); // is this right?!
          break;
        default:
          if (!op.arg().isConstant()) {
            getters.add(op.arg());
          }
          break;
      }
    }

    @Override
    public void visit(Transfer op) {
      if (!op.source().isConstant()) {
        getters.add(op.source());
      }
      setters.add(op.destination());
    }

    @Override
    public void visit(BinOp op) {
      if (!op.left().isConstant()) {
        getters.add(op.left());
      }
      if (!op.right().isConstant()) {
        getters.add(op.right());
      }
      setters.add(op.destination());
    }

    @Override
    public void visit(UnaryOp op) {
      if (!op.operand().isConstant()) {
        getters.add(op.operand());
      }
      setters.add(op.destination());
    }
  }

  // 1. find all the _loop starts
  // 2. find all the ifs after the _loop starts
  // 3. find the location of the __loop_ends
  // 4. match them up
  private class LoopFinder extends DefaultOpcodeVisitor {
    private int ip;

    private int mostRecentBegin = -1;
    // Map from loop end label to start ip
    private Map<String, Integer> loopStarts = new HashMap<>();
    private List<Loop> loops = new ArrayList<>();

    @Override
    public void visit(Label op) {
      if (op.label().startsWith("__" + Label.LOOP_BEGIN_PREFIX)) {
        mostRecentBegin = ip;
      } else if (op.label().startsWith("__" + Label.LOOP_END_PREFIX)) {
        Integer start = loopStarts.get(op.label());
        if (start != null) {
          loops.add(new Loop(start, ip));
        } else {
          logger.atWarning().log("Could not find start to loop %s", op.label());
        }
      }
    }

    @Override
    public void visit(IfOp op) {
      if (op.destination().startsWith("__" + Label.LOOP_END_PREFIX)) {
        if (mostRecentBegin != -1) {
          loopStarts.put(op.destination(), mostRecentBegin);
        } else {
          logger.atWarning().log("Found 'if' at %d without preceding begin", ip);
        }
        mostRecentBegin = -1;
      }
    }

    public List<Loop> findLoops() {
      for (ip = 0; ip < code.size(); ++ip) {
        code.get(ip).accept(this);
      }
      return loops;
    }
  }

  @Override
  public boolean isChanged() {
    return changed;
  }
}

/*
 * with nested loops, each one has an invariant (y, x)

sum = 0
n = 10
i = 0
while i < n do i = i + 1 {
  y = (n*4)/(n-1)
  j = 0
  while j < n do j = j + 1 {
    x = n + 5
    k = 0
    while k < n do k = k + 1 {
      z = n * 3
      sum = sum + i
    }
    sum = sum + i
  }
  sum = sum + i
}
println sum


0   __temp1 = 0;
1   sum = __temp1;
2   __temp2 = 10;
3   n = __temp2;
4   __temp3 = 0;
5   i = __temp3;
6   __loop_begin_1:
7   __temp4 = i;
8   __temp5 = n; // can be lifted
9   __temp6 = __temp4 < __temp5;
10  __temp7 = NOT __temp6;
11  if (__temp7) goto __loop_end_3;
12  __temp8 = n; // can be lifted
13  __temp9 = __temp8 * 4;
14  __temp10 = n; // can be lifted
15  __temp11 = __temp10 - 1;
16  __temp12 = __temp9 / __temp11;
17  y = __temp12;
18  __temp13 = 0; // can be lifted
19  j = __temp13;
20  __loop_begin_4:
21  __temp14 = j;
22  __temp15 = n; // can be lifted
23  __temp16 = __temp14 < __temp15;
24  __temp17 = NOT __temp16;
25  if (__temp17) goto __loop_end_6;
26  __temp18 = n; // can be lifted
27  __temp19 = __temp18 + 5;
28  x = __temp19;
29  __temp20 = 0; // can be lifted
30  k = __temp20;

31  __loop_begin_7:
32  __temp21 = k;
33  __temp22 = n; // can be lifted
34  __temp23 = __temp21 < __temp22;
35  __temp24 = NOT __temp23;
36  if (__temp24) goto __loop_end_9;
37  __temp25 = n; // can be lifted
38  __temp26 = __temp25 * 3;
39  z = __temp26;
40  __temp27 = sum;
41  __temp28 = i;
42  __temp29 = __temp27 + __temp28;
43  sum = __temp29;
44  __loop_increment_8:
45  __temp30 = k;
46  __temp31 = __temp30 + 1;
47  k = __temp31;
48  goto __loop_begin_7;
49  __loop_end_9:

50  __temp32 = sum;
51  __temp33 = i;
52  __temp34 = __temp32 + __temp33;
53  sum = __temp34;
54  __loop_increment_5:
55  __temp35 = j;
56  __temp36 = __temp35 + 1;
57  j = __temp36;
58  goto __loop_begin_4;
59  __loop_end_6:
60  __temp37 = sum;
61  __temp38 = i;
62  __temp39 = __temp37 + __temp38;
63  sum = __temp39;
64  __loop_increment_2:
65  __temp40 = i;
66  __temp41 = __temp40 + 1;
67  i = __temp41;
68  goto __loop_begin_1;
69  __loop_end_3:
70  __temp42 = sum;
71  printf("%s", __temp42);
72  printf("%s", "\n");
73  __main:
74  exit(0); // a.k.a. Stop
*/
