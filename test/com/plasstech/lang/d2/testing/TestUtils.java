package com.plasstech.lang.d2.testing;

import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.ExecutionEnvironment;
import com.plasstech.lang.d2.codegen.CodeGenerator;
import com.plasstech.lang.d2.codegen.ILCodeGenerator;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.interpreter.ExecutionResult;
import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.optimize.ILOptimizer;
import com.plasstech.lang.d2.optimize.Optimizer;
import com.plasstech.lang.d2.parse.Parser;
import com.plasstech.lang.d2.parse.node.Node;
import com.plasstech.lang.d2.type.StaticChecker;
import com.plasstech.lang.d2.type.SymTab;
import com.plasstech.lang.d2.type.TypeCheckResult;

public class TestUtils {

  public static ExecutionResult optimizeAssertSameVariables(String program) {
    return optimizeAssertSameVariables(program, new ILOptimizer(2));
  }

  public static ExecutionResult optimizeAssertSameVariables(String program, Optimizer optimizer) {
    ExecutionEnvironment ee = new ExecutionEnvironment(program);
    ExecutionResult unoptimizedResult = ee.execute();
    System.out.printf("\nUNOPTIMIZED:\n");
    System.out.println(Joiner.on("\n").join(unoptimizedResult.code()));

    System.out.println("\nUNOPTIMIZED SYSTEM.OUT:");
    System.out.println("------------------------------");
    System.out.println(Joiner.on("").join(unoptimizedResult.environment().output()));

    ImmutableList<Op> originalCode = ImmutableList.copyOf(unoptimizedResult.code());

    ImmutableList<Op> optimized = optimizer.optimize(originalCode);
    System.out.printf("\n%s OPTIMIZED:\n", optimizer.getClass().getSimpleName());
    System.out.println(Joiner.on("\n").join(optimized));

    ExecutionResult optimizedResult = ee.execute(optimized);

    System.out.println("\nOPTIMIZED SYSTEM.OUT:");
    System.out.println("------------------------------");
    System.out.println(Joiner.on("").join(optimizedResult.environment().output()));

    assertWithMessage("Output should be the same")
        .that(optimizedResult.environment().output())
        .isEqualTo(unoptimizedResult.environment().output());
    assertWithMessage("Environment should be the same")
        .that(optimizedResult.environment().variables())
        .isEqualTo(unoptimizedResult.environment().variables());
    //    assertWithMessage("New code should be smaller")
    //        .that(unoptimizedResult.linesOfCode())
    //        .isAtLeast(optimizedResult.linesOfCode());
    assertWithMessage("New code should run in fewer cycles")
        .that(unoptimizedResult.instructionCycles())
        .isAtLeast(optimizedResult.instructionCycles());
    return optimizedResult;
  }

  public static final String LINKED_LIST =
      "      intlist: record { "
          + "  value: int "
          + "  next: intlist "
          + "} "
          + "new_list: proc(): intlist { "
          + "  return new intlist "
          + "} "
          + "append: proc(this:intlist, newvalue:int) { "
          + "  head = this"
          + "  while head.next != null do head = head.next {} "
          + "  node = new intlist "
          + "  node.value = newvalue "
          + "  head.next = node "
          + "} "
          + "print_list: proc(this: intlist) { "
          + "  if this != null { "
          + "    println this.value "
          + "    print_list(this.next) "
          + "  } "
          + "} "
          + "main { "
          + "  list = new_list() "
          + "  list.value = 0 "
          + "  append(list, 1)  "
          + "  append(list, 2)  "
          + "  print_list(list) "
          + "}";

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

  // Hm, maybe move this to ExecutionEnvironment?
  public static ImmutableList<Op> compile(String text) {
    Lexer lex = new Lexer(text);
    Parser parser = new Parser(lex);
    Node node = parser.parse();
    assertWithMessage(node.message()).that(node.isError()).isFalse();

    StaticChecker checker = new StaticChecker(node);
    TypeCheckResult typeCheckResult = checker.execute();
    assertWithMessage(typeCheckResult.message()).that(typeCheckResult.isError()).isFalse();
    SymTab symbolTable = typeCheckResult.symbolTable();

    CodeGenerator<Op> codegen = new ILCodeGenerator(node, symbolTable);
    ImmutableList<Op> ilCode = codegen.generate();
    // Runs all the optimizers.
    ILOptimizer optimizer = new ILOptimizer(2);
    return optimizer.optimize(ilCode);
  }
}