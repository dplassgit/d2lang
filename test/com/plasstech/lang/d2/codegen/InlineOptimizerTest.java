package com.plasstech.lang.d2.codegen;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.Call;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.interpreter.ExecutionResult;

public class InlineOptimizerTest {
  private Optimizer optimizer = new InlineOptimizer(2);

  @Test
  public void shortVoidProcGlobal() {
    ExecutionResult result =
        TestUtils.optimizeAssertSameVariables(
            "      g = 0"
                + "    shortVoidProcGlobal:proc(n:int) { g = g + n } " //
                + "shortVoidProcGlobal(10) "
                + "println g",
            optimizer);

    ImmutableList<Op> code = result.code();
    // show that there are no calls to the procedure
    for (Op op : code) {
      op.accept(
          new DefaultOpcodeVisitor() {
            @Override
            public void visit(Call op) {
              assertThat(op.functionToCall()).isNotEqualTo("shortVoidProcGlobal");
            }
          });
    }
  }

  @Test
  public void shortVoidProc() {
    ExecutionResult result =
        TestUtils.optimizeAssertSameVariables(
            "      g = 0"
                + "    shortVoidProc:proc(n:int) { f=n g = g + n } " //
                + "shortVoidProc(10) "
                + "println g",
            optimizer);

    ImmutableList<Op> code = result.code();
    // show that there are no calls to the procedure
    for (Op op : code) {
      op.accept(
          new DefaultOpcodeVisitor() {
            @Override
            public void visit(Call op) {
              assertThat(op.functionToCall()).isNotEqualTo("shortVoidProc");
            }
          });
    }
  }

  @Test
  public void shortProc() {
    ExecutionResult result =
        TestUtils.optimizeAssertSameVariables(
            "      shortProc:proc(n:int):int { return n + 1 } " //
                + "println shortProc(10)",
            optimizer);

    ImmutableList<Op> code = result.code();
    // show that there are no calls to the procedure
    for (Op op : code) {
      op.accept(
          new DefaultOpcodeVisitor() {
            @Override
            public void visit(Call op) {
              assertThat(op.functionToCall()).isNotEqualTo("shortProc");
            }
          });
    }
  }

  @Test
  public void mediumProc() {
    ExecutionResult result =
        TestUtils.optimizeAssertSameVariables(
            "      mediumProc:proc(c:string):bool { return c >= '0' and c <= '9' } " //
                + "println mediumProc('hi') println mediumProc('12')",
            optimizer);

    ImmutableList<Op> code = result.code();
    // show that there are no calls to the procedure
    for (Op op : code) {
      op.accept(
          new DefaultOpcodeVisitor() {
            @Override
            public void visit(Call op) {
              assertThat(op.functionToCall()).isNotEqualTo("mediumProc");
            }
          });
    }
  }

  @Test
  public void longProc() {
    ExecutionResult result =
        TestUtils.optimizeAssertSameVariables(
            "      longProc: proc(n:int):int {"
                + "  sum = 0 i=0 while i < n do i = i + 1 {"
                + "    sum = sum + i"
                + "  }"
                + "  return sum"
                + "}"
                + "println longProc(10)",
            optimizer);
    ImmutableList<Op> code = result.code();
    // show that there are still calls to the procedure
    for (Op op : code) {
      op.accept(
          new DefaultOpcodeVisitor() {
            @Override
            public void visit(Call op) {
              assertThat(op.functionToCall()).isEqualTo("longProc");
            }
          });
    }
  }
}
