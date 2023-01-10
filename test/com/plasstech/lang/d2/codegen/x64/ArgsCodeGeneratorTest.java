package com.plasstech.lang.d2.codegen.x64;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.google.common.base.Joiner;
import com.plasstech.lang.d2.codegen.Emitter;
import com.plasstech.lang.d2.codegen.ListEmitter;
import com.plasstech.lang.d2.type.ArrayType;
import com.plasstech.lang.d2.type.SymTab;
import com.plasstech.lang.d2.type.VarType;

public class ArgsCodeGeneratorTest {
  private Emitter emitter = new ListEmitter();
  private SymTab symbolTable = new SymTab();

  private ArgsCodeGenerator generator = new ArgsCodeGenerator(emitter, symbolTable);

  @Test
  public void empty() {
    generator.generate();
    assertThat(emitter.all()).isEmpty();
    assertThat(emitter.data()).isEmpty();
    assertThat(emitter.externs()).isEmpty();
  }

  @Test
  public void noArgs() {
    symbolTable.declare("a", VarType.STRING);
    generator.generate();
    assertThat(emitter.all()).isEmpty();
    assertThat(emitter.data()).isEmpty();
    assertThat(emitter.externs()).isEmpty();
  }

  @Test
  public void args() {
    symbolTable.declare("ARGS", new ArrayType(VarType.STRING, 1));
    generator.generate();
    assertThat(emitter.externs()).isNotEmpty();
    assertThat(emitter.all()).isNotEmpty();
    assertThat(emitter.data()).isEmpty();
    System.err.println(Joiner.on("\n").join(emitter.all()));
  }
}
