package com.plasstech.lang.d2.codegen;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.plasstech.lang.d2.YetAnotherCompiler;
import com.plasstech.lang.d2.codegen.il.ArrayAlloc;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.codegen.testing.LocationUtils;
import com.plasstech.lang.d2.common.CompilationConfiguration;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.phase.Phase;
import com.plasstech.lang.d2.phase.PhaseName;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.type.ArrayType;
import com.plasstech.lang.d2.type.VarType;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(TestParameterInjector.class)
public class RuntimeChecksGeneratorTest {
  private static final Location INT_TEMP = LocationUtils.newTempLocation("inttemp", VarType.INT);
  private static final Operand STRING_TEMP =
      LocationUtils.newTempLocation("stringtemp", VarType.STRING);
  private static final Position POSITION = new Position(1, 1);
  private static final ArrayType ARRAY_TYPE = new ArrayType(VarType.INT, 1);
  private static final Location ARRAY_TEMP = LocationUtils.newTempLocation("arraytemp", ARRAY_TYPE);

  private Phase sut = new RuntimeChecksGenerator();

  @Test
  public void stringLength() {
    ImmutableList<Op> input =
        ImmutableList.of(
            new UnaryOp(
                LocationUtils.newStackLocation("i", VarType.INT, 0),
                TokenType.LENGTH,
                LocationUtils.newStackLocation("s", VarType.STRING, 0),
                POSITION));
    ImmutableList<Op> output = augment(input);
    assertThat(output.size()).isGreaterThan(1);
  }

  @Test
  public void stringAsc() {
    ImmutableList<Op> input =
        ImmutableList.of(new UnaryOp(INT_TEMP, TokenType.ASC, STRING_TEMP, POSITION));
    ImmutableList<Op> output = augment(input);
    // Needs to check for null ANd length
    assertThat(output.size()).isGreaterThan(7);
  }

  @Test
  public void arrayLength() {
    ImmutableList<Op> input =
        ImmutableList.of(new UnaryOp(INT_TEMP, TokenType.LENGTH, ARRAY_TEMP, POSITION));
    ImmutableList<Op> output = augment(input);
    assertThat(output.size()).isGreaterThan(6);
  }

  @Test
  public void arrayIndex() {
    ImmutableList<Op> input =
        ImmutableList.of(new BinOp(INT_TEMP, ARRAY_TEMP, TokenType.LBRACKET, INT_TEMP, POSITION));
    ImmutableList<Op> output = augment(input);
    assertThat(output.size()).isGreaterThan(6);
  }

  @Test
  public void divBy0Const(@TestParameter({"DIV", "MOD"}) TokenType tokenType) {
    ImmutableList<Op> input =
        ImmutableList.of(new BinOp(INT_TEMP, INT_TEMP, tokenType, ConstantOperand.of(0), POSITION));
    assertAugmentHasError(input, "Division by 0");
  }

  @Test
  public void divBy0(@TestParameter({"DIV", "MOD"}) TokenType tokenType) {
    ImmutableList<Op> input =
        ImmutableList.of(new BinOp(INT_TEMP, INT_TEMP, tokenType, INT_TEMP, POSITION));
    ImmutableList<Op> output = augment(input);
    assertThat(output.size()).isGreaterThan(1);
  }

  @Test
  public void binOpsNoChange(@TestParameter({"PLUS", "MINUS", "MULT"}) TokenType tokenType) {
    ImmutableList<Op> input =
        ImmutableList.of(new BinOp(INT_TEMP, INT_TEMP, tokenType, INT_TEMP, POSITION));
    ImmutableList<Op> output = augment(input);
    assertThat(output).hasSize(1);
  }

  @Test
  public void arrayIndexConst() {
    ImmutableList<Op> input =
        ImmutableList.of(
            new BinOp(INT_TEMP, ARRAY_TEMP, TokenType.LBRACKET, ConstantOperand.of(0), POSITION));
    ImmutableList<Op> output = augment(input);
    assertThat(output.size()).isGreaterThan(6);
  }

  @Test
  public void transferIsNotChecked() {
    ImmutableList<Op> input = ImmutableList.of(new Transfer(INT_TEMP, ARRAY_TEMP, POSITION));
    ImmutableList<Op> output = augment(input);
    assertThat(output).isEqualTo(input);
  }

  @Test
  public void arrayAllocChecks() {
    ImmutableList<Op> input =
        ImmutableList.of(new ArrayAlloc(ARRAY_TEMP, ARRAY_TYPE, INT_TEMP, POSITION));
    ImmutableList<Op> output = augment(input);
    assertThat(output.size()).isGreaterThan(5);
  }

  @Test
  public void arrayAllocNegative() {
    ImmutableList<Op> input =
        ImmutableList.of(new ArrayAlloc(ARRAY_TEMP, ARRAY_TYPE, ConstantOperand.of(-1), POSITION));
    assertAugmentHasError(input, "ARRAY size must be non-negative; was -1");
  }

  private ImmutableList<Op> augment(ImmutableList<Op> program) {
    CompilationConfiguration config =
        CompilationConfiguration.builder()
            .setSourceCode("")
            .setLastPhase(PhaseName.TYPE_CHECK)
            .build();
    YetAnotherCompiler yac = new YetAnotherCompiler();
    // this just sets up a blank state.
    State state = yac.compile(config);
    assertThat(state.lastIlCode()).isNull();
    assertThat(state.error()).isFalse();

    state = state.setIlCode(program);
    state = sut.execute(state);
    System.out.println(Joiner.on('\n').join(state.lastIlCode()));
    return state.lastIlCode();
  }

  private void assertAugmentHasError(ImmutableList<Op> program, String error) {
    CompilationConfiguration config =
        CompilationConfiguration.builder()
            .setSourceCode("")
            .setLastPhase(PhaseName.TYPE_CHECK)
            .build();
    YetAnotherCompiler yac = new YetAnotherCompiler();
    State state = yac.compile(config);
    assertThat(state.lastIlCode()).isNull();

    state = state.setIlCode(program);
    state = sut.execute(state);
    assertThat(state.error()).isTrue();
    assertThat(state.errorMessage()).contains(error);
  }
}
