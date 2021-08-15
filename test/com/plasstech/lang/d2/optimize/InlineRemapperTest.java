package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.MemoryAddress;
import com.plasstech.lang.d2.codegen.StackLocation;
import com.plasstech.lang.d2.codegen.TempLocation;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.VarType;

public class InlineRemapperTest {
  private static final TempLocation TEMP_DEST = new TempLocation("__dest", VarType.INT);
  private static final TempLocation TEMP_SOURCE = new TempLocation("__source", VarType.INT);
  private static final TempLocation TEMP_LEFT = new TempLocation("__left", VarType.INT);
  private static final TempLocation TEMP_RIGHT = new TempLocation("__right", VarType.INT);
  private static final StackLocation STACK = new StackLocation("stack", VarType.INT);
  private static final MemoryAddress MEMORY = new MemoryAddress("memory", VarType.INT);

  @Test
  public void transferConstantToStack() {
    ImmutableList<Op> input = ImmutableList.of(new Transfer(STACK, ConstantOperand.ONE));
    List<Op> mapped = new InlineRemapper(input).remap();
    Transfer op = (Transfer) mapped.get(0);
    assertThat(op.destination().name()).startsWith("__stack__inline__");
    assertThat(op.source().isConstant()).isTrue();
  }

  @Test
  public void transferConstantToTemp() {
    List<Op> mapped =
        new InlineRemapper(ImmutableList.of(new Transfer(TEMP_DEST, ConstantOperand.ONE)))
            .remap();
    Transfer op = (Transfer) mapped.get(0);
    assertThat(op.destination().name()).startsWith("__dest__inline__");
    assertThat(op.source().isConstant()).isTrue();
  }

  @Test
  public void transferTemps() {
    List<Op> mapped =
        new InlineRemapper(ImmutableList.of(new Transfer(TEMP_DEST, TEMP_SOURCE))).remap();
    Transfer op = (Transfer) mapped.get(0);
    assertThat(op.destination().name()).startsWith("__dest__inline__");
    assertThat(op.source().toString()).startsWith("__source__inline__");
  }

  @Test
  public void transferFromTemp() {
    List<Op> mapped =
        new InlineRemapper(ImmutableList.of(new Transfer(STACK, TEMP_SOURCE))).remap();
    Transfer op = (Transfer) mapped.get(0);
    assertThat(op.destination().toString()).startsWith("__stack__inline__");
    assertThat(op.source().toString()).startsWith("__source__inline__");
  }

  @Test
  public void transferNoTemps() {
    List<Op> mapped =
        new InlineRemapper(
                ImmutableList.of(new Transfer(STACK, MEMORY)))
            .remap();
    Transfer op = (Transfer) mapped.get(0);
    assertThat(op.destination().toString()).startsWith("__stack__inline__");
    assertThat(op.source()).isEqualTo(MEMORY);
  }

  @Test
  public void binOpTempDest() {
    List<Op> mapped =
        new InlineRemapper(
                ImmutableList.of(new BinOp(TEMP_DEST, STACK, TokenType.PLUS, MEMORY)))
            .remap();
    BinOp op = (BinOp) mapped.get(0);
    assertThat(op.destination().name()).startsWith("__dest__inline__");
    assertThat(op.left().toString()).startsWith("__stack__inline__");
    assertThat(op.operator()).isEqualTo(TokenType.PLUS);
    assertThat(op.right()).isEqualTo(MEMORY);
  }

  @Test
  public void binOpTempSource() {
    List<Op> mapped =
        new InlineRemapper(
                ImmutableList.of(new BinOp(STACK, TEMP_LEFT, TokenType.AND, TEMP_RIGHT)))
            .remap();
    BinOp op = (BinOp) mapped.get(0);
    assertThat(op.destination().toString()).startsWith("__stack__inline__");
    assertThat(op.left().toString()).startsWith("__left__inline__");
    assertThat(op.operator()).isEqualTo(TokenType.AND);
    assertThat(op.right().toString()).startsWith("__right__inline__");
  }

  @Test
  public void unaryOpTempSource_formal() {
    List<Op> mapped =
        new InlineRemapper(
                ImmutableList.of(new UnaryOp(STACK, TokenType.MINUS, TEMP_SOURCE)))
            .remap();
    UnaryOp op = (UnaryOp) mapped.get(0);
    assertThat(op.destination().toString()).startsWith("__stack__inline__");
    assertThat(op.operator()).isEqualTo(TokenType.MINUS);
    assertThat(op.operand().toString()).startsWith("__source__inline__");
  }
}
