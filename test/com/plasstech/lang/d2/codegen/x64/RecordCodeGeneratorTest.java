package com.plasstech.lang.d2.codegen.x64;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.DelegatingEmitter;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.il.AllocateOp;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.testing.LocationUtils;
import com.plasstech.lang.d2.codegen.x64.testing.AsmUtils;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.parse.node.DeclarationNode;
import com.plasstech.lang.d2.parse.node.RecordDeclarationNode;
import com.plasstech.lang.d2.type.RecordReferenceType;
import com.plasstech.lang.d2.type.RecordSymbol;
import com.plasstech.lang.d2.type.SymbolTable;
import com.plasstech.lang.d2.type.UnboundType;
import com.plasstech.lang.d2.type.VarType;
import org.junit.Before;
import org.junit.Test;

public class RecordCodeGeneratorTest {
  private static final String RECORD_NAME = "recordDefinitionName";
  private static final String UNBOUND_RECORD_NAME = "list";
  private static final Joiner NEWLINE_JOINER = Joiner.on("\n");
  private static final VarType RECORD_TYPE = new RecordReferenceType(RECORD_NAME);
  private static final Location LEFT_RECORD = LocationUtils.newTempLocation("left", RECORD_TYPE);
  private static final Location RIGHT_RECORD =
      LocationUtils.newStackLocation("right", RECORD_TYPE, 4);
  private static final Location BOOL_DESTINATION =
      new RegisterLocation("dest", IntRegister.RCX, VarType.BOOL);
  private static final Operand NULL = new ConstantOperand<Void>(null, VarType.NULL);
  private static final VarType UNBOUND_TYPE = new UnboundType("T");

  private DelegatingEmitter emitter = new DelegatingEmitter(new X64Emitter());
  private Registers registers = new Registers();
  private Resolver resolver = new Resolver(registers, null, null, emitter);
  private final SymbolTable symTab = new SymbolTable();

  private RecordCodeGenerator generator = new RecordCodeGenerator(resolver, symTab, emitter);
  private RecordSymbol nonGenericRecord;
  private RecordSymbol boundRecord;

  @Before
  public void setUp() {
    // Empty record
    nonGenericRecord =
        symTab.declareRecord(new RecordDeclarationNode(RECORD_NAME, ImmutableList.of(), null));

    RecordSymbol unboundRecord =
        symTab.declareRecord(
            new RecordDeclarationNode(
                UNBOUND_RECORD_NAME,
                ImmutableList.of(new DeclarationNode("value", UNBOUND_TYPE, null)),
                null,
                ImmutableList.of(UNBOUND_TYPE.name())));
    boundRecord = unboundRecord.bind(ImmutableMap.of(UNBOUND_TYPE.name(), VarType.INT));
    symTab.declareBoundRecordSymbol(boundRecord);
  }

  @Test
  public void variableEqeqNull() {
    BinOp op = new BinOp(BOOL_DESTINATION, LEFT_RECORD, TokenType.EQEQ, NULL, null);
    ImmutableList<String> code = generateUncommentedCode(op);
    // left == null (comparing to constant null) should generate:
    // cmp QWORD RDX, 0, setz CL
    assertThat(code).doesNotContain("call memcmp");
    assertThat(code).containsExactly("cmp QWORD RBX, 0", "setz CL");
  }

  @Test
  public void variableNeqNull() {
    BinOp op = new BinOp(BOOL_DESTINATION, LEFT_RECORD, TokenType.NEQ, NULL, null);
    ImmutableList<String> code = generateUncommentedCode(op);
    // left != null (comparing to constant null) should generate:
    // cmp QWORD RBX, 0, setnz CL (allocated)
    assertThat(code).doesNotContain("call memcmp");
    assertThat(code).containsExactly("cmp QWORD RBX, 0", "setnz CL");
  }

  @Test
  public void nullEqeqVariable() {
    BinOp op = new BinOp(BOOL_DESTINATION, NULL, TokenType.EQEQ, RIGHT_RECORD, null);
    ImmutableList<String> code = generateUncommentedCode(op);
    // It generates no code now; nasmcodegenerator is responsible
    assertThat(code).isEmpty();
  }

  @Test
  public void nullNeqVariable() {
    BinOp op = new BinOp(BOOL_DESTINATION, NULL, TokenType.NEQ, RIGHT_RECORD, null);
    ImmutableList<String> code = generateUncommentedCode(op);
    // It generates no code now; nasmcodegenerator is responsible
    assertThat(code).isEmpty();
  }

  @Test
  public void variableEqVariable() {
    BinOp op = new BinOp(BOOL_DESTINATION, LEFT_RECORD, TokenType.EQEQ, RIGHT_RECORD, null);
    ImmutableList<String> code = generateUncommentedCode(op);
    assertThat(code).containsAtLeast("call memcmp", "cmp RAX, 0", "setz CL").inOrder();
  }

  @Test
  public void variableNeqVariable() {
    BinOp op = new BinOp(BOOL_DESTINATION, LEFT_RECORD, TokenType.NEQ, RIGHT_RECORD, null);
    ImmutableList<String> code = generateUncommentedCode(op);
    assertThat(code).containsAtLeast("call memcmp", "cmp RAX, 0", "setnz CL").inOrder();
  }

  @Test
  public void dotIntoRegister() {
    String fieldName = "fieldName";
    DeclarationNode fieldDecl1 = new DeclarationNode(fieldName + "0", VarType.LONG, null);
    DeclarationNode fieldDecl2 = new DeclarationNode(fieldName, VarType.BYTE, null);
    RecordSymbol recordDecl =
        symTab.declareRecord(
            new RecordDeclarationNode(
                "recordWithField", ImmutableList.of(fieldDecl1, fieldDecl2), null));
    VarType recordRefType = new RecordReferenceType(recordDecl.name());
    Location source = LocationUtils.newMemoryAddress("source", recordRefType);
    Location dest = LocationUtils.newParamLocation("dest", VarType.BYTE, 2, 0);
    Operand fieldOperand = ConstantOperand.of(fieldName);
    BinOp op = new BinOp(dest, source, TokenType.DOT, fieldOperand, null);
    ImmutableList<String> code = generateUncommentedCode(op);
    // It's R8 because the dest is the 3rd param.
    assertThat(code).contains("mov BYTE R8b, [RBX]");
  }

  @Test
  public void dotIntoTemp() {
    String fieldName = "fieldName";
    DeclarationNode fieldDecl1 = new DeclarationNode(fieldName + "0", VarType.LONG, null);
    DeclarationNode fieldDecl2 = new DeclarationNode(fieldName, VarType.BYTE, null);
    RecordSymbol recordDecl =
        symTab.declareRecord(
            new RecordDeclarationNode(
                "recordWithField", ImmutableList.of(fieldDecl1, fieldDecl2), null));
    Location source = LocationUtils.newMemoryAddress("source", recordDecl.varType());

    Location dest = LocationUtils.newTempLocation("dest", VarType.BYTE);
    Operand fieldOperand = ConstantOperand.of(fieldName);
    BinOp op = new BinOp(dest, source, TokenType.DOT, fieldOperand, null);
    ImmutableList<String> code = generateUncommentedCode(op);
    // It's RSI because it allocated "dest" to RSI
    assertThat(code).contains("mov BYTE SIL, [RBX]");
  }

  @Test
  public void dotIntoMemory() {
    String fieldName = "fieldName";
    DeclarationNode fieldDecl1 = new DeclarationNode(fieldName + "0", VarType.LONG, null);
    DeclarationNode fieldDecl2 = new DeclarationNode(fieldName, VarType.BYTE, null);
    RecordSymbol recordDecl =
        symTab.declareRecord(
            new RecordDeclarationNode(
                "recordWithField", ImmutableList.of(fieldDecl1, fieldDecl2), null));
    Location source = LocationUtils.newMemoryAddress("source", recordDecl.varType());

    Location dest = LocationUtils.newMemoryAddress("dest", VarType.BYTE);
    Operand fieldOperand = ConstantOperand.of(fieldName);
    BinOp op = new BinOp(dest, source, TokenType.DOT, fieldOperand, null);
    ImmutableList<String> code = generateUncommentedCode(op);
    // RSI is the indirect register
    assertThat(code).containsAtLeast("mov BYTE SIL, [RBX]", "mov BYTE [_dest], SIL").inOrder();
  }

  @Test
  public void dotIntoStack() {
    String fieldName = "fieldName";
    DeclarationNode fieldDecl1 = new DeclarationNode(fieldName + "0", VarType.LONG, null);
    DeclarationNode fieldDecl2 = new DeclarationNode(fieldName, VarType.BYTE, null);
    RecordSymbol recordDecl =
        symTab.declareRecord(
            new RecordDeclarationNode(
                "recordWithField", ImmutableList.of(fieldDecl1, fieldDecl2), null));
    Location source = LocationUtils.newMemoryAddress("source", recordDecl.varType());

    Location dest = LocationUtils.newStackLocation("dest", VarType.BYTE, 12);
    Operand fieldOperand = ConstantOperand.of(fieldName);
    BinOp op = new BinOp(dest, source, TokenType.DOT, fieldOperand, null);
    ImmutableList<String> code = generateUncommentedCode(op);
    assertThat(code).containsAtLeast("mov BYTE SIL, [RBX]", "mov BYTE [RBP - 12], SIL").inOrder();
  }

  @Test
  public void allocateNonGeneric() {
    Location dest = LocationUtils.newStackLocation("dest", nonGenericRecord.varType(), 12);
    AllocateOp op = new AllocateOp(dest, nonGenericRecord, null);
    ImmutableList<String> code = generateUncommentedCode(op);
    assertThat(code).hasSize(6);
    assertThat(code.get(5)).isEqualTo("mov [RBP - 12], RAX");
  }

  @Test
  public void allocateGeneric() {
    Location dest = LocationUtils.newStackLocation("dest", boundRecord.varType(), 12);
    AllocateOp op = new AllocateOp(dest, boundRecord, null);
    ImmutableList<String> code = generateUncommentedCode(op);
    assertThat(code).hasSize(6);
    assertThat(code).contains("mov EDX, 8");
    assertThat(code.get(5)).isEqualTo("mov [RBP - 12], RAX");
  }

  private ImmutableList<String> generateUncommentedCode(Op op) {
    op.accept(generator);
    System.err.printf("\nTEST CASE: %s\n\n", op);
    System.err.println(NEWLINE_JOINER.join(emitter.all()));
    return AsmUtils.trimComments(emitter.all());
  }
}
