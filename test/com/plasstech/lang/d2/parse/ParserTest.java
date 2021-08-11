package com.plasstech.lang.d2.parse;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.parse.node.ArrayDeclarationNode;
import com.plasstech.lang.d2.parse.node.AssignmentNode;
import com.plasstech.lang.d2.parse.node.BinOpNode;
import com.plasstech.lang.d2.parse.node.BlockNode;
import com.plasstech.lang.d2.parse.node.BreakNode;
import com.plasstech.lang.d2.parse.node.CallNode;
import com.plasstech.lang.d2.parse.node.ConstNode;
import com.plasstech.lang.d2.parse.node.ContinueNode;
import com.plasstech.lang.d2.parse.node.DeclarationNode;
import com.plasstech.lang.d2.parse.node.ExitNode;
import com.plasstech.lang.d2.parse.node.ExprNode;
import com.plasstech.lang.d2.parse.node.FieldSetNode;
import com.plasstech.lang.d2.parse.node.IfNode;
import com.plasstech.lang.d2.parse.node.InputNode;
import com.plasstech.lang.d2.parse.node.MainNode;
import com.plasstech.lang.d2.parse.node.NewNode;
import com.plasstech.lang.d2.parse.node.Node;
import com.plasstech.lang.d2.parse.node.PrintNode;
import com.plasstech.lang.d2.parse.node.ProcedureNode;
import com.plasstech.lang.d2.parse.node.ProgramNode;
import com.plasstech.lang.d2.parse.node.RecordDeclarationNode;
import com.plasstech.lang.d2.parse.node.ReturnNode;
import com.plasstech.lang.d2.parse.node.StatementNode;
import com.plasstech.lang.d2.parse.node.UnaryNode;
import com.plasstech.lang.d2.parse.node.VariableNode;
import com.plasstech.lang.d2.parse.node.VariableSetNode;
import com.plasstech.lang.d2.parse.node.WhileNode;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.type.ArrayType;
import com.plasstech.lang.d2.type.RecordReferenceType;
import com.plasstech.lang.d2.type.VarType;

@RunWith(TestParameterInjector.class)
public class ParserTest {
  @Test
  public void print() {
    BlockNode root = parseStatements("print 123");
    assertThat(root.statements()).hasSize(1);

    PrintNode node = (PrintNode) root.statements().get(0);
    assertThat(node.position().line()).isEqualTo(1);
    assertThat(node.position().column()).isEqualTo(1);
    assertThat(node.isPrintln()).isFalse();

    ExprNode expr = node.expr();
    ConstNode<Integer> intNode = (ConstNode<Integer>) expr;
    assertThat(intNode.value()).isEqualTo(123);
    assertThat(intNode.position().line()).isEqualTo(1);
    assertThat(intNode.position().column()).isEqualTo(7);
  }

  @Test
  public void println() {
    BlockNode root = parseStatements("println 123");
    assertThat(root.statements()).hasSize(1);

    PrintNode node = (PrintNode) root.statements().get(0);
    assertThat(node.isPrintln()).isTrue();
  }

  @Test
  public void printError() {
    assertParseError("print", "Unexpected 'EOF'");
  }

  @Test
  public void assignErrors() {
    assertParseError("a=", "expected literal");
    assertParseError("a=(3+", "expected literal");
    assertParseError("a=3+", "expected literal");
    assertParseError("a=3+5*", "expected literal");
    assertParseError("a=3+*5", "expected literal");
    assertParseError("a=3**5", "expected literal");
    assertParseError("a=print", "expected literal");
  }

  @Test
  public void assignInt() {
    BlockNode root = parseStatements("a=3");
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.variable();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    ConstNode<Integer> intNode = (ConstNode<Integer>) expr;
    assertThat(intNode.value()).isEqualTo(3);
  }

  @Test
  public void assignShiftLeft() {
    BlockNode root = parseStatements("a=b<<3");
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    ExprNode expr = node.expr();
    BinOpNode binOp = (BinOpNode) expr;
    VariableNode left = (VariableNode) binOp.left();
    assertThat(left.name()).isEqualTo("b");

    assertThat(binOp.operator()).isEqualTo(TokenType.SHIFT_LEFT);

    ConstNode<Integer> right = (ConstNode<Integer>) binOp.right();
    assertThat(right.value()).isEqualTo(3);
  }

  @Test
  public void assignBoolean() {
    BlockNode root = parseStatements("a=true b=FALSE");
    assertThat(root.statements()).hasSize(2);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.variable();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    assertThat(((ConstNode<Boolean>) expr).value()).isTrue();

    node = (AssignmentNode) root.statements().get(1);
    expr = node.expr();
    assertThat(((ConstNode<Boolean>) expr).value()).isFalse();
  }

  @Test
  public void assignAdd() {
    BlockNode root = parseStatements("a=3 + 4");
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.variable();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();

    BinOpNode binOp = (BinOpNode) expr;
    ConstNode<Integer> left = (ConstNode<Integer>) binOp.left();
    assertThat(left.value()).isEqualTo(3);

    assertThat(binOp.operator()).isEqualTo(TokenType.PLUS);

    ConstNode<Integer> right = (ConstNode<Integer>) binOp.right();
    assertThat(right.value()).isEqualTo(4);
  }

  @Test
  public void assignMult() {
    BlockNode root = parseStatements("a=3 * 4");
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.variable();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();

    BinOpNode binOp = (BinOpNode) expr;

    ConstNode<Integer> left = (ConstNode<Integer>) binOp.left();
    assertThat(left.value()).isEqualTo(3);

    assertThat(binOp.operator()).isEqualTo(TokenType.MULT);

    ConstNode<Integer> right = (ConstNode<Integer>) binOp.right();
    assertThat(right.value()).isEqualTo(4);
  }

  @Test
  public void assignAddChained() {
    parseStatements("a=3+4*b-5");
  }

  @Test
  public void assign() {
    BlockNode root = parseStatements("a=b");
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.variable();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    VariableNode atom = (VariableNode) expr;
    assertThat(atom.name()).isEqualTo("b");
  }

  @Test
  public void unaryMinus() {
    BlockNode root = parseStatements("a=-b");
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.variable();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    UnaryNode unary = (UnaryNode) expr;
    assertThat(unary.operator()).isEqualTo(TokenType.MINUS);
    VariableNode right = (VariableNode) unary.expr();
    assertThat(right.name()).isEqualTo("b");
  }

  @Test
  public void unaryMinusConstant() {
    BlockNode root = parseStatements("a=-3");
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.variable();
    assertThat(var.name()).isEqualTo("a");

    ConstNode<Integer> expr = (ConstNode<Integer>) node.expr();
    assertThat(expr.value()).isEqualTo(-3);
  }

  @Test
  public void unaryPlusConstant() {
    BlockNode root = parseStatements("a=+3");
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.variable();
    assertThat(var.name()).isEqualTo("a");

    ConstNode<Integer> expr = (ConstNode<Integer>) node.expr();
    assertThat(expr.value()).isEqualTo(3);
  }

  @Test
  public void unaryNotConstant() {
    BlockNode root = parseStatements("a=NOT true");
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.variable();
    assertThat(var.name()).isEqualTo("a");

    ConstNode<Boolean> constNode = (ConstNode<Boolean>) node.expr();
    assertThat(constNode.value()).isFalse();
  }

  @Test
  public void unaryNot() {
    BlockNode root = parseStatements("a=!b");
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.variable();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    assertThat(((UnaryNode) expr).operator()).isEqualTo(TokenType.BIT_NOT);
  }

  @Test
  public void unaryPlus() {
    BlockNode root = parseStatements("a=+b");
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.variable();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    UnaryNode unary = (UnaryNode) expr;
    assertThat(unary.operator()).isEqualTo(TokenType.PLUS);
    VariableNode right = (VariableNode) unary.expr();
    assertThat(right.name()).isEqualTo("b");
  }

  @Test
  public void unaryExpr() {
    BlockNode root = parseStatements("a=+(b+-c)");
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.variable();
    assertThat(var.name()).isEqualTo("a");

    Node expr = node.expr();
    UnaryNode unary = (UnaryNode) expr;
    assertThat(unary.operator()).isEqualTo(TokenType.PLUS);
  }

  @Test
  public void unaryNegativeInt() {
    assertUnaryAssignConstant("a=-5", -5);
    assertUnaryAssignConstant("a=-+5", -5);
    assertUnaryAssignConstant("a=+-5", -5);
    assertUnaryAssignConstant("a=---5", -5);
  }

  @Test
  public void multipleUnary() {
    assertUnaryAssignConstant("a=++5", 5);
    assertUnaryAssignConstant("a=+++5", 5);
    assertUnaryAssignConstant("a=--5", 5);
    assertUnaryAssignConstant("a=-+-5", 5);
    assertUnaryAssignConstant("a=--+5", 5);
  }

  private void assertUnaryAssignConstant(String expression, int value) {
    BlockNode root = parseStatements(expression);
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableNode var = (VariableNode) node.variable();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    assertThat(((ConstNode<Integer>) expr).value()).isEqualTo(value);
  }

  @Test
  public void unaryLength() {
    BlockNode root = parseStatements("a=length('hi')");
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.variable();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    UnaryNode unary = (UnaryNode) expr;
    assertThat(unary.operator()).isEqualTo(TokenType.LENGTH);
    ConstNode<String> right = (ConstNode<String>) unary.expr();
    assertThat(right.value()).isEqualTo("hi");
  }

  @Test
  public void unaryAsc() {
    BlockNode root = parseStatements("a=asc('hi')");
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.variable();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    UnaryNode unary = (UnaryNode) expr;
    assertThat(unary.operator()).isEqualTo(TokenType.ASC);
    ConstNode<?> right = (ConstNode<?>) unary.expr();
    assertThat(right.value()).isEqualTo("hi");
  }

  @Test
  public void unaryChr() {
    BlockNode root = parseStatements("a=chr(65)");
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.variable();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    UnaryNode unary = (UnaryNode) expr;
    assertThat(unary.operator()).isEqualTo(TokenType.CHR);
    ConstNode<?> right = (ConstNode<?>) unary.expr();
    assertThat(right.value()).isEqualTo(65);
  }

  @Test
  public void binOpOperator(
      @TestParameter({"+", "-", "*", "/", "%", "|", "&", "^"}) String operator) {
    parseStatements(String.format("a=b%s5", operator));
  }

  @Test
  public void binOpCompare(@TestParameter({">", "<", "==", "!=", "<=", ">="}) String operator) {
    parseStatements(String.format("a=b%s5", operator));
  }

  @Test
  public void allExprTypes_exceptAndOr() {
    // boolean a = ((1 + 2) * (3 - 4) / (-5) == 6) == true
    // || ((2 - 3) * (4 - 5) / (-6) == 7) == false && ((3 + 4) * (5 + 6) / (-7) >=
    // (8 % 2));
    BlockNode root = parseStatements("a=((1 + 2) * (3 - 4) / (-5) == 6) != true");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);
  }

  @Test
  public void allExprTypes() {
    BlockNode root =
        parseStatements(
            "a=((1 + 2) * (3 - 4) / (-5) == 6) != true\n"
                + " | ((2 - 3) * (4 - 5) / (-6) < 7) == !false & \n"
                + " ((3 + 4) * (5 + 6) / (-7) >= (8 % 2))"
                + "b=1+2*3-4/5==6!=true|2-3*4-5/-6<7==!a & 3+4*5+6/-7>=8%2");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(2);
  }

  @Test
  public void program() {
    BlockNode root = parseStatements("a=3 print a\n abc =   123 +a-b print 123\nprin=t");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(5);

    assertThat(statements.get(0)).isInstanceOf(AssignmentNode.class);
    assertThat(statements.get(1)).isInstanceOf(PrintNode.class);
    assertThat(statements.get(2)).isInstanceOf(AssignmentNode.class);
    assertThat(statements.get(3)).isInstanceOf(PrintNode.class);
    assertThat(statements.get(4)).isInstanceOf(AssignmentNode.class);
  }

  @Test
  public void assignParens() {
    BlockNode root = parseStatements("a=(3)");

    assertThat(root.statements()).hasSize(1);
    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.variable();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    ConstNode<Integer> intNode = (ConstNode<Integer>) expr;
    assertThat(intNode.value()).isEqualTo(3);
  }

  @Test
  public void parseIf() {
    BlockNode root = parseStatements("if a==3 { print a a=4 }");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    IfNode ifNode = (IfNode) statements.get(0);
    assertThat(ifNode.cases()).hasSize(1);
    IfNode.Case first = ifNode.cases().get(0);
    assertThat(first.block().statements()).hasSize(2);
  }

  @Test
  public void ifEmptyBlock() {
    BlockNode root = parseStatements("if a==3 { }");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    IfNode ifNode = (IfNode) statements.get(0);
    assertThat(ifNode.cases()).hasSize(1);
    assertThat(ifNode.elseBlock()).isNull();
    IfNode.Case first = ifNode.cases().get(0);
    assertThat(first.block().statements()).isEmpty();
  }

  @Test
  public void ifNested() {
    BlockNode root =
        parseStatements(
            "      if a==3 { "
                + "  if a==4 { "
                + "   if a == 5 {"
                + "     print a"
                + "   } "
                + "  }"
                + "}"
                + "else { print 4 print a}");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    IfNode ifNode = (IfNode) statements.get(0);
    assertThat(ifNode.cases()).hasSize(1);
    assertThat(ifNode.elseBlock().statements()).hasSize(2);
  }

  @Test
  public void ifElse() {
    BlockNode root = parseStatements("if a==3 { print a } else { print 4 print a}");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    IfNode ifNode = (IfNode) statements.get(0);
    assertThat(ifNode.cases()).hasSize(1);
    assertThat(ifNode.elseBlock().statements()).hasSize(2);
  }

  @Test
  public void ifElif() {
    BlockNode root =
        parseStatements(
            "      if a==3 { print a } "
                + "elif a==4 { print 4 print a} "
                + "elif a==5 { print 5}"
                + "else { print 6 print 7}");
    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    IfNode ifNode = (IfNode) statements.get(0);
    assertThat(ifNode.cases()).hasSize(3);
    assertThat(ifNode.elseBlock()).isNotNull();
  }

  @Test
  public void ifError() {
    assertParseError("if a==3 { print a } else print 4}", "expected {");
    assertParseError("if a==3 { print a } else {print 4", "Unexpected start of statement 'EOF'");
    assertParseError("if a==3 print a } else {print 4", "expected {");
    assertParseError("if print a else {print 4", "expected literal");
    assertParseError(
        "if a==3 { print a } else  { print 4 print a} "
            + "elif a==5 { print 5}else { print 6 print 7}",
        "Unexpected start of statement 'ELIF'");
  }

  @Test
  public void whileTrue() {
    BlockNode root = parseStatements("while true {}");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    WhileNode whileNode = (WhileNode) statements.get(0);
    assertThat(whileNode.doStatement().isPresent()).isFalse();
    ExprNode condition = whileNode.condition();
    assertThat(((ConstNode<Boolean>) condition).value()).isTrue();
    BlockNode block = whileNode.block();
    assertThat(block.statements()).isEmpty();
  }

  @Test
  public void whileDo() {
    BlockNode root = parseStatements("while true do i = 1 {}");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    WhileNode whileNode = (WhileNode) statements.get(0);

    AssignmentNode assignment = (AssignmentNode) whileNode.doStatement().get();
    VariableSetNode var = (VariableSetNode) assignment.variable();
    assertThat(var.name()).isEqualTo("i");

    ExprNode expr = assignment.expr();
    ConstNode<Integer> intNode = (ConstNode<Integer>) expr;
    assertThat(intNode.value()).isEqualTo(1);
  }

  @Test
  public void whileExprDo() {
    BlockNode root = parseStatements("while i < 30 do i = 1 {}");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    WhileNode whileNode = (WhileNode) statements.get(0);

    ExprNode condition = whileNode.condition();

    BinOpNode binOp = (BinOpNode) condition;
    VariableNode left = (VariableNode) binOp.left();
    assertThat(left.name()).isEqualTo("i");
    assertThat(binOp.operator()).isEqualTo(TokenType.LT);
    ConstNode<Integer> right = (ConstNode<Integer>) binOp.right();
    assertThat(right.value()).isEqualTo(30);
  }

  @Test
  public void whileExprDoBlock() {
    BlockNode root = parseStatements("while i < 30 do i = 1 {print i a=i}");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    WhileNode whileNode = (WhileNode) statements.get(0);

    BlockNode block = whileNode.block();
    assertThat(block.statements()).hasSize(2);
  }

  @Test
  public void whileBreak() {
    BlockNode root = parseStatements("while true {break continue}");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    WhileNode whileNode = (WhileNode) statements.get(0);

    BlockNode block = whileNode.block();
    assertThat(block.statements()).hasSize(2);
    assertThat(block.statements().get(0)).isInstanceOf(BreakNode.class);
    assertThat(block.statements().get(1)).isInstanceOf(ContinueNode.class);
  }

  @Test
  public void whileDoNotAssignment() {
    parseStatements("while true do advance(3) {}");
  }

  @Test
  public void whileError() {
    assertParseError("while print", "expected literal");
    assertParseError("while a==3 print", "expected {");
    assertParseError("while a==3 {print", "expected literal");
    assertParseError("while a==3 do {print}", "Unexpected start of statement '{'");
    assertParseError("while a==3 do print {print}", "expected literal");
    assertParseError("while a==3 do a=a+1 {a=}", "expected literal");
    assertParseError("continue", "CONTINUE not found in WHILE");
    assertParseError("break", "BREAK not found in WHILE");
    assertParseError("if true {break while true {continue }}", "BREAK not found in WHILE");
    assertParseError("if true {continue while true {break}}", "CONTINUE not found in WHILE");
  }

  @Test
  public void mainEmpty() {
    ProgramNode root = parseProgram("main{}");
    assertThat(root.main().isPresent()).isTrue();
  }

  @Test
  public void statementThenMainEmpty() {
    ProgramNode root = parseProgram("print 123 main{}");
    BlockNode block = root.statements();
    assertThat(block.statements()).hasSize(1);

    PrintNode node = (PrintNode) block.statements().get(0);
    assertThat(node.position().line()).isEqualTo(1);
    assertThat(node.position().column()).isEqualTo(1);

    ExprNode expr = node.expr();
    ConstNode<Integer> intNode = (ConstNode<Integer>) expr;
    assertThat(intNode.value()).isEqualTo(123);
    assertThat(intNode.position().line()).isEqualTo(1);
    assertThat(intNode.position().column()).isEqualTo(7);
    assertThat(root.main().isPresent()).isTrue();
  }

  @Test
  public void mainWithStatement() {
    ProgramNode root = parseProgram("main{ print 123 }");
    BlockNode globalBlock = root.statements();
    assertThat(globalBlock.statements()).isEmpty();
    MainNode main = root.main().get();
    BlockNode block = main.block();

    PrintNode node = (PrintNode) block.statements().get(0);

    ExprNode expr = node.expr();
    ConstNode<Integer> intNode = (ConstNode<Integer>) expr;
    assertThat(intNode.value()).isEqualTo(123);
  }

  @Test
  public void mainError() {
    assertParseError("main", "expected {");
    assertParseError("main {", "Unexpected start of statement 'EOF'");
    assertParseError("main {} print ", "expected EOF");
    assertParseError("main { print ", "expected literal");
    assertParseError("main: proc() {}", "expected {");
  }

  @Test
  public void decl() {
    BlockNode root = parseStatements("a:int");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    DeclarationNode declarationNode = (DeclarationNode) statements.get(0);
    assertThat(declarationNode.name()).isEqualTo("a");
    assertThat(declarationNode.varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void declBool() {
    BlockNode root = parseStatements("a:bool");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    DeclarationNode declarationNode = (DeclarationNode) statements.get(0);
    assertThat(declarationNode.name()).isEqualTo("a");
    assertThat(declarationNode.varType()).isEqualTo(VarType.BOOL);
  }

  @Test
  public void declString() {
    BlockNode root = parseStatements("a:string");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    DeclarationNode declarationNode = (DeclarationNode) statements.get(0);
    assertThat(declarationNode.name()).isEqualTo("a");
    assertThat(declarationNode.varType()).isEqualTo(VarType.STRING);
  }

  @Test
  public void declarationError() {
    assertParseError("a:", "expected INT, BOOL");
    assertParseError("a::", "expected INT, BOOL");
    assertParseError("a:print", "expected INT, BOOL");
  }

  @Test
  public void assignString() {
    BlockNode root = parseStatements("a='hi'");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.variable();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    assertThat(expr.varType()).isEqualTo(VarType.STRING);
  }

  @Test
  public void addStrings() {
    BlockNode root = parseStatements("a='hi' + 'Hi'");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.variable();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    assertThat(expr).isInstanceOf(BinOpNode.class);
  }

  @Test
  public void procErrors() {
    assertParseError("fib:proc(a:int b) {}", "expected )");
    assertParseError("fib:proc(a:proc, b) {}", "expected INT");
    assertParseError("fib:proc(a:, b) {}", "expected INT");
    assertParseError("fib:proc(a:) {}", "expected INT");
    assertParseError("fib:proc(a {}", "expected )");
    assertParseError("fib:proc(a:int, ) {}", "expected VARIABLE");
    assertParseError("fib:proc(a:int) print a", "expected {");
    assertParseError("fib:proc  print a", "expected {");
    assertParseError("fib:proc() {return", "Unexpected start of statement 'EOF'");
    assertParseError("fib:proc() {return {", "Unexpected start of statement '{'");
    assertParseError("fib:proc() {return )}", "Unexpected start of statement ')'");
  }

  @Test
  public void simpleProc() {
    // the simplest possible procedure
    ProgramNode root = parseProgram("fib:proc {}");
    ProcedureNode proc = (ProcedureNode) (root.statements().statements().get(0));
    assertThat(proc.name()).isEqualTo("fib");
    assertThat(proc.returnType()).isEqualTo(VarType.VOID);
  }

  @Test
  public void procWithParam() {
    ProgramNode root = parseProgram("fib:proc(param1) {}");
    ProcedureNode proc = (ProcedureNode) (root.statements().statements().get(0));
    assertThat(proc.name()).isEqualTo("fib");
    assertThat(proc.returnType()).isEqualTo(VarType.VOID);
    assertThat(proc.parameters()).hasSize(1);
    assertThat(proc.parameters().get(0).name()).isEqualTo("param1");
    assertThat(proc.parameters().get(0).type().isUnknown()).isTrue();
  }

  @Test
  public void procWith2Params() {
    ProgramNode root = parseProgram("fib:proc(param1, param2: string) {}");
    ProcedureNode proc = (ProcedureNode) (root.statements().statements().get(0));
    assertThat(proc.name()).isEqualTo("fib");
    assertThat(proc.returnType()).isEqualTo(VarType.VOID);
    assertThat(proc.parameters()).hasSize(2);
    assertThat(proc.parameters().get(0).name()).isEqualTo("param1");
    assertThat(proc.parameters().get(0).type().isUnknown()).isTrue();
    assertThat(proc.parameters().get(1).name()).isEqualTo("param2");
    assertThat(proc.parameters().get(1).type()).isEqualTo(VarType.STRING);
  }

  @Test
  public void procWithLocals() {
    ProgramNode root = parseProgram("fib:proc() {local:int}");
    ProcedureNode proc = (ProcedureNode) (root.statements().statements().get(0));
    assertThat(proc.name()).isEqualTo("fib");
    assertThat(proc.returnType()).isEqualTo(VarType.VOID);
    assertThat(proc.block().statements().get(0)).isInstanceOf(DeclarationNode.class);
  }

  @Test
  public void fullProc() {
    ProgramNode root =
        parseProgram(
            "      fib:proc(typed:int, nontyped) : string {"
                + "  typed = typed + 1"
                + "  nontyped = typed + 1"
                + "  return 'hi'"
                + "}");

    ProcedureNode proc = (ProcedureNode) (root.statements().statements().get(0));
    assertThat(proc.name()).isEqualTo("fib");
    assertThat(proc.returnType()).isEqualTo(VarType.STRING);
    assertThat(proc.parameters()).hasSize(2);
    assertThat(proc.block().statements()).hasSize(3);
  }

  @Test
  public void procReturnVoid() {
    ProgramNode root = parseProgram("fib:proc() {return}");

    ProcedureNode proc = (ProcedureNode) (root.statements().statements().get(0));
    assertThat(proc.name()).isEqualTo("fib");
    assertThat(proc.returnType()).isEqualTo(VarType.VOID);
    assertThat(proc.block().statements()).hasSize(1);
    ReturnNode returnNode = (ReturnNode) proc.block().statements().get(0);
    assertThat(returnNode.expr().isPresent()).isFalse();

    // This is allowed, but the static checker will eventually prevent it
    parseProgram("fib:proc() {return print 'hi'}");
  }

  @Test
  public void procCallNoArgs() {
    ProgramNode root = parseProgram("a = doit()");
    AssignmentNode assignment = (AssignmentNode) (root.statements().statements().get(0));
    ExprNode expr = assignment.expr();
    assertThat(expr).isInstanceOf(CallNode.class);
  }

  @Test
  public void procCallAsStatement() {
    ProgramNode root = parseProgram("doit(3)");
    CallNode call = (CallNode) (root.statements().statements().get(0));

    assertThat(call.functionToCall()).isEqualTo("doit");
    assertThat(call.actuals()).hasSize(1);
    ExprNode param = call.actuals().get(0);
    assertThat(param.isSimpleType()).isTrue();
  }

  @Test
  public void procCallAsExpression() {
    ProgramNode root = parseProgram("a = doit((3*6*(3-4)*(5-5)), (abc==doit()))");

    AssignmentNode assignment = (AssignmentNode) (root.statements().statements().get(0));
    ExprNode expr = assignment.expr();
    assertThat(expr).isInstanceOf(CallNode.class);
  }

  @Test
  public void procCallOneArgs() {
    ProgramNode root = parseProgram("a = doit(1)");

    AssignmentNode assignment = (AssignmentNode) (root.statements().statements().get(0));
    ExprNode expr = assignment.expr();
    assertThat(expr).isInstanceOf(CallNode.class);
  }

  @Test
  public void procCallErrors() {
    assertParseError("a = doit(1 b=3", "expected )");
    assertParseError("a = doit(1,)", "expected literal");
  }

  @Test
  public void procCallMultipleArgs() {
    ProgramNode root = parseProgram("a = doit(1, 2, 3)");
    AssignmentNode assignment = (AssignmentNode) (root.statements().statements().get(0));
    ExprNode expr = assignment.expr();
    assertThat(expr).isInstanceOf(CallNode.class);
  }

  @Test
  public void procCallNested() {
    ProgramNode root = parseProgram("a = doit3(1, doit1(2), doit2(3, 4))");
    AssignmentNode assignment = (AssignmentNode) (root.statements().statements().get(0));
    ExprNode expr = assignment.expr();
    assertThat(expr).isInstanceOf(CallNode.class);
  }

  @Test
  public void arrayGet() {
    BlockNode root = parseStatements("a=b[3+c]");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.variable();
    assertThat(var.name()).isEqualTo("a");

    BinOpNode expr = (BinOpNode) node.expr();
    assertThat(expr.left()).isInstanceOf(VariableNode.class);
    assertThat(expr.operator()).isEqualTo(TokenType.LBRACKET);
    assertThat(expr.right()).isInstanceOf(BinOpNode.class);
  }

  @Test
  public void arrayGetWeirdYetParseable() {
    parseStatements("a=3[4+c]"); // will fail type checker
    parseStatements("a='hi'['lol']"); // will fail type checker
    parseStatements("a=fn()[4+c]");
  }

  @Test
  public void arrayDecl() {
    BlockNode root = parseStatements("a:int[3]");
    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    ArrayDeclarationNode node = (ArrayDeclarationNode) statements.get(0);
    assertThat(node.name()).isEqualTo("a");
    assertThat(node.varType()).isInstanceOf(ArrayType.class);
    assertThat(node.sizeExpr()).isInstanceOf(ConstNode.class);

    ArrayType arrType = (ArrayType) node.varType();
    assertThat(arrType.baseType()).isEqualTo(VarType.INT);
  }

  @Test
  public void arrayDeclVariableSize() {
    BlockNode root = parseStatements("a:int[b+3]");
    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    ArrayDeclarationNode node = (ArrayDeclarationNode) statements.get(0);
    assertThat(node.name()).isEqualTo("a");
    assertThat(node.varType()).isInstanceOf(ArrayType.class);
    assertThat(node.sizeExpr()).isInstanceOf(BinOpNode.class);

    ArrayType arrType = (ArrayType) node.varType();
    assertThat(arrType.baseType()).isEqualTo(VarType.INT);
  }

  @Test
  public void arrayDeclError() {
    assertParseError("a:int[3", "expected ]");
    assertParseError("a:int[3 4]", "expected ]");
    // This is now a record reference declaration so the bracket is an error.
    assertParseError("a:hello[3]", "expected start of statement");
  }

  @Test
  @Ignore("Issue #38: Support multidimensional arrays")
  public void multiDimArrayGet() {
    BlockNode root = parseStatements("a=b[3+c][4][5]");
    System.out.println(root);
  }

  @Test
  @Ignore("Assignments are still broken")
  public void arraySet() {
    parseStatements("a[3] = 4");
  }

  @Test
  @Ignore
  public void arrayStmt() {
    parseStatements("fn()[fn()]");
  }

  @Test
  public void arrayGetError() {
    assertParseError("a=3[4+c b", "expected ]");
    assertParseError("a=3[4+c b]", "expected ]");
  }

  @Test
  public void literalIntArray() {
    BlockNode root = parseStatements("a=[1,2,3]");
    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.variable();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    @SuppressWarnings("unchecked")
    ConstNode<Integer[]> array = (ConstNode<Integer[]>) expr;
    assertThat(array.value()[0]).isEqualTo(1);
  }

  @Test
  public void literalStringArray() {
    BlockNode root = parseStatements("a=['one', 'two', 'three']");
    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.variable();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    @SuppressWarnings("unchecked")
    ConstNode<String[]> array = (ConstNode<String[]>) expr;
    assertThat(array.value()[0]).isEqualTo("one");
  }

  @Test
  public void literalBoolArray() {
    BlockNode root = parseStatements("a=[true, false]");
    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.variable();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    @SuppressWarnings("unchecked")
    ConstNode<Boolean[]> array = (ConstNode<Boolean[]>) expr;
    assertThat(array.value()[0]).isTrue();
    assertThat(array.value()[1]).isFalse();
  }

  @Test
  public void literalArrayIndex() {
    BlockNode root = parseStatements("a=[true, false][1]");
    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.variable();
    assertThat(var.name()).isEqualTo("a");

    BinOpNode expr = (BinOpNode) node.expr();
    assertThat(expr.left()).isInstanceOf(ConstNode.class);
    assertThat(expr.right()).isInstanceOf(ConstNode.class);
  }

  @Test
  public void literalArrayErrors() {
    assertParseError("a=[]", "Empty");
    assertParseError("a=[a]", "only scalar");
    assertParseError("a=[a+1]", "only scalar");
    assertParseError("a=[1,'hi']", "Inconsistent types");
  }

  @Test
  public void exit() {
    BlockNode root = parseStatements("exit");
    ExitNode node = (ExitNode) root.statements().get(0);
    assertThat(node.exitMessage().isPresent()).isFalse();
  }

  @Test
  public void exit_withMessage() {
    BlockNode root = parseStatements("exit 'sorry/not sorry'");
    ExitNode node = (ExitNode) root.statements().get(0);
    ConstNode<String> message = (ConstNode<String>) node.exitMessage().get();
    assertThat(message.value()).isEqualTo("sorry/not sorry");
  }

  @Test
  public void input() {
    BlockNode root = parseStatements("f=input");
    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.variable();
    assertThat(var.name()).isEqualTo("f");

    ExprNode expr = node.expr();
    assertThat(expr).isInstanceOf(InputNode.class);
  }

  @Test
  public void input_fail() {
    assertParseError("input(f)", "Unexpected start of statement 'INPUT'");
    assertParseError("f=input()", "Unexpected start of statement '('");
  }

  @Test
  public void declRecord_empty() {
    BlockNode root = parseStatements("r: record{}");
    RecordDeclarationNode node = (RecordDeclarationNode) root.statements().get(0);
    assertThat(node.name()).isEqualTo("r");
    assertThat(node.fields()).isEmpty();
  }

  @Test
  public void declRecord() {
    BlockNode root = parseStatements("R: record{i: int s: string}");
    RecordDeclarationNode node = (RecordDeclarationNode) root.statements().get(0);
    assertThat(node.name()).isEqualTo("R");
    assertThat(node.fields()).hasSize(2);
    assertThat(node.fields().get(0).varType()).isEqualTo(VarType.INT);
    assertThat(node.fields().get(0).name()).isEqualTo("i");
    assertThat(node.fields().get(1).varType()).isEqualTo(VarType.STRING);
    assertThat(node.fields().get(1).name()).isEqualTo("s");
  }

  @Test
  public void declRecordRecursive() {
    BlockNode root = parseStatements("R: record {r: R}");
    RecordDeclarationNode node = (RecordDeclarationNode) root.statements().get(0);
    assertThat(node.name()).isEqualTo("R");
    assertThat(node.fields()).hasSize(1);
    assertThat(node.fields().get(0).name()).isEqualTo("r");
    assertThat(node.fields().get(0).varType()).isInstanceOf(RecordReferenceType.class);
    RecordReferenceType fieldType = (RecordReferenceType) node.fields().get(0).varType();
    assertThat(fieldType.name()).isEqualTo("R");
  }

  @Test
  public void declRecord_badField() {
    assertParseError("r: record{p:int int}", "expected VARIABLE");
    assertParseError("r: record{int}", "expected VARIABLE");
    assertParseError("r: record{proc}", "expected VARIABLE");
    // the error here is actually that it's trying to parse a procedure, but meh
    assertParseError("r: record{p:proc}", "expected");
    // not parse errors, but are type errors
    // assertParseError("r: record{p:proc{}}", "expected");-
    // assertParseError("r: record{r2:record{}}", "expected VARIABLE");
  }

  @Test
  public void declVar_asRecord() {
    BlockNode root = parseStatements("a: R");
    DeclarationNode node = (DeclarationNode) root.statements().get(0);
    assertThat(node.name()).isEqualTo("a");
    RecordReferenceType type = (RecordReferenceType) node.varType();
    assertThat(type.name()).isEqualTo("R");
  }

  @Test
  public void recordAsFormalParam() {
    BlockNode root = parseStatements("p:proc(a: R) {}");
    ProcedureNode proc = (ProcedureNode) root.statements().get(0);
    ProcedureNode.Parameter param = proc.parameters().get(0);
    assertThat(param.name()).isEqualTo("a");
    RecordReferenceType type = (RecordReferenceType) param.type();
    assertThat(type.name()).isEqualTo("R");
  }

  @Test
  public void recordAsReturnType() {
    BlockNode root = parseStatements("p:proc():R {}");
    ProcedureNode proc = (ProcedureNode) root.statements().get(0);
    RecordReferenceType type = (RecordReferenceType) proc.returnType();
    assertThat(type.name()).isEqualTo("R");
  }

  @Test
  public void newRecord() {
    BlockNode root = parseStatements("R: record{i: int} rec = new R");
    AssignmentNode assignment = (AssignmentNode) root.statements().get(1);
    NewNode node = (NewNode) assignment.expr();
    assertThat(node.recordName()).isEqualTo("R");
    RecordReferenceType type = (RecordReferenceType) node.varType();
    assertThat(type.name()).isEqualTo("R");
  }

  @Test
  public void newRecord_returnValue() {
    parseStatements("r1:record{i:int} p:proc():r1{return new r1} var1=p()");
  }

  @Test
  public void new_asOperand() {
    assertParseError("R: record{i: int} rec = new R.i", "Unexpected start of statement '.'");
  }

  @Test
  public void newRecord_Error() {
    assertParseError("R: record{i: int s: string} rec = new new", "expected VARIABLE");
  }

  @Test
  public void recordGet() {
    BlockNode root = parseStatements("i=rec.i");
    AssignmentNode assignment = (AssignmentNode) root.statements().get(0);
    BinOpNode node = (BinOpNode) assignment.expr();
    assertThat(node.left()).isInstanceOf(VariableNode.class);
    assertThat(node.operator()).isEqualTo(TokenType.DOT);
    assertThat(node.right()).isInstanceOf(VariableNode.class);
  }

  @Test
  public void ifRecordGet() {
    parseStatements("if rec.i ==0 {print rec.i}");
  }

  @Test
  public void recordGetRecursive() {
    BlockNode root = parseStatements("s=rec.s[(a+b)] s=rec.f1.f2[3] ");
    System.err.println(root);
    AssignmentNode assignment = (AssignmentNode) root.statements().get(0);
    BinOpNode node = (BinOpNode) assignment.expr();
    assertThat(node.left()).isInstanceOf(BinOpNode.class);
    assertThat(node.operator()).isEqualTo(TokenType.LBRACKET);
    assertThat(node.right()).isInstanceOf(BinOpNode.class);
  }

  @Test
  public void recordGetError() {
    assertParseError("i = rec.[\n", "expected literal");
  }

  @SuppressWarnings("unchecked")
  @Test
  public void recordSet() {
    BlockNode root = parseStatements("rec.i = 3 rec.s = 'hi'");
    AssignmentNode assignment = (AssignmentNode) root.statements().get(0);
    FieldSetNode lvalue = (FieldSetNode) assignment.variable();
    assertThat(lvalue.variableName()).isEqualTo("rec");
    assertThat(lvalue.fieldName()).isEqualTo("i");
    assertThat(((ConstNode<Integer>) assignment.expr()).value()).isEqualTo(3);

    assignment = (AssignmentNode) root.statements().get(1);
    lvalue = (FieldSetNode) assignment.variable();
    assertThat(lvalue.variableName()).isEqualTo("rec");
    assertThat(lvalue.fieldName()).isEqualTo("s");
    assertThat(((ConstNode<String>) assignment.expr()).value()).isEqualTo("hi");
  }

  @Test
  public void recordSetError() {
    assertParseError("rec.3 = i\n", "expected VARIABLE");
  }

  @Test
  public void unsetRecordCompareToNull() {
    BlockNode root =
        parseStatements(
            "R: record{i: int s: string}\n" //
                + " rec: R\n"
                + " isNull = rec == null");
    AssignmentNode assignment = (AssignmentNode) root.statements().get(2);
    BinOpNode node = (BinOpNode) assignment.expr();
    assertThat(node.left()).isInstanceOf(VariableNode.class);
    assertThat(node.operator()).isEqualTo(TokenType.EQEQ);
    ConstNode<Void> right = (ConstNode<Void>) node.right();
    assertThat(right.value()).isNull();
  }

  private BlockNode parseStatements(String expression) {
    ProgramNode node = parseProgram(expression);
    return node.statements();
  }

  private ProgramNode parseProgram(String expression) {
    Lexer lexer = new Lexer(expression);
    Parser parser = new Parser(lexer);
    State output = parser.execute(State.create());
    if (output.error()) {
      fail(output.errorMessage());
    }
    return output.programNode();
  }

  private void assertParseError(String expressionToParse, String errorMsgContains) {
    Lexer lexer = new Lexer(expressionToParse);
    Parser parser = new Parser(lexer);
    State output = parser.execute(State.create());
    assertWithMessage("Should be an error node").that(output.error()).isTrue();
    assertThat(output.errorMessage()).contains(errorMsgContains);
  }
}
