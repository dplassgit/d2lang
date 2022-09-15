package com.plasstech.lang.d2.codegen;

import static com.google.common.truth.Truth.assertThat;
import static com.plasstech.lang.d2.codegen.EmitterSubject.assertThat;

import org.junit.Test;

import com.plasstech.lang.d2.type.VarType;

public class ResolverTest {

  private static final MemoryAddress GLOBAL_BYTE = new MemoryAddress("b", VarType.BYTE);
  private static final MemoryAddress GLOBAL_DOUBLE = new MemoryAddress("d", VarType.DOUBLE);
  private static final MemoryAddress GLOBAL_INT = new MemoryAddress("i", VarType.INT);
  private static final MemoryAddress GLOBAL_STRING = new MemoryAddress("s", VarType.STRING);
  private static final MemoryAddress GLOBAL_BYTE2 = new MemoryAddress("b2", VarType.BYTE);
  private static final MemoryAddress GLOBAL_DOUBLE2 = new MemoryAddress("d2", VarType.DOUBLE);
  private static final MemoryAddress GLOBAL_INT2 = new MemoryAddress("i2", VarType.INT);
  private static final MemoryAddress GLOBAL_STRING2 = new MemoryAddress("s2", VarType.STRING);

  private static final TempLocation TEMP_BYTE = new TempLocation("__tempb", VarType.BYTE);
  private static final TempLocation TEMP_DOUBLE = new TempLocation("__tempd", VarType.DOUBLE);
  private static final TempLocation TEMP_INT = new TempLocation("__tempi", VarType.INT);
  private static final TempLocation TEMP_STRING = new TempLocation("__temps", VarType.STRING);

  private Emitter listEmitter = new ListEmitter();
  private Registers registers = new Registers();
  private Resolver resolver = new Resolver(registers, null, null, listEmitter);

  @Test
  public void mov_byteRegToReg() {
    resolver.mov(VarType.BYTE, IntRegister.RAX, IntRegister.RBX);
    assertThat(listEmitter).containsExactly("mov BL, AL");
  }

  @Test
  public void mov_doubleRegToReg() {
    resolver.mov(VarType.DOUBLE, XmmRegister.XMM0, XmmRegister.XMM1);
    assertThat(listEmitter).containsExactly("movq XMM1, XMM0");
  }

  @Test
  public void mov_intRegToReg() {
    resolver.mov(VarType.INT, IntRegister.RAX, IntRegister.RBX);
    assertThat(listEmitter).containsExactly("mov EBX, EAX");
  }

  @Test
  public void mov_stringRegToReg() {
    resolver.mov(VarType.STRING, IntRegister.RAX, IntRegister.RBX);
    assertThat(listEmitter).containsExactly("mov RBX, RAX");
  }

  @Test
  public void mov_int0ToReg() {
    resolver.mov(ConstantOperand.ZERO, IntRegister.RAX);
    assertThat(listEmitter).containsExactly("xor RAX, RAX");
  }

  @Test
  public void mov_byte0ToReg() {
    resolver.mov(ConstantOperand.ZERO_BYTE, IntRegister.RAX);
    assertThat(listEmitter).containsExactly("xor RAX, RAX");
  }

  @Test
  public void mov_doubleRegToInt() {
    resolver.mov(VarType.DOUBLE, XmmRegister.XMM0, IntRegister.RAX);
    assertThat(listEmitter).containsExactly("movq RAX, XMM0");
  }

  @Test
  public void mov_doubleIntToReg() {
    resolver.mov(VarType.DOUBLE, IntRegister.RAX, XmmRegister.XMM0);
    assertThat(listEmitter).containsExactly("movq XMM0, RAX");
  }

  @Test
  public void mov_byteTempToReg() {
    assertThat(registers.isAllocated(IntRegister.RBX)).isFalse();
    resolver.mov(TEMP_BYTE, IntRegister.RAX);
    assertThat(listEmitter).containsExactly("mov AL, BL");
    assertThat(registers.isAllocated(IntRegister.RBX)).isTrue();
  }

  @Test
  public void mov_doubleTempToReg() {
    assertThat(registers.isAllocated(XmmRegister.XMM4)).isFalse();
    resolver.mov(TEMP_DOUBLE, XmmRegister.XMM0);
    assertThat(listEmitter).containsExactly("movq XMM0, XMM4");
    assertThat(registers.isAllocated(XmmRegister.XMM4)).isTrue();
  }

  @Test
  public void mov_intTempToReg() {
    registers.reserve(IntRegister.RBX);
    resolver.mov(TEMP_INT, IntRegister.RAX);
    assertThat(listEmitter).containsExactly("mov EAX, ESI");
    assertThat(registers.isAllocated(IntRegister.RSI)).isTrue();
  }

  @Test
  public void mov_stringTempToReg() {
    resolver.mov(TEMP_STRING, IntRegister.RAX);
    assertThat(listEmitter).containsExactly("mov RAX, RBX");
  }

  @Test
  public void mov_byteRegToTemp() {
    resolver.mov(IntRegister.RAX, TEMP_BYTE);
    assertThat(listEmitter).containsExactly("mov BL, AL");
  }

  @Test
  public void mov_doubleRegToTemp() {
    resolver.mov(XmmRegister.XMM0, TEMP_DOUBLE);
    assertThat(listEmitter).containsExactly("movq XMM4, XMM0");
  }

  @Test
  public void mov_intRegToTemp() {
    resolver.mov(IntRegister.RAX, TEMP_INT);
    assertThat(listEmitter).containsExactly("mov EBX, EAX");
  }

  @Test
  public void mov_stringRegToTemp() {
    resolver.mov(IntRegister.RAX, TEMP_STRING);
    assertThat(listEmitter).containsExactly("mov RBX, RAX");
  }

  @Test
  public void mov_byteRegToGlobal() {
    resolver.mov(IntRegister.RAX, GLOBAL_BYTE);
    assertThat(listEmitter).containsExactly("mov BYTE [_b], AL");
  }

  @Test
  public void mov_doubleRegToGlobal() {
    resolver.mov(XmmRegister.XMM0, GLOBAL_DOUBLE);
    assertThat(listEmitter).containsExactly("movq [_d], XMM0");
  }

  @Test
  public void mov_intRegToGlobal() {
    resolver.mov(IntRegister.RAX, GLOBAL_INT);
    assertThat(listEmitter).containsExactly("mov DWORD [_i], EAX");
  }

  @Test
  public void mov_stringRegToGlobal() {
    resolver.mov(IntRegister.RAX, GLOBAL_STRING);
    assertThat(listEmitter).containsExactly("mov [_s], RAX");
  }

  @Test
  public void mov_byteGlobalToReg() {
    resolver.mov(GLOBAL_BYTE, IntRegister.RAX);
    assertThat(listEmitter).containsExactly("mov BYTE AL, [_b]");
  }

  @Test
  public void mov_doubleGlobalToReg() {
    resolver.mov(GLOBAL_DOUBLE, XmmRegister.XMM0);
    assertThat(listEmitter).containsExactly("movsd XMM0, [_d]");
  }

  @Test
  public void mov_intGlobalToReg() {
    resolver.mov(GLOBAL_INT, IntRegister.RAX);
    assertThat(listEmitter).containsExactly("mov DWORD EAX, [_i]");
  }

  @Test
  public void mov_stringGlobalToReg() {
    resolver.mov(GLOBAL_STRING, IntRegister.RAX);
    assertThat(listEmitter).containsExactly("mov RAX, [_s]");
  }

  @Test
  public void mov_byteTempToGlobal() {
    assertThat(registers.isAllocated(IntRegister.RBX)).isFalse();
    resolver.mov(TEMP_BYTE, GLOBAL_BYTE);
    assertThat(listEmitter).containsExactly("mov BYTE [_b], BL");
    assertThat(registers.isAllocated(IntRegister.RBX)).isTrue();
  }

  @Test
  public void mov_doubleTempToGlobal() {
    assertThat(registers.isAllocated(XmmRegister.XMM4)).isFalse();
    resolver.mov(TEMP_DOUBLE, GLOBAL_DOUBLE);
    assertThat(listEmitter).containsExactly("movq [_d], XMM4");
    assertThat(registers.isAllocated(XmmRegister.XMM4)).isTrue();
  }

  @Test
  public void mov_intTempToGlobal() {
    resolver.mov(TEMP_INT, GLOBAL_INT);
    assertThat(listEmitter).containsExactly("mov DWORD [_i], EBX");
  }

  @Test
  public void mov_stringTempToGlobal() {
    resolver.mov(TEMP_STRING, GLOBAL_STRING);
    assertThat(listEmitter).containsExactly("mov [_s], RBX");
  }

  @Test
  public void mov_byteGlobalToGlobal() {
    resolver.mov(GLOBAL_BYTE, GLOBAL_BYTE2);
    assertThat(listEmitter).contains("mov BYTE BL, [_b]");
    assertThat(listEmitter).contains("mov BYTE [_b2], BL");
    assertThat(registers.isAllocated(IntRegister.RBX)).isFalse();
  }

  @Test
  public void mov_doubleGlobalToGlobal() {
    resolver.mov(GLOBAL_DOUBLE, GLOBAL_DOUBLE2);
    assertThat(listEmitter).contains("movsd XMM4, [_d]");
    assertThat(listEmitter).contains("movq [_d2], XMM4");
    assertThat(registers.isAllocated(XmmRegister.XMM4)).isFalse();
  }

  @Test
  public void mov_intGlobalToGlobal() {
    registers.reserve(IntRegister.RBX);
    resolver.mov(GLOBAL_INT, GLOBAL_INT2);
    assertThat(listEmitter).contains("mov DWORD ESI, [_i]");
    assertThat(listEmitter).contains("mov DWORD [_i2], ESI");
    assertThat(registers.isAllocated(IntRegister.RBX)).isTrue();
    assertThat(registers.isAllocated(IntRegister.RSI)).isFalse();
  }

  @Test
  public void mov_stringGlobalToGlobal() {
    resolver.mov(GLOBAL_STRING, GLOBAL_STRING2);
    assertThat(listEmitter).contains("mov RBX, [_s]");
    assertThat(listEmitter).contains("mov [_s2], RBX");
  }
}
