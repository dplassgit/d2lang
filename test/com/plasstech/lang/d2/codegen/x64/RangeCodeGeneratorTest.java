package com.plasstech.lang.d2.codegen.x64;

import static com.google.common.truth.Truth.assertThat;
import static com.plasstech.lang.d2.codegen.x64.testing.ExecutionSubject.assertThatCompiling;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.DelegatingEmitter;
import com.plasstech.lang.d2.codegen.ListEmitter;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.StringTable;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.testing.LocationUtils;
import com.plasstech.lang.d2.codegen.x64.testing.AsmUtils;
import com.plasstech.lang.d2.common.Range;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.VarType;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(TestParameterInjector.class)
public class RangeCodeGeneratorTest {
  private static final Joiner NEWLINE_JOINER = Joiner.on('\n');
  private static final Location TEMP_RANGE = LocationUtils.newTempLocation("__temp", VarType.RANGE);
  private static final Location PARAM_RANGE =
      LocationUtils.newParamLocation("paramrange", VarType.RANGE, 1, 0);
  private static final Location LOCAL_RANGE =
      LocationUtils.newStackLocation("localrange", VarType.RANGE, 8);
  private static final Location PARAM1 = LocationUtils.newParamLocation("param", VarType.INT, 0, 0);
  private static final Location PARAM2 =
      LocationUtils.newParamLocation("param2", VarType.INT, 5, 16);
  private static final Location LOCAL1 = LocationUtils.newStackLocation("local1", VarType.INT, 16);
  private static final Location LOCAL2 = LocationUtils.newStackLocation("local2", VarType.INT, 32);

  private DelegatingEmitter emitter =
      new DelegatingEmitter(
          new ListEmitter() {
            @Override
            public void emitExternCall(String call) {}

            @Override
            public void emitLabel(String label) {}
          });
  private Registers registers = new Registers();
  private StringTable stringTable = new StringTable();
  private Resolver resolver = new Resolver(registers, stringTable, null, emitter);
  private RangeCodeGenerator sut = new RangeCodeGenerator(resolver, emitter);

  @Test
  public void createConstantRange() {
    BinOp op =
        new BinOp(TEMP_RANGE, ConstantOperand.of(1), TokenType.COLON, ConstantOperand.of(2), null);
    ImmutableList<String> code = generateUncommentedCode(op);
    assertThat(code).hasSize(3);
    assertThat(code).containsExactly("mov DWORD EBX, 1", "shl QWORD RBX, 32", "add RBX, 2");
  }

  @Test
  public void transferFromConstantRange() {
    Range range = new Range(3, 4);
    ConstantOperand<Range> constRange = new ConstantOperand<Range>(range, VarType.RANGE);

    Transfer op = new Transfer(TEMP_RANGE, constRange, null);
    ImmutableList<String> code = generateUncommentedCode(op);
    assertThat(code).hasSize(1);
    assertThat(code).containsExactly("mov QWORD RBX, 12884901892");
  }

  @Test
  public void createRangeParams() {
    BinOp op = new BinOp(TEMP_RANGE, PARAM1, TokenType.COLON, PARAM2, null);
    ImmutableList<String> code = generateUncommentedCode(op);
    assertThat(code).hasSize(4);
    assertThat(code)
        .containsExactly(
            "mov DWORD EBX, ECX", "shl QWORD RBX, 32", "mov DWORD ESI, [RBP + 16]", "add RBX, RSI");
  }

  @Test
  public void createRangeToParam() {
    BinOp op = new BinOp(PARAM_RANGE, LOCAL1, TokenType.COLON, LOCAL2, null);
    ImmutableList<String> code = generateUncommentedCode(op);
    assertThat(code).hasSize(4);
    assertThat(code)
        .containsExactly(
            "mov DWORD EDX, [RBP - 16]",
            "shl QWORD RDX, 32",
            "mov DWORD EBX, [RBP - 32]",
            "add RDX, RBX");
  }

  @Test
  public void createRangeToLocal() {
    BinOp op = new BinOp(LOCAL_RANGE, LOCAL1, TokenType.COLON, LOCAL2, null);
    ImmutableList<String> code = generateUncommentedCode(op);
    assertThat(code).hasSize(5);
  }

  @Test
  public void globalCreateAndGet() {
    assertThatCompiling("r=0:1 print r[0] print r[1]").executedEqualsInterpreted();
  }

  @Test
  public void localCreateAndGet() {
    assertThatCompiling("f:proc{r=1:3 print r[0] print r[1]} f()").executedEqualsInterpreted();
  }

  @Test
  public void rangeParam() {
    assertThatCompiling("f:proc(r:range){print r[0] print r[1]} f(3:5)")
        .executedEqualsInterpreted();
  }

  @Test
  public void indexParam() {
    assertThatCompiling("f:proc(r:range, index:int){print r[index]} r=3:5 f(r, 0) f(r, 1)")
        .executedEqualsInterpreted();
  }

  @Test
  public void createRangeFromParams(@TestParameter boolean optimize) {
    assertThatCompiling(
            "f:proc(start:int, end:int, index:int){r=start:end print r[index]} f(3, 5, 0) f(4, 6,"
                + " 1)")
        .withOptimize(optimize)
        .executedEqualsInterpreted();
  }

  @Test
  public void createRangeFromLocals(@TestParameter boolean optimize) {
    assertThatCompiling("f:proc(index:int){start=3 end=5 r=start:end print r[index]} f(0) f(1)")
        .withOptimize(optimize)
        .executedEqualsInterpreted();
  }

  @Test
  public void createRangeFromGlobals(@TestParameter boolean optimize) {
    assertThatCompiling("start=3 end=5 f:proc(index:int){r=start:end print r[index]} f(0) f(1)")
        .withOptimize(optimize)
        .executedEqualsInterpreted();
  }

  @Test
  public void createRangeToParams(@TestParameter boolean optimize) {
    // It doesn't copy *out* the value from the proc. Ranges are acting more like a long than
    // an array. Note, the same thing happens if r was a string, or a long, so it's fine.
    assertThatCompiling(
            "f:proc(r:range, start:int, end:int, index:int){r=start:end print r[index]} "
                + "gr=7:9 f(gr, 3, 5, 0) println gr[0]")
        .withOptimize(optimize)
        .executedEqualsInterpreted();
  }

  @Test
  public void createRangeToGlobal(@TestParameter boolean optimize) {
    assertThatCompiling(
            "r:range start=3 end=5 f:proc(index:int){r=start:end print r[index]} f(0) f(1)")
        .withOptimize(optimize)
        .executedEqualsInterpreted();
  }

  @Test
  public void invalidIndexRuntime() {
    assertThatCompiling("f:proc(r:range, index:int) {print r[index]} r=3:5 f(r, 2)")
        .withOptimize(true)
        .hasCompileTimeError("^.*index must be 0 or 1.*$");
    assertThatCompiling("f:proc(r:range, index:int) {print r[index]} r=3:5 f(r, 2)")
        .withOptimize(false)
        .withRuntimeError("index must be 0 or 1")
        .executes();
  }

  @Test
  public void invalidIndexCompileTime() {
    // This actually trips in the type checker, but, shrug.
    assertThatCompiling("r=3:5 print r[2]").hasCompileTimeError("^.*index must be 0 or 1.*$");
  }

  @Test
  public void returnRange(@TestParameter boolean optimize) {
    assertThatCompiling("f:proc:range{r=2:4 return r} x=f() print x[0] print x[1]")
        .withOptimize(optimize)
        .executedEqualsInterpreted();
  }

  private ImmutableList<String> generateUncommentedCode(Op op) {
    op.accept(sut);
    System.err.printf("\nTEST CASE: %s\n\n", op);
    System.err.println(NEWLINE_JOINER.join(emitter.all()));
    return AsmUtils.trimComments(emitter.all());
  }
}
