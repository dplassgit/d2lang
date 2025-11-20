package com.plasstech.lang.d2.optimize;

import static com.plasstech.lang.d2.optimize.testing.OptimizerSubject.assertThatInterpreting;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.testing.TestCode;

public class InlineOptimizerTest {
  // don't make this static because it needs to be reset every test.
  private Optimizer optimizers =
      new ILOptimizer(
          ImmutableList.of(
              new NopOptimizer(),
              new ConstantPropagationOptimizer(0),
              new DeadAssignmentOptimizer(0),
              new DeadCodeOptimizer(0),
              new DeadLabelOptimizer(0),
              new DeadProcOptimizer(0),
              new InlineOptimizer(2)),
          2);

  @Test
  public void shortVoidNoArg() {
    assertThatInterpreting(
            """
            g = 0
            shortVoidNoArg:proc() { g = 3 }
            shortVoidNoArg()
            println g
            """)
        .withOptimizer(optimizers)
        .hasNoCalls();
  }

  @Test
  public void shortVoidGlobal() {
    assertThatInterpreting(
            """
            g = 0
            shortVoidGlobal:proc(n:int) { g = g + n }
            shortVoidGlobal(10)
            println g
            """)
        .withOptimizer(optimizers)
        .hasNoCalls();
  }

  @Test
  public void shortProc() {
    assertThatInterpreting(
            """
            shortProc:proc(n:int):int {
              return n + 1
            }
            """)
        .withOptimizer(optimizers)
        .hasNoCalls();
  }

  @Test
  public void shortProcForward() {
    assertThatInterpreting(
            """
            println shortProc(10)
            shortProc:proc(n:int):int {
              println n
              return n + 1
            }
            """)
        .withOptimizer(optimizers)
        .hasNoCalls();
  }

  @Test
  public void shortProcRecord() {
    assertThatInterpreting(
            """
            rt:record{i:int}
              shortProcRecord:proc():rt {
              x = new rt
              x.i=3
              return x
            }
            r = shortProcRecord()
            println r.i
            """)
        .withOptimizer(optimizers)
        .hasNoCalls();
  }

  @Test
  public void shortButNotinlined() {
    assertThatInterpreting(
            """
            rt:record{i:int} r:rt
            shortButNotinlined:proc() {
              r = new rt
              r.i=3
              r2 = new rt
              r2.i=r.i
              r = null
              r.i=3
            }
            shortButNotinlined()
            println r.i
            """)
        .withOptimizer(optimizers)
        .hasCallsTo("shortButNotinlined");
  }

  @Test
  public void shortProcWithCall() {
    assertThatInterpreting(
            """
            p:proc(n:int):int { return n+1 }
            shortProcWithCall:proc(n:int):int { return p(n) }
            println shortProcWithCall(10)
            """)
        .withOptimizer(optimizers)
        .hasNoCalls();
  }

  @Test
  public void medium() {
    assertThatInterpreting(
            """
            medium:proc(c:string):bool {
              println 'The string is '
              print c
              print ' and its first char is '
              println c>= '1'
              return c >= '1'
            }
            println medium('12')
            println medium('0')
            """)
        .hasNoCalls();
  }

  @Test
  public void linkedList() {
    assertThatInterpreting(TestCode.LINKED_LIST).withOptimizer(optimizers).hasSameVariables();
  }

  @Test
  public void recordLoopInvariant() {
    assertThatInterpreting(TestCode.RECORD_LOOP_INVARIANT)
        .withOptimizer(optimizers)
        .hasSameVariables();
  }

  @Test
  public void ignoreReturnValue() {
    assertThatInterpreting(
            """
            ignoredReturnValue:proc():int {
              return 6
            }
            ignoredReturnValue()
            """)
        .withOptimizer(optimizers)
        .hasNoCalls();
  }

  @Test
  public void ignoreReturnValueSometimes() {
    assertThatInterpreting(
            """
            ignoredReturnValueSometimes:proc():int {
              return 6
            }
            ignoredReturnValueSometimes()
            println ignoredReturnValueSometimes()
            """)
        .withOptimizer(optimizers)
        .hasNoCalls();
  }

  @Test
  public void ignoreReturnValueSometimesAllOpts() {
    assertThatInterpreting(
            """
            ignoredReturnValueSometimesAllOpts:proc():int {
              return 6
            }
            ignoredReturnValueSometimesAllOpts()
            println ignoredReturnValueSometimesAllOpts()
            """)
        .hasNoCalls();
  }

  @Test
  public void multipleCalls() {
    assertThatInterpreting(
            """
            multipleCalls:proc(c:string):bool {
              return c >= '0'
            }
            println multipleCalls('12')
            println multipleCalls('3')
            println multipleCalls('no')
            """)
        .withOptimizer(optimizers)
        .hasNoCalls();
  }

  @Test
  public void tooManyCalls() {
    assertThatInterpreting(
            """
            tooManyCalls:proc(c:string):bool {
              print 'The string is '
              print c
              print ' and its first char is '
              println c >= '5'
              return c >= '5'
            }
            println tooManyCalls('1')
            println tooManyCalls('2')
            println tooManyCalls('3')
            println tooManyCalls('4')
            println tooManyCalls('5')
            println tooManyCalls('6')
            println tooManyCalls('7')
            println tooManyCalls('8')
            println tooManyCalls('9')
            println tooManyCalls('10')
            println tooManyCalls('a')
            """)
        .withOptimizer(optimizers)
        .hasCallsTo("tooManyCalls");
  }

  @Test
  public void twoReturns() {
    assertThatInterpreting(
            """
            twoReturns: proc(n:int):bool {
              if n>0 {return true} else {return false}
            }
            println twoReturns(10)
            println twoReturns(-10)
            """)
        .withOptimizer(optimizers)
        .hasCallsTo("twoReturns");
  }

  @Test
  public void longProc() {
    assertThatInterpreting(
            """
            longProc: proc(n:int):int {
              sum = 0 i=0 while i < n do i = i + 1 {
                sum = sum + i
              }
              return sum
            }
            println longProc(10)
            """)
        .withOptimizer(optimizers)
        .hasCallsTo("longProc");
  }
}
