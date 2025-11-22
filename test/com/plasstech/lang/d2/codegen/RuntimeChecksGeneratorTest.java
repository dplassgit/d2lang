package com.plasstech.lang.d2.codegen;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.plasstech.lang.d2.YetAnotherCompiler;
import com.plasstech.lang.d2.codegen.il.ArrayAlloc;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Call;
import com.plasstech.lang.d2.codegen.il.FieldSetOp;
import com.plasstech.lang.d2.codegen.il.IfOp;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.codegen.testing.LocationUtils;
import com.plasstech.lang.d2.common.CompilationConfiguration;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.parse.node.ProcedureNode;
import com.plasstech.lang.d2.parse.node.RecordDeclarationNode;
import com.plasstech.lang.d2.phase.Phase;
import com.plasstech.lang.d2.phase.PhaseName;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.type.ArrayType;
import com.plasstech.lang.d2.type.ProcSymbol;
import com.plasstech.lang.d2.type.RecordReferenceType;
import com.plasstech.lang.d2.type.RecordSymbol;
import com.plasstech.lang.d2.type.VarType;

@RunWith(TestParameterInjector.class)
public class RuntimeChecksGeneratorTest {
  private static final Position POSITION = new Position(1, 1);

  private static final Location INT_TEMP = LocationUtils.newTempLocation("inttemp", VarType.INT);

  private static final Location RANGE_TEMP =
      LocationUtils.newTempLocation("rangetemp", VarType.RANGE);

  private static final Location STRING_TEMP =
      LocationUtils.newTempLocation("stringtemp", VarType.STRING);
  private static final Location SOURCE_STRING =
      LocationUtils.newLongTempLocation("source", VarType.STRING);
  private static final Location SOURCE_STRING2 =
      LocationUtils.newLongTempLocation("source2", VarType.STRING);
  private static final Location DEST_STRING =
      LocationUtils.newLongTempLocation("dest", VarType.STRING);

  private static final Operand NULL_OPERAND = new ConstantOperand<Void>(null, VarType.NULL);

  private static final ArrayType ARRAY_TYPE = new ArrayType(VarType.INT, 1);
  private static final Location ARRAY_TEMP = LocationUtils.newTempLocation("arraytemp", ARRAY_TYPE);

  private static final VarType RECORD_TYPE = new RecordReferenceType("recordType");
  private static final RecordSymbol RECORD_SYM =
      new RecordSymbol(
          new RecordDeclarationNode("rec", ImmutableList.of(), null, ImmutableList.of("T")));
  private static final Location RECORD_LOC = LocationUtils.newMemoryAddress("rec", RECORD_TYPE);

  private Phase generator = new RuntimeChecksGenerator();

  @Test
  public void stringLength() {
    ImmutableList<Op> input =
        ImmutableList.of(new UnaryOp(INT_TEMP, TokenType.LENGTH, STRING_TEMP, POSITION));
    ImmutableList<Op> output = augment(input);
    assertThat(output.size()).isGreaterThan(1);
  }

  @Test
  public void stringLengthTwice() {
    ImmutableList<Op> one =
        ImmutableList.of(new UnaryOp(INT_TEMP, TokenType.LENGTH, STRING_TEMP, POSITION));
    int oneSize = augment(one).size();

    ImmutableList<Op> two =
        ImmutableList.of(
            new UnaryOp(INT_TEMP, TokenType.LENGTH, STRING_TEMP, POSITION),
            new UnaryOp(INT_TEMP, TokenType.LENGTH, STRING_TEMP, POSITION));
    int secondSize = augment(two).size();
    assertThat(secondSize).isEqualTo(oneSize + 1);
  }

  @Test
  public void stringLengthAndTransfer() {
    ImmutableList<Op> one =
        ImmutableList.of(new UnaryOp(INT_TEMP, TokenType.LENGTH, STRING_TEMP, POSITION));
    int oneSize = augment(one).size();

    Location newString = LocationUtils.newParamLocation("newString", VarType.STRING, 0, 0);
    ImmutableList<Op> two =
        ImmutableList.of(
            new UnaryOp(INT_TEMP, TokenType.LENGTH, STRING_TEMP, POSITION),
            // Null check attribute is transferred
            new Transfer(newString, STRING_TEMP, POSITION),
            new UnaryOp(INT_TEMP, TokenType.LENGTH, newString, POSITION));
    int secondSize = augment(two).size();
    assertThat(secondSize).isEqualTo(oneSize + 2);
  }

  @Test
  public void stringIndexAndLength() {
    ImmutableList<Op> one =
        ImmutableList.of(
            new BinOp(
                STRING_TEMP, STRING_TEMP, TokenType.LBRACKET, ConstantOperand.of(0), POSITION));
    int oneSize = augment(one).size();

    ImmutableList<Op> two =
        ImmutableList.of(
            new BinOp(
                STRING_TEMP, STRING_TEMP, TokenType.LBRACKET, ConstantOperand.of(0), POSITION),
            new UnaryOp(INT_TEMP, TokenType.LENGTH, STRING_TEMP, POSITION));
    int secondSize = augment(two).size();
    assertThat(secondSize).isEqualTo(oneSize + 1);
  }

  @Test
  public void stringAsc() {
    ImmutableList<Op> input =
        ImmutableList.of(new UnaryOp(INT_TEMP, TokenType.ASC, STRING_TEMP, POSITION));
    ImmutableList<Op> output = augment(input);
    // Needs to check for null AND length
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

  @Test
  public void twoFieldSets() {
    ImmutableList<Op> one =
        ImmutableList.of(
            new FieldSetOp(RECORD_LOC, RECORD_SYM, "f1", ConstantOperand.of(-1), POSITION));
    int oneSize = augment(one).size();

    ImmutableList<Op> two =
        ImmutableList.of(
            one.get(0),
            new FieldSetOp(RECORD_LOC, RECORD_SYM, "f2", ConstantOperand.of(-1), POSITION));
    int twoSize = augment(two).size();
    assertThat(twoSize).isEqualTo(oneSize + 1);
  }

  @Test
  public void fieldSetThenGet() {
    ImmutableList<Op> one =
        ImmutableList.of(
            new FieldSetOp(RECORD_LOC, RECORD_SYM, "f1", ConstantOperand.of(-1), POSITION));
    int oneSize = augment(one).size();

    ImmutableList<Op> two =
        ImmutableList.of(
            one.get(0),
            new BinOp(INT_TEMP, RECORD_LOC, TokenType.DOT, ConstantOperand.of("f2"), POSITION));
    int twoSize = augment(two).size();
    assertThat(twoSize).isEqualTo(oneSize + 1);
  }

  @Test
  public void twoFieldGets() {
    ImmutableList<Op> one =
        ImmutableList.of(
            new BinOp(INT_TEMP, RECORD_LOC, TokenType.DOT, ConstantOperand.of("f1"), POSITION));
    int oneSize = augment(one).size();

    ImmutableList<Op> two =
        ImmutableList.of(
            one.get(0),
            new BinOp(INT_TEMP, RECORD_LOC, TokenType.DOT, ConstantOperand.of("f2"), POSITION));
    int twoSize = augment(two).size();
    assertThat(twoSize).isEqualTo(oneSize + 1);
  }

  @Test
  public void callBetweenFieldGets() {
    ImmutableList<Op> one =
        ImmutableList.of(
            new BinOp(INT_TEMP, RECORD_LOC, TokenType.DOT, ConstantOperand.of("f1"), POSITION));
    int oneSize = augment(one).size();

    ProcSymbol procSym =
        new ProcSymbol(new ProcedureNode("f", ImmutableList.of(), VarType.VOID, null, null), null);
    ImmutableList<Op> two =
        ImmutableList.of(
            one.get(0),
            new Call(
                procSym,
                /* actuals= */ ImmutableList.of(),
                /* formals= */ ImmutableList.of(),
                POSITION),
            new BinOp(INT_TEMP, RECORD_LOC, TokenType.DOT, ConstantOperand.of("f2"), POSITION));
    int twoSize = augment(two).size();
    assertThat(twoSize).isGreaterThan(oneSize + 1);
  }

  @Test
  public void callBetweenStringIndex() {
    ImmutableList<Op> one =
        ImmutableList.of(new UnaryOp(INT_TEMP, TokenType.LENGTH, STRING_TEMP, POSITION));
    int oneSize = augment(one).size();

    ProcSymbol procSym =
        new ProcSymbol(new ProcedureNode("f", ImmutableList.of(), VarType.VOID, null, null), null);
    ImmutableList<Op> two =
        ImmutableList.of(
            one.get(0),
            new Call(
                procSym,
                /* actuals= */ ImmutableList.of(),
                /* formals= */ ImmutableList.of(),
                POSITION),
            one.get(0));
    int twoSize = augment(two).size();
    assertThat(twoSize).isEqualTo(oneSize + 2);
  }

  @Test
  public void stringAdd() {
    ImmutableList<Op> one =
        ImmutableList.of(
            new BinOp(DEST_STRING, SOURCE_STRING, TokenType.PLUS, SOURCE_STRING2, null));
    ImmutableList<Op> code = augment(one);
    assertThat(code.size()).isGreaterThan(1);

    long ifCount = code.stream().filter(op -> op instanceof IfOp).count();
    assertThat(ifCount).isEqualTo(2);
  }

  @Test
  public void stringAddToSame() {
    ImmutableList<Op> one =
        ImmutableList.of(
            new BinOp(DEST_STRING, SOURCE_STRING, TokenType.PLUS, SOURCE_STRING, null));
    ImmutableList<Op> code = augment(one);
    // There should only be one "if"
    long ifCount = code.stream().filter(op -> op instanceof IfOp).count();
    assertThat(ifCount).isEqualTo(1);

    Op last = code.getLast();
    assertThat(last).isInstanceOf(BinOp.class);
    BinOp lastOp = (BinOp) last;
    assertThat(lastOp.left()).isEqualTo(lastOp.right());
  }

  @Test
  public void stringAddToNull() {
    ImmutableList<Op> one =
        ImmutableList.of(new BinOp(DEST_STRING, SOURCE_STRING, TokenType.PLUS, NULL_OPERAND, null));
    int oneSize = augment(one).size();
    assertThat(oneSize).isGreaterThan(1);
  }

  @Test
  public void stringAddNoSecondCheck() {
    ImmutableList<Op> one =
        ImmutableList.of(
            new UnaryOp(INT_TEMP, TokenType.LENGTH, SOURCE_STRING, POSITION),
            new UnaryOp(INT_TEMP, TokenType.LENGTH, SOURCE_STRING2, POSITION));
    int oneSize = augment(one).size();

    ImmutableList<Op> two =
        ImmutableList.of(
            one.get(0),
            one.get(1),
            // we know that source_string and source_string2 are not null so we don't have
            // to check them now
            new BinOp(DEST_STRING, SOURCE_STRING, TokenType.PLUS, SOURCE_STRING2, null));
    int twoSize = augment(two).size();
    assertThat(twoSize).isEqualTo(oneSize + 1);
  }

  @Test
  public void stringLengthAfterChr() {
    ImmutableList<Op> one =
        ImmutableList.of(new UnaryOp(DEST_STRING, TokenType.CHR, ConstantOperand.of(32), null));
    int oneSize = augment(one).size();

    ImmutableList<Op> two =
        ImmutableList.of(one.get(0), new UnaryOp(INT_TEMP, TokenType.LENGTH, DEST_STRING, null));
    int twoSize = augment(two).size();
    assertThat(twoSize).isEqualTo(oneSize + 1);
  }

  @Test
  public void rangeBrakcetNoNpeCheck() {
    ImmutableList<Op> one =
        ImmutableList.of(
            new BinOp(INT_TEMP, RANGE_TEMP, TokenType.LBRACKET, ConstantOperand.of(2), POSITION));
    assertThat(augment(one)).hasSize(1);
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

    System.out.println("\nBefore:\n" + Joiner.on('\n').join(program));
    state = state.setIlCode(program);
    state = generator.execute(state);
    System.out.println("\nAfter:\n" + Joiner.on('\n').join(state.lastIlCode()));
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
    state = generator.execute(state);
    assertThat(state.error()).isTrue();
    assertThat(state.errorMessage()).contains(error);
  }
}
