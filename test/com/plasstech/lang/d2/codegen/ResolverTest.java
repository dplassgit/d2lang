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

  private static final StackLocation STACK_DOUBLE = new StackLocation("d1", VarType.DOUBLE, 8);
  private static final StackLocation STACK_BYTE = new StackLocation("p1", VarType.BYTE, 4);
  private static final StackLocation STACK_INT = new StackLocation("i1", VarType.INT, 12);
  private static final StackLocation STACK_INT2 = new StackLocation("i1", VarType.INT, 4);
  private static final StackLocation STACK_STRING = new StackLocation("s2", VarType.STRING, 16);

  private static final ParamLocation PARAM_BYTE = new ParamLocation("p1", VarType.BYTE, 0);
  private static final ParamLocation PARAM_DOUBLE = new ParamLocation("d1", VarType.DOUBLE, 1);
  private static final ParamLocation PARAM_INT = new ParamLocation("i1", VarType.INT, 2);
  private static final ParamLocation PARAM_STRING = new ParamLocation("s2", VarType.STRING, 3);
  private static final TempLocation TEMP_BYTE = new TempLocation("__tempb", VarType.BYTE);
  private static final TempLocation TEMP_DOUBLE = new TempLocation("__tempd", VarType.DOUBLE);
  private static final TempLocation TEMP_INT = new TempLocation("__tempi", VarType.INT);
  private static final TempLocation TEMP_STRING = new TempLocation("__temps", VarType.STRING);

  private DelegatingEmitter emitter = new DelegatingEmitter(new ListEmitter());
  private Registers registers = new Registers();
  private Resolver resolver = new Resolver(registers, null, null, emitter);

  @Test
  public void mov_regToSelf() {
    resolver.mov(VarType.INT, IntRegister.RAX, IntRegister.RAX);
    assertThat(emitter).isEmpty();
  }

  @Test
  public void mov_tempToSelf() {
    resolver.mov(TEMP_INT, TEMP_INT);
    assertThat(emitter).isEmpty();
  }

  @Test
  public void mov_globalToSelf() {
    resolver.mov(GLOBAL_INT, GLOBAL_INT);
    // Woot, this found a bug!
    assertThat(emitter).isEmpty();
  }

  @Test
  public void mov_paramToSelf() {
    resolver.mov(PARAM_INT, PARAM_INT);
    assertThat(emitter).isEmpty();
  }

  @Test
  public void mov_stackToSelf() {
    resolver.mov(STACK_INT, STACK_INT);
    assertThat(emitter).isEmpty();
  }

  @Test
  public void mov_byteRegToReg() {
    resolver.mov(VarType.BYTE, IntRegister.RAX, IntRegister.RBX);
    assertThat(emitter).containsExactly("mov BL, AL");
  }

  @Test
  public void mov_doubleRegToReg() {
    resolver.mov(VarType.DOUBLE, XmmRegister.XMM0, XmmRegister.XMM1);
    assertThat(emitter).containsExactly("movq XMM1, XMM0");
  }

  @Test
  public void mov_intRegToReg() {
    resolver.mov(VarType.INT, IntRegister.RAX, IntRegister.RBX);
    assertThat(emitter).containsExactly("mov EBX, EAX");
  }

  @Test
  public void mov_stringRegToReg() {
    resolver.mov(VarType.STRING, IntRegister.RAX, IntRegister.RBX);
    assertThat(emitter).containsExactly("mov RBX, RAX");
  }

  @Test
  public void mov_boolFalseToReg() {
    resolver.mov(ConstantOperand.FALSE, IntRegister.RAX);
    assertThat(emitter).containsExactly("xor RAX, RAX");
  }

  @Test
  public void mov_boolTrueToReg() {
    resolver.mov(ConstantOperand.TRUE, IntRegister.RAX);
    assertThat(emitter).containsExactly("mov BYTE AL, 1");
  }

  @Test
  public void mov_byte0ToReg() {
    resolver.mov(ConstantOperand.ZERO_BYTE, IntRegister.RAX);
    assertThat(emitter).containsExactly("xor RAX, RAX");
  }

  @Test
  public void mov_int0ToReg() {
    resolver.mov(ConstantOperand.ZERO, IntRegister.RAX);
    assertThat(emitter).containsExactly("xor RAX, RAX");
  }

  @Test
  public void mov_nullToReg() {
    resolver.mov(new ConstantOperand<Void>(null, VarType.NULL), IntRegister.RAX);
    assertThat(emitter).containsExactly("xor RAX, RAX");
  }

  @Test
  public void mov_doubleRegToInt() {
    resolver.mov(VarType.DOUBLE, XmmRegister.XMM0, IntRegister.RAX);
    assertThat(emitter).containsExactly("movq RAX, XMM0");
  }

  @Test
  public void mov_doubleIntToReg() {
    resolver.mov(VarType.DOUBLE, IntRegister.RAX, XmmRegister.XMM0);
    assertThat(emitter).containsExactly("movq XMM0, RAX");
  }

  @Test
  public void mov_byteTempToReg() {
    assertThat(registers.isAllocated(IntRegister.RBX)).isFalse();
    resolver.mov(TEMP_BYTE, IntRegister.RAX);
    assertThat(emitter).containsExactly("mov AL, BL");
    assertThat(registers.isAllocated(IntRegister.RBX)).isTrue();
  }

  @Test
  public void mov_doubleTempToReg() {
    assertThat(registers.isAllocated(XmmRegister.XMM4)).isFalse();
    resolver.mov(TEMP_DOUBLE, XmmRegister.XMM0);
    assertThat(emitter).containsExactly("movq XMM0, XMM4");
    assertThat(registers.isAllocated(XmmRegister.XMM4)).isTrue();
  }

  @Test
  public void mov_intTempToReg() {
    resolver.reserve(IntRegister.RBX);
    resolver.mov(TEMP_INT, IntRegister.RAX);
    assertThat(emitter).containsExactly("mov EAX, ESI");
    assertThat(registers.isAllocated(IntRegister.RSI)).isTrue();
  }

  @Test
  public void mov_stringTempToReg() {
    resolver.mov(TEMP_STRING, IntRegister.RAX);
    assertThat(emitter).containsExactly("mov RAX, RBX");
  }

  @Test
  public void mov_byteRegToTemp() {
    resolver.mov(IntRegister.RAX, TEMP_BYTE);
    assertThat(emitter).containsExactly("mov BL, AL");
  }

  @Test
  public void mov_doubleRegToTemp() {
    resolver.mov(XmmRegister.XMM0, TEMP_DOUBLE);
    assertThat(emitter).containsExactly("movq XMM4, XMM0");
  }

  @Test
  public void mov_intRegToTemp() {
    resolver.mov(IntRegister.RAX, TEMP_INT);
    assertThat(emitter).containsExactly("mov EBX, EAX");
  }

  @Test
  public void mov_stringRegToTemp() {
    resolver.mov(IntRegister.RAX, TEMP_STRING);
    assertThat(emitter).containsExactly("mov RBX, RAX");
  }

  @Test
  public void mov_byteRegToGlobal() {
    resolver.mov(IntRegister.RAX, GLOBAL_BYTE);
    assertThat(emitter).containsExactly("mov BYTE [_b], AL");
  }

  @Test
  public void mov_doubleRegToGlobal() {
    resolver.mov(XmmRegister.XMM0, GLOBAL_DOUBLE);
    assertThat(emitter).containsExactly("movq [_d], XMM0");
  }

  @Test
  public void mov_intRegToGlobal() {
    resolver.mov(IntRegister.RAX, GLOBAL_INT);
    assertThat(emitter).containsExactly("mov DWORD [_i], EAX");
  }

  @Test
  public void mov_stringRegToGlobal() {
    resolver.mov(IntRegister.RAX, GLOBAL_STRING);
    assertThat(emitter).containsExactly("mov [_s], RAX");
  }

  @Test
  public void mov_byteGlobalToReg() {
    resolver.mov(GLOBAL_BYTE, IntRegister.RAX);
    assertThat(emitter).containsExactly("mov BYTE AL, [_b]");
  }

  @Test
  public void mov_doubleGlobalToReg() {
    resolver.mov(GLOBAL_DOUBLE, XmmRegister.XMM0);
    assertThat(emitter).containsExactly("movsd XMM0, [_d]");
  }

  @Test
  public void mov_intGlobalToReg() {
    resolver.mov(GLOBAL_INT, IntRegister.RAX);
    assertThat(emitter).containsExactly("mov DWORD EAX, [_i]");
  }

  @Test
  public void mov_stringGlobalToReg() {
    resolver.mov(GLOBAL_STRING, IntRegister.RAX);
    assertThat(emitter).containsExactly("mov RAX, [_s]");
  }

  @Test
  public void mov_byteTempToGlobal() {
    assertThat(registers.isAllocated(IntRegister.RBX)).isFalse();
    resolver.mov(TEMP_BYTE, GLOBAL_BYTE);
    assertThat(emitter).containsExactly("mov BYTE [_b], BL");
    assertThat(registers.isAllocated(IntRegister.RBX)).isTrue();
  }

  @Test
  public void mov_doubleTempToGlobal() {
    assertThat(registers.isAllocated(XmmRegister.XMM4)).isFalse();
    resolver.mov(TEMP_DOUBLE, GLOBAL_DOUBLE);
    assertThat(emitter).containsExactly("movq [_d], XMM4");
    assertThat(registers.isAllocated(XmmRegister.XMM4)).isTrue();
  }

  @Test
  public void mov_intTempToGlobal() {
    resolver.mov(TEMP_INT, GLOBAL_INT);
    assertThat(emitter).containsExactly("mov DWORD [_i], EBX");
  }

  @Test
  public void mov_stringTempToGlobal() {
    resolver.mov(TEMP_STRING, GLOBAL_STRING);
    assertThat(emitter).containsExactly("mov [_s], RBX");
  }

  @Test
  public void mov_byteGlobalToGlobal() {
    resolver.mov(GLOBAL_BYTE, GLOBAL_BYTE2);
    assertThat(emitter)
        .containsExactly(
            "mov BYTE BL, [_b]", //
            "mov BYTE [_b2], BL");
    assertThat(registers.isAllocated(IntRegister.RBX)).isFalse();
  }

  @Test
  public void mov_doubleGlobalToGlobal() {
    resolver.mov(GLOBAL_DOUBLE, GLOBAL_DOUBLE2);
    assertThat(emitter)
        .containsExactly(
            "movsd XMM4, [_d]", //
            "movq [_d2], XMM4");
    assertThat(registers.isAllocated(XmmRegister.XMM4)).isFalse();
  }

  @Test
  public void mov_intGlobalToGlobal() {
    registers.reserve(IntRegister.RBX);
    resolver.mov(GLOBAL_INT, GLOBAL_INT2);
    assertThat(emitter)
        .containsExactly(
            "mov DWORD ESI, [_i]", //
            "mov DWORD [_i2], ESI");
    assertThat(registers.isAllocated(IntRegister.RBX)).isTrue();
    assertThat(registers.isAllocated(IntRegister.RSI)).isFalse();
  }

  @Test
  public void mov_stringGlobalToGlobal() {
    resolver.mov(GLOBAL_STRING, GLOBAL_STRING2);
    assertThat(emitter)
        .containsExactly(
            "mov RBX, [_s]", //
            "mov [_s2], RBX");
  }

  @Test
  public void mov_byteParamToGlobal() {
    resolver.mov(PARAM_BYTE, GLOBAL_BYTE2);
    assertThat(emitter).containsExactly("mov BYTE [_b2], CL");
  }

  @Test
  public void mov_doubleParamToGlobal() {
    resolver.mov(PARAM_DOUBLE, GLOBAL_DOUBLE2);
    assertThat(emitter).containsExactly("movq [_d2], XMM1");
  }

  @Test
  public void mov_intParamToGlobal() {
    resolver.mov(PARAM_INT, GLOBAL_INT);
    assertThat(emitter).containsExactly("mov DWORD [_i], R8d");
  }

  @Test
  public void mov_intParamToStack() {
    resolver.mov(PARAM_INT, STACK_INT);
    assertThat(emitter).containsExactly("mov DWORD [RBP - 12], R8d");
  }

  @Test
  public void mov_stringParamToGlobal() {
    resolver.mov(PARAM_STRING, GLOBAL_STRING2);
    assertThat(emitter).containsExactly("mov [_s2], R9");
  }

  @Test
  public void mov_intGlobalToParam() {
    resolver.mov(GLOBAL_INT, PARAM_INT);
    assertThat(emitter).containsExactly("mov DWORD R8d, [_i]");
  }

  @Test
  public void mov_byteLocalToReg() {
    resolver.mov(STACK_BYTE, IntRegister.RAX);
    assertThat(emitter).containsExactly("mov BYTE AL, [RBP - 4]");
  }

  @Test
  public void mov_doubleLocalToReg() {
    resolver.mov(STACK_DOUBLE, XmmRegister.XMM0);
    assertThat(emitter).containsExactly("movsd XMM0, [RBP - 8]");
  }

  @Test
  public void mov_intLocalToReg() {
    resolver.mov(STACK_INT, IntRegister.RAX);
    assertThat(emitter).containsExactly("mov DWORD EAX, [RBP - 12]");
  }

  @Test
  public void mov_localToLocal() {
    resolver.mov(STACK_INT, STACK_INT2);
    assertThat(emitter)
        .containsExactly(
            "mov DWORD EBX, [RBP - 12]", //
            "mov DWORD [RBP - 4], EBX");
  }

  @Test
  public void mov_stringLocalToReg() {
    resolver.mov(STACK_STRING, IntRegister.RAX);
    assertThat(emitter).contains("mov RAX, [RBP - 16]");
  }

  @Test
  public void resolve_byte() {
    String value = resolver.resolve(TEMP_BYTE);
    assertThat(value).isEqualTo("BL");
    assertThat(resolver.isAllocated(IntRegister.RBX)).isTrue();

    // Resolve it again
    assertThat(resolver.resolve(TEMP_BYTE)).isEqualTo("BL");
  }

  @Test
  public void resolve_int() {
    assertThat(resolver.resolve(TEMP_INT)).isEqualTo("EBX");
  }

  @Test
  public void resolve_double() {
    assertThat(resolver.resolve(TEMP_DOUBLE)).isEqualTo("XMM4");
  }

  @Test
  public void resolve_string() {
    assertThat(resolver.resolve(TEMP_STRING)).isEqualTo("RBX");
  }

  @Test
  public void toRegister() {
    resolver.resolve(TEMP_BYTE);
    assertThat(resolver.toRegister(TEMP_BYTE)).isEqualTo(IntRegister.RBX);
  }

  @Test
  public void isInAnyRegister() {
    resolver.resolve(TEMP_INT);
    assertThat(resolver.isInAnyRegister(TEMP_INT)).isTrue();
  }

  @Test
  public void isInRegister() {
    resolver.resolve(TEMP_STRING);
    assertThat(resolver.isInRegister(TEMP_STRING, IntRegister.RBX)).isTrue();
  }
}
