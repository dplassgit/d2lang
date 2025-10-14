package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.collect.ImmutableList;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.TempLocation;
import com.plasstech.lang.d2.codegen.il.ArrayAlloc;
import com.plasstech.lang.d2.codegen.il.ArraySet;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.codegen.testing.LocationUtils;
import com.plasstech.lang.d2.common.D2RuntimeException;
import com.plasstech.lang.d2.common.InvalidIndexException;
import com.plasstech.lang.d2.common.Range;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.ArrayType;
import com.plasstech.lang.d2.type.VarType;

@RunWith(TestParameterInjector.class)
public class RangeCheckerTest {
  private final Optimizer checker = new RangeChecker();

  private static final TempLocation INT_TEMP =
      LocationUtils.newTempLocation("inttemp", VarType.INT);
  private static final TempLocation STRING_TEMP =
      LocationUtils.newTempLocation("stringtemp", VarType.STRING);
  private static final ConstantOperand<String> NULL_STRING =
      new ConstantOperand<String>(null, VarType.STRING);
  private static final ArrayType ARRAY_TYPE = new ArrayType(VarType.INT, 1);
  private static final Location ARRAY_TEMP = LocationUtils.newTempLocation("arraytemp", ARRAY_TYPE);
  private static final Location NULL_ARRAY =
      LocationUtils.newTempLocation("nullarray", VarType.NULL);

  private static final Operand RANGE_TEMP = LocationUtils.newTempLocation("range", VarType.RANGE);

  private static final Range RANGE_NEG_START = new Range(-1, 0);
  private static final Range RANGE_NEG_END = new Range(1, -1);

  @Test
  public void negativeArraySize() {
    ImmutableList<Op> program =
        ImmutableList.of(new ArrayAlloc(ARRAY_TEMP, ARRAY_TYPE, ConstantOperand.of(-1), null));
    RuntimeException exception =
        assertThrows(D2RuntimeException.class, () -> run(program));
    assertThat(exception).hasMessageThat().contains("ARRAY size must be non-negative; was -1");
  }

  @Test
  public void negativeArrayIndex() {
    ImmutableList<Op> program =
        ImmutableList
            .of(new BinOp(INT_TEMP, ARRAY_TEMP, TokenType.LBRACKET, ConstantOperand.of(-1), null));
    RuntimeException exception =
        assertThrows(D2RuntimeException.class, () -> run(program));
    assertThat(exception).hasMessageThat().contains("index must be non-negative; was -1");

  }

  @Test
  public void divBy0(@TestParameter({"DIV", "MOD"}) TokenType op) {
    ImmutableList<Op> program =
        ImmutableList
            .of(new BinOp(INT_TEMP, ARRAY_TEMP, op, ConstantOperand.of(0), null));
    RuntimeException exception =
        assertThrows(D2RuntimeException.class, () -> run(program));
    assertThat(exception).hasMessageThat().contains("Division by 0");
  }

  @Test
  public void negativeStringIndex() {
    ImmutableList<Op> program =
        ImmutableList
            .of(new BinOp(INT_TEMP, STRING_TEMP, TokenType.LBRACKET, ConstantOperand.of(-1), null));
    RuntimeException exception =
        assertThrows(D2RuntimeException.class, () -> run(program));
    assertThat(exception).hasMessageThat().contains("STRING index must be non-negative; was -1");
  }

  @Test
  public void negativeStringSliceStart() {
    ImmutableList<Op> program =
        ImmutableList
            .of(new BinOp(INT_TEMP, STRING_TEMP, TokenType.LBRACKET,
                new ConstantOperand<Range>(RANGE_NEG_START, VarType.RANGE), null));
    RuntimeException exception =
        assertThrows(D2RuntimeException.class, () -> run(program));
    assertThat(exception).hasMessageThat()
        .contains("STRING RANGE start index must be non-negative; was -1");
  }

  @Test
  public void negativeStringSliceEnd() {
    ImmutableList<Op> program =
        ImmutableList
            .of(new BinOp(INT_TEMP, STRING_TEMP, TokenType.LBRACKET,
                new ConstantOperand<Range>(RANGE_NEG_END, VarType.RANGE), null));
    RuntimeException exception =
        assertThrows(D2RuntimeException.class, () -> run(program));
    assertThat(exception).hasMessageThat()
        .contains("STRING RANGE end index must be non-negative; was -1");
  }

  @Test
  public void rangeIndexNeg1() {
    ImmutableList<Op> program =
        ImmutableList
            .of(new BinOp(INT_TEMP, RANGE_TEMP, TokenType.LBRACKET, ConstantOperand.of(-1), null));
    RuntimeException exception =
        assertThrows(D2RuntimeException.class, () -> run(program));
    assertThat(exception).hasMessageThat().contains("RANGE index must be 0 or 1; was -1");
  }

  @Test
  public void rangeIndex2() {
    ImmutableList<Op> program =
        ImmutableList
            .of(new BinOp(INT_TEMP, RANGE_TEMP, TokenType.LBRACKET, ConstantOperand.of(2), null));
    RuntimeException exception =
        assertThrows(D2RuntimeException.class, () -> run(program));
    assertThat(exception).hasMessageThat().contains("RANGE index must be 0 or 1; was 2");
  }

  @Test
  public void nullLength(@TestParameter({"LENGTH", "ASC"}) TokenType operator) {
    ImmutableList<Op> program =
        ImmutableList
            .of(new UnaryOp(STRING_TEMP, operator, NULL_STRING, null));
    RuntimeException exception =
        assertThrows(D2RuntimeException.class, () -> run(program));
    assertThat(exception).hasMessageThat().contains("Null pointer error");
  }

  @Test
  public void varStringPlusNull() {
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(STRING_TEMP, STRING_TEMP, TokenType.PLUS, NULL_STRING, null));
    RuntimeException exception =
        assertThrows(D2RuntimeException.class, () -> run(program));
    assertThat(exception).hasMessageThat().contains("Cannot add NULL to STRING");
  }

  @Test
  public void varNullPlusString() {
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(STRING_TEMP, NULL_STRING, TokenType.PLUS, STRING_TEMP, null));
    RuntimeException exception =
        assertThrows(D2RuntimeException.class, () -> run(program));
    assertThat(exception).hasMessageThat().contains("Cannot add NULL to STRING");
  }

  @Test
  public void nullArraySet() {
    ImmutableList<Op> program =
        ImmutableList
            .of(new ArraySet(INT_TEMP, ARRAY_TYPE, ConstantOperand.of(0), NULL_ARRAY, false, null));

    RuntimeException exception =
        assertThrows(D2RuntimeException.class, () -> run(program));
    assertThat(exception).hasMessageThat().contains("Cannot set value of NULL ARRAY");
  }

  @Test
  public void negativeArraySet() {
    ImmutableList<Op> program =
        ImmutableList
            .of(new ArraySet(INT_TEMP, ARRAY_TYPE, ConstantOperand.of(-1), ARRAY_TEMP, false,
                null));

    RuntimeException exception =
        assertThrows(InvalidIndexException.class, () -> run(program));
    assertThat(exception).hasMessageThat().contains("ARRAY index must be non-negative; was -1");
  }

  @Test
  public void constStringPlusNull() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(STRING_TEMP, ConstantOperand.of("hi"), TokenType.PLUS, NULL_STRING, null));
    RuntimeException exception =
        assertThrows(D2RuntimeException.class, () -> run(program));
    assertThat(exception).hasMessageThat().contains("Cannot add NULL to STRING");
  }

  private void run(ImmutableList<Op> program) {
    checker.optimize(program, null);
  }
}
