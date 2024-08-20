package com.plasstech.lang.d2.codegen.x64;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.collect.ImmutableList;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.plasstech.lang.d2.codegen.ConstEntry;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.DelegatingEmitter;
import com.plasstech.lang.d2.codegen.StringTable;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.SysCall.Call;
import com.plasstech.lang.d2.codegen.x64.testing.AsmUtils;

@RunWith(TestParameterInjector.class)
public class PrintCodeGeneratorTest {
  private DelegatingEmitter emitter = new DelegatingEmitter(new X64Emitter());
  private Registers registers = new Registers();
  private StringTable stringTable = new StringTable();
  private Resolver resolver = new Resolver(registers, stringTable, null, emitter);
  private PrintCodeGenerator sut = new PrintCodeGenerator(resolver, stringTable, emitter);

  @Before
  public void setUp() {
    stringTable.add("hi");
  }

  @Test
  public void printStringConstant() {
    SysCall op = new SysCall(Call.PRINT, ConstantOperand.of("hi"));
    sut.visit(op);
    ImmutableList<String> code = AsmUtils.trimComments(emitter.all());
    assertThat(code).containsAtLeast("mov RCX, CONST_hi_0", "call printf").inOrder();
  }

  @Test
  public void printlnStringConstant() {
    SysCall op = new SysCall(Call.PRINTLN, ConstantOperand.of("hi"));
    sut.visit(op);
    ImmutableList<String> code = AsmUtils.trimComments(emitter.all());
    assertThat(code)
        .containsAtLeast("mov RDX, CONST_hi_0", "mov RCX, PRINTLN_STRING", "call printf")
        .inOrder();
  }

  @Test
  public void printParameterizedMessage() {
    String message = "Bad call line %d col %d";
    stringTable.add(message);
    ConstEntry<String> entry = stringTable.lookup(message);
    SysCall op = new SysCall(message,
        ImmutableList.of(ConstantOperand.of(1), ConstantOperand.of(2)));
    sut.visit(op);
    ImmutableList<String> code = AsmUtils.trimComments(emitter.all());
    assertThat(code)
        .containsAtLeast("mov RCX, " + entry.name(), "mov DWORD EDX, 1", "mov DWORD R8d, 2",
            "call printf");
  }

  @Test
  public void printParameterizedMessageWithOperands() {
    String message = "Bad call line %d col %d index %d";
    stringTable.add(message);
    ConstEntry<String> entry = stringTable.lookup(message);
    SysCall op = new SysCall(message, ImmutableList.of(
        ConstantOperand.of(1),
        ConstantOperand.of(2),
        ConstantOperand.of(3)));
    sut.visit(op);
    ImmutableList<String> code = AsmUtils.trimComments(emitter.all());
    assertThat(code)
        .containsAtLeast("mov DWORD R9d, 3", "mov RCX, " + entry.name(), "mov DWORD EDX, 1",
            "mov DWORD R8d, 2", "call printf");
  }

  @Test
  public void printParameterizedMessageWithManyOperands() {
    String message = "Bad call line %d col %d index %d (was %d)";
    stringTable.add(message);
    ConstEntry<String> entry = stringTable.lookup(message);
    SysCall op = new SysCall(message, ImmutableList.of(
        ConstantOperand.of(1),
        ConstantOperand.of(2),
        ConstantOperand.of(3),
        ConstantOperand.of(4)));
    sut.visit(op);
    ImmutableList<String> code = AsmUtils.trimComments(emitter.all());
    assertThat(code)
        .containsAtLeast("mov DWORD R9d, 3",
            "mov DWORD ECX, 4", "push RCX",
            "mov RCX, " + entry.name(), "mov DWORD EDX, 1",
            "mov DWORD R8d, 2", "call printf");
  }
}
