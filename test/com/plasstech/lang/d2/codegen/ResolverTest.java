package com.plasstech.lang.d2.codegen;

import static com.plasstech.lang.d2.codegen.EmitterSubject.assertThat;

import org.junit.Test;

import com.plasstech.lang.d2.type.VarType;

public class ResolverTest {

  private Emitter listEmitter = new ListEmitter();
  private Resolver resolver = new Resolver(new Registers(), null, null, listEmitter);

  @Test
  public void mov_regRegInt() {
    resolver.mov(VarType.INT, IntRegister.RAX, IntRegister.RBX);
    assertThat(listEmitter).containsExactly("mov EBX, EAX");
  }

  @Test
  public void mov_regInt0() {
    resolver.mov(ConstantOperand.ZERO, IntRegister.RAX);
    assertThat(listEmitter).containsExactly("xor RAX, RAX");
  }

  @Test
  public void mov_regByte0() {
    resolver.mov(ConstantOperand.ZERO_BYTE, IntRegister.RAX);
    assertThat(listEmitter).containsExactly("xor RAX, RAX");
  }

  @Test
  public void mov_regRegByte() {
    resolver.mov(VarType.BYTE, IntRegister.RAX, IntRegister.RBX);
    assertThat(listEmitter).containsExactly("mov BL, AL");
  }

  @Test
  public void mov_regRegDouble() {
    resolver.mov(VarType.DOUBLE, XmmRegister.XMM0, XmmRegister.XMM1);
    assertThat(listEmitter).containsExactly("movq XMM1, XMM0");
  }

  @Test
  public void mov_regRegDoubleInt() {
    resolver.mov(VarType.DOUBLE, XmmRegister.XMM0, IntRegister.RAX);
    assertThat(listEmitter).containsExactly("movq RAX, XMM0");
  }
}
