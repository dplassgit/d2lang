package com.plasstech.lang.d2.codegen;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class LoopInvariantOptimizerTest {
  private static Optimizer loopOptimizer = new LoopInvariantOptimizer(2);
  private static Optimizer ilOptimizer =
      new ILOptimizer(
              ImmutableList.of(
                  new ArithmeticOptimizer(2),
                  new ConstantPropagationOptimizer(2),
                  new DeadCodeOptimizer(2),
                  new DeadLabelOptimizer(2),
                  new DeadAssignmentOptimizer(2),
                  new IncDecOptimizer(2),
                  new InlineOptimizer(2),
                  new LoopInvariantOptimizer(2) // ,
                  ))
          .setDebugLevel(2);
  private static Optimizer loopAndConstantOptimizer =
      new ILOptimizer(
              ImmutableList.of(new ConstantPropagationOptimizer(2), new LoopInvariantOptimizer(2)))
          .setDebugLevel(2);

  @Test
  public void oneLoop() {
    TestUtils.optimizeAssertSameVariables(
        "      oneLoop:proc(n:int):int { "
            + "  sum = 0 "
            + "  i = 0 "
            + "  while i < 10 do i = i + 1 {"
            + "    x = n + 1 "
            + "    sum = sum + 1 "
            + "  }"
            + "  return sum"
            + "}"
            + "println oneLoop(10)",
        loopAndConstantOptimizer);
  }

  @Test
  public void oneLoopContinue() {
    TestUtils.optimizeAssertSameVariables(
        "      oneLoopContinue:proc(n:int):int { "
            + "  sum = 0 "
            + "  i = 0 "
            + "  while true do i = i + 1 {"
            + "    x = n + 1 "
            + "    sum = sum + i "
            + "    if i == 5 { "
            + "      continue "
            + "    } elif i == 10 { break } "
            + "  } "
            + "  return sum"
            + "}"
            + "println oneLoopContinue(10)",
        loopAndConstantOptimizer);
  }

  @Test
  public void loopNeverRunStatic() {
    TestUtils.optimizeAssertSameVariables(
        "      loopNeverRun:proc(n:int):int { "
            + "  sum = 0 "
            + "  i = 0 x = 0"
            + "  while false do i = i + 1 { "
            + "    x = n + 1 "
            + "    sum = sum + x "
            + "    println x"
            + "  } "
            + "  return sum + x"
            + "}"
            + "println loopNeverRun(10)",
        loopAndConstantOptimizer);
  }

  @Test
  public void loopNeverRunDynamic() {
    TestUtils.optimizeAssertSameVariables(
        "      loopNeverRun:proc(n:int, m:int):int { "
            + "  sum = 0 "
            + "  i = 0 x = 0"
            + "  while n > 100 do i = i + 1 {"
            + "    x = m + 1 "
            + "    sum = sum + x "
            + "  } "
            + "  return sum + x"
            + "}"
            + "println loopNeverRun(10, 20)",
        loopAndConstantOptimizer);
  }

  @Test
  public void oneLoopUnary() {
    TestUtils.optimizeAssertSameVariables(
        "      oneLoopUnary:proc(n:int):int { "
            + "  sum = 0 "
            + "  i = 0 "
            + "  while i < 10 do i = i + 1 {"
            + "    x = -n "
            + "    sum = sum + 1 "
            + "  }"
            + "  return sum"
            + "}"
            + "println oneLoopUnary(10)",
        loopAndConstantOptimizer);
  }

  @Test
  public void oneLoopBreak() {
    TestUtils.optimizeAssertSameVariables(
        "      oneLoopBreak:proc(n:int):int { "
            + "  sum = 0 "
            + "  i = 0 x = 0"
            + "  while i < 10 do i = i + 1 {"
            + "    x = n + 1 "
            + "    sum = sum + 1 "
            + "    break"
            + "  }"
            + "  return sum + x"
            + "}"
            + "println oneLoopBreak(10)",
        loopAndConstantOptimizer);
  }

  @Test
  public void oneLoopBreakInvariantAfterBreak() {
    // this passes if the dead code optimizer runs first because it had already killed the loop
    TestUtils.optimizeAssertSameVariables(
        "      oneLoopBreakInvariantAfterBreak:proc (n:int):int { "
            + "  sum = 0 "
            + "  i = 0 x = 0"
            + "  while i < 10 do i = i + 1 {"
            + "    break"
            + "    sum = sum + 1 "
            + "    x = n + 1 "
            + "  }"
            + "  return sum + x"
            + "}"
            + "println oneLoopBreakInvariantAfterBreak(10)",
        ilOptimizer);
  }

  @Test
  public void oneLoopConstant() {
    TestUtils.optimizeAssertSameVariables(
        "      oneLoop:proc(n:int):int { "
            + "  sum = 0 "
            + "  i = 0 "
            + "  while i < 10 do i = i + 1 {"
            + "    x = 1 "
            + "    sum = sum + x "
            + "  }"
            + "  return x"
            + "}"
            + "println oneLoop(10)",
        loopAndConstantOptimizer);
  }

  @Test
  public void oneLoopGlobal() {
    TestUtils.optimizeAssertSameVariables(
        "      lexer_text: string "
            + "lexer_loc=0 "
            + "lexer_cc='' "
            + "isDigit: proc(c: string): bool {"
            + "  return c >= '0' and c <= '9'"
            + "}"
            + "advance: proc() {"
            + "  if lexer_loc < length(lexer_text) {"
            + "    lexer_cc=lexer_text[lexer_loc]"
            + "  } else { "
            + "    lexer_cc=''"
            + "  }"
            + "  lexer_loc=lexer_loc + 1"
            + "}"
            + "makeInt: proc(): int {"
            + "  value=0"
            + "  while isDigit(lexer_cc) do advance() {"
            + "    value = value * 10"
            + "    c = asc(lexer_cc) - asc('0')"
            + "    value = value + c"
            + "  }"
            + "  return value"
            + "}"
            + "lexer_text='314159' "
            + "advance()"
            + "println 'Should be 314159:' "
            + "pi = makeInt() "
            + "println pi ",
        loopAndConstantOptimizer);
  }

  @Test
  public void oneLoopGlobalSmaller() {
    TestUtils.optimizeAssertSameVariables(
        "      lexer_text='314159 ' // full text "
            + "lexer_loc=0  // location inside text "
            + "lexer_cc='' // current character "
            + " "
            + "advance: proc() { "
            + "  if lexer_loc < length(lexer_text) { "
            + "    lexer_cc=lexer_text[lexer_loc] "
            + "  } else { "
            + "    // Indicates no more characters "
            + "    lexer_cc='' "
            + "  } "
            + "  lexer_loc=lexer_loc + 1 "
            + "} "
            + " "
            + "makeInt: proc(): int { "
            + "  value=0 "
            + "  while lexer_cc!='' and lexer_cc != ' ' do advance() { "
            + "    value = value * 10 "
            + "    c = asc(lexer_cc) - asc('0') "
            + "    value = value + c "
            + "  } "
            + "  return value "
            + "} "
            + "advance()"
            + "pi = makeInt() "
            + "if pi != 314159 {"
            + "   exit 'Bad result'"
            + "}",
        loopAndConstantOptimizer);
  }

  @Test
  public void nestedLoopsGlobals() {
    TestUtils.optimizeAssertSameVariables(
        "      sum = 0 "
            + "n = 10 "
            + "i = 0 while i < n do i = i + 1 { "
            + "  y = (n*4)/(n-1) "
            + "  j = 0 while j < n do j = j + 1 { "
            + "    x = n + 5 "
            + "    k = 0 while k < n do k = k + 1 { "
            + "      z = n * 3 "
            + "      sum = sum + i "
            + "    } "
            + "    sum = sum + i "
            + "  } "
            + "  sum = sum + i "
            + "} "
            + "println sum",
        loopAndConstantOptimizer);
  }

  @Test
  public void nestedLoopsLocals() {
    TestUtils.optimizeAssertSameVariables(
        "      nestedLoopsLocals:proc(n:int):int { "
            + "  sum = 0 "
            + "  i = 0 while i < n do i = i + 1 { "
            + "    y = (n*4)/(n-1) "
            + "    j = 0 while j < n do j = j + 1 { "
            + "      x = n + y "
            + "      k = 0 while k < n do k = k + 1 { "
            + "        z = 3 "
            + "        sum = sum + y "
            + "      } "
            + "      sum = sum + i "
            + "    } "
            + "    sum = sum + i "
            + "  }"
            + "  return sum * z + x - y"
            + "}"
            + "println nestedLoopsLocals(10)",
        loopAndConstantOptimizer);
  }

  @Test
  public void twoNestedLoopsWithInvariants() {
    TestUtils.optimizeAssertSameVariables(
        "      twoNestedLoopsWithInvariants:proc(n:int):int { "
            + "  sum = 0 "
            + "  i = 0 while i < n do i = i + 1 { "
            + "    y = (n*4)/(n-1) "
            + "    j = 0 while j < n do j = j + 1 { "
            + "      x = n + y "
            + "      sum = sum + i "
            + "    } "
            + "    sum = sum + i println sum"
            + "  }"
            + "  return sum + x - y"
            + "}"
            + "println twoNestedLoopsWithInvariants(10)",
        loopAndConstantOptimizer);
  }

  @Test
  public void twoNestedLoops() {
    TestUtils.optimizeAssertSameVariables(
        "      twoNestedLoops:proc(n:int):int { "
            + "  sum = 0 "
            + "  i = 0 while i < n do i = i + 1 { "
            + "    j = 0 while j < n do j = j + 1 { "
            + "      sum = sum + i "
            + "    } "
            + "    sum = sum + i "
            + "  }"
            + "  return sum"
            + "}"
            + "println twoNestedLoops(10)",
        loopOptimizer);
  }

  @Test
  public void recordModifiedInCall() {
    TestUtils.optimizeAssertSameVariables(
        "      r: record{i:int}"
            + "updaterec: proc(rec:r) { rec.i = rec.i + 1 }"
            + "recordloopinvariant: proc(rec:r): int {"
            + "  while rec.i < 10 { updaterec(rec) }"
            + "  return rec.i"
            + "}"
            + "print recordloopinvariant(new r)",
        loopOptimizer);
  }

  @Test
  public void recordModified() {
    TestUtils.optimizeAssertSameVariables(
        "      r: record{i:int}"
            + "recordloopinvariant: proc(rec:r): int {"
            + "  while rec.i < 10 { rec.i = rec.i + 1 }"
            + "  return rec.i"
            + "}"
            + "print recordloopinvariant(new r)",
        loopOptimizer);
  }

  @Test
  public void recordModifiedInCallGlobal() {
    TestUtils.optimizeAssertSameVariables(
        "      r: record{i:int}"
            + "updaterec: proc(rec:r) { rec.i = rec.i * 2 }"
            + "rec = new r rec.i = 1"
            + "while rec.i < 100 { updaterec(rec) }"
            + "print rec.i",
        loopOptimizer);
  }

  @Test
  public void recordModifiedGlobal() {
    TestUtils.optimizeAssertSameVariables(
        "      r: record{i:int}"
            + "rec = new r rec.i = 1 "
            + "while rec.i < 100 { rec.i = rec.i * 2 }"
            + "print rec.i",
        loopOptimizer);
  }

  @Test
  public void recordLoopInvariant() {
    TestUtils.optimizeAssertSameVariables(TestUtils.RECORD_LOOP_INVARIANT, loopOptimizer);
  }

  @Test
  public void recordLoopInvariant_loopAndConstantOptimizers() {
    TestUtils.optimizeAssertSameVariables(
        TestUtils.RECORD_LOOP_INVARIANT, loopAndConstantOptimizer);
  }
}
