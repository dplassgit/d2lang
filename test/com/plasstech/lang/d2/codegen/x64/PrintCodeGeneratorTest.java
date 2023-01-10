package com.plasstech.lang.d2.codegen.x64;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.collect.ImmutableList;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.DelegatingEmitter;
import com.plasstech.lang.d2.codegen.ListEmitter;
import com.plasstech.lang.d2.codegen.StringTable;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.SysCall.Call;
import com.plasstech.lang.d2.codegen.x64.PrintCodeGenerator;
import com.plasstech.lang.d2.codegen.x64.Registers;
import com.plasstech.lang.d2.codegen.x64.Resolver;
import com.plasstech.lang.d2.testing.TestUtils;

@RunWith(TestParameterInjector.class)
public class PrintCodeGeneratorTest {
  private DelegatingEmitter emitter = new DelegatingEmitter(new ListEmitter());
  private Registers registers = new Registers();
  private StringTable stringTable = new StringTable();
  private Resolver resolver = new Resolver(registers, stringTable, null, emitter);
  private PrintCodeGenerator sut = new PrintCodeGenerator(resolver, emitter);

  @Before
  public void setUp() {
    stringTable.add("hi");
  }

  @Test
  public void printStringConstant() {
    SysCall op = new SysCall(Call.PRINT, ConstantOperand.of("hi"));
    sut.visit(op);
    ImmutableList<String> code = TestUtils.trimComments(emitter.all());
    assertThat(code).containsAtLeast("mov RCX, CONST_hi_0", "call printf").inOrder();
  }

  @Test
  public void printlnStringConstant() {
    SysCall op = new SysCall(Call.PRINTLN, ConstantOperand.of("hi"));
    sut.visit(op);
    ImmutableList<String> code = TestUtils.trimComments(emitter.all());
    assertThat(code)
        .containsAtLeast("mov RDX, CONST_hi_0", "mov RCX, PRINTLN_STRING", "call printf")
        .inOrder();
  }
}
