package com.plasstech.lang.d2.codegen;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.VarType;

@RunWith(TestParameterInjector.class)
public class StringCodeGeneratorTest {
  private static final Joiner NEWLINE_JOINER = Joiner.on("\n");
  private static final Location DESTINATION =
      new RegisterLocation("dest", IntRegister.RCX, VarType.BOOL);
  private static final Location LEFT =
      new RegisterLocation("left", IntRegister.RDX, VarType.STRING);
  private static final Location RIGHT = new StackLocation("right", VarType.STRING, 4);
  private static final Operand NULL = new ConstantOperand<Void>(null, VarType.NULL);
  private static final Operand CONSTANT = ConstantOperand.of("hi");
  private static final Operand CONSTANT2 = ConstantOperand.of("hi2");

  private DelegatingEmitter emitter = new DelegatingEmitter(new ListEmitter());
  private Registers registers = new Registers();
  private StringTable stringTable = new StringTable();
  private Resolver resolver = new Resolver(registers, stringTable, null, emitter);

  @Before
  public void setUp() {
    stringTable.add("hi");
    stringTable.add("hi2");
  }

  private StringCodeGenerator sut = new StringCodeGenerator(resolver, emitter);

  @Test
  public void nullVSConstantIsFalse(@TestParameter({"EQEQ", "GT", "GEQ"}) TokenType operand) {
    BinOp op = new BinOp(DESTINATION, NULL, operand, CONSTANT, null);
    ImmutableList<String> code = generateUncommentedCode(op);
    assertThat(code.get(0)).isEqualTo("mov BYTE CL, 0");
  }

  @Test
  public void nullVsConstantIsTrue(@TestParameter({"NEQ", "LT", "LEQ"}) TokenType operand) {
    BinOp op = new BinOp(DESTINATION, NULL, TokenType.NEQ, CONSTANT, null);
    ImmutableList<String> code = generateUncommentedCode(op);
    assertThat(code.get(0)).isEqualTo("mov BYTE CL, 1");
  }

  @Test
  public void nullEqVariable() {
    BinOp op = new BinOp(DESTINATION, NULL, TokenType.EQEQ, RIGHT, null);
    ImmutableList<String> code = generateUncommentedCode(op);
    assertThat(code).hasSize(2);
    assertThat(code.get(0)).isEqualTo("cmp QWORD [RBP - 4], 0");
    assertThat(code.get(1)).isEqualTo("setz CL");
  }

  @Test
  public void nullNeqVariable() {
    BinOp op = new BinOp(DESTINATION, NULL, TokenType.NEQ, RIGHT, null);
    ImmutableList<String> code = generateUncommentedCode(op);
    assertThat(code).hasSize(2);
    assertThat(code.get(0)).isEqualTo("cmp QWORD [RBP - 4], 0");
    assertThat(code.get(1)).isEqualTo("setnz CL");
  }

  @Test
  public void constantVsNullIsFalse(@TestParameter({"EQEQ", "LT", "LEQ"}) TokenType operand) {
    BinOp op = new BinOp(DESTINATION, CONSTANT, operand, NULL, null);
    ImmutableList<String> code = generateUncommentedCode(op);
    assertThat(code.get(0)).isEqualTo("mov BYTE CL, 0");
  }

  @Test
  public void constantVsNullIsTrue(@TestParameter({"NEQ", "GT", "GEQ"}) TokenType operand) {
    BinOp op = new BinOp(DESTINATION, CONSTANT, operand, NULL, null);
    ImmutableList<String> code = generateUncommentedCode(op);
    assertThat(code.get(0)).isEqualTo("mov BYTE CL, 1");
  }

  @Test
  public void variableEqNull() {
    BinOp op = new BinOp(DESTINATION, LEFT, TokenType.EQEQ, NULL, null);
    ImmutableList<String> code = generateUncommentedCode(op);
    assertThat(code).hasSize(2);
    assertThat(code.get(0)).isEqualTo("cmp QWORD RDX, 0");
    assertThat(code.get(1)).isEqualTo("setz CL");
  }

  @Test
  public void variableLeqNull() {
    BinOp op = new BinOp(DESTINATION, LEFT, TokenType.LEQ, NULL, null);
    ImmutableList<String> code = generateUncommentedCode(op);
    assertThat(code).hasSize(2);
    assertThat(code.get(0)).isEqualTo("cmp QWORD RDX, 0");
    assertThat(code.get(1)).isEqualTo("setle CL");
  }

  @Test
  public void variableLtNull() {
    BinOp op = new BinOp(DESTINATION, LEFT, TokenType.LT, NULL, null);
    ImmutableList<String> code = generateUncommentedCode(op);
    assertThat(code.get(0)).isEqualTo("mov BYTE CL, 0");
  }

  @Test
  public void variableGeqNull() {
    BinOp op = new BinOp(DESTINATION, LEFT, TokenType.GEQ, NULL, null);
    ImmutableList<String> code = generateUncommentedCode(op);
    // left >= null will always be true
    assertThat(code.get(0)).isEqualTo("mov BYTE CL, 1");
  }

  @Test
  public void variableNeqNull() {
    BinOp op = new BinOp(DESTINATION, LEFT, TokenType.NEQ, NULL, null);
    ImmutableList<String> code = generateUncommentedCode(op);
    assertThat(code).hasSize(2);
    assertThat(code.get(0)).isEqualTo("cmp QWORD RDX, 0");
    assertThat(code.get(1)).isEqualTo("setnz CL");
  }

  @Test
  public void variableGtNull() {
    BinOp op = new BinOp(DESTINATION, LEFT, TokenType.GT, NULL, null);
    ImmutableList<String> code = generateUncommentedCode(op);
    assertThat(code).hasSize(2);
    assertThat(code.get(0)).isEqualTo("cmp QWORD RDX, 0");
    assertThat(code.get(1)).isEqualTo("setg CL");
  }

  @Test
  public void variableVsVariable(
      @TestParameter({"NEQ", "EQEQ", "LT", "LEQ", "GT", "GEQ"}) TokenType operator) {
    BinOp op = new BinOp(DESTINATION, LEFT, operator, RIGHT, null);
    ImmutableList<String> code = generateUncommentedCode(op);
    assertThat(code).contains("call strcmp");
  }

  @Test
  public void constantVsConstant(
      @TestParameter({"NEQ", "EQEQ", "LT", "LEQ", "GT", "GEQ"}) TokenType operator) {
    BinOp op = new BinOp(DESTINATION, CONSTANT, operator, CONSTANT2, null);
    ImmutableList<String> code = generateUncommentedCode(op);
    assertThat(code).contains("call strcmp");
    assertThat(code).doesNotContain("mov BYTE CL, 1");
    assertThat(code).doesNotContain("mov BYTE CL, 0");
  }

  private ImmutableList<String> generateUncommentedCode(BinOp op) {
    sut.generateStringCompare(op);
    System.err.printf("\nTEST CASE: %s\n\n", op);
    System.err.println(NEWLINE_JOINER.join(emitter.all()));
    return trimComments(emitter.all());
  }

  private static ImmutableList<String> trimComments(ImmutableList<String> code) {
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
