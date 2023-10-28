package com.plasstech.lang.d2.codegen.x64;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Ignore;
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
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.parse.node.RecordDeclarationNode;
import com.plasstech.lang.d2.testing.TestUtils;
import com.plasstech.lang.d2.type.RecordReferenceType;
import com.plasstech.lang.d2.type.SymTab;
import com.plasstech.lang.d2.type.SymbolTable;
import com.plasstech.lang.d2.type.VarType;

public class RecordCodeGeneratorTest {
  private static final String RECORD_NAME = "rec";

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
  @Ignore
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

  private ImmutableList<String> generateUncommentedCode(BinOp op) {
    sut.visit(op);
    System.err.printf("\nTEST CASE: %s\n\n", op);
    System.err.println(NEWLINE_JOINER.join(emitter.all()));
    return TestUtils.trimComments(emitter.all());
  }
}
