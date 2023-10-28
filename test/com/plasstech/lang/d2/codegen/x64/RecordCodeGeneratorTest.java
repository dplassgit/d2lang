package com.plasstech.lang.d2.codegen.x64;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.DelegatingEmitter;
import com.plasstech.lang.d2.codegen.ListEmitter;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.testing.LocationUtils;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.parse.node.DeclarationNode;
import com.plasstech.lang.d2.parse.node.RecordDeclarationNode;
import com.plasstech.lang.d2.testing.TestUtils;
import com.plasstech.lang.d2.type.RecordReferenceType;
import com.plasstech.lang.d2.type.RecordSymbol;
import com.plasstech.lang.d2.type.SymTab;
import com.plasstech.lang.d2.type.SymbolTable;
import com.plasstech.lang.d2.type.VarType;
import com.plasstech.lang.d2.type.VariableSymbol;

public class RecordCodeGeneratorTest {
  private static final String RECORD_NAME = "recordDefinitionName";

  private static final Joiner NEWLINE_JOINER = Joiner.on("\n");

  private SymbolTable symTab = new SymTab();

  private static final VarType RECORD_TYPE = new RecordReferenceType(RECORD_NAME);
  private static final Location LEFT = LocationUtils.newTempLocation("left", RECORD_TYPE);
  private static final Location RIGHT = LocationUtils.newStackLocation("right", RECORD_TYPE, 4);
  private static final Location DESTINATION =
      new RegisterLocation("dest", IntRegister.RCX, VarType.BOOL);

  private static final Operand NULL = new ConstantOperand<Void>(null, VarType.NULL);

  private DelegatingEmitter emitter = new DelegatingEmitter(new ListEmitter());
  private Registers registers = new Registers();
  private Resolver resolver = new Resolver(registers, null, null, emitter);

  private RecordCodeGenerator sut = new RecordCodeGenerator(resolver, symTab, emitter);

  @Before
  public void setUp() {
    symTab.declareRecord(new RecordDeclarationNode(RECORD_NAME, ImmutableList.of(), null));
  }

  @Test
  public void variableEqeqNull() {
    BinOp op = new BinOp(DESTINATION, LEFT, TokenType.EQEQ, NULL, null);
    ImmutableList<String> code = generateUncommentedCode(op);
    // left == null (comparing to constant null) should generate:
    // cmp QWORD RDX, 0, setz CL
    assertThat(code).doesNotContain("call memcmp");
    assertThat(code).containsExactly("cmp QWORD RBX, 0", "setz CL");
  }

  @Test
  public void variableNeqNull() {
    BinOp op = new BinOp(DESTINATION, LEFT, TokenType.NEQ, NULL, null);
    ImmutableList<String> code = generateUncommentedCode(op);
    // left != null (comparing to constant null) should generate:
    // cmp QWORD RBX, 0, setnz CL (allocated)
    assertThat(code).doesNotContain("call memcmp");
    assertThat(code).containsExactly("cmp QWORD RBX, 0", "setnz CL");
  }

  @Test
  public void nullEqeqVariable() {
    BinOp op = new BinOp(DESTINATION, NULL, TokenType.EQEQ, RIGHT, null);
    ImmutableList<String> code = generateUncommentedCode(op);
    // It generates no code now; nasmcodegenerator is responsible
    assertThat(code).isEmpty();
  }

  @Test
  public void nullNeqVariable() {
    BinOp op = new BinOp(DESTINATION, NULL, TokenType.NEQ, RIGHT, null);
    ImmutableList<String> code = generateUncommentedCode(op);
    // It generates no code now; nasmcodegenerator is responsible
    assertThat(code).isEmpty();
  }

  @Test
  public void variableEqVariable() {
    BinOp op = new BinOp(DESTINATION, LEFT, TokenType.EQEQ, RIGHT, null);
    ImmutableList<String> code = generateUncommentedCode(op);
    assertThat(code).containsAtLeast("call memcmp", "cmp RAX, 0", "setz CL").inOrder();
  }

  @Test
  public void variableNeqVariable() {
    BinOp op = new BinOp(DESTINATION, LEFT, TokenType.NEQ, RIGHT, null);
    ImmutableList<String> code = generateUncommentedCode(op);
    assertThat(code).containsAtLeast("call memcmp", "cmp RAX, 0", "setnz CL").inOrder();
  }

  @Test
  public void dotIntoRegister() {
    String fieldName = "fieldName";
    DeclarationNode fieldDecl1 = new DeclarationNode(fieldName + "0", VarType.LONG, null);
    DeclarationNode fieldDecl2 = new DeclarationNode(fieldName, VarType.BYTE, null);
    RecordSymbol recordDecl = symTab
        .declareRecord(
            new RecordDeclarationNode("recordWithField", ImmutableList.of(fieldDecl1, fieldDecl2),
                null));
    VarType recordRefType = new RecordReferenceType(recordDecl.name());
    VariableSymbol rec = symTab.declare("source", recordRefType);
    rec.setRecordSymbol(recordDecl);

    Location source = LocationUtils.newMemoryAddress("source", recordRefType);
    Location dest = LocationUtils.newParamLocation("dest", VarType.BYTE, 2, 0);
    Operand fieldOperand = ConstantOperand.of(fieldName);
    BinOp op = new BinOp(dest, source, TokenType.DOT, fieldOperand, new Position(0, 0));
    ImmutableList<String> code = generateUncommentedCode(op);
    // It's R8 because the dest is the 3rd param.
    assertThat(code).contains("mov BYTE R8b, [RBX]");
  }

  @Test
  public void dotIntoTemp() {
    String fieldName = "fieldName";
    DeclarationNode fieldDecl1 = new DeclarationNode(fieldName + "0", VarType.LONG, null);
    DeclarationNode fieldDecl2 = new DeclarationNode(fieldName, VarType.BYTE, null);
    RecordSymbol recordDecl = symTab
        .declareRecord(
            new RecordDeclarationNode("recordWithField", ImmutableList.of(fieldDecl1, fieldDecl2),
                null));
    VariableSymbol rec = symTab.declare("source", recordDecl.varType());
    rec.setRecordSymbol(recordDecl);

    Location source = LocationUtils.newMemoryAddress("source", recordDecl.varType());

    Location dest = LocationUtils.newTempLocation("dest", VarType.BYTE);
    Operand fieldOperand = ConstantOperand.of(fieldName);
    BinOp op = new BinOp(dest, source, TokenType.DOT, fieldOperand, new Position(0, 0));
    ImmutableList<String> code = generateUncommentedCode(op);
    // It's RSI because it allocated "dest" to RSI
    assertThat(code).contains("mov BYTE SIL, [RBX]");
  }

  @Test
  public void dotIntoMemory() {
    String fieldName = "fieldName";
    DeclarationNode fieldDecl1 = new DeclarationNode(fieldName + "0", VarType.LONG, null);
    DeclarationNode fieldDecl2 = new DeclarationNode(fieldName, VarType.BYTE, null);
    RecordSymbol recordDecl = symTab
        .declareRecord(
            new RecordDeclarationNode("recordWithField", ImmutableList.of(fieldDecl1, fieldDecl2),
                null));
    VariableSymbol rec = symTab.declare("source", recordDecl.varType());
    rec.setRecordSymbol(recordDecl);

    Location source = LocationUtils.newMemoryAddress("source", recordDecl.varType());

    Location dest = LocationUtils.newMemoryAddress("dest", VarType.BYTE);
    Operand fieldOperand = ConstantOperand.of(fieldName);
    BinOp op = new BinOp(dest, source, TokenType.DOT, fieldOperand, new Position(0, 0));
    ImmutableList<String> code = generateUncommentedCode(op);
    // RSI is the indirect register
    assertThat(code).containsAtLeast("mov BYTE SIL, [RBX]", "mov BYTE [_dest], SIL").inOrder();
  }

  @Test
  public void dotIntoStack() {
    String fieldName = "fieldName";
    DeclarationNode fieldDecl1 = new DeclarationNode(fieldName + "0", VarType.LONG, null);
    DeclarationNode fieldDecl2 = new DeclarationNode(fieldName, VarType.BYTE, null);
    RecordSymbol recordDecl = symTab
        .declareRecord(
            new RecordDeclarationNode("recordWithField", ImmutableList.of(fieldDecl1, fieldDecl2),
                null));
    VariableSymbol rec = symTab.declare("source", recordDecl.varType());
    rec.setRecordSymbol(recordDecl);

    Location source = LocationUtils.newMemoryAddress("source", recordDecl.varType());

    Location dest = LocationUtils.newStackLocation("dest", VarType.BYTE, 12);
    Operand fieldOperand = ConstantOperand.of(fieldName);
    BinOp op = new BinOp(dest, source, TokenType.DOT, fieldOperand, new Position(0, 0));
    ImmutableList<String> code = generateUncommentedCode(op);
    assertThat(code).containsAtLeast("mov BYTE SIL, [RBX]", "mov BYTE [RBP - 12], SIL").inOrder();
  }

  private ImmutableList<String> generateUncommentedCode(BinOp op) {
    sut.visit(op);
    System.err.printf("\nTEST CASE: %s\n\n", op);
    System.err.println(NEWLINE_JOINER.join(emitter.all()));
    return TestUtils.trimComments(emitter.all());
  }
}
