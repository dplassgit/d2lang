package com.plasstech.lang.d2.codegen;

import static com.plasstech.lang.d2.codegen.EmitterSubject.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.type.SymTab;
import com.plasstech.lang.d2.type.TypeCheckResult;
import com.plasstech.lang.d2.type.VarType;

public class NasmCodeGeneratorTest {

  private Emitter emitter = new ListEmitter();
  private Registers registers = new Registers();
  private NasmCodeGenerator codeGen;

  @Before
  public void setUp() {
    codeGen = new NasmCodeGenerator(emitter, registers);
  }

  @Test
  public void shiftLeftParamParamParamByte() {
    Operand right = new ParamLocation("right", VarType.BYTE, 0);
    Operand left = new ParamLocation("left", VarType.BYTE, 0);
    Location dest = new ParamLocation("dest", VarType.BYTE, 0);
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
    Operand right = new ParamLocation("right", VarType.INT, 0);
    Operand left = new ParamLocation("left", VarType.INT, 0);
    Location dest = new ParamLocation("dest", VarType.INT, 0);
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
    Operand right = new ParamLocation("right", VarType.INT, 0);
    Operand left = new ParamLocation("left", VarType.INT, 0);
    Location dest = new TempLocation("dest", VarType.INT);
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
    Operand right = new ParamLocation("right", VarType.INT, 1);
    Operand left = new StackLocation("left", VarType.INT, 12);
    Location dest = new TempLocation("dest", VarType.INT);
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
    Operand right = new StackLocation("right", VarType.INT, 12);
    Operand left = new ParamLocation("left", VarType.INT, 2);
    Location dest = new TempLocation("dest", VarType.INT);
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
    Operand right = new ParamLocation("right", VarType.INT, 2);
    Operand left = new StackLocation("left", VarType.INT, 12);
    Location dest = new TempLocation("dest", VarType.INT);
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
    Operand right = new StackLocation("right", VarType.INT, 12);
    Operand left = new MemoryAddress("left", VarType.INT);
    Location dest = new TempLocation("dest", VarType.INT);
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
    Operand right = new MemoryAddress("right", VarType.INT);
    Operand left = new MemoryAddress("left", VarType.INT);
    Location dest = new TempLocation("dest", VarType.INT);
    BinOp shiftOp = new BinOp(dest, left, TokenType.SHIFT_LEFT, right, null);

    generateOne(shiftOp);

    assertThat(emitter)
        .containsAtLeast(
            "mov DWORD EBX, [_left]", // left (dest)
            "mov DWORD ECX, [_right]", // right (amount)
            "shl EBX, CL")
        .inOrder();
  }

  private State generateOne(Op shiftOp) {
    State state = State.create();
    TypeCheckResult typeCheckResult = new TypeCheckResult(new SymTab());
    state = state.addTypecheckResult(typeCheckResult);
    state = state.addIlCode(ImmutableList.of(shiftOp));
    state = codeGen.execute(state);
    System.err.println(Joiner.on('\n').join(state.asmCode()));
    return state;
  }
}
