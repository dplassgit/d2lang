package com.plasstech.lang.d2.codegen;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class LoopInvariantOptimizerTest {
  private Optimizer optimizer =
      new ILOptimizer(
          ImmutableList.of(new ConstantPropagationOptimizer(2), new LoopInvariantOptimizer(2)));

  @Test
  public void oneLoop() {
    TestUtils.optimizeAssertSameVariables(
        "      oneLoop:proc(n:int):int {\n"
            + "  sum = 0\n"
            + "  i = 0 "
            + "  while i < 10 do i = i + 1 {"
            + "    x = n + 1\n"
            + "    sum = sum + 1\n"
            + "  }"
            + "  return sum"
            + "}"
            + "println oneLoop(10)",
        optimizer);
  }

  @Test
  public void oneLoopConstant() {
    TestUtils.optimizeAssertSameVariables(
        "      oneLoop:proc(n:int):int {\n"
            + "  sum = 0\n"
            + "  i = 0 "
            + "  while i < 10 do i = i + 1 {"
            + "    x = 1\n"
            + "    sum = sum + x\n"
            + "  }"
            + "  return x"
            + "}"
            + "println oneLoop(10)",
        optimizer);
  }

  @Test
  public void oneLoopGlobal() {
    TestUtils.optimizeAssertSameVariables(
        "      lexer_text: string // full text\n"
            + "lexer_loc=0  // location inside text\n"
            + "lexer_cc='' // current character\n"
            + "isDigit: proc(c: string): bool {\n"
            + "  return c >= '0' and c <= '9'\n"
            + "}\n"
            + "\n"
            + "advance: proc() {\n"
            + "  if lexer_loc < length(lexer_text) {\n"
            + "    lexer_cc=lexer_text[lexer_loc]\n"
            + "  } else {\n"
            + "    // Indicates no more characters\n"
            + "    lexer_cc=''\n"
            + "  }\n"
            + "  lexer_loc=lexer_loc + 1\n"
            + "}\n"
            + "\n"
            + "makeInt: proc(): int {\n"
            + "  value=0\n"
            + "  while isDigit(lexer_cc) do advance() {\n"
            + "    value = value * 10\n"
            + "    c = asc(lexer_cc) - asc('0')\n"
            + "    value = value + c\n"
            + "  }\n"
            + "  return value\n"
            + "}\n"
            + "lexer_text='314159'\n"
            + "advance()"
            + "println 'Should be 314159:'\n"
            + "pi = makeInt()\n"
            + "println pi\n"
            + "if pi != 314159 {"
            + "   exit 'Bad result'"
            + "}",
        optimizer);
  }

  @Test
  public void oneLoopGlobalSmaller() {
    TestUtils.optimizeAssertSameVariables(
        "      lexer_text='314159 ' // full text\n"
            + "lexer_loc=0  // location inside text\n"
            + "lexer_cc='' // current character\n"
            + "\n"
            + "advance: proc() {\n"
            + "  if lexer_loc < length(lexer_text) {\n"
            + "    lexer_cc=lexer_text[lexer_loc]\n"
            + "  } else {\n"
            + "    // Indicates no more characters\n"
            + "    lexer_cc=''\n"
            + "  }\n"
            + "  lexer_loc=lexer_loc + 1\n"
            + "}\n"
            + "\n"
            + "makeInt: proc(): int {\n"
            + "  value=0\n"
            + "  while lexer_cc!='' and lexer_cc != ' ' do advance() {\n"
            + "    value = value * 10\n"
            + "    c = asc(lexer_cc) - asc('0')\n"
            + "    value = value + c\n"
            + "  }\n"
            + "  return value\n"
            + "}\n"
            + "advance()"
            + "pi = makeInt()\n"
            + "if pi != 314159 {"
            + "   exit 'Bad result'"
            + "}",
        optimizer);
  }

  @Test
  public void nestedLoopsGlobals() {
    TestUtils.optimizeAssertSameVariables(
        "      sum = 0\n"
            + "n = 10\n"
            + "i = 0 while i < n do i = i + 1 {\n"
            + "  y = (n*4)/(n-1)\n"
            + "  j = 0 while j < n do j = j + 1 {\n"
            + "    x = n + 5\n"
            + "    k = 0 while k < n do k = k + 1 {\n"
            + "      z = n * 3\n"
            + "      sum = sum + i\n"
            + "    }\n"
            + "    sum = sum + i\n"
            + "  }\n"
            + "  sum = sum + i\n"
            + "}\n"
            + "println sum",
        optimizer);
  }

  @Test
  public void nestedLoopsLocals() {
    Optimizer optimizer =
        new ILOptimizer(
            ImmutableList.of(new ConstantPropagationOptimizer(2), new LoopInvariantOptimizer(2)));

    TestUtils.optimizeAssertSameVariables(
        "      nestedLoopsLocals:proc(n:int):int {\n"
            + "  sum = 0\n"
            + "  i = 0 while i < n do i = i + 1 {\n"
            + "    y = (n*4)/(n-1)\n"
            + "    j = 0 while j < n do j = j + 1 {\n"
            + "      x = n + 5\n"
            + "      k = 0 while k < n do k = k + 1 {\n"
            + "        z = 3\n"
            + "        sum = sum + y\n"
            + "      }\n"
            + "      sum = sum + i\n"
            + "    }\n"
            + "    sum = sum + i\n"
            + "  }"
            + "  return sum * z"
            + "}"
            + "println nestedLoopsLocals(10)",
        optimizer);
  }
}
