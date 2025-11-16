package com.plasstech.lang.d2.parse;

import static com.google.common.truth.Truth.assertThat;
import static com.plasstech.lang.d2.parse.testing.ParserSubject.assertThatParsing;
import static com.plasstech.lang.d2.type.testing.VarTypeSubject.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.parse.node.ArrayDeclarationNode;
import com.plasstech.lang.d2.parse.node.ArrayLiteralNode;
import com.plasstech.lang.d2.parse.node.ArraySetNode;
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
import com.plasstech.lang.d2.parse.node.ExternProcedureNode;
import com.plasstech.lang.d2.parse.node.FieldSetNode;
import com.plasstech.lang.d2.parse.node.IfNode;
import com.plasstech.lang.d2.parse.node.IncDecNode;
import com.plasstech.lang.d2.parse.node.InputNode;
import com.plasstech.lang.d2.parse.node.LValueNode;
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
import com.plasstech.lang.d2.type.RecordReferenceType;
import com.plasstech.lang.d2.type.UnboundType;
import com.plasstech.lang.d2.type.VarType;
import java.util.List;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(TestParameterInjector.class)
public class ParserTest {
  @Test
  public void print() {
    ProgramNode programNode = assertThatParsing("print 123").succeeds();
    BlockNode root = programNode.statements();
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
    ProgramNode programNode = assertThatParsing("println 123").succeeds();
    BlockNode root = programNode.statements();
    assertThat(root.statements()).hasSize(1);

    PrintNode node = (PrintNode) root.statements().get(0);
    assertThat(node.isPrintln()).isTrue();
  }

  @Test
  public void printLong() {
    ProgramNode programNode = assertThatParsing("print 123L").succeeds();
    BlockNode root = programNode.statements();

    PrintNode node = (PrintNode) root.statements().get(0);
    ExprNode expr = node.expr();
    ConstNode<Long> intNode = (ConstNode<Long>) expr;
    assertThat(intNode.value()).isEqualTo(123L);
    assertThat(intNode.position().line()).isEqualTo(1);
    assertThat(intNode.position().column()).isEqualTo(7);
  }

  @Test
  public void printError() {
    assertThatParsing("print").hasError("Unexpected 'EOF'");
  }

  @Test
  public void assignErrors() {
    assertThatParsing("a=").hasError("expected literal");
    assertThatParsing("a=(3+").hasError("expected literal");
    assertThatParsing("a=3+").hasError("expected literal");
    assertThatParsing("a=3+5*").hasError("expected literal");
    assertThatParsing("a=3+*5").hasError("expected literal");
    assertThatParsing("a=3**5").hasError("expected literal");
    assertThatParsing("a=print").hasError("expected literal");
  }

  @Test
  public void invalidVariableName() {
    assertThatParsing("_hi=3").hasError("Illegal variable name _hi");
  }

  @Test
  public void assignInt() {
    ProgramNode programNode = assertThatParsing("a=3").succeeds();
    BlockNode root = programNode.statements();
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.lvalue();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    ConstNode<Integer> intNode = (ConstNode<Integer>) expr;
    assertThat(intNode.value()).isEqualTo(3);
  }

  @Test
  public void assignShiftLeft() {
    ProgramNode programNode = assertThatParsing("a=b<<3").succeeds();
    BlockNode root = programNode.statements();
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
    ProgramNode programNode = assertThatParsing("a=true b=FALSE").succeeds();
    BlockNode root = programNode.statements();
    assertThat(root.statements()).hasSize(2);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.lvalue();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    assertThat(((ConstNode<Boolean>) expr).value()).isTrue();

    node = (AssignmentNode) root.statements().get(1);
    expr = node.expr();
    assertThat(((ConstNode<Boolean>) expr).value()).isFalse();
  }

  @Test
  public void assignAdd() {
    ProgramNode programNode = assertThatParsing("a=3 + 4").succeeds();
    BlockNode root = programNode.statements();
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.lvalue();
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
    ProgramNode programNode = assertThatParsing("a=3 * 4").succeeds();
    BlockNode root = programNode.statements();
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.lvalue();
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
    assertThatParsing("a=3+4*b-5").succeeds();
  }

  @Test
  public void assign() {
    ProgramNode programNode = assertThatParsing("a=b").succeeds();
    BlockNode root = programNode.statements();
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.lvalue();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    VariableNode atom = (VariableNode) expr;
    assertThat(atom.name()).isEqualTo("b");
  }

  @Test
  public void unaryMinus() {
    ProgramNode programNode = assertThatParsing("a=-b").succeeds();
    BlockNode root = programNode.statements();
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.lvalue();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    UnaryNode unary = (UnaryNode) expr;
    assertThat(unary.operator()).isEqualTo(TokenType.MINUS);
    VariableNode right = (VariableNode) unary.expr();
    assertThat(right.name()).isEqualTo("b");
  }

  @Test
  public void unaryMinusConstant() {
    ProgramNode programNode = assertThatParsing("a=-3").succeeds();
    BlockNode root = programNode.statements();
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.lvalue();
    assertThat(var.name()).isEqualTo("a");

    ConstNode<Integer> expr = (ConstNode<Integer>) node.expr();
    assertThat(expr.value()).isEqualTo(-3);
  }

  @Test
  public void unaryPlusConstant() {
    ProgramNode programNode = assertThatParsing("a=+3").succeeds();
    BlockNode root = programNode.statements();
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.lvalue();
    assertThat(var.name()).isEqualTo("a");

    ConstNode<Integer> expr = (ConstNode<Integer>) node.expr();
    assertThat(expr.value()).isEqualTo(3);
  }

  @Test
  public void unaryNotConstant() {
    ProgramNode programNode = assertThatParsing("a=NOT true").succeeds();
    BlockNode root = programNode.statements();
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.lvalue();
    assertThat(var.name()).isEqualTo("a");

    ConstNode<Boolean> constNode = (ConstNode<Boolean>) node.expr();
    assertThat(constNode.value()).isFalse();
  }

  @Test
  public void unaryBoolNotSwaps() {
    ProgramNode programNode = assertThatParsing("a=NOT (a==b)").succeeds();
    BlockNode root = programNode.statements();
    AssignmentNode node = (AssignmentNode) root.statements().get(0);
    BinOpNode binOpNode = (BinOpNode) node.expr();
    assertThat(binOpNode.operator()).isEqualTo(TokenType.NEQ);
    ProgramNode node2 = assertThatParsing("a=NOT (a!=b)").succeeds();

    root = node2.statements();
    node = (AssignmentNode) root.statements().get(0);
    binOpNode = (BinOpNode) node.expr();
    assertThat(binOpNode.operator()).isEqualTo(TokenType.EQEQ);
    ProgramNode node3 = assertThatParsing("a=NOT (a>b)").succeeds();

    root = node3.statements();
    node = (AssignmentNode) root.statements().get(0);
    binOpNode = (BinOpNode) node.expr();
    assertThat(binOpNode.operator()).isEqualTo(TokenType.LEQ);
    ProgramNode node4 = assertThatParsing("a=NOT (a<=b)").succeeds();

    root = node4.statements();
    node = (AssignmentNode) root.statements().get(0);
    binOpNode = (BinOpNode) node.expr();
    assertThat(binOpNode.operator()).isEqualTo(TokenType.GT);
  }

  @Test
  public void unaryBoolNotNot() {
    ProgramNode programNode = assertThatParsing("a=NOT NOT (a==b)").succeeds();
    BlockNode root = programNode.statements();
    AssignmentNode node = (AssignmentNode) root.statements().get(0);
    BinOpNode binOpNode = (BinOpNode) node.expr();
    assertThat(binOpNode.operator()).isEqualTo(TokenType.EQEQ);
  }

  @Test
  public void unaryBitNot() {
    ProgramNode programNode = assertThatParsing("a=!b").succeeds();
    BlockNode root = programNode.statements();
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.lvalue();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    assertThat(((UnaryNode) expr).operator()).isEqualTo(TokenType.BIT_NOT);
  }

  @Test
  public void unaryBitNotNot() {
    ProgramNode programNode = assertThatParsing("a=!!b").succeeds();
    BlockNode root = programNode.statements();
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.lvalue();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    assertThat(expr).isInstanceOf(VariableNode.class);
    assertThat(((VariableNode) expr).name()).isEqualTo("b");
  }

  @Test
  public void unaryPlus() {
    ProgramNode programNode = assertThatParsing("a=+b").succeeds();
    BlockNode root = programNode.statements();
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.lvalue();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    VariableNode unary = (VariableNode) expr;
    assertThat(unary.name()).isEqualTo("b");
  }

  @Test
  public void unaryExpr() {
    ProgramNode programNode = assertThatParsing("a=+(b+-c)").succeeds();
    BlockNode root = programNode.statements();
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.lvalue();
    assertThat(var.name()).isEqualTo("a");

    Node expr = node.expr();
    BinOpNode unary = (BinOpNode) expr;
    assertThat(unary.operator()).isEqualTo(TokenType.PLUS);
  }

  @Test
  public void unaryNegativeInt() {
    assertUnaryAssignConstant("a=-5", -5);
  }

  @Test
  public void multipleUnary() {
    assertUnaryAssignConstant("a=+-5", -5);
    assertUnaryAssignConstant("a=-+5", -5);
    assertUnaryAssignConstant("a=-+-5", 5);
  }

  private BlockNode assertUnaryAssignConstant(String expression, int value) {
    ProgramNode programNode = assertThatParsing(expression).succeeds();
    BlockNode root = programNode.statements();
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableNode var = (VariableNode) node.lvalue();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    assertThat(((ConstNode<Integer>) expr).value()).isEqualTo(value);
    return root;
  }

  @Test
  public void unaryLength() {
    ProgramNode programNode = assertThatParsing("a=length('hi')").succeeds();
    BlockNode root = programNode.statements();
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.lvalue();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    UnaryNode unary = (UnaryNode) expr;
    assertThat(unary.operator()).isEqualTo(TokenType.LENGTH);
    ConstNode<String> right = (ConstNode<String>) unary.expr();
    assertThat(right.value()).isEqualTo("hi");
  }

  @Test
  public void unaryAsc() {
    ProgramNode programNode = assertThatParsing("a=asc('hi')").succeeds();
    BlockNode root = programNode.statements();
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.lvalue();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    UnaryNode unary = (UnaryNode) expr;
    assertThat(unary.operator()).isEqualTo(TokenType.ASC);
    ConstNode<?> right = (ConstNode<?>) unary.expr();
    assertThat(right.value()).isEqualTo("hi");
  }

  @Test
  public void unaryChr() {
    ProgramNode programNode = assertThatParsing("a=chr(65)").succeeds();
    BlockNode root = programNode.statements();
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.lvalue();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    UnaryNode unary = (UnaryNode) expr;
    assertThat(unary.operator()).isEqualTo(TokenType.CHR);
    ConstNode<?> right = (ConstNode<?>) unary.expr();
    assertThat(right.value()).isEqualTo(65);
  }

  @Test
  public void binOpOperator(
      @TestParameter({"+", "-", "*", "/", "%", "|", "&", "^", "??"}) String operator) {
    ProgramNode programNode = assertThatParsing(String.format("a=b%s5", operator)).succeeds();
    BlockNode root = programNode.statements();
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    ExprNode expr = node.expr();
    BinOpNode binop = (BinOpNode) expr;
    assertThat(binop.operator().toString()).isEqualTo(operator);
  }

  @Test
  public void binEqualOpOperator(
      @TestParameter({"PLUS", "MINUS", "MULT", "DIV"}) TokenType operator) {
    ProgramNode node = assertThatParsing(String.format("a%s=b", operator.toString())).succeeds();
    BlockNode block = node.statements();
    assertThat(block.statements()).hasSize(1);
    AssignmentNode statement = (AssignmentNode) block.statements().get(0);
    assertThat(statement.lvalue().name()).isEqualTo("a");
    BinOpNode expectedRhs =
        new BinOpNode(new VariableNode("a", null), operator, new VariableNode("b", null));
    assertThat(statement.expr()).isEqualTo(expectedRhs);
  }

  @Test
  public void binOpCompare(@TestParameter({">", "<", "==", "!=", "<=", ">="}) String operator) {
    assertThatParsing(String.format("a=b%s5", operator)).succeeds();
  }

  @Test
  public void allExprTypes_exceptAndOr() {
    // boolean a = ((1 + 2) * (3 - 4) / (-5) == 6) == true
    // || ((2 - 3) * (4 - 5) / (-6) == 7) == false && ((3 + 4) * (5 + 6) / (-7) >=
    // (8 % 2));
    ProgramNode node = assertThatParsing("a=((1 + 2) * (3 - 4) / (-5) == 6) != true").succeeds();
    BlockNode root = node.statements();

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);
  }

  @Test
  public void allExprTypes() {
    ProgramNode node =
        assertThatParsing(
                "a=((1 + 2) * (3 - 4) / (-5) == 6) != true "
                    + " | ((2 - 3) * (4 - 5) / (-6) < 7) == !false "
                    + " & ((3 + 4) * (5 + 6) / (-7) >= (8 % 2)) "
                    + "b=1+2*3-4/5==6!=true|2-3*4-5/-6<7==!a & 3+4*5+6/-7>=8%2")
            .succeeds();
    BlockNode root = node.statements();
    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(2);
  }

  @Test
  public void program() {
    ProgramNode node =
        assertThatParsing("a=3 print a\n abc =   123 +a-b print 123\nprin=t").succeeds();
    BlockNode root = node.statements();

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
    ProgramNode programNode = assertThatParsing("a=(3)").succeeds();
    BlockNode root = programNode.statements();

    assertThat(root.statements()).hasSize(1);
    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.lvalue();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    ConstNode<Integer> intNode = (ConstNode<Integer>) expr;
    assertThat(intNode.value()).isEqualTo(3);
  }

  @Test
  public void parseIf() {
    ProgramNode node = assertThatParsing("if a==3 { print a a=4 }").succeeds();
    BlockNode root = node.statements();

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    IfNode ifNode = (IfNode) statements.get(0);
    assertThat(ifNode.cases()).hasSize(1);
    IfNode.Case first = ifNode.cases().get(0);
    assertThat(first.block().statements()).hasSize(2);
  }

  @Test
  public void ifEmptyBlock() {
    ProgramNode node = assertThatParsing("if a==3 { }").succeeds();
    BlockNode root = node.statements();

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    IfNode ifNode = (IfNode) statements.get(0);
    assertThat(ifNode.cases()).hasSize(1);
    assertThat(ifNode.elseBlock()).isEmpty();
    IfNode.Case first = ifNode.cases().get(0);
    assertThat(first.block().statements()).isEmpty();
  }

  @Test
  public void ifNested() {
    ProgramNode node =
        assertThatParsing(
                "      if a==3 { "
                    + "  if a==4 { "
                    + "   if a == 5 {"
                    + "     print a"
                    + "   } "
                    + "  }"
                    + "}"
                    + "else { print 4 print a}")
            .succeeds();
    BlockNode root = node.statements();

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    IfNode ifNode = (IfNode) statements.get(0);
    assertThat(ifNode.cases()).hasSize(1);
    assertThat(ifNode.elseBlock()).isPresent();
    // there may be a better way to do this but I don't know how.
    assertThat(ifNode.elseBlock().get().statements()).hasSize(2);
  }

  @Test
  public void ifElse() {
    ProgramNode node = assertThatParsing("if a==3 { print a } else { print 4 print a}").succeeds();
    BlockNode root = node.statements();

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    IfNode ifNode = (IfNode) statements.get(0);
    assertThat(ifNode.cases()).hasSize(1);
    assertThat(ifNode.elseBlock()).isPresent();
    assertThat(ifNode.elseBlock().get().statements()).hasSize(2);
  }

  @Test
  public void ifElif() {
    ProgramNode node =
        assertThatParsing(
                "      if a==3 { print a } "
                    + "elif a==4 { print 4 print a} "
                    + "elif a==5 { print 5}"
                    + "else { print 6 print 7}")
            .succeeds();
    BlockNode root = node.statements();
    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    IfNode ifNode = (IfNode) statements.get(0);
    assertThat(ifNode.cases()).hasSize(3);
    assertThat(ifNode.elseBlock()).isPresent();
  }

  @Test
  public void ifError() {
    assertThatParsing("if a==3 { print a } else print 4}").hasError("expected \\{");
    assertThatParsing("if a==3 { print a } else {print 4")
        .hasError("Unexpected start of statement 'EOF'");
    assertThatParsing("if a==3 print a } else {print 4").hasError("expected \\{");
    assertThatParsing("if print a else {print 4").hasError("expected literal");
    assertThatParsing(
            "if a==3 { print a } else  { print 4 print a} "
                + "elif a==5 { print 5}else { print 6 print 7}")
        .hasError("Unexpected start of statement 'ELIF'");
  }

  @Test
  public void whileTrue() {
    ProgramNode node = assertThatParsing("while true {}").succeeds();
    BlockNode root = node.statements();

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
    ProgramNode node = assertThatParsing("while true do i = 1 {}").succeeds();
    BlockNode root = node.statements();

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    WhileNode whileNode = (WhileNode) statements.get(0);

    AssignmentNode assignment = (AssignmentNode) whileNode.doStatement().get();
    VariableSetNode var = (VariableSetNode) assignment.lvalue();
    assertThat(var.name()).isEqualTo("i");

    ExprNode expr = assignment.expr();
    ConstNode<Integer> intNode = (ConstNode<Integer>) expr;
    assertThat(intNode.value()).isEqualTo(1);
  }

  @Test
  public void whileExprDo() {
    ProgramNode node = assertThatParsing("while i < 30 do i = 1 {}").succeeds();
    BlockNode root = node.statements();

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
    ProgramNode node = assertThatParsing("while i < 30 do i = 1 {print i a=i}").succeeds();
    BlockNode root = node.statements();

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    WhileNode whileNode = (WhileNode) statements.get(0);

    BlockNode block = whileNode.block();
    assertThat(block.statements()).hasSize(2);
  }

  @Test
  public void whileBreak() {
    ProgramNode node = assertThatParsing("while true {break continue}").succeeds();
    BlockNode root = node.statements();

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
    assertThatParsing("while true do advance(3) {}").succeeds();
  }

  @Test
  public void whileError() {
    assertThatParsing("while print").hasError("expected literal");
    assertThatParsing("while a==3 print").hasError("expected \\{");
    assertThatParsing("while a==3 {print").hasError("expected literal");
    assertThatParsing("while a==3 do {print}").hasError("Unexpected start of statement '\\{'");
    assertThatParsing("while a==3 do print {print}").hasError("expected literal");
    assertThatParsing("while a==3 do a=a+1 {a=}").hasError("expected literal");
    assertThatParsing("continue").hasError("CONTINUE found outside of WHILE");
    assertThatParsing("if true {break while true {continue }}")
        .hasError("BREAK found outside of WHILE");
    assertThatParsing("break").hasError("BREAK found outside of WHILE");
    assertThatParsing("if true {continue while true {break}}")
        .hasError("CONTINUE found outside of WHILE");
  }

  @Test
  public void mainEmpty() {
    assertThatParsing("main{}").hasError("Unexpected '\\{'");
  }

  @Test
  public void decl() {
    ProgramNode node = assertThatParsing("a:int").succeeds();
    BlockNode root = node.statements();

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    DeclarationNode declarationNode = (DeclarationNode) statements.get(0);
    assertThat(declarationNode.name()).isEqualTo("a");
    assertThat(declarationNode.varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void declBool() {
    ProgramNode node = assertThatParsing("a:bool").succeeds();
    BlockNode root = node.statements();

    List<StatementNode> statements = root.statements();
    DeclarationNode declarationNode = (DeclarationNode) statements.get(0);
    assertThat(declarationNode.varType()).isEqualTo(VarType.BOOL);
  }

  @Test
  public void declString() {
    ProgramNode node = assertThatParsing("a:string").succeeds();
    BlockNode root = node.statements();
    List<StatementNode> statements = root.statements();
    DeclarationNode declarationNode = (DeclarationNode) statements.get(0);
    assertThat(declarationNode.varType()).isEqualTo(VarType.STRING);
  }

  @Test
  public void declByte() {
    ProgramNode node = assertThatParsing("a:byte").succeeds();
    BlockNode root = node.statements();

    List<StatementNode> statements = root.statements();
    DeclarationNode declarationNode = (DeclarationNode) statements.get(0);
    assertThat(declarationNode.varType()).isEqualTo(VarType.BYTE);
  }

  @Test
  public void declLong() {
    ProgramNode node = assertThatParsing("a:long").succeeds();
    BlockNode root = node.statements();

    List<StatementNode> statements = root.statements();
    DeclarationNode declarationNode = (DeclarationNode) statements.get(0);
    assertThat(declarationNode.varType()).isEqualTo(VarType.LONG);
  }

  @Test
  public void declError() {
    assertThatParsing("a:").hasError("expected built-in");
    assertThatParsing("a::").hasError("expected built-in");
    assertThatParsing("a:print").hasError("expected built-in");
    assertThatParsing("a:void").hasError("expected built-in");
  }

  @Test
  public void assignString() {
    ProgramNode programNode = assertThatParsing("a='hi'").succeeds();
    BlockNode root = programNode.statements();

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.lvalue();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    assertThat(expr.varType()).isEqualTo(VarType.STRING);
  }

  @Test
  public void addStrings() {
    ProgramNode programNode = assertThatParsing("a='hi' + 'Hi'").succeeds();
    BlockNode root = programNode.statements();

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.lvalue();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    assertThat(expr).isInstanceOf(BinOpNode.class);
  }

  @Test
  public void procErrors() {
    assertThatParsing("fib:proc(a:int b) {}").hasError("expected \\)");
    assertThatParsing("fib:proc(a:proc, b) {}").hasError("expected built-in");
    assertThatParsing("fib:proc(a:, b) {}").hasError("expected built-in");
    assertThatParsing("fib:proc(a:) {}").hasError("expected built-in");
    assertThatParsing("fib:proc(a {}").hasError("expected \\)");
    assertThatParsing("fib:proc(a:int, ) {}").hasError("expected VARIABLE");
    assertThatParsing("fib:proc(a:int) print a").hasError("expected \\{");
    assertThatParsing("fib:proc  print a").hasError("expected \\{");
    assertThatParsing("fib:proc() {return").hasError("Unexpected start of statement 'EOF'");
    assertThatParsing("fib:proc() {return {").hasError("Unexpected start of statement '\\{'");
    assertThatParsing("fib:proc() {return )}").hasError("Unexpected start of statement '\\)'");
    assertThatParsing("fib:proc(arg:void) {}").hasError("Unexpected 'VOID'");
    assertThatParsing("fib:proc(void:arg) {}").hasError("Unexpected 'VOID'");
  }

  @Test
  public void simpleProc() {
    // the simplest possible procedure
    ProgramNode root = assertThatParsing("fib:proc {}").succeeds();
    ProcedureNode proc = (ProcedureNode) (root.statements().statements().get(0));
    assertThat(proc.name()).isEqualTo("fib");
    assertThat(proc.returnType()).isEqualTo(VarType.VOID);
  }

  @Test
  public void procWithParam() {
    ProgramNode root = assertThatParsing("fib:proc(param1) {}").succeeds();
    ProcedureNode proc = (ProcedureNode) (root.statements().statements().get(0));
    assertThat(proc.name()).isEqualTo("fib");
    assertThat(proc.returnType()).isEqualTo(VarType.VOID);
    assertThat(proc.parameters()).hasSize(1);
    assertThat(proc.parameters().get(0).name()).isEqualTo("param1");
    assertThat(proc.parameters().get(0).varType().isUnknown()).isTrue();
  }

  @Test
  public void procWith2Params() {
    ProgramNode root = assertThatParsing("fib:proc(param1, param2: string) {}").succeeds();
    ProcedureNode proc = (ProcedureNode) (root.statements().statements().get(0));
    assertThat(proc.name()).isEqualTo("fib");
    assertThat(proc.returnType()).isEqualTo(VarType.VOID);
    assertThat(proc.parameters()).hasSize(2);
    assertThat(proc.parameters().get(0).name()).isEqualTo("param1");
    assertThat(proc.parameters().get(0).varType().isUnknown()).isTrue();
    assertThat(proc.parameters().get(1).name()).isEqualTo("param2");
    assertThat(proc.parameters().get(1).varType()).isEqualTo(VarType.STRING);
  }

  @Test
  public void procWithArrayParam() {
    ProgramNode root = assertThatParsing("f:proc(param1:int[]) {}").succeeds();
    ProcedureNode proc = (ProcedureNode) (root.statements().statements().get(0));
    assertThat(proc.name()).isEqualTo("f");
    assertThat(proc.returnType()).isEqualTo(VarType.VOID);
    assertThat(proc.parameters()).hasSize(1);
    assertThat(proc.parameters().get(0).name()).isEqualTo("param1");
    assertThat(proc.parameters().get(0).varType()).hasArrayBaseType(VarType.INT);
  }

  @Test
  public void procWithByteParam() {
    ProgramNode root = assertThatParsing("f:proc(param1:byte) {}").succeeds();
    ProcedureNode proc = (ProcedureNode) (root.statements().statements().get(0));
    assertThat(proc.parameters().get(0).name()).isEqualTo("param1");
    assertThat(proc.parameters().get(0).varType()).isEqualTo(VarType.BYTE);
  }

  @Test
  public void procWithLongParam() {
    ProgramNode root = assertThatParsing("f:proc(param1:long) {}").succeeds();
    ProcedureNode proc = (ProcedureNode) (root.statements().statements().get(0));
    assertThat(proc.parameters().get(0).name()).isEqualTo("param1");
    assertThat(proc.parameters().get(0).varType()).isEqualTo(VarType.LONG);
  }

  @Test
  public void procWithLocals() {
    ProgramNode root = assertThatParsing("fib:proc() {local:int}").succeeds();
    ProcedureNode proc = (ProcedureNode) (root.statements().statements().get(0));
    assertThat(proc.name()).isEqualTo("fib");
    assertThat(proc.returnType()).isEqualTo(VarType.VOID);
    assertThat(proc.block().statements().get(0)).isInstanceOf(DeclarationNode.class);
  }

  @Test
  public void fullProc() {
    ProgramNode root =
        assertThatParsing(
                "      fib:proc(typed:int, nontyped) : string {"
                    + "  typed = typed + 1"
                    + "  nontyped = typed + 1"
                    + "  return 'hi'"
                    + "}")
            .succeeds();

    ProcedureNode proc = (ProcedureNode) (root.statements().statements().get(0));
    assertThat(proc.name()).isEqualTo("fib");
    assertThat(proc.returnType()).isEqualTo(VarType.STRING);
    assertThat(proc.parameters()).hasSize(2);
    assertThat(proc.block().statements()).hasSize(3);
  }

  @Test
  public void procReturnVoid() {
    ProgramNode root = assertThatParsing("fib:proc() {return}").succeeds();

    ProcedureNode proc = (ProcedureNode) (root.statements().statements().get(0));
    assertThat(proc.name()).isEqualTo("fib");
    assertThat(proc.returnType()).isEqualTo(VarType.VOID);
    assertThat(proc.block().statements()).hasSize(1);
    ReturnNode returnNode = (ReturnNode) proc.block().statements().get(0);
    assertThat(returnNode.expr().isPresent()).isFalse();

    // This is allowed, but the static checker will eventually prevent it
    assertThatParsing("fib:proc() {return print 'hi'}").succeeds();
  }

  @Test
  public void procReturnVoidExplicit() {
    ProgramNode root = assertThatParsing("fib:proc(): void {return}").succeeds();

    ProcedureNode proc = (ProcedureNode) (root.statements().statements().get(0));
    assertThat(proc.name()).isEqualTo("fib");
    assertThat(proc.returnType()).isEqualTo(VarType.VOID);
    assertThat(proc.block().statements()).hasSize(1);
    ReturnNode returnNode = (ReturnNode) proc.block().statements().get(0);
    assertThat(returnNode.expr().isPresent()).isFalse();

    // This is allowed, but the static checker will eventually prevent it
    assertThatParsing("fib:proc():void {return print 'hi'}").succeeds();
  }

  @Test
  public void externSimpleProc() {
    // the simplest possible procedure
    ProgramNode root = assertThatParsing("fib:extern proc").succeeds();
    ExternProcedureNode proc = (ExternProcedureNode) (root.statements().statements().get(0));
    assertThat(proc.name()).isEqualTo("fib");
    assertThat(proc.returnType()).isEqualTo(VarType.VOID);
  }

  @Test
  public void externProcWithParam() {
    ProgramNode root = assertThatParsing("fib:extern proc(param1:string)").succeeds();
    ProcedureNode proc = (ProcedureNode) (root.statements().statements().get(0));
    assertThat(proc.name()).isEqualTo("fib");
    assertThat(proc.returnType()).isEqualTo(VarType.VOID);
    assertThat(proc.parameters()).hasSize(1);
    assertThat(proc.parameters().get(0).name()).isEqualTo("param1");
    assertThat(proc.parameters().get(0).varType()).isEqualTo(VarType.STRING);
  }

  @Test
  public void externFullProc() {
    ProgramNode root =
        assertThatParsing("fib:extern proc(typed:int, nontyped) : string").succeeds();

    ProcedureNode proc = (ProcedureNode) (root.statements().statements().get(0));
    assertThat(proc.name()).isEqualTo("fib");
    assertThat(proc.returnType()).isEqualTo(VarType.STRING);
    assertThat(proc.parameters()).hasSize(2);
    assertThat(proc.parameters().get(0).varType()).isEqualTo(VarType.INT);
    assertThat(proc.parameters().get(1).varType()).isUnknown();
    assertThrows(IllegalStateException.class, () -> proc.block());
  }

  @Test
  public void procCallNoArgs() {
    ProgramNode root = assertThatParsing("a = doit()").succeeds();
    AssignmentNode assignment = (AssignmentNode) (root.statements().statements().get(0));
    ExprNode expr = assignment.expr();
    assertThat(expr).isInstanceOf(CallNode.class);
  }

  @Test
  public void procCallAsStatement() {
    ProgramNode root = assertThatParsing("doit(3)").succeeds();
    CallNode call = (CallNode) (root.statements().statements().get(0));

    assertThat(call.procName()).isEqualTo("doit");
    assertThat(call.actuals()).hasSize(1);
    ExprNode param = call.actuals().get(0);
    assertThat(param.isConstant()).isTrue();
  }

  @Test
  public void procCallAsExpression() {
    ProgramNode root = assertThatParsing("a = doit((3*6*(3-4)*(5-5)), (abc==doit()))").succeeds();

    AssignmentNode assignment = (AssignmentNode) (root.statements().statements().get(0));
    ExprNode expr = assignment.expr();
    assertThat(expr).isInstanceOf(CallNode.class);
  }

  @Test
  public void procCallOneArgs() {
    ProgramNode root = assertThatParsing("a = doit(1)").succeeds();

    AssignmentNode assignment = (AssignmentNode) (root.statements().statements().get(0));
    ExprNode expr = assignment.expr();
    assertThat(expr).isInstanceOf(CallNode.class);
  }

  @Test
  public void procCallErrors() {
    assertThatParsing("a = doit(1 b=3").hasError("expected \\)");
    assertThatParsing("a = doit(1,)").hasError("expected literal");
  }

  @Test
  public void procCallMultipleArgs() {
    ProgramNode root = assertThatParsing("a = doit(1, 2, 3)").succeeds();
    AssignmentNode assignment = (AssignmentNode) (root.statements().statements().get(0));
    ExprNode expr = assignment.expr();
    assertThat(expr).isInstanceOf(CallNode.class);
  }

  @Test
  public void procCallNested() {
    ProgramNode root = assertThatParsing("a = doit3(1, doit1(2), doit2(3, 4))").succeeds();
    AssignmentNode assignment = (AssignmentNode) (root.statements().statements().get(0));
    ExprNode expr = assignment.expr();
    assertThat(expr).isInstanceOf(CallNode.class);
  }

  @Test
  public void returnOutsideProc() {
    assertThatParsing("return").hasError("Cannot RETURN from outside a PROC");
    assertThatParsing("f:proc{} return").hasError("Cannot RETURN from outside a PROC");
  }

  @Test
  public void invalidReturn() {
    assertThatParsing("f:proc {return print}").hasError("expected literal, variable, or");
  }

  @Test
  public void arrayGet() {
    ProgramNode programNode = assertThatParsing("a=b[3+c]").succeeds();
    BlockNode root = programNode.statements();

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.lvalue();
    assertThat(var.name()).isEqualTo("a");

    BinOpNode expr = (BinOpNode) node.expr();
    assertThat(expr.left()).isInstanceOf(VariableNode.class);
    assertThat(expr.operator()).isEqualTo(TokenType.LBRACKET);
    assertThat(expr.right()).isInstanceOf(BinOpNode.class);
  }

  @Test
  public void arrayGetWeirdYetParseable() {
    assertThatParsing("a=3[4+c]").succeeds();
    assertThatParsing("a='hi'['lol']").succeeds(); // will fail type checker
    assertThatParsing("a=fn()[4+c]").succeeds(); // will fail type checker
  }

  @Test
  public void allocArray() {
    ProgramNode programNode = assertThatParsing("a:int[3]").succeeds();
    BlockNode root = programNode.statements();
    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    ArrayDeclarationNode node = (ArrayDeclarationNode) statements.get(0);
    assertThat(node.name()).isEqualTo("a");
    assertThat(node.varType()).isArray();
    assertThat(node.sizeExpr()).isInstanceOf(ConstNode.class);

    assertThat(node.varType()).hasArrayBaseType(VarType.INT);
  }

  @Test
  public void declArray() {
    ProgramNode programNode = assertThatParsing("a:int[]").succeeds();
    BlockNode root = programNode.statements();
    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    DeclarationNode node = (DeclarationNode) statements.get(0);
    assertThat(node.name()).isEqualTo("a");
    assertThat(node.varType()).isArray();

    assertThat(node.varType()).hasArrayBaseType(VarType.INT);
  }

  @Test
  public void allocEmptyArray() {
    ProgramNode programNode = assertThatParsing("a:int[0]").succeeds();
    BlockNode root = programNode.statements();
    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    ArrayDeclarationNode node = (ArrayDeclarationNode) statements.get(0);
    assertThat(node.name()).isEqualTo("a");
    assertThat(node.varType()).isArray();
    assertThat(node.sizeExpr()).isInstanceOf(ConstNode.class);

    assertThat(node.varType()).hasArrayBaseType(VarType.INT);
  }

  @Test
  public void arrayDeclVariableSize() {
    ProgramNode programNode = assertThatParsing("a:int[b+3]").succeeds();
    BlockNode root = programNode.statements();
    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    ArrayDeclarationNode node = (ArrayDeclarationNode) statements.get(0);
    assertThat(node.name()).isEqualTo("a");
    assertThat(node.varType()).isArray();
    assertThat(node.sizeExpr()).isInstanceOf(BinOpNode.class);

    assertThat(node.varType()).hasArrayBaseType(VarType.INT);
  }

  @Test
  public void arrayDeclError() {
    assertThatParsing("a:int[3").hasError("expected ]");
    assertThatParsing("a:int[3 4]").hasError("expected ]");
  }

  @Test
  @Ignore("Issue #38: Support multidimensional arrays")
  public void multiDimArrayGet() {
    ProgramNode node = assertThatParsing("a=b[3+c][4][5]").succeeds();
    BlockNode root = node.statements();
    System.out.println(root);
  }

  @Test
  public void arraySet() {
    ProgramNode programNode = assertThatParsing("a[3] = 4").succeeds();
    BlockNode root = programNode.statements();
    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    AssignmentNode node = (AssignmentNode) statements.get(0);
    LValueNode lValue = node.lvalue();
    assertThat(lValue).isInstanceOf(ArraySetNode.class);
    ArraySetNode asn = (ArraySetNode) lValue;
    assertThat(asn.variableName()).isEqualTo("a");
    assertThat(asn.indexNode()).isInstanceOf(ConstNode.class);
    ConstNode<Integer> value = (ConstNode<Integer>) asn.indexNode();
    assertThat(value.value()).isEqualTo(3);
  }

  @Test
  public void arraySetError() {
    assertThatParsing("a[3] = ").hasError("expected literal");
    assertThatParsing("a[3 = ").hasError("expected ]");
  }

  @Test
  @Ignore("Array assignments are still unimplemented")
  public void arrayStmt() {
    assertThatParsing("fn()[fn()]").succeeds();
  }

  @Test
  public void arrayGetError() {
    assertThatParsing("a=3[4+c b").hasError("expected ]");
    assertThatParsing("a=3[4+c b]").hasError("expected ]");
    assertThatParsing("a=b[4").hasError("expected ]");
  }

  @Test
  public void arrayLiteralInts() {
    ProgramNode programNode = assertThatParsing("a=[1,2,3]").succeeds();
    BlockNode blockNode = programNode.statements();
    StatementNode statementNode = blockNode.statements().get(0);
    assertThat(statementNode).isInstanceOf(AssignmentNode.class);
    AssignmentNode node = (AssignmentNode) statementNode;
    ExprNode rhs = node.expr();
    assertThat(rhs).isInstanceOf(ArrayLiteralNode.class);
    ArrayLiteralNode array = (ArrayLiteralNode) rhs;
    assertThat(array.elements()).hasSize(3);
    assertThat(array.varType()).hasArrayBaseType(VarType.INT);

    ConstNode<Integer> first = (ConstNode<Integer>) array.elements().get(0);
    assertThat(first.value()).isEqualTo(1);
    ConstNode<Integer> second = (ConstNode<Integer>) array.elements().get(1);
    assertThat(second.value()).isEqualTo(2);
  }

  @Test
  public void arrayLiteral() {
    ProgramNode programNode = assertThatParsing("a=['1', '2']").succeeds();
    BlockNode blockNode = programNode.statements();
    StatementNode statementNode = blockNode.statements().get(0);
    assertThat(statementNode).isInstanceOf(AssignmentNode.class);
    AssignmentNode node = (AssignmentNode) statementNode;
    ExprNode rhs = node.expr();
    assertThat(rhs).isInstanceOf(ArrayLiteralNode.class);
    ArrayLiteralNode array = (ArrayLiteralNode) rhs;
    assertThat(array.varType()).hasArrayBaseType(VarType.STRING);

    ConstNode<String> first = (ConstNode<String>) array.elements().get(0);
    assertThat(first.value()).isEqualTo("1");
    ConstNode<String> second = (ConstNode<String>) array.elements().get(1);
    assertThat(second.value()).isEqualTo("2");
  }

  @Test
  public void arrayLiteralBools() {
    ProgramNode programNode = assertThatParsing("a=[true, false]").succeeds();
    BlockNode blockNode = programNode.statements();
    StatementNode statementNode = blockNode.statements().get(0);
    assertThat(statementNode).isInstanceOf(AssignmentNode.class);
    AssignmentNode node = (AssignmentNode) statementNode;
    ExprNode rhs = node.expr();
    assertThat(rhs).isInstanceOf(ArrayLiteralNode.class);
    ArrayLiteralNode array = (ArrayLiteralNode) rhs;
    assertThat(array.varType()).hasArrayBaseType(VarType.BOOL);

    ConstNode<Boolean> first = (ConstNode<Boolean>) array.elements().get(0);
    assertThat(first.value()).isTrue();
    ConstNode<Boolean> second = (ConstNode<Boolean>) array.elements().get(1);
    assertThat(second.value()).isFalse();
  }

  @Test
  public void arrayLiteralIndex() {
    ProgramNode programNode = assertThatParsing("a=[true, false][1]").succeeds();
    BlockNode root = programNode.statements();
    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.lvalue();
    assertThat(var.name()).isEqualTo("a");

    BinOpNode expr = (BinOpNode) node.expr();
    assertThat(expr.left()).isInstanceOf(ArrayLiteralNode.class);
    assertThat(expr.right()).isInstanceOf(ConstNode.class);
  }

  @Test
  public void arrayLiteralErrors() {
    assertThatParsing("a=[a]").hasError("all elements are UNKNOWN");
    assertThatParsing("a=[a+1]").hasError("all elements are UNKNOWN");
    // this is no longer checked in parser; it's in the static checker now.
    // assertParseError("a=[1,'hi']", "Inconsistent types");
  }

  @Test
  public void arrayLiteralEmpty() {
    assertThatParsing("a=[]").hasError("Unexpected ']'");
  }

  @Test
  public void exit() {
    ProgramNode programNode = assertThatParsing("exit").succeeds();
    BlockNode root = programNode.statements();
    ExitNode node = (ExitNode) root.statements().get(0);
    assertThat(node.exitMessage().isPresent()).isFalse();
  }

  @Test
  public void exit_withMessage() {
    ProgramNode programNode = assertThatParsing("exit 'sorry/not sorry'").succeeds();
    BlockNode root = programNode.statements();
    ExitNode node = (ExitNode) root.statements().get(0);
    ConstNode<String> message = (ConstNode<String>) node.exitMessage().get();
    assertThat(message.value()).isEqualTo("sorry/not sorry");
  }

  @Test
  public void input() {
    ProgramNode programNode = assertThatParsing("f=input").succeeds();
    BlockNode root = programNode.statements();
    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableSetNode var = (VariableSetNode) node.lvalue();
    assertThat(var.name()).isEqualTo("f");

    ExprNode expr = node.expr();
    assertThat(expr).isInstanceOf(InputNode.class);
  }

  @Test
  public void input_fail() {
    assertThatParsing("input(f)").hasError("Unexpected start of statement 'INPUT'");
    assertThatParsing("f=input()").hasError("Unexpected start of statement '\\('");
  }

  @Test
  public void declRecord_empty() {
    ProgramNode programNode = assertThatParsing("r: record{}").succeeds();
    BlockNode root = programNode.statements();
    RecordDeclarationNode node = (RecordDeclarationNode) root.statements().get(0);
    assertThat(node.name()).isEqualTo("r");
    assertThat(node.fields()).isEmpty();
  }

  @Test
  public void declRecord() {
    ProgramNode programNode = assertThatParsing("R: record{i: int s: string}").succeeds();
    BlockNode root = programNode.statements();
    RecordDeclarationNode node = (RecordDeclarationNode) root.statements().get(0);
    assertThat(node.name()).isEqualTo("R");
    assertThat(node.fields()).hasSize(2);
    assertThat(node.fields().get(0).varType()).isEqualTo(VarType.INT);
    assertThat(node.fields().get(0).name()).isEqualTo("i");
    assertThat(node.fields().get(1).varType()).isEqualTo(VarType.STRING);
    assertThat(node.fields().get(1).name()).isEqualTo("s");
  }

  @Test
  public void declGenericRecord() {
    ProgramNode programNode = assertThatParsing("r: record<T, UV>{i: T s: UV}").succeeds();
    BlockNode root = programNode.statements();
    RecordDeclarationNode node = (RecordDeclarationNode) root.statements().get(0);
    assertThat(node.formalTypeVariables()).containsExactly("T", "UV").inOrder();

    assertThat(node.fields().get(0).name()).isEqualTo("i");
    UnboundType fieldType = (UnboundType) node.fields().get(0).varType();
    assertThat(fieldType.name()).isEqualTo("T");
    assertThat(node.fields().get(1).name()).isEqualTo("s");
    fieldType = (UnboundType) node.fields().get(1).varType();
    assertThat(fieldType.name()).isEqualTo("UV");
  }

  @Test
  public void declGenericRecord_mix() {
    ProgramNode programNode = assertThatParsing("r: record<T>{i: T s: UV}").succeeds();
    BlockNode root = programNode.statements();
    RecordDeclarationNode node = (RecordDeclarationNode) root.statements().get(0);
    assertThat(node.formalTypeVariables()).containsExactly("T");

    assertThat(node.fields().get(0).name()).isEqualTo("i");
    UnboundType fieldType = (UnboundType) node.fields().get(0).varType();
    assertThat(fieldType.name()).isEqualTo("T");

    assertThat(node.fields().get(1).name()).isEqualTo("s");
    RecordReferenceType recordFieldType = (RecordReferenceType) node.fields().get(1).varType();
    assertThat(recordFieldType.name()).isEqualTo("UV");
  }

  @Test
  public void declGenericRecord_bad() {
    assertThatParsing("r: record<T, 1>{i: T s: UV}").hasError("expected VARIABLE");
    assertThatParsing("r: record<>{i: T s: UV}").hasError("expected VARIABLE");
    assertThatParsing("r: record<(3)>{i: T s: UV}").hasError("expected VARIABLE");
    assertThatParsing("r: record<record>{i: T s: UV}").hasError("expected VARIABLE");
    // assertThatParsing("r: record<T, 1>{}").hasError("expected VARIABLE");
    // assertThatParsing("r: record<>{}").hasError("expected VARIABLE");
    // assertThatParsing("r: record<(3)>{}").hasError("expected VARIABLE");
    // assertThatParsing("r: record<record>{}").hasError("expected VARIABLE");
    // assertThatParsing("r: record<T,>{}").hasError("expected VARIABLE");
    // assertThatParsing("r: record<int>{}").hasError("expected VARIABLE");
  }

  @Test
  public void declRecordRecursive() {
    ProgramNode programNode = assertThatParsing("R: record {r: R}").succeeds();
    BlockNode root = programNode.statements();
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
    assertThatParsing("r: record{p:int int}").hasError("expected VARIABLE");
    assertThatParsing("r: record{int}").hasError("expected VARIABLE");
    assertThatParsing("r: record{proc}").hasError("expected VARIABLE");
    // the error here is actually that it's trying to parse a procedure, but meh
    // assertThatParsing("r: record{p:proc}").hasError("expected");
    // not parse errors, but are type errors
    // assertParseError("r: record{p:proc{}}", "expected");-
    // assertParseError("r: record{r2:record{}}", "expected VARIABLE");
    assertThatParsing("r: record{f:record{f2:int}}")
        .hasError("Unexpected 'RECORD' in RECORD declaration");
    assertThatParsing("r: record{p:proc() {} }")
        .hasError("Unexpected 'PROC' in RECORD declaration");
  }

  @Test
  public void declVar_asRecord() {
    ProgramNode programNode = assertThatParsing("a: R").succeeds();
    BlockNode root = programNode.statements();
    DeclarationNode node = (DeclarationNode) root.statements().get(0);
    assertThat(node.name()).isEqualTo("a");
    RecordReferenceType type = (RecordReferenceType) node.varType();
    assertThat(type.name()).isEqualTo("R");
  }

  @Test
  public void declVar_asGenericRecord() {
    ProgramNode programNode = assertThatParsing("a: R<string>").succeeds();
    BlockNode root = programNode.statements();
    DeclarationNode node = (DeclarationNode) root.statements().get(0);
    assertThat(node.name()).isEqualTo("a");
    RecordReferenceType type = (RecordReferenceType) node.varType();
    assertThat(type.name()).isEqualTo("R");
    assertThat(type.actualTypes()).containsExactly(VarType.STRING);
  }

  @Test
  public void recordAsFormalParam() {
    ProgramNode node = assertThatParsing("p:proc(a: R) {}").succeeds();
    BlockNode root = node.statements();
    ProcedureNode proc = (ProcedureNode) root.statements().get(0);
    ProcedureNode.Parameter param = proc.parameters().get(0);
    assertThat(param.name()).isEqualTo("a");
    RecordReferenceType type = (RecordReferenceType) param.varType();
    assertThat(type.name()).isEqualTo("R");
  }

  @Test
  public void genericRecordAsFormalParam() {
    ProgramNode node = assertThatParsing("p:proc(a: R<int>) {}").succeeds();
    BlockNode root = node.statements();
    ProcedureNode proc = (ProcedureNode) root.statements().get(0);
    ProcedureNode.Parameter param = proc.parameters().get(0);
    assertThat(param.name()).isEqualTo("a");
    RecordReferenceType type = (RecordReferenceType) param.varType();
    assertThat(type.name()).isEqualTo("R");
    assertThat(type.actualTypes()).containsExactly(VarType.INT);
  }

  @Test
  public void recordAsReturnType() {
    ProgramNode node = assertThatParsing("p:proc():R {}").succeeds();
    BlockNode root = node.statements();
    ProcedureNode proc = (ProcedureNode) root.statements().get(0);
    RecordReferenceType type = (RecordReferenceType) proc.returnType();
    assertThat(type.name()).isEqualTo("R");
  }

  @Test
  public void genericRecordAsReturnType() {
    ProgramNode programNode = assertThatParsing("p:proc():R<bool, int> {}").succeeds();
    BlockNode root = programNode.statements();
    ProcedureNode proc = (ProcedureNode) root.statements().get(0);
    RecordReferenceType type = (RecordReferenceType) proc.returnType();
    assertThat(type.name()).isEqualTo("R");
    assertThat(type.actualTypes()).containsExactly(VarType.BOOL, VarType.INT);
  }

  @Test
  public void newRecord() {
    ProgramNode programNode = assertThatParsing("R: record{i: int} rec = new R").succeeds();
    BlockNode root = programNode.statements();
    AssignmentNode assignment = (AssignmentNode) root.statements().get(1);
    NewNode node = (NewNode) assignment.expr();
    assertThat(node.baseRecordName()).isEqualTo("R");
    RecordReferenceType type = (RecordReferenceType) node.varType();
    assertThat(type.name()).isEqualTo("R");
  }

  @Test
  public void newGenericRecord() {
    ProgramNode programNode = assertThatParsing("R: record<T>{i: T} rec = new R<int>").succeeds();
    BlockNode root = programNode.statements();
    AssignmentNode assignment = (AssignmentNode) root.statements().get(1);
    NewNode node = (NewNode) assignment.expr();
    assertThat(node.actualTypes()).containsExactly(VarType.INT);
  }

  @Test
  public void newGenericRecord_bad() {
    assertThatParsing("R: record<T>{i: T} rec = new R<>")
        .hasError("expected built-in or RECORD type");
    assertThatParsing("R: record<T>{i: T} rec = new R<int, >")
        .hasError("expected built-in or RECORD type");
  }

  @Test
  public void newRecord_returnValue() {
    assertThatParsing("r1:record{i:int} p:proc():r1{return new r1} var1=p()").succeeds();
  }

  @Test
  public void new_asOperand() {
    assertThatParsing("R: record{i: int} rec = new R.i")
        .hasError("Unexpected start of statement '.'");
  }

  @Test
  public void newRecord_Error() {
    assertThatParsing("R: record{i: int s: string} rec = new new").hasError("expected VARIABLE");
  }

  @Test
  public void recordGet() {
    ProgramNode programNode = assertThatParsing("i=rec.i").succeeds();
    BlockNode root = programNode.statements();
    AssignmentNode assignment = (AssignmentNode) root.statements().get(0);
    BinOpNode node = (BinOpNode) assignment.expr();
    assertThat(node.left()).isInstanceOf(VariableNode.class);
    assertThat(node.operator()).isEqualTo(TokenType.DOT);
    assertThat(node.right()).isInstanceOf(VariableNode.class);
  }

  @Test
  public void ifRecordGet() {
    assertThatParsing("if rec.i ==0 {print rec.i}").succeeds();
  }

  @Test
  public void recordGetRecursive() {
    ProgramNode programNode = assertThatParsing("s=rec.s[(a+b)] s=rec.f1.f2[3] ").succeeds();
    BlockNode root = programNode.statements();
    AssignmentNode assignment = (AssignmentNode) root.statements().get(0);
    BinOpNode node = (BinOpNode) assignment.expr();
    assertThat(node.left()).isInstanceOf(BinOpNode.class);
    assertThat(node.operator()).isEqualTo(TokenType.LBRACKET);
    assertThat(node.right()).isInstanceOf(BinOpNode.class);
  }

  @Test
  public void recordGetError() {
    assertThatParsing("i = rec.[\n").hasError("expected literal");
  }

  @SuppressWarnings("unchecked")
  @Test
  public void recordSet() {
    ProgramNode node = assertThatParsing("rec.i = 3 rec.s = 'hi'").succeeds();
    BlockNode root = node.statements();
    AssignmentNode assignment = (AssignmentNode) root.statements().get(0);
    FieldSetNode lvalue = (FieldSetNode) assignment.lvalue();
    assertThat(lvalue.variableName()).isEqualTo("rec");
    assertThat(lvalue.fieldName()).isEqualTo("i");
    assertThat(((ConstNode<Integer>) assignment.expr()).value()).isEqualTo(3);

    assignment = (AssignmentNode) root.statements().get(1);
    lvalue = (FieldSetNode) assignment.lvalue();
    assertThat(lvalue.variableName()).isEqualTo("rec");
    assertThat(lvalue.fieldName()).isEqualTo("s");
    assertThat(((ConstNode<String>) assignment.expr()).value()).isEqualTo("hi");
  }

  @Test
  public void recordSetError() {
    assertThatParsing("rec.3 = i\n").hasError("expected VARIABLE");
  }

  @Test
  public void unsetRecordCompareToNull() {
    ProgramNode programNode =
        assertThatParsing(
                """
                R: record{i: int s: string}
                rec: R
                isNull = rec == null
                """)
            .succeeds();
    BlockNode root = programNode.statements();
    AssignmentNode assignment = (AssignmentNode) root.statements().get(2);
    BinOpNode node = (BinOpNode) assignment.expr();
    assertThat(node.left()).isInstanceOf(VariableNode.class);
    assertThat(node.operator()).isEqualTo(TokenType.EQEQ);
    ConstNode<Void> right = (ConstNode<Void>) node.right();
    assertThat(right.value()).isNull();
  }

  @Test
  public void arrayOfRecord() {
    assertThatParsing("r:record{a:string} rs:r[2]").succeeds();
  }

  @Test
  public void arrayInRecord() {
    assertThatParsing("r:record{a:string[1]} anr=new r print anr.a").succeeds();
  }

  @Test
  @Ignore("Bug #155")
  public void advancedLValue() {
    assertThatParsing("foo[3].bar.baz[4].qux = 3").succeeds();
  }

  @Test
  public void advancedRValue() {
    assertThatParsing("bam = foo.bar[3].bar.baz[4].qux").succeeds();
    assertThatParsing("bam = foo[3+a].bar.baz[f()].qux").succeeds();
    // this passes now (!). bug #158
    assertThatParsing("bam = foo.3").succeeds();
    // this parses but shouldn't pass static checking
  }

  @Test
  public void args() {
    ProgramNode programNode = assertThatParsing("a = args[0]").succeeds();
    BlockNode node = programNode.statements();
    AssignmentNode assignment = (AssignmentNode) node.statements().get(0);
    ExprNode rhs = assignment.expr();
    assertThat(rhs).isInstanceOf(BinOpNode.class);
    BinOpNode binOp = (BinOpNode) rhs;
    ExprNode left = binOp.left();
    assertThat(left).isInstanceOf(VariableNode.class);
    VariableNode leftVar = (VariableNode) left;
    assertThat(leftVar.name()).isEqualTo("ARGS");
    assertThat(leftVar.varType()).isArray();
    assertThat(leftVar.varType()).hasArrayBaseType(VarType.STRING);
  }

  @Test
  public void argsLen() {
    assertThatParsing(
            """
            len=length(args)
            print 'length is ' println len
            b=args
            a=args[0]
            println 'first is ' + a
            """)
        .succeeds();
  }

  @Test
  public void badArgs() {
    assertThatParsing("ARGS = 3").hasError("Unexpected start of statement 'ARGS'");
    assertThatParsing("args = 3").hasError("Unexpected start of statement 'ARGS'");
    assertThatParsing("args:int").hasError("Unexpected start of statement 'ARGS'");
    assertThatParsing("args[3]=3").hasError("Unexpected start of statement 'ARGS'");
    assertThatParsing("f:proc(args:String[]) {print args[0]}").hasError("Unexpected 'ARGS'");
  }

  @Test
  public void badLength() {
    // tests bug #211
    assertThatParsing("x = length(string)").hasError("Unexpected 'STRING'; expected literal");
    assertThatParsing("x = length(int)").hasError("Unexpected 'INT'; expected literal");
  }

  @Test
  public void increment() {
    ProgramNode programNode = assertThatParsing("a++").succeeds();
    BlockNode root = programNode.statements();
    assertThat(root.statements()).hasSize(1);

    IncDecNode node = (IncDecNode) root.statements().get(0);

    assertThat(node.name()).isEqualTo("a");
    assertThat(node.isIncrement()).isTrue();
  }

  @Test
  public void decrement() {
    ProgramNode programNode = assertThatParsing("a--").succeeds();
    BlockNode root = programNode.statements();
    assertThat(root.statements()).hasSize(1);

    IncDecNode node = (IncDecNode) root.statements().get(0);

    assertThat(node.name()).isEqualTo("a");
    assertThat(node.isIncrement()).isFalse();
  }

  @Test
  public void stringIndex() {
    ProgramNode programNode = assertThatParsing("a=b[3]").succeeds();
    BlockNode root = programNode.statements();
    assertThat(root.statements()).hasSize(1);
    AssignmentNode node = (AssignmentNode) root.statements().get(0);
    assertThat(node.lvalue().name()).isEqualTo("a");
    assertThat(node.expr()).isInstanceOf(BinOpNode.class);
  }

  @Test
  public void declareRange() {
    ProgramNode programNode = assertThatParsing("a:range").succeeds();
    BlockNode root = programNode.statements();
    assertThat(root.statements()).hasSize(1);

    DeclarationNode node = (DeclarationNode) root.statements().get(0);
    assertThat(node.name()).isEqualTo("a");
    assertThat(node.varType()).isEqualTo(VarType.RANGE);
  }

  @Test
  public void assignRange() {
    ProgramNode programNode = assertThatParsing("a=0:1").succeeds();
    BlockNode root = programNode.statements();
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);
    BinOpNode expr = (BinOpNode) node.expr();
    assertThat(expr.left()).isEqualTo(new ConstNode<Integer>(0, VarType.INT, null));
    assertThat(expr.right()).isEqualTo(new ConstNode<Integer>(1, VarType.INT, null));
    assertThat(expr.operator()).isEqualTo(TokenType.COLON);
  }

  @Test
  public void badRange() {
    assertThatParsing("a=0:1:2").hasError("':'");
    assertThatParsing("a=0:").hasError("expected literal");
    // This isn't a parser error, but will be a type check error
    // assertParseError("a=(0:1):2", "COLON");
  }

  @Test
  public void stringSliceSimple() {
    ProgramNode programNode = assertThatParsing("c=0 a=b[c+2:3]").succeeds();
    BlockNode root = programNode.statements();
    assertThat(root.statements()).hasSize(2);

    AssignmentNode node = (AssignmentNode) root.statements().get(1);
    BinOpNode expr = (BinOpNode) node.expr();
    BinOpNode index = (BinOpNode) expr.right();
    assertThat(index.right()).isEqualTo(new ConstNode<Integer>(3, VarType.INT, null));
    assertThat(index.operator()).isEqualTo(TokenType.COLON);
  }

  @Test
  public void stringSliceAsExpressions() {
    ProgramNode programNode = assertThatParsing("a=b[(a*(3+1)):b+1]").succeeds();
    BlockNode root = programNode.statements();
    assertThat(root.statements()).hasSize(1);
    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    BinOpNode expr = (BinOpNode) node.expr();
    BinOpNode index = (BinOpNode) expr.right();
    assertThat(index.operator()).isEqualTo(TokenType.COLON);
    assertThat(index.left()).isInstanceOf(BinOpNode.class);
    BinOpNode bPlusOne = (BinOpNode) index.right();
    assertThat(bPlusOne.left()).isEqualTo(new VariableNode("b", null));
    assertThat(bPlusOne.right()).isEqualTo(new ConstNode<Integer>(1, VarType.INT, null));
  }

  @Test
  public void badStringSlice() {
    // not allowed yet
    assertThatParsing("a=b[1:]").hasError("expected literal");
    assertThatParsing("a=b[:2]").hasError("expected literal");
  }
}
