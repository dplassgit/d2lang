package com.plasstech.lang.d2.codegen.x64;

import static com.plasstech.lang.d2.codegen.testing.EmitterSubject.assertThat;
import static com.plasstech.lang.d2.codegen.testing.EmitterSubject.assertWithoutTrimmingThat;

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
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.parse.node.BlockNode;
import com.plasstech.lang.d2.parse.node.ProgramNode;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.type.SymTab;
import com.plasstech.lang.d2.type.TypeCheckResult;
import com.plasstech.lang.d2.type.VarType;

public class NasmCodeGeneratorTest {

  private static final Position START = new Position(0, 0);
  private static final Location TEMP = LocationUtils.newTempLocation("__temp", VarType.INT);
  private static final Location GLOBAL = LocationUtils.newMemoryAddress("global", VarType.INT);
  private static final Location LLTEMP =
      LocationUtils.newLongTempLocation("__longtemp", VarType.INT);

  private Emitter emitter = new X64Emitter();
  private Registers registers = new Registers();
  private NasmCodeGenerator codeGen;

  @Before
  public void setUp() {
    codeGen = new NasmCodeGenerator(emitter, registers);
  }

  @Test
  public void shiftLeftParamParamParamByte() {
    Operand right = LocationUtils.newParamLocation("right", VarType.BYTE, 0, 0);
    Operand left = LocationUtils.newParamLocation("left", VarType.BYTE, 0, 0);
    Location dest = LocationUtils.newParamLocation("dest", VarType.BYTE, 0, 0);
    BinOp shiftOp = new BinOp(dest, left, TokenType.SHIFT_LEFT, right, null);

    generateOne(shiftOp);
    assertThat(emitter)
        .containsAtLeast(
            "mov BL, CL", // save ecx (param) -> temp ebx
            "mov SIL, CL", // save ecx (param) -> dest esi
            "mov BYTE SIL, CL", // set up dest. this is stupid
            "mov BYTE CL, BL", // set up ecx. this is stupid, but, shrug.
            "shl SIL, CL")
        .inOrder();
  }

  @Test
  public void shiftLeftParamParamParam() {
    Operand right = LocationUtils.newParamLocation("right", VarType.INT, 0, 0);
    Operand left = LocationUtils.newParamLocation("left", VarType.INT, 0, 0);
    Location dest = LocationUtils.newParamLocation("dest", VarType.INT, 0, 0);
    BinOp shiftOp = new BinOp(dest, left, TokenType.SHIFT_LEFT, right, null);

    generateOne(shiftOp);
    assertThat(emitter)
        .containsAtLeast(
            "mov EBX, ECX", // save ecx (param) -> temp ebx
            "mov ESI, ECX", // save ecx (param) -> dest esi
            "mov DWORD ESI, ECX", // set up dest. this is stupid
            "mov DWORD ECX, EBX", // set up ecx. this is stupid, but, shrug.
            "shl ESI, CL")
        .inOrder();
  }

  @Test
  public void shiftLeftTempParamParam() {
    registers.reserve(IntRegister.RCX);
    Operand right = LocationUtils.newParamLocation("right", VarType.INT, 0, 0);
    Operand left = LocationUtils.newParamLocation("left", VarType.INT, 0, 0);
    Location dest = LocationUtils.newTempLocation("dest", VarType.INT);
    BinOp shiftOp = new BinOp(dest, left, TokenType.SHIFT_LEFT, right, null);

    generateOne(shiftOp);

    assertThat(emitter)
        .containsAtLeast(
            "mov ESI, ECX",
            "push RCX",
            "mov DWORD EBX, ECX", // this is stupid
            "mov DWORD ECX, ESI", // this is stupid, but, shrug.
            "shl EBX, CL",
            "pop RCX")
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
            "mov DWORD ECX, EDX", // right (amount)
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
            "mov DWORD EBX, R8d", // left (dest)
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
            "mov DWORD ECX, R8d", // right (amount)
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
    UnaryOp ascOp = new UnaryOp(dest, TokenType.ASC, source, START);

    generateOne(ascOp);

    assertThat(emitter).contains("mov EBX, 104");
    assertThat(emitter).doesNotContain("and EBX, 0xff");
  }

  @Test
  public void ascParamToTemp() {
    // really, reg to reg
    Operand source = LocationUtils.newParamLocation("source", VarType.STRING, 0, 0);
    Location dest = LocationUtils.newTempLocation("dest", VarType.INT);
    UnaryOp ascOp = new UnaryOp(dest, TokenType.ASC, source, START);

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
    UnaryOp ascOp = new UnaryOp(dest, TokenType.ASC, source, START);

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
    UnaryOp ascOp = new UnaryOp(dest, TokenType.ASC, source, START);

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
  public void printDoubleRegister() {
    Operand doubleReg = new RegisterLocation("__double", XmmRegister.XMM3, VarType.DOUBLE);
    Op op = new SysCall(Call.PRINT, doubleReg);
    generateOne(op);
    assertThat(emitter).containsAtLeast("movq RDX, XMM3", "mov RCX, PRINT_DOUBLE", "call printf");
  }

  @Test
  public void deallocateOp_doesNothingForNonTemps() {
    Location doubleReg = new RegisterLocation("__double", XmmRegister.XMM3, VarType.DOUBLE);
    Op op = new DeallocateTemp(doubleReg, START);
    generateOne(op);
    assertThat(emitter).contains("main:");
  }

  @Test
  public void deallocateOp() {
    // 1. temp=foo
    // 2. deallocate
    ImmutableList<Op> program =
        ImmutableList.of(new Transfer(TEMP, ConstantOperand.ONE, START),
            new DeallocateTemp(TEMP, START));
    generate(program);
    assertWithoutTrimmingThat(emitter).containsAtLeast("  ; Allocating __temp (TEMP) to RBX",
        "  ; Deallocating __temp from RBX");
  }

  @Test
  public void tempAllocation() {
    ImmutableList<Op> program =
        ImmutableList.of(new Transfer(TEMP, ConstantOperand.ONE, START));
    generate(program);
    assertWithoutTrimmingThat(emitter).contains("  ; Allocating __temp (TEMP) to RBX");
  }

  @Test
  public void tempAutoDeallocated() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(TEMP, ConstantOperand.ONE, START),
            new Transfer(GLOBAL, TEMP, START));
    generate(program);
    assertWithoutTrimmingThat(emitter).containsAtLeast("  ; Allocating __temp (TEMP) to RBX",
        "  ; Deallocating __temp from RBX");
  }

  @Test
  public void longLivedTempNotAutoDeallocated() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(LLTEMP, ConstantOperand.ONE, START),
            new Transfer(GLOBAL, LLTEMP, START));
    generate(program);
    assertWithoutTrimmingThat(emitter).contains("  ; Allocating __longtemp (LONG_TEMP) to RBX");
    assertWithoutTrimmingThat(emitter).doesNotContain("  ; Deallocating __longtemp from RBX");
  }

  @Test
  public void longLivedTempDeallocated() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(LLTEMP, ConstantOperand.ONE, START),
            new Transfer(GLOBAL, LLTEMP, START),
            new DeallocateTemp(LLTEMP, START));
    generate(program);
    assertWithoutTrimmingThat(emitter).contains("  ; Allocating __longtemp (LONG_TEMP) to RBX");
    assertWithoutTrimmingThat(emitter).contains("  ; Deallocating __longtemp from RBX");
  }

  private State generateOne(Op op) {
    return generate(ImmutableList.of(op));
  }

  private State generate(ImmutableList<Op> ops) {
    State state = State.create();
    state = state.addProgramNode(new ProgramNode(BlockNode.EMPTY));
    TypeCheckResult typeCheckResult = new TypeCheckResult(new SymTab());
    state = state.addTypecheckResult(typeCheckResult);
    state = state.addIlCode(ops);
    state = codeGen.execute(state);
    System.err.println(Joiner.on('\n').join(state.asmCode()));
    return state;
  }
}
