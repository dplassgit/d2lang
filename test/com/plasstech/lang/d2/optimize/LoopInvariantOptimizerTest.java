package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;
import static com.plasstech.lang.d2.optimize.testing.OptimizerSubject.assertThatInterpreting;

import java.util.function.Predicate;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.YetAnotherCompiler;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.il.Label;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.testing.LocationUtils;
import com.plasstech.lang.d2.common.CompilationConfiguration;
import com.plasstech.lang.d2.interpreter.InterpreterResult;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.testing.TestCode;
import com.plasstech.lang.d2.type.VarType;

public class LoopInvariantOptimizerTest {
  private Optimizer loopOptimizer = new LoopInvariantOptimizer(2);
  private ILOptimizer loopAndConstantOptimizer =
      new ILOptimizer(
          ImmutableList.of(
              new NopOptimizer(),
              new ConstantPropagationOptimizer(0),
              loopOptimizer),
          0);

  @Test
  public void oneLoop() {
    assertThatInterpreting("""
        oneLoop:proc(n:int):int {
          sum = 0
          i = 0
          while i < 10 do i = i + 1 {
            // this can be lifted but... it's temp=n+1 x = temp, and it will only
            // move one instruction, not two...
            sum = sum + 1
            x = n + 1
          }
          return sum
        }
        println oneLoop(10)
        """).withOptimizer(loopAndConstantOptimizer).hasSameVariables();
  }

  @Test
  public void simplest() {
    String program = """
        simplest:proc(n:int) {
          x = 1
          while n > 0 do n = n - 1 {
            x = 0  // this should be lifted out of the loop
          }
          print x
        }
        """;
    CompilationConfiguration config =
        CompilationConfiguration.builder().setSourceCode(program).setOptimize(false)
            .setCodeGenDebugLevel(2).build();
    State state = new YetAnotherCompiler().compile(config);
    // unoptimized:
    /**
     * <pre>
     * __loop_begin_(something):
     * ... 
     * x = 0
     * </pre>
     */
    Transfer transferToX =
        new Transfer(LocationUtils.newStackLocation("x", VarType.INT, 4), ConstantOperand.ZERO,
            null);
    Predicate<Op> matchLoopBegin = (op) -> {
      if (op instanceof Label label) {
        return label.label().startsWith("__loop_begin");
      }
      return false;
    };
    int loopLoc = findMatchingOp(state.ilCode(), matchLoopBegin);
    int transferLoc = findLastMatchingOp(state.ilCode(), (op) -> op.equals(transferToX));
    assertThat(loopLoc).isLessThan(transferLoc);

    InterpreterResult optimizedResult =
        assertThatInterpreting(program).withOptimizer(loopAndConstantOptimizer).hasSameVariables();
    // optimized:
    /**
     * <pre>
     * x = 0
     * ...
     * __loop_begin_(something):
     * </pre>
     */
    loopLoc = findLastMatchingOp(optimizedResult.code(), matchLoopBegin);
    transferLoc = findMatchingOp(optimizedResult.code(), (op) -> op.equals(transferToX));
    assertThat(loopLoc).isGreaterThan(transferLoc);
  }

  private static int findLastMatchingOp(ImmutableList<Op> ilCode, Predicate<Op> predicate) {
    for (int i = ilCode.size() - 1; i > 0; --i) {
      if (predicate.test(ilCode.get(i))) {
        return i;
      }
    }
    return ilCode.size();
  }

  private static int findMatchingOp(ImmutableList<Op> ilCode, Predicate<Op> predicate) {
    return (int) ilCode.stream()
        .takeWhile(predicate)
        .count();
  }

  @Test
  public void simplest_tempInvariant() {
    assertThatInterpreting("""
        simplest:proc(s:string) {
          i = 0 while i < length(s) do i = i + 1 {
            print s[length(s)-1]
          }
        }
        simplest('hi')
        """).withOptimizer(loopAndConstantOptimizer).hasSameVariables();
  }

  @Test
  public void oneLoopContinue() {
    assertThatInterpreting("""
        oneLoopContinue:proc(n:int):int {
          sum = 0
          i = 0
          while true do i = i + 1 {
            x = n + 1
            sum = sum + i
            if i == 5 {
              continue
            } elif i == 10 { break }
          }
          return sum
        }
        println oneLoopContinue(10)
        """).withOptimizer(loopAndConstantOptimizer).hasSameVariables();
  }

  @Test
  public void loopNeverRunStatic() {
    assertThatInterpreting("""
        loopNeverRun:proc(n:int):int {
          sum = 0
          i = 0 x = 0
          while false do i = i + 1 {
            x = n + 1
            sum = sum + x
            println x
          }
          return sum + x
        }
        println loopNeverRun(10)
        """).withOptimizer(loopAndConstantOptimizer).hasSameVariables();
  }

  @Test
  public void loopNeverRunDynamic() {
    assertThatInterpreting("""
        loopNeverRun:proc(n:int, m:int):int {
          sum = 0
          i = 0 x = 0
          while n > 100 do i = i + 1 {
            x = m + 1
            sum = sum + x
          }
          return sum + x
        }
        println loopNeverRun(10, 20)
        """).withOptimizer(loopAndConstantOptimizer).hasSameVariables();
  }

  @Test
  public void oneLoopUnary() {
    assertThatInterpreting("""
        oneLoopUnary:proc(n:int):int {
          sum = 0
          i = 0
          while i < 10 do i = i + 1 {
            x = -n
            sum = sum + 1
          }
          return sum
        }
        println oneLoopUnary(10)
        """).withOptimizer(loopAndConstantOptimizer).hasSameVariables();
  }

  @Test
  public void oneLoopBreak() {
    assertThatInterpreting("""
        oneLoopBreak:proc(n:int):int {
          sum = 0
          i = 0 x = 0
          while i < 10 do i = i + 1 {
            x = n + 1
            sum = sum + 1
            break
          }
          return sum + x
        }
        println oneLoopBreak(10)
        """).withOptimizer(loopAndConstantOptimizer).hasSameVariables();
  }

  @Test
  public void oneLoopBreakInvariantAfterBreak() {
    // this passes if the dead code optimizer runs first because it had already killed the loop
    ILOptimizer ilOptimizer =
        new ILOptimizer(
            ImmutableList.of(
                new NopOptimizer(),
                new DeadCodeOptimizer(0),
                loopOptimizer),
            2);
    assertThatInterpreting("""
        oneLoopBreakInvariantAfterBreak:proc (n:int):int {
          sum = 0
          i = 0 x = 0
          while i < 10 do i = i + 1 {
            break
            sum = sum + 1
            x = n + 1
          }
          return sum + x
        }
        println oneLoopBreakInvariantAfterBreak(10)
        """).withOptimizer(ilOptimizer).hasSameVariables();
  }

  @Test
  public void oneLoopConstant() {
    assertThatInterpreting("""
        oneLoop:proc(n:int):int {
          sum = 0
          i = 0
          x = 0
          while i < 10 do i = i + 1 {
            x = 1
            sum = sum + x
          }
          return x
        }
        println oneLoop(10)
        """).withOptimizer(loopAndConstantOptimizer).hasSameVariables();
  }

  @Test
  public void oneLoopGlobal() {
    assertThatInterpreting("""
        lexer_text: string
        lexer_loc=0
        lexer_cc=''
        isDigit: proc(c: string): bool {
          return c >= '0' and c <= '9'
        }
        advance: proc() {
          if lexer_loc < length(lexer_text) {
            lexer_cc=lexer_text[lexer_loc]
          } else {
            lexer_cc=''
          }
          lexer_loc=lexer_loc + 1
        }
        makeInt: proc(): int {
          value=0
          while isDigit(lexer_cc) do advance() {
            value = value * 10
            c = asc(lexer_cc) - asc('0')
            value = value + c
          }
          return value
        }
        lexer_text='314159'
        advance()
        println 'Should be 314159:'
        pi = makeInt()
        println pi
        """).withOptimizer(loopAndConstantOptimizer).hasSameVariables();
  }

  @Test
  public void oneLoopGlobalSmaller() {
    assertThatInterpreting("""
        lexer_text='314159 ' // full text
        lexer_loc=0  // location inside text
        lexer_cc='' // current character

        advance: proc() {
          if lexer_loc < length(lexer_text) {
            lexer_cc=lexer_text[lexer_loc]
          } else {
            // Indicates no more characters
            lexer_cc=''
          }
          lexer_loc=lexer_loc + 1
        }

        makeInt: proc(): int {
          value=0
          while lexer_cc!='' and lexer_cc != ' ' do advance() {
            value = value * 10
            c = asc(lexer_cc) - asc('0')
            value = value + c
          }
          return value
        }
        advance()
        pi = makeInt()
        if pi != 314159 {
           exit 'Bad result'
        }
        """).withOptimizer(loopAndConstantOptimizer).hasSameVariables();
  }

  @Test
  public void nestedLoopsGlobals() {
    assertThatInterpreting("""
        sum = 0
        n = 10
        i = 0 while i < n do i = i + 1 {
          y = (n*4)/(n-1)
          j = 0 while j < n do j = j + 1 {
            x = n + 5
            k = 0 while k < n do k = k + 1 {
              z = n * 3
              sum = sum + i
            }
            sum = sum + i
          }
          sum = sum + i
        }
        println sum
        """).withOptimizer(loopAndConstantOptimizer).hasSameVariables();
  }

  @Test
  public void nestedLoopsLocals() {
    assertThatInterpreting("""
        nestedLoopsLocals:proc(n:int):int {
          sum = 0
          x = 0
          y = 0
          z = 0
          i = 0 while i < n do i = i + 1 {
            y = (n*4)/(n-1)
            j = 0 while j < n do j = j + 1 {
              x = n + y
              k = 0 while k < n do k = k + 1 {
                z = 3
                sum = sum + y
              }
              sum = sum + i
            }
            sum = sum + i
          }
          return sum * z + x - y
        }
        println nestedLoopsLocals(10)
        """).withOptimizer(loopAndConstantOptimizer).hasSameVariables();
  }

  @Test
  public void twoNestedLoopsWithInvariants() {
    assertThatInterpreting("""
        twoNestedLoopsWithInvariants:proc(n:int):int {
          sum = 0
          x = 0
          y = 0
          i = 0 while i < n do i = i + 1 {
            y = (n*4)/(n-1)
            j = 0 while j < n do j = j + 1 {
              x = n + y
              sum = sum + i
            }
            sum = sum + i println sum
          }
          return sum + x - y
        }
        println twoNestedLoopsWithInvariants(10)
        """).withOptimizer(loopAndConstantOptimizer).hasSameVariables();
  }

  @Test
  public void twoNestedLoops() {
    assertThatInterpreting("""
        twoNestedLoops:proc(n:int):int {
          sum = 0
          i = 0 while i < n do i = i + 1 {
            j = 0 while j < n do j = j + 1 {
              sum = sum + i
            }
            sum = sum + i
          }
          return sum
        }
        println twoNestedLoops(10)
        """).withOptimizer(loopOptimizer).hasSameVariables();
  }

  @Test
  public void recordModifiedInCall() {
    assertThatInterpreting("""
        r: record{i:int}
        updaterec: proc(rec:r) { rec.i = rec.i + 1 }
        recordloopinvariant: proc(rec:r): int {
          while rec.i < 10 { updaterec(rec) }
          return rec.i
        }
        print recordloopinvariant(new r)
        """).withOptimizer(loopOptimizer).hasSameVariables();
  }

  @Test
  public void recordModified() {
    assertThatInterpreting("""
        r: record{i:int}
        recordloopinvariant: proc(rec:r): int {
          while rec.i < 10 { rec.i = rec.i + 1 }
          return rec.i
        }
        print recordloopinvariant(new r)
        """).withOptimizer(loopOptimizer).hasSameVariables();
  }

  @Test
  public void recordModifiedInCallGlobal() {
    assertThatInterpreting("""
        r: record{i:int}
        updaterec: proc(rec:r) { rec.i = rec.i * 2 }
        rec = new r rec.i = 1
        while rec.i < 100 { updaterec(rec) }
        print rec.i
        """).withOptimizer(loopOptimizer).hasSameVariables();
  }

  @Test
  public void recordModifiedGlobal() {
    assertThatInterpreting("""
        r: record{i:int}
        rec = new r rec.i = 1
        while rec.i < 100 { rec.i = rec.i * 2 }
        print rec.i
        """).withOptimizer(loopOptimizer).hasSameVariables();
  }

  @Test
  public void recordLoopInvariant() {
    assertThatInterpreting(TestCode.RECORD_LOOP_INVARIANT).withOptimizer(loopOptimizer)
        .hasSameVariables();
  }

  @Test
  public void recordLoopNonInvariant() {
    assertThatInterpreting(TestCode.RECORD_LOOP_NOT_INVARIANT).withOptimizer(loopOptimizer)
        .hasSameVariables();
  }

  @Test
  public void recordLoopInvariant_loopAndConstantOptimizers() {
    assertThatInterpreting(TestCode.RECORD_LOOP_INVARIANT).withOptimizer(loopAndConstantOptimizer)
        .hasSameVariables();
  }
}
