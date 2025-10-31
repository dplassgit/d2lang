package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.MemoryAddress;
import com.plasstech.lang.d2.codegen.StackLocation;
import com.plasstech.lang.d2.codegen.TempLocation;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.codegen.testing.LocationUtils;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.SymbolTable;
import com.plasstech.lang.d2.type.VarType;

public class InlineRemapperTest {
  private SymbolTable symbolTable = new SymbolTable();
  private SymbolTable localSymbolTable = new SymbolTable(symbolTable, SymbolStorage.LOCAL);

  private final TempLocation TEMP_DEST =
      LocationUtils.newTempLocation(symbolTable, "__dest", VarType.INT);
  private final TempLocation TEMP_SOURCE =
      LocationUtils.newTempLocation(symbolTable, "__source", VarType.INT);
  private final TempLocation TEMP_LEFT =
      LocationUtils.newTempLocation(symbolTable, "__left", VarType.INT);
  private final TempLocation TEMP_RIGHT =
      LocationUtils.newTempLocation(symbolTable, "__right", VarType.INT);
  private final StackLocation STACK =
      LocationUtils.newStackLocation(localSymbolTable, "stack", VarType.INT, 0);
  private final MemoryAddress MEMORY = LocationUtils.newMemoryAddress("memory", VarType.INT);

  @Test
  public void transferConstantToStack() {
    ImmutableList<Op> input = ImmutableList.of(new Transfer(STACK, ConstantOperand.ONE, null));
    List<Op> mapped = new InlineRemapper(input, symbolTable).remap();
    Transfer op = (Transfer) mapped.get(0);
    assertThat(op.destination().name()).startsWith("_stack__inline");
    assertThat(op.source().isConstant()).isTrue();
  }

  @Test
  public void remapsToLongTemps() {
    ImmutableList<Op> input = ImmutableList.of(
        new Transfer(STACK, ConstantOperand.ONE, null),
        new Transfer(STACK, ConstantOperand.ZERO, null));
    InlineRemapper mapper = new InlineRemapper(input, symbolTable);
    List<Op> mapped = mapper.remap();
    Transfer op = (Transfer) mapped.get(0);
    Location destination = op.destination();
    assertThat(destination.storage()).isEqualTo(SymbolStorage.LONG_TEMP);
    assertThat(mapper.getLongTemps()).containsExactly(destination);
  }

  @Test
  public void transferConstantToTemp() {
    List<Op> mapped =
        new InlineRemapper(
            ImmutableList.of(new Transfer(TEMP_DEST, ConstantOperand.ONE, null)), symbolTable)
            .remap();
    Transfer op = (Transfer) mapped.get(0);
    assertThat(op.destination().name()).contains("__dest__inline");
    assertThat(op.source().isConstant()).isTrue();
  }

  @Test
  public void transferTemps() {
    List<Op> mapped =
        new InlineRemapper(ImmutableList.of(new Transfer(TEMP_DEST, TEMP_SOURCE, null)),
            symbolTable)
            .remap();
    Transfer op = (Transfer) mapped.get(0);
    assertThat(op.destination().name()).contains("__dest__inline");
    assertThat(op.source().toString()).contains("__source__inline");
  }

  @Test
  public void transferFromTemp() {
    List<Op> mapped =
        new InlineRemapper(ImmutableList.of(new Transfer(STACK, TEMP_SOURCE, null)),
            symbolTable)
            .remap();
    Transfer op = (Transfer) mapped.get(0);
    assertThat(op.destination().toString()).startsWith("_stack__inline");
    assertThat(op.source().toString()).contains("__source__inline");
  }

  @Test
  public void transferNoTemps() {
    List<Op> mapped =
        new InlineRemapper(ImmutableList.of(new Transfer(STACK, MEMORY, null)), symbolTable)
            .remap();
    Transfer op = (Transfer) mapped.get(0);
    assertThat(op.destination().toString()).startsWith("_stack__inline");
    assertThat(op.source()).isEqualTo(MEMORY);
  }

  @Test
  public void binOpTempDest() {
    List<Op> mapped =
        new InlineRemapper(
            ImmutableList.of(new BinOp(TEMP_DEST, STACK, TokenType.PLUS, MEMORY, null)),
            symbolTable)
            .remap();
    BinOp op = (BinOp) mapped.get(0);
    assertThat(op.destination().name()).contains("__dest__inline");
    assertThat(op.left().toString()).startsWith("_stack__inline");
    assertThat(op.operator()).isEqualTo(TokenType.PLUS);
    assertThat(op.right()).isEqualTo(MEMORY);
  }

  @Test
  public void binOpTempSource() {
    List<Op> mapped =
        new InlineRemapper(
            ImmutableList.of(new BinOp(STACK, TEMP_LEFT, TokenType.AND, TEMP_RIGHT, null)),
            symbolTable)
            .remap();
    BinOp op = (BinOp) mapped.get(0);
    assertThat(op.destination().toString()).startsWith("_stack__inline");
    assertThat(op.left().toString()).contains("__left__inline");
    assertThat(op.operator()).isEqualTo(TokenType.AND);
    assertThat(op.right().toString()).contains("__right__inline");
  }

  @Test
  public void unaryOpTempSource_formal() {
    List<Op> mapped =
        new InlineRemapper(
            ImmutableList.of(new UnaryOp(STACK, TokenType.MINUS, TEMP_SOURCE, null)),
            symbolTable)
            .remap();
    UnaryOp op = (UnaryOp) mapped.get(0);
    assertThat(op.destination().toString()).startsWith("_stack__inline");
    assertThat(op.operator()).isEqualTo(TokenType.MINUS);
    assertThat(op.operand().toString()).contains("__source__inline");
  }
}
