package com.plasstech.lang.d2.codegen.x64;

import static com.plasstech.lang.d2.codegen.testing.EmitterSubject.assertThat;
import static com.plasstech.lang.d2.codegen.testing.EmitterSubject.assertWithoutTrimmingThat;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Emitter;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.DeallocateTemp;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.SysCall.Call;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.codegen.testing.LocationUtils;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.parse.node.BlockNode;
import com.plasstech.lang.d2.parse.node.ProgramNode;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.type.SymbolTable;
import com.plasstech.lang.d2.type.TypeCheckResult;
import com.plasstech.lang.d2.type.VarType;

public class NasmCodeGeneratorTest {

  private static final Location TEMP = LocationUtils.newTempLocation("__temp", VarType.INT);
  private static final Location GLOBAL = LocationUtils.newMemoryAddress("global", VarType.INT);
  private static final Location LONG_TEMP =
      LocationUtils.newLongTempLocation("__longtemp", VarType.INT);
  private static final Location STACK_RANGE =
      LocationUtils.newStackLocation("__stackrange", VarType.RANGE, 16);
  private static final Location PARAM_RANGE =
      LocationUtils.newParamLocation("__stackrange", VarType.RANGE, 0, 16);
  private static final Location TEMP_RANGE =
      LocationUtils.newTempLocation("__rangetemp", VarType.RANGE);
  private static final Location GLOBAL_RANGE =
      LocationUtils.newMemoryAddress("globalrange", VarType.RANGE);

  private Emitter emitter = new X64Emitter();
  private Registers registers = new Registers();
  private NasmCodeGenerator codeGen;

  @Before
  public void setUp() {
    codeGen = new NasmCodeGenerator(emitter, registers);
  }

  @Test
  public void shiftLeftParamParamParamByte() {
    Location dest = LocationUtils.newParamLocation("dest", VarType.BYTE, 0, 0);
    BinOp shiftOp = new BinOp(dest, dest, TokenType.SHIFT_LEFT, dest, null);

    generateOne(shiftOp);
    assertThat(emitter)
        .containsAtLeast(
            "mov BL, CL", // save ecx (param) -> temp ebx
            "shl BL, CL",
            "mov CL, BL")
        .inOrder();
  }

  @Test
  public void shiftLeftParamParamParam() {
    Location dest = LocationUtils.newParamLocation("dest", VarType.INT, 0, 0);
    BinOp shiftOp = new BinOp(dest, dest, TokenType.SHIFT_LEFT, dest, null);

    generateOne(shiftOp);
    assertThat(emitter)
        .containsAtLeast(
            "mov EBX, ECX", // save ecx (param) -> temp ebx
            "shl EBX, CL",
            "mov ECX, EBX")
        .inOrder();
  }

  @Test
  public void shiftLeftTempParamParam() {
    registers.reserve(IntRegister.RCX);
    // rdx
    Operand left = LocationUtils.newParamLocation("left", VarType.INT, 1, 0);
    // should be rbx
    Location dest = LocationUtils.newTempLocation("dest", VarType.INT);
    BinOp shiftOp = new BinOp(dest, left, TokenType.SHIFT_LEFT, left, null);

    generateOne(shiftOp);

    assertThat(emitter)
        .containsAtLeast(
            "mov EBX, EDX", // put left into ebx/dest
            "mov ECX, EDX", // put shift amount into ecx
            "shl EBX, CL")
        .inOrder();
  }

  @Test
  public void shiftLeftTempMemoryParam() {
    Operand right = LocationUtils.newParamLocation("right", VarType.INT, 1, 0);
    Operand left = LocationUtils.newStackLocation("left", VarType.INT, 12);
    Location dest = LocationUtils.newTempLocation("dest", VarType.INT);
    BinOp shiftOp = new BinOp(dest, left, TokenType.SHIFT_LEFT, right, null);

    generateOne(shiftOp);

    assertThat(emitter)
        .containsAtLeast(
            "mov DWORD EBX, [RBP - 12]", // left (dest)
            "mov ECX, EDX", // right (amount)
            "shl EBX, CL")
        .inOrder();
  }

  @Test
  public void shiftLeftTempParamStack() {
    Operand right = LocationUtils.newStackLocation("right", VarType.INT, 12);
    Operand left = LocationUtils.newParamLocation("left", VarType.INT, 2, 0);
    Location dest = LocationUtils.newTempLocation("dest", VarType.INT);
    BinOp shiftOp = new BinOp(dest, left, TokenType.SHIFT_LEFT, right, null);

    generateOne(shiftOp);

    assertThat(emitter)
        .containsAtLeast(
            "mov EBX, R8d", // left (dest)
            "mov DWORD ECX, [RBP - 12]", // right (amount)
            "shl EBX, CL")
        .inOrder();
  }

  @Test
  public void shiftLeftTempStackParam() {
    Operand right = LocationUtils.newParamLocation("right", VarType.INT, 2, 0);
    Operand left = LocationUtils.newStackLocation("left", VarType.INT, 12);
    Location dest = LocationUtils.newTempLocation("dest", VarType.INT);
    BinOp shiftOp = new BinOp(dest, left, TokenType.SHIFT_LEFT, right, null);

    generateOne(shiftOp);

    assertThat(emitter)
        .containsAtLeast(
            "mov DWORD EBX, [RBP - 12]", // left (dest)
            "mov ECX, R8d", // right (amount)
            "shl EBX, CL")
        .inOrder();
  }

  @Test
  public void shiftLeftTempMemoryStack() {
    Operand right = LocationUtils.newStackLocation("right", VarType.INT, 12);
    Operand left = LocationUtils.newMemoryAddress("left", VarType.INT);
    Location dest = LocationUtils.newTempLocation("dest", VarType.INT);
    BinOp shiftOp = new BinOp(dest, left, TokenType.SHIFT_LEFT, right, null);

    generateOne(shiftOp);

    assertThat(emitter)
        .containsAtLeast(
            "mov DWORD EBX, [_left]", // left (dest)
            "mov DWORD ECX, [RBP - 12]", // right (amount)
            "shl EBX, CL")
        .inOrder();
  }

  @Test
  public void shiftLeftTempMemoryMemory() {
    Operand right = LocationUtils.newMemoryAddress("right", VarType.INT);
    Operand left = LocationUtils.newMemoryAddress("left", VarType.INT);
    Location dest = LocationUtils.newTempLocation("dest", VarType.INT);
    BinOp shiftOp = new BinOp(dest, left, TokenType.SHIFT_LEFT, right, null);

    generateOne(shiftOp);

    assertThat(emitter)
        .containsAtLeast(
            "mov DWORD EBX, [_left]", // left (dest)
            "mov DWORD ECX, [_right]", // right (amount)
            "shl EBX, CL")
        .inOrder();
  }

  @Test
  public void ascConstantToTemp() {
    Operand source = ConstantOperand.of("hi");
    Location dest = LocationUtils.newTempLocation("dest", VarType.INT);
    UnaryOp ascOp = new UnaryOp(dest, TokenType.ASC, source, null);

    generateOne(ascOp);

    assertThat(emitter).contains("mov EBX, 104");
    assertThat(emitter).doesNotContain("and EBX, 0xff");
  }

  @Test
  public void ascParamToTemp() {
    // really, reg to reg
    Operand source = LocationUtils.newParamLocation("source", VarType.STRING, 0, 0);
    Location dest = LocationUtils.newTempLocation("dest", VarType.INT);
    UnaryOp ascOp = new UnaryOp(dest, TokenType.ASC, source, null);

    generateOne(ascOp);

    assertThat(emitter)
        .containsAtLeast(
            "mov BYTE BL, [RCX]", // fixes bug 160
            "and EBX, 0xff")
        .inOrder();
  }

  @Test
  public void ascStackToTemp() {
    Operand source = LocationUtils.newStackLocation("source", VarType.STRING, 4);
    Location dest = LocationUtils.newTempLocation("dest", VarType.INT);
    UnaryOp ascOp = new UnaryOp(dest, TokenType.ASC, source, null);

    generateOne(ascOp);

    assertThat(emitter)
        .containsAtLeast(
            // moves "source" to a temp reg
            "mov RSI, [RBP - 4]",
            "mov BYTE BL, [RSI]", // fixes bug 160
            "and EBX, 0xff")
        .inOrder();
  }

  @Test
  @Ignore("Should not pass")
  public void ascStackToStack() {
    Operand source = LocationUtils.newStackLocation("source", VarType.STRING, 4);
    // This should never happen; dests are usually temps stored in registers.
    Location dest = LocationUtils.newStackLocation("dest", VarType.INT, 8);
    UnaryOp ascOp = new UnaryOp(dest, TokenType.ASC, source, null);

    generateOne(ascOp);

    assertThat(emitter)
        .containsAtLeast(
            "mov RBX, [RBP - 4]",
            "mov BYTE [RBP - 8], [RBX]", // this is illegal
            "and [RBP - 8], 0xff")
        .inOrder();
  }

  @Test
  public void printDoubleConstant() {
    Operand doubleReg = ConstantOperand.of(123.0);
    Op op = new SysCall(Call.PRINT, doubleReg);
    generateOne(op);
    assertThat(emitter)
        .containsAtLeast(
            "movsd XMM4, [DOUBLE_123_0_0]",
            "movq RDX, XMM4",
            "mov RCX, PRINT_DOUBLE",
            "call printf");
  }

  @Test
  public void printDoubleGlobal() {
    Operand doubleReg = LocationUtils.newMemoryAddress("double", VarType.DOUBLE);
    Op op = new SysCall(Call.PRINT, doubleReg);
    generateOne(op);
    assertThat(emitter)
        .containsAtLeast("mov RDX, [_double]", "mov RCX, PRINT_DOUBLE", "call printf");
  }

  @Test
  public void printDoubleStack() {
    Operand doubleReg = LocationUtils.newStackLocation("_double", VarType.DOUBLE, 4);
    Op op = new SysCall(Call.PRINT, doubleReg);
    generateOne(op);
    assertThat(emitter)
        .containsAtLeast("mov RDX, [RBP - 4]", "mov RCX, PRINT_DOUBLE", "call printf");
  }

  @Test
  public void printlnDoubleStack() {
    Operand doubleReg = LocationUtils.newStackLocation("_double", VarType.DOUBLE, 4);
    Op op = new SysCall(Call.PRINTLN, doubleReg);
    generateOne(op);
    assertThat(emitter)
        .containsAtLeast("mov RDX, [RBP - 4]", "mov RCX, PRINTLN_DOUBLE", "call printf");
  }

  @Test
  public void printDoubleTemp() {
    Operand doubleReg = LocationUtils.newTempLocation("__temp", VarType.DOUBLE);
    Op op = new SysCall(Call.PRINT, doubleReg);
    generateOne(op);
    assertThat(emitter).containsAtLeast("movq RDX, XMM4", "mov RCX, PRINT_DOUBLE", "call printf");
  }

  @Test
  public void printDoubleParam() {
    Operand doubleReg = LocationUtils.newParamLocation("param", VarType.DOUBLE, 0, 0);
    Op op = new SysCall(Call.PRINT, doubleReg);
    generateOne(op);
    assertThat(emitter).containsAtLeast("movq RDX, XMM0", "mov RCX, PRINT_DOUBLE", "call printf");
  }

  @Test
  public void deallocateOp_doesNothingForNonTemps() {
    Location doubleReg = new RegisterLocation("__double", XmmRegister.XMM3, VarType.DOUBLE);
    Op op = new DeallocateTemp(doubleReg, null);
    generateOne(op);
    assertThat(emitter).contains("main:");
  }

  @Test
  public void deallocateOp() {
    // 1. temp=foo
    // 2. deallocate
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(LONG_TEMP, ConstantOperand.ONE, null),
            new Transfer(TEMP, LONG_TEMP, null),
            // This should never happen, because the nasm code generator adds its own
            // deallocates where needed...
            new DeallocateTemp(LONG_TEMP, null));
    generate(program);
    assertWithoutTrimmingThat(emitter).containsAtLeast(
        "  ; Allocating __longtemp (LONG_TEMP) to RBX",
        "  ; Deallocating __longtemp from RBX");
  }

  @Test
  public void tempAllocation() {
    ImmutableList<Op> program =
        ImmutableList.of(new Transfer(TEMP, ConstantOperand.ONE, null));
    generate(program);
    assertWithoutTrimmingThat(emitter).contains("  ; Allocating __temp (TEMP) to RBX");
  }

  @Test
  public void tempAutoDeallocated() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(TEMP, ConstantOperand.ONE, null),
            new Transfer(GLOBAL, TEMP, null));
    generate(program);
    assertWithoutTrimmingThat(emitter).containsAtLeast("  ; Allocating __temp (TEMP) to RBX",
        "  ; Deallocating __temp from RBX");
  }

  @Test
  public void longLivedTempAutoDeallocated() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(LONG_TEMP, ConstantOperand.ONE, null),
            new Transfer(GLOBAL, LONG_TEMP, null));
    generate(program);
    assertWithoutTrimmingThat(emitter).contains("  ; Allocating __longtemp (LONG_TEMP) to RBX");
    assertWithoutTrimmingThat(emitter).contains("  ; Deallocating __longtemp from RBX");
  }

  @Test
  public void longLivedTempDeallocated() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(LONG_TEMP, ConstantOperand.ONE, null),
            new Transfer(GLOBAL, LONG_TEMP, null),
            new DeallocateTemp(LONG_TEMP, null));
    generate(program);
    assertWithoutTrimmingThat(emitter).contains("  ; Allocating __longtemp (LONG_TEMP) to RBX");
    assertWithoutTrimmingThat(emitter).contains("  ; Deallocating __longtemp from RBX");
  }

  @Test
  public void longLivedTempTransferred() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(TEMP, ConstantOperand.ONE, null),
            new Transfer(LONG_TEMP, TEMP, null),
            new DeallocateTemp(LONG_TEMP, null));
    generate(program);
    assertThat(emitter).doesNotContain("mov ESI, EBX");
  }

  @Test
  public void compareNulls() {
    Operand nullOperand = new ConstantOperand<Void>(null, VarType.NULL);

    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(TEMP, nullOperand, TokenType.NEQ, nullOperand, null));
    generate(program);
    assertThat(emitter).contains("  xor RSI, RSI");
    assertThat(emitter).contains("  xor RDI, RDI");
  }

  @Test
  public void fromConstantRange() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(TEMP_RANGE, ConstantOperand.of(12), TokenType.COLON, ConstantOperand.of(24),
                null),
            new Transfer(GLOBAL_RANGE, TEMP_RANGE, null));
    generate(program);
    assertThat(emitter).contains("  mov DWORD EBX, 12");
    assertThat(emitter).contains("  shl QWORD RBX, 32");
    assertThat(emitter).contains("  add RBX, 24");
    assertThat(emitter).contains("  mov QWORD [_globalrange], RBX");
  }

  @Test
  public void toConstantRange() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(GLOBAL_RANGE, ConstantOperand.of(12), TokenType.COLON, ConstantOperand.of(24),
                null));
    generate(program);
    assertThat(emitter).contains("  mov DWORD [_globalrange], 12");
    assertThat(emitter).contains("  shl QWORD [_globalrange], 32");
  }

  @Test
  public void toStackRange() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(TEMP_RANGE, ConstantOperand.of(12), TokenType.COLON, ConstantOperand.of(24),
                null),
            new Transfer(STACK_RANGE, TEMP_RANGE, null));
    generate(program);
    assertThat(emitter).contains("  mov DWORD EBX, 12");
    assertThat(emitter).contains("  shl QWORD RBX, 32");
    assertThat(emitter).contains("  add RBX, 24");
    assertThat(emitter).contains("  mov QWORD [RBP - 16], RBX");
  }

  @Test
  public void toParamRange() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(TEMP_RANGE, ConstantOperand.of(12), TokenType.COLON, ConstantOperand.of(24),
                null),
            new Transfer(PARAM_RANGE, TEMP_RANGE, null));
    generate(program);
    assertThat(emitter).contains("  mov DWORD EBX, 12");
    assertThat(emitter).contains("  shl QWORD RBX, 32");
    assertThat(emitter).contains("  add RBX, 24");
    assertThat(emitter).contains("  mov RCX, RBX");
  }

  @Test
  public void fromParamRange() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(TEMP_RANGE, TEMP, TokenType.COLON, TEMP,
                null));
    generate(program);
    assertThat(emitter).contains("  mov DWORD ESI, EBX");
    assertThat(emitter).contains("  shl QWORD RSI, 32");
    assertThat(emitter).contains("  add QWORD RSI, RBX");
  }

  @Test
  public void rangeOfTemps() {
    Location temp1 = LocationUtils.newTempLocation("__temp1", VarType.INT);
    Location temp2 = LocationUtils.newTempLocation("__temp2", VarType.INT);
    Location temp3 = LocationUtils.newTempLocation("__temp3", VarType.RANGE);
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(temp1, ConstantOperand.ZERO, TokenType.PLUS, ConstantOperand.of(1), null),
            new BinOp(temp2, ConstantOperand.of(2), TokenType.PLUS, ConstantOperand.of(3), null),
            new BinOp(temp3, temp1, TokenType.COLON, temp2, null),
            new Transfer(GLOBAL_RANGE, temp3, null));
    generate(program);
    assertThat(emitter).contains("  mov DWORD EDI, EBX");
    assertThat(emitter).contains("  shl QWORD RDI, 32");
    assertThat(emitter).contains("  add QWORD RDI, RSI");
    assertThat(emitter).contains("  mov QWORD [_globalrange], RDI");
  }

  private State generateOne(Op op) {
    return generate(ImmutableList.of(op));
  }

  private State generate(ImmutableList<Op> ops) {
    State state = State.create();
    state = state.addProgramNode(new ProgramNode(BlockNode.EMPTY));
    TypeCheckResult typeCheckResult = new TypeCheckResult(new SymbolTable());
    state = state.addTypecheckResult(typeCheckResult);
    state = state.setIlCode(ops);
    state = codeGen.execute(state);
    System.err.println(Joiner.on('\n').join(state.asmCode()));
    if (state.error()) {
      fail(state.errorMessage());
    }
    return state;
  }
}
