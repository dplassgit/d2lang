package com.plasstech.lang.d2.testing;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assert.fail;

import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.InterpreterExecutor;
import com.plasstech.lang.d2.codegen.ILCodeGenerator;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.interpreter.InterpreterResult;
import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.optimize.ILOptimizer;
import com.plasstech.lang.d2.optimize.Optimizer;
import com.plasstech.lang.d2.parse.Parser;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.type.StaticChecker;

public class TestUtils {

  public static InterpreterResult optimizeAssertSameVariables(String program) {
    return optimizeAssertSameVariables(program, new ILOptimizer(2));
  }

  public static InterpreterResult optimizeAssertSameVariables(String program, Optimizer optimizer) {
    InterpreterExecutor ee = new InterpreterExecutor(program);
    InterpreterResult unoptimizedResult = ee.execute();
    ImmutableList<Op> originalCode = unoptimizedResult.code();

    System.out.printf("\nUNOPTIMIZED:\n");
    System.out.println(Joiner.on("\n").join(originalCode));

    System.out.println("\nUNOPTIMIZED SYSTEM.OUT:");
    System.out.println("------------------------------");
    System.out.println(Joiner.on("").join(unoptimizedResult.environment().output()));

    ImmutableList<Op> optimized = optimizer.optimize(originalCode, unoptimizedResult.symbolTable());
    InterpreterResult optimizedResult = ee.execute(ee.state().addOptimizedCode(optimized));

    System.out.printf("\n%s OPTIMIZED:\n", optimizer.getClass().getSimpleName());
    System.out.println(Joiner.on("\n").join(optimized));

    System.out.println("\nOPTIMIZED SYSTEM.OUT:");
    System.out.println("------------------------------");
    System.out.println(Joiner.on("").join(optimizedResult.environment().output()));

    assertWithMessage("Output should be the same")
        .that(Joiner.on("").join(optimizedResult.environment().output()))
        .isEqualTo(Joiner.on("").join(unoptimizedResult.environment().output()));
    assertMapsSame(
        optimizedResult.environment().variables(), unoptimizedResult.environment().variables());
    // new code should either be faster or smaller or both
    if (unoptimizedResult.linesOfCode() >= optimizedResult.linesOfCode()) {
      assertWithMessage("New code should run in fewer cycles (if it's not smaller)")
          .that(unoptimizedResult.instructionCycles())
          .isAtLeast(optimizedResult.instructionCycles());
    }
    return optimizedResult;
  }

  private static void assertMapsSame(Map<String, Object> actuals, Map<String, Object> expecteds) {
    assertThat(actuals.size()).isEqualTo(expecteds.size());
    for (Map.Entry<String, Object> entry : actuals.entrySet()) {
      // make sure everything's there.
      Object actual = entry.getValue();
      Object expected = expecteds.get(entry.getKey());
      assertWithMessage(String.format("Value of %s is wrong", entry.getKey()))
          .that(actual)
          .isEqualTo(expected);
    }
  }

  public static final String LINKED_LIST =
      "      intlist: record { "
          + "  value: int "
          + "  next: intlist "
          + "} "
          + "new_list: proc(): intlist { "
          + "  return new intlist "
          + "} "
          + "append: proc(it:intlist, newvalue:int) { "
          + "  head = it"
          + "  while head.next != null do head = head.next {} "
          + "  node = new intlist "
          + "  node.value = newvalue "
          + "  head.next = node "
          + "} "
          + "print_list: proc(it: intlist) { "
          + "  if it != null { "
          + "    println it.value "
          + "    print_list(it.next) "
          + "  } "
          + "} "
          + "thelist = new_list() "
          + "thelist.value = 0 "
          + "append(thelist, 1)  "
          + "append(thelist, 2)  "
          + "print_list(thelist) ";

  public static final String RECORD_LOOP_INVARIANT =
      "      rt: record{i:int} "
          + "updaterec: proc(re:rt) { "
          + "  re.i = re.i + 1 "
          + "} "
          + "recordloopinvariant: proc(rec:rt): int { "
          + "  rec.i = 0"
          + "  while rec.i < 10 { "
          + "    updaterec(rec) "
          + "  } "
          + "  return rec.i "
          + "} "
          + "val = recordloopinvariant(new rt) "
          + "println val";

  public static final String RECORD_LOOP_NOT_INVARIANT =
      "      rt: record{i:int} "
          + "recordloopnoninvariant: proc(rec:rt): int { "
          + "  rec.i = 0"
          + "  while rec.i < 10 { "
          + "     re= rec"
          + "     re.i = re.i + 1 "
          + "  } "
          + "  return rec.i "
          + "} "
          + "val = recordloopnoninvariant(new rt) "
          + "println val";

  // Hm, maybe move this to Executor?
  public static State compile(String text) {
    return compile(text, new ILOptimizer(0));
  }

  public static State compile(String text, Optimizer optimizer) {
    Lexer lex = new Lexer(text);
    State state = State.create(text).build();
    Parser parser = new Parser(lex);
    state = parser.execute(state);
    if (state.error()) {
      fail(state.errorMessage());
    }

    StaticChecker checker = new StaticChecker();
    state = checker.execute(state);
    if (state.error()) {
      fail(state.errorMessage());
    }

    ILCodeGenerator codegen = new ILCodeGenerator();
    state = codegen.execute(state);
    if (state.error()) {
      fail(state.errorMessage());
    }

    // Runs all the optimizers.
    ILOptimizer opt = new ILOptimizer(ImmutableList.of(optimizer));
    state = opt.execute(state);
    if (state.error()) {
      fail(state.errorMessage());
    }
    return state;
  }

  /** Trims all comments from the code, and trims each line. */
  public static ImmutableList<String> trimComments(ImmutableList<String> code) {
    return code.stream()
        .map(s -> s.trim())
        .filter(s -> !s.startsWith(";"))
        .map(
            old -> {
              int semi = old.indexOf(';');
              if (semi != -1) {
                return old.substring(0, semi - 1);
              } else {
                return old;
              }
            })
        .map(s -> s.trim())
        .collect(toImmutableList());
  }
}
