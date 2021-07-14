package com.plasstech.lang.d2.codegen;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.lex.Token;

public class InlineRemapperTest {
  private static final TempLocation TEMP_DEST = new TempLocation("dest");
  private static final TempLocation TEMP_SOURCE = new TempLocation("source");
  private static final TempLocation TEMP_LEFT = new TempLocation("left");
  private static final TempLocation TEMP_RIGHT = new TempLocation("right");
  private static final StackLocation STACK = new StackLocation("stack");
  private static final MemoryAddress MEMORY = new MemoryAddress("memory");
  private static final ImmutableList<String> EMPTY = ImmutableList.of();

  @Test
  public void transferConstantToStack() {
    ImmutableList<Op> input = ImmutableList.of(new Transfer(STACK, ConstantOperand.ONE));
    List<Op> mapped = new InlineRemapper(input, EMPTY).remap();
    Transfer op = (Transfer) mapped.get(0);
    assertThat(op.destination().name()).startsWith("stack__inline__");
    assertThat(op.source().isConstant()).isTrue();
  }

  @Test
  public void transferConstantToTemp() {
    List<Op> mapped =
        new InlineRemapper(ImmutableList.of(new Transfer(TEMP_DEST, ConstantOperand.ONE)), EMPTY)
            .remap();
    Transfer op = (Transfer) mapped.get(0);
    assertThat(op.destination().name()).startsWith("dest__inline__");
    assertThat(op.source().isConstant()).isTrue();
  }

  @Test
  public void transferTemps() {
    List<Op> mapped =
        new InlineRemapper(ImmutableList.of(new Transfer(TEMP_DEST, TEMP_SOURCE)), EMPTY).remap();
    Transfer op = (Transfer) mapped.get(0);
    assertThat(op.destination().name()).startsWith("dest__inline__");
    assertThat(op.source().toString()).startsWith("source__inline__");
  }

  @Test
  public void transferFromTemp() {
    List<Op> mapped =
        new InlineRemapper(ImmutableList.of(new Transfer(STACK, TEMP_SOURCE)), EMPTY).remap();
    Transfer op = (Transfer) mapped.get(0);
    assertThat(op.destination().toString()).startsWith("stack__inline__");
    assertThat(op.source().toString()).startsWith("source__inline__");
  }

  @Test
  public void transferNoTemps() {
    List<Op> mapped =
        new InlineRemapper(
                ImmutableList.of(new Transfer(STACK, MEMORY)), ImmutableList.of(STACK.name()))
            .remap();
    Transfer op = (Transfer) mapped.get(0);
    assertThat(op.destination()).isEqualTo(STACK);
    assertThat(op.source()).isEqualTo(MEMORY);
  }

  @Test
  public void binOpTempDest() {
    List<Op> mapped =
        new InlineRemapper(
                ImmutableList.of(new BinOp(TEMP_DEST, STACK, Token.Type.PLUS, MEMORY)),
                ImmutableList.of(STACK.name()))
            .remap();
    BinOp op = (BinOp) mapped.get(0);
    assertThat(op.destination().name()).startsWith("dest__inline__");
    assertThat(op.left()).isEqualTo(STACK);
    assertThat(op.operator()).isEqualTo(Token.Type.PLUS);
    assertThat(op.right()).isEqualTo(MEMORY);
  }

  @Test
  public void binOpTempSource() {
    List<Op> mapped =
        new InlineRemapper(
                ImmutableList.of(new BinOp(STACK, TEMP_LEFT, Token.Type.AND, TEMP_RIGHT)), EMPTY)
            .remap();
    BinOp op = (BinOp) mapped.get(0);
    assertThat(op.destination().toString()).startsWith("stack__inline__");
    assertThat(op.left().toString()).startsWith("left__inline__");
    assertThat(op.operator()).isEqualTo(Token.Type.AND);
    assertThat(op.right().toString()).startsWith("right__inline__");
  }

  @Test
  public void unaryOpTempSource_formal() {
    List<Op> mapped =
        new InlineRemapper(
                ImmutableList.of(new UnaryOp(STACK, Token.Type.MINUS, TEMP_SOURCE)),
                ImmutableList.of(STACK.name()))
            .remap();
    UnaryOp op = (UnaryOp) mapped.get(0);
    assertThat(op.destination()).isEqualTo(STACK);
    assertThat(op.operator()).isEqualTo(Token.Type.MINUS);
    assertThat(op.operand().toString()).startsWith("source__inline__");
  }
}
