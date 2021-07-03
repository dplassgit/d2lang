package com.plasstech.lang.d2.codegen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.IfOp;
import com.plasstech.lang.d2.codegen.il.Label;
import com.plasstech.lang.d2.codegen.il.Op;

/*
* If a variable is set in the loop but none of its **non-temp** dependencies are modified
* in the loop -> it's an invariant

* Can push it above the loop, including all its dependencies.
*
* General idea:
*  1. find the next loop start & end.
*     Start = "__loop_begin".
*     end = the __loop_end in the next if.
*     This should work for nested loops.
*  2. Find all **non-temps** that are set in the loop (transfers, inc/dec)
*  3. Find all **non-temps** that are read in the loop (transfers, inc/dec, calls, if, return, syscall, unary?)
*  4. remove vars set from vars read - remaining ones are never read.
*  5. for the remaining vars that are set but not read:
*    a. find all dependencies (go *up* from its set)
*  6. move "above" the __loop_begin
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

    // find loop starts & ends
    List<Loop> loops = findLoops();
    System.out.println(loops);
    for (Loop loop : loops) {
      optimizeLoop(loop);
    }

    return ImmutableList.copyOf(code);
  }

  private void optimizeLoop(Loop loop) {
    changed = false;
  }

  private class LoopFinder extends DefaultOpcodeVisitor {
    private int ip;

    private int mostRecentBegin;
    // Map from loop end label to start ip
    private Map<String, Integer> loopStarts = new HashMap<>();
    private List<Loop> loops = new ArrayList<>();

    @Override
    public void visit(Label op) {
      if (op.label().startsWith("__" + Label.LOOP_BEGIN_PREFIX)) {
        logger.atInfo().log("Found start label %s at %d", op.label(), ip);
        mostRecentBegin = ip;
      } else if (op.label().startsWith("__" + Label.LOOP_END_PREFIX)) {
        int start = loopStarts.get(op.label());
        logger.atInfo().log(
            "Found end label %s at %d matched up with start ip %d", op.label(), ip, start);
        loops.add(new Loop(start, ip));
      }
    }

    @Override
    public void visit(IfOp op) {
      if (op.destination().startsWith("__" + Label.LOOP_END_PREFIX)) {
        logger.atInfo().log(
            "Found next if at %d going to %s, after most recent begin at %d",
            ip, op.destination(), mostRecentBegin);
        loopStarts.put(op.destination(), mostRecentBegin);
      }
    }

    public List<Loop> findLoops() {
      for (ip = 0; ip < code.size(); ++ip) {
        code.get(ip).accept(this);
      }
      return loops;
    }
  }

  private List<Loop> findLoops() {
    LoopFinder finder = new LoopFinder();
    // 1. find all the _loop locations
    // 2. find all the ifs after the _loop locations
    // 3. find the location of the __loop_ends
    // 4. match them up
    return finder.findLoops();
  }

  @Override
  public boolean isChanged() {
    return changed;
  }
}

/*
 * loopinvariant.d, with nested loops, each one has an invariant (y, x)

f:proc(n:int):int {
   sum = 0
   i = 0 while i < n do i = i + 1 {
     y = n + (n*4+5)/(n-1)
     j = 1 while j < n do j = j + 1 {
       x = n * 3 // can be lifted
     }
     sum = sum + i
   }
   return sum
}

val = f(10)
println val
if val != 45 {
  exit "Should have been 45"
}

IL:
goto __after_user_proc_f_1;
f() {
f:
sum = 0;
i = 0;

__loop_begin_2:
__temp4 = i;
__temp5 = n; // loop invariant
__temp6 = __temp4 < __temp5;
__temp7 = NOT __temp6;
if (__temp7) goto __loop_end_4;

// y = n + (n*4+5)/(n-1)
__temp8 = n; // loop invariant
__temp9 = n; // loop invariant
__temp10 = __temp9 << 2;
__temp11 = __temp10 + 5;
__temp12 = n; // loop invariant
__temp13 = __temp12 - 1;
__temp14 = __temp11 / __temp13;
__temp15 = __temp8 + __temp14;
y = __temp15;   // y is invariant since all its non-temp dependencies are never changed in the loop. all the above statements can be lifted.
// so, need to find all the dependencies for each non-temp.

j = 1;

__loop_begin_5:
__temp17 = j;
__temp18 = n; // loop invariant
__temp19 = __temp17 < __temp18;
__temp20 = NOT __temp19;
if (__temp20) goto __loop_end_7;

__temp21 = n; // loop invariant
__temp22 = __temp21 * 3;
x = __temp22; // x is invariant since it's set but never read in its loop. the above 2 statements can be lifted, and then lifted again.
j++;
goto __loop_begin_5;
__loop_end_7:

__temp25 = sum;
__temp26 = i;
__temp27 = __temp25 + __temp26;
sum = __temp27;
i++;
goto __loop_begin_2;

__loop_end_4:
__temp30 = sum;
__stack1 = __temp30;
return __stack1;
} // end proc

__after_user_proc_f_1:
__temp32 = f(10);
val = __temp32;
__temp33 = val;
printf("%s", __temp33);
printf("%s", "
");
__temp34 = val;
__temp35 = __temp34 != 45;
__temp36 = NOT __temp35;
if (__temp36) goto __elif_9;
printf("ERROR: %s", "Should have been 45");
exit(-1); // a.k.a. Stop
__elif_9:
__main:
exit(0); // a.k.a. Stop
*/

/* dumbsort.d, with nested loops. very few invariants except __temp = data
MAX = 99999999;
__main:
data = [2, 1, 4, 8, 6, 98, 0];
__temp3 = MAX;
min = __temp3;
__temp4 = MAX;
__temp5 = - __temp4;
last_min = __temp5;
j = 0;

__loop_begin_1:
__temp7 = j;
__temp8 = data;  // never changes
__temp9 = LENGTH __temp8;  // never changes
__temp10 = __temp7 < __temp9;
__temp11 = NOT __temp10;
if (__temp11) goto __loop_end_3;

__temp12 = MAX;
min = __temp12;
i = 0;

__loop_begin_4:
__temp14 = i;
__temp15 = data;   // never changes
__temp16 = LENGTH __temp15;  // never changes
__temp17 = __temp14 < __temp16;
__temp18 = NOT __temp17;
if (__temp18) goto __loop_end_6;

__temp19 = data;   // never changes
__temp20 = i;
__temp21 = __temp19 [ __temp20;
__temp22 = last_min;
__temp23 = __temp21 > __temp22;
__temp24 = data;   // never changes
__temp25 = i;
__temp26 = __temp24 [ __temp25;
__temp27 = min;
__temp28 = __temp26 < __temp27;
__temp29 = __temp23 AND __temp28;
__temp30 = NOT __temp29;
if (__temp30) goto __elif_8;
__temp31 = data;
__temp32 = i;
__temp33 = __temp31 [ __temp32;
min = __temp33;
__elif_8:
i++;
goto __loop_begin_4;
__loop_end_6:

__temp36 = min;
printf("%s", __temp36);
printf("%s", "
");
__temp37 = min;
last_min = __temp37;
j++;
goto __loop_begin_1;
__loop_end_3:

exit(0); // a.k.a. Stop
*/
