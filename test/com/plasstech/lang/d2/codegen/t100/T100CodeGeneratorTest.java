package com.plasstech.lang.d2.codegen.t100;

import static com.plasstech.lang.d2.codegen.testing.EmitterSubject.assertThat;
import static com.plasstech.lang.d2.codegen.testing.LocationUtils.newMemoryAddress;
import static com.plasstech.lang.d2.codegen.testing.LocationUtils.newTempLocation;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Emitter;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Stop;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.parse.node.BlockNode;
import com.plasstech.lang.d2.parse.node.ProgramNode;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.type.SymTab;
import com.plasstech.lang.d2.type.SymbolTable;
import com.plasstech.lang.d2.type.TypeCheckResult;
import com.plasstech.lang.d2.type.VarType;

public class T100CodeGeneratorTest {
  private Emitter emitter = new T100Emitter();
  private Registers registers = new Registers();
  private SymbolTable symTab = new SymTab();
  private T100CodeGenerator codeGen = new T100CodeGenerator(emitter, registers);

  @Test
  public void printConstantString() {
    SysCall op = new SysCall(SysCall.Call.PRINT, ConstantOperand.of("hello world"));
    generate(op);
    assertThat(emitter)
        .containsAtLeast(
            "lxi H, CONST_helloworld_0", //
            "call 0x11A2")
        .inOrder();
  }

  @Test
  public void printConstantInt() {
    // note less than 32767
    SysCall op = new SysCall(SysCall.Call.PRINT, ConstantOperand.of(12345));
    generate(op);
    assertThat(emitter)
        .containsAtLeast(
            "lxi H, 0x3039", //
            // print the ASCII value of the number in HL (destroys all)");
            "call 0x39D4")
        .inOrder();
  }

  @Test
  public void printConstantByte() {
    SysCall op = new SysCall(SysCall.Call.PRINT, ConstantOperand.of((byte) 8));
    generate(op);
    assertThat(emitter)
        .containsAtLeast(
            "lxi H, 0x0008", //
            // print the ASCII value of the number in HL (destroys all)");
            "call 0x39D4")
        .inOrder();
  }

  @Test
  public void printInt() {
    Location glob = newMemoryAddress("glob", VarType.INT);
    symTab.declare("glob", VarType.INT);
    SysCall op = new SysCall(SysCall.Call.PRINT, glob);
    generate(op);
    assertThat(emitter)
        .containsAtLeast(
            "lxi B, _glob", //
            "call D_print32")
        .inOrder();
  }

  @Test
  public void printByte() {
    Location glob = newMemoryAddress("glob", VarType.BYTE);
    symTab.declare("glob", VarType.BYTE);
    SysCall op = new SysCall(SysCall.Call.PRINT, glob);
    generate(op);
    assertThat(emitter)
        .containsAtLeast(
            "lda _glob", //
            "call D_print8")
        .inOrder();
  }

  @Test
  public void globalConstant() {
    SysCall op = new SysCall(SysCall.Call.PRINT, ConstantOperand.of("hello world"));
    generate(op);
    assertThat(emitter)
        .containsAtLeast(
            "CONST_helloworld_0: db 0x68,0x65,0x6c,0x6c,0x6f,0x20,0x77,0x6f,0x72,0x6c,0x64,0x00")
        .inOrder();
  }

  @Test
  public void printTrue() {
    SysCall op = new SysCall(SysCall.Call.PRINT, ConstantOperand.TRUE);
    generate(op);
    assertThat(emitter)
        .containsAtLeast(
            "lxi H, TRUE_MSG", //
            "call 0x11A2")
        .inOrder();
  }

  @Test
  public void printFalse() {
    SysCall op = new SysCall(SysCall.Call.PRINT, ConstantOperand.FALSE);
    generate(op);
    assertThat(emitter)
        .containsAtLeast(
            "lxi H, FALSE_MSG", //
            "call 0x11A2")
        .inOrder();
  }

  @Test
  public void messageConstant() {
    SysCall op = new SysCall(SysCall.Call.MESSAGE, ConstantOperand.of("hello world"));
    generate(op);
    assertThat(emitter)
        .containsAtLeast(
            "lxi H, CONST_helloworld_0", //
            "call 0x11A2")
        .inOrder();
  }

  @Test
  public void globalIsDeclared() {
    SysCall op = new SysCall(SysCall.Call.PRINT, ConstantOperand.EMPTY_STRING);
    symTab.declare("glob", VarType.INT);
    generate(op);
    assertThat(emitter).contains("_glob: db 0x00,0x00,0x00,0x00");
  }

  @Test
  public void globalBoolIsDeclared() {
    SysCall op = new SysCall(SysCall.Call.PRINT, ConstantOperand.EMPTY_STRING);
    symTab.declare("glob", VarType.BOOL);
    generate(op);
    assertThat(emitter).contains("_glob: db 0x00");
  }

  @Test
  public void transferConstantToGlobal() {
    Location glob = newMemoryAddress("glob", VarType.INT);
    Transfer op = new Transfer(glob, ConstantOperand.of(12345678), null);
    generate(op);
    assertThat(emitter)
        .containsAtLeast("lxi H, 0x614e", "shld _glob", "lxi H, 0x00bc", "shld _glob + 0x02")
        .inOrder();
  }

  @Test
  public void transferToTemp() {
    Location temp = newTempLocation("temp0", VarType.INT);
    Transfer op = new Transfer(temp, ConstantOperand.of(12345678), null);
    generate(op);
    assertThat(emitter)
        .containsAtLeast(
            "lxi H, 0x614e",
            "shld _TEMP_INT_0",
            "lxi H, 0x00bc",
            "shld _TEMP_INT_0 + 0x02")
        .inOrder();
  }

  @Test
  public void transferBoolGlobal() {
    Location glob = newMemoryAddress("glob", VarType.BOOL);
    Transfer op = new Transfer(glob, ConstantOperand.TRUE, null);
    generate(op);
    assertThat(emitter).containsAtLeast("lxi H, _glob", "mvi M, 0x01").inOrder();
  }

  @Test
  public void eqeqBytes() {
    Location glob = newMemoryAddress("glob", VarType.BOOL);
    Location left = newMemoryAddress("left", VarType.BYTE);
    Location right = newMemoryAddress("right", VarType.BYTE);
    BinOp op = new BinOp(glob, left, TokenType.EQEQ, right, null);
    generate(op);
    assertThat(emitter).containsAtLeast(
        "lda _left",
        "lxi H, _right",
        "cmp M",
        "sta _glob").inOrder();
  }

  @Test
  public void leqBytes() {
    Location glob = newMemoryAddress("glob", VarType.BOOL);
    Location left = newMemoryAddress("left", VarType.BYTE);
    Location right = newMemoryAddress("right", VarType.BYTE);
    BinOp op = new BinOp(glob, left, TokenType.LEQ, right, null);
    generate(op);
    assertThat(emitter).containsAtLeast("lda _left", "lxi H, _right", "cmp M").inOrder();
  }

  @Test
  public void gtZero() {
    Location glob = newMemoryAddress("glob", VarType.BOOL);
    Location left = newMemoryAddress("left", VarType.BYTE);
    Operand right = ConstantOperand.ZERO_BYTE;
    BinOp op = new BinOp(glob, left, TokenType.GT, right, null);
    generate(op);
    assertThat(emitter).containsAtLeast("lda _left", "cpi 0x00");
  }

  @Test
  public void gtOne() {
    Location glob = newMemoryAddress("glob", VarType.BOOL);
    Location left = newMemoryAddress("left", VarType.BYTE);
    Operand right = ConstantOperand.ONE_BYTE;
    BinOp op = new BinOp(glob, left, TokenType.GT, right, null);
    generate(op);
    assertThat(emitter).containsAtLeast("lda _left", "cpi 0x01");
  }

  @Test
  public void addBytes() {
    Location glob = newMemoryAddress("glob", VarType.BYTE);
    Location left = newMemoryAddress("left", VarType.BYTE);
    Location right = newMemoryAddress("right", VarType.BYTE);
    BinOp op = new BinOp(glob, left, TokenType.PLUS, right, null);
    generate(op);
    assertThat(emitter).containsAtLeast("lda _left", "lxi H, _right", "add M", "sta _glob");
  }

  @Test
  public void minusByte() {
    Location operand = newMemoryAddress("operand", VarType.BYTE);
    Location glob = newMemoryAddress("glob", VarType.BYTE);
    UnaryOp op = new UnaryOp(glob, TokenType.MINUS, operand, null);
    generate(op);
    assertThat(emitter).containsAtLeast("lda _operand", "cma", "inr A", "sta _glob").inOrder();
  }

  @Test
  public void minusByteConstant() {
    Location glob = newMemoryAddress("glob", VarType.BYTE);
    UnaryOp op = new UnaryOp(glob, TokenType.MINUS, ConstantOperand.of((byte) 3), null);
    generate(op);
    assertThat(emitter).containsAtLeast("mvi A, 0xfd", "sta _glob").inOrder();
  }

  @Test
  public void minusIntConstant() {
    Location glob = newMemoryAddress("glob", VarType.BYTE);
    UnaryOp op = new UnaryOp(glob, TokenType.MINUS, ConstantOperand.of(3), null);
    generate(op);
    assertThat(emitter).containsAtLeast("lxi H, 0xfffd", "lxi H, 0xffff").inOrder();
  }

  @Test
  public void minusInt() {
    Location operand = newMemoryAddress("operand", VarType.INT);
    Location glob = newMemoryAddress("glob", VarType.INT);
    UnaryOp op = new UnaryOp(glob, TokenType.MINUS, operand, null);
    generate(op);
    assertThat(emitter)
        .containsAtLeast("lxi H, 0x0000", "shld _glob", "lxi H, _operand", "call D_sub32")
        .inOrder();
  }

  @Test
  public void addInts() {
    Location glob = newMemoryAddress("glob", VarType.INT);
    Location left = newMemoryAddress("left", VarType.INT);
    Location right = newMemoryAddress("right", VarType.INT);
    BinOp op = new BinOp(glob, left, TokenType.PLUS, right, null);
    generate(op);
    assertThat(emitter).containsAtLeast("lxi B, _left", "lxi H, _right", "call D_add32");
  }

  @Test
  public void subInts() {
    Location glob = newMemoryAddress("glob", VarType.INT);
    Location left = newMemoryAddress("left", VarType.INT);
    Location right = newMemoryAddress("right", VarType.INT);
    BinOp op = new BinOp(glob, left, TokenType.MINUS, right, null);
    generate(op);
    assertThat(emitter).containsAtLeast("lxi B, _left", "lxi H, _right", "call D_sub32");
  }

  @Test
  public void andInts() {
    Location glob = newMemoryAddress("glob", VarType.INT);
    Location left = newMemoryAddress("left", VarType.INT);
    Location right = newMemoryAddress("right", VarType.INT);
    BinOp op = new BinOp(glob, left, TokenType.BIT_AND, right, null);
    generate(op);
    assertThat(emitter).containsAtLeast("call D_bitand32", "ana M");
  }

  @Test
  public void divInts() {
    Location glob = newMemoryAddress("glob", VarType.INT);
    Location left = newMemoryAddress("left", VarType.INT);
    Location right = newMemoryAddress("right", VarType.INT);
    BinOp op = new BinOp(glob, left, TokenType.DIV, right, null);
    generate(op);
    assertThat(emitter).containsAtLeast("call D_div32", "call D_copy32", "call D_inc32");
  }

  private State generate(Op op) {
    return generate(ImmutableList.of(op, new Stop()));
  }

  private State generate(ImmutableList<Op> ops) {
    State state = State.create();
    TypeCheckResult typeCheckResult = new TypeCheckResult(symTab);
    state = state.addProgramNode(new ProgramNode(BlockNode.EMPTY));
    state = state.addTypecheckResult(typeCheckResult);
    state = state.addIlCode(ops);
    state = codeGen.execute(state);
    if (state.error()) {
      fail(state.errorMessage());
    }
    System.err.println("");
    System.err.println(Joiner.on('\n').join(state.asmCode()));
    return state;
  }
}
