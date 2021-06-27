package com.plasstech.lang.d2.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.base.Joiner;
import static com.google.common.collect.ImmutableList.toImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.lex.IntToken;
import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.lex.Token;
import com.plasstech.lang.d2.lex.Token.Type;
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
import com.plasstech.lang.d2.parse.node.IfNode;
import com.plasstech.lang.d2.parse.node.InputNode;
import com.plasstech.lang.d2.parse.node.MainNode;
import com.plasstech.lang.d2.parse.node.Node;
import com.plasstech.lang.d2.parse.node.PrintNode;
import com.plasstech.lang.d2.parse.node.ProcedureNode;
import com.plasstech.lang.d2.parse.node.ProcedureNode.Parameter;
import com.plasstech.lang.d2.parse.node.ProgramNode;
import com.plasstech.lang.d2.parse.node.ReturnNode;
import com.plasstech.lang.d2.parse.node.StatementNode;
import com.plasstech.lang.d2.parse.node.UnaryNode;
import com.plasstech.lang.d2.parse.node.VariableNode;
import com.plasstech.lang.d2.parse.node.WhileNode;
import com.plasstech.lang.d2.type.ArrayType;
import com.plasstech.lang.d2.type.RecordReferenceType;
import com.plasstech.lang.d2.type.RecordType;
import com.plasstech.lang.d2.type.RecordType.Field;
import com.plasstech.lang.d2.type.VarType;

public class Parser {

  private static final ImmutableMap<Token.Type, VarType> BUILTINS =
      ImmutableMap.of(
          Token.Type.INT, VarType.INT,
          Token.Type.BOOL, VarType.BOOL,
          Token.Type.STRING, VarType.STRING,
          Token.Type.PROC, VarType.PROC);

  private static final Set<Token.Type> EXPRESSION_STARTS =
      ImmutableSet.of(
          Token.Type.VARIABLE,
          Token.Type.LPAREN,
          Token.Type.MINUS,
          Token.Type.PLUS,
          Token.Type.NOT,
          Token.Type.BIT_NOT,
          Token.Type.INT,
          Token.Type.STRING,
          Token.Type.BOOL,
          Token.Type.TRUE,
          Token.Type.FALSE,
          Token.Type.LENGTH,
          Token.Type.ASC,
          Token.Type.CHR);

  private static Set<Type> UNARY_KEYWORDS =
      ImmutableSet.of(Token.Type.LENGTH, Token.Type.ASC, Token.Type.CHR);

  private final Lexer lexer;
  private Token token;
  private int inWhile;

  public Parser(Lexer lexer) {
    this.lexer = lexer;
    this.advance();
  }

  private void advance() {
    token = lexer.nextToken();
  }

  public Node parse() {
    try {
      return program();
    } catch (ParseException te) {
      return te.errorNode();
    }
  }

  private void expect(Token.Type first, Token.Type... rest) {
    ImmutableList<Token.Type> expected = ImmutableList.copyOf(Lists.asList(first, rest));
    if (!expected.contains(token.type())) {
      String expectedStr;
      if (expected.size() == 1) {
        expectedStr = expected.get(0).toString();
      } else {
        expectedStr =
            String.format(
                "%s or %s",
                Joiner.on(", ").join(expected.subList(0, expected.size() - 1)),
                expected.get(expected.size() - 1));
      }
      throw new ParseException(
          String.format("Unexpected '%s'; expected %s", token.text(), expectedStr), token.start());
    }
  }

  private ProgramNode program() {
    // Read statements until EOF or "main"
    BlockNode statements = statements(matchesEofOrMain());

    // It's restrictive: must have main at the bottom of the file. Sorry/not sorry.
    if (token.type() == Token.Type.EOF) {
      return new ProgramNode(statements);
    } else if (token.type() == Token.Type.MAIN) {
      // if main, process main
      Token start = token;
      advance(); // eat the main
      // TODO: parse arguments
      BlockNode mainBlock = block();
      expect(Token.Type.EOF);
      MainNode mainProc = new MainNode(mainBlock, start.start());
      return new ProgramNode(statements, mainProc);
    }
    expect(Token.Type.MAIN, Token.Type.EOF);
    return null;
  }

  private BlockNode statements(Function<Token, Boolean> matcher) {
    List<StatementNode> children = new ArrayList<>();
    Position start = token.start();
    while (!matcher.apply(token)) {
      StatementNode child = statement();
      children.add(child);
    }
    return new BlockNode(children, start);
  }

  private static Function<Token, Boolean> matchesEofOrMain() {
    return token -> {
      if (token.type() == Token.Type.EOF) {
        return true;
      }
      return token.type() == Token.Type.MAIN;
    };
  }

  // This is a statements node surrounded by braces.
  private BlockNode block() {
    expect(Token.Type.LBRACE);
    advance();

    BlockNode statements = statements(token -> token.type() == Token.Type.RBRACE);
    expect(Token.Type.RBRACE);
    advance();
    return statements;
  }

  private StatementNode statement() {
    switch (token.type()) {
      case PRINT:
      case PRINTLN:
        return print(token);

      case IF:
        return ifStmt(token);

      case WHILE:
        {
          inWhile++;
          WhileNode whileStmt = whileStmt(token);
          inWhile--;
          return whileStmt;
        }

      case BREAK:
        if (inWhile == 0) {
          throw new ParseException("BREAK not found in WHILE block", token.start());
        }
        advance();
        return new BreakNode(token.start());

      case CONTINUE:
        if (inWhile == 0) {
          throw new ParseException("CONTINUE not found in WHILE block", token.start());
        }
        advance();
        return new ContinueNode(token.start());

      case RETURN:
        advance();
        return returnStmt(token.start());

      case EXIT:
        advance();
        return exitStmt(token.start());

      case VARIABLE:
        return startsWithVariableStmt();

      default:
        throw new ParseException(
            String.format("Unexpected start of statement '%s'", token.text()), token.start());
    }
  }

  private ReturnNode returnStmt(Position start) {
    // If it's the start of an expression, read the whole expression...
    if (EXPRESSION_STARTS.contains(token.type())) {
      return new ReturnNode(start, expr());
    }
    // ...else it returns void.
    return new ReturnNode(start);
  }

  private ExitNode exitStmt(Position start) {
    // If it's the start of an expression, read the whole expression...
    if (EXPRESSION_STARTS.contains(token.type())) {
      return new ExitNode(start, expr());
    }
    // ...else it returns void.
    return new ExitNode(start);
  }

  /**
   * Parse a statement starting with a variable name: either an assignment, variable declaration or
   * procedure call statement.
   */
  private StatementNode startsWithVariableStmt() {
    expect(Token.Type.VARIABLE);

    Token variable = token;
    advance(); // eat the variable name
    switch (token.type()) {
        // Assignment
      case EQ:
        advance(); // eat the =
        VariableNode var = new VariableNode(variable.text(), variable.start());
        ExprNode expr = expr();
        return new AssignmentNode(var, expr);

        // Declaration
      case COLON:
        return declaration(variable);

        // Procedure call statement
      case LPAREN:
        return procedureCall(variable, true);

        // TODO: dot for record field set
        // TODO: bracket for string or array slot assignment
      default:
        break;
    }
    throw new ParseException(
        String.format("Unexpected '%s'; expected '=' or ':'", token.text()), token.start());
  }

  private DeclarationNode declaration(Token varToken) {
    expect(Token.Type.COLON);
    advance(); // eat the colon
    if (token.type().isKeyword()) {
      Token.Type declaredType = token.type();

      if (declaredType == Token.Type.RECORD) {
        return parseRecordDefinition(varToken);
      }

      VarType varType = BUILTINS.get(declaredType);
      if (varType != null) {
        advance(); // int, string, bool, proc
        if (varType == VarType.PROC) {
          return procedureDecl(varToken);
        } else {
          // See if it's an array declaration and build a "compound type" from the
          // declaration, e.g., "array of int"
          if (token.type() == Token.Type.LBRACKET) {
            return arrayDecl(varToken, varType);
          }
          return new DeclarationNode(varToken.text(), varType, varToken.start());
        }
      }
    } else if (token.type() == Token.Type.VARIABLE) {
      Token typeToken = token;
      advance(); // eat the variable type record reference
      return new DeclarationNode(
          varToken.text(), new RecordReferenceType(typeToken.text()), varToken.start());
    }
    throw new ParseException(
        String.format(
            "Unexpected '%s' in declaration; expected INT, BOOL, STRING, PROC or RECORD",
            token.text()),
        token.start());
  }

  private DeclarationNode parseRecordDefinition(Token varToken) {
    expect(Token.Type.RECORD);
    advance(); // eat "record"
    expect(Token.Type.LBRACE);
    advance();

    // read field declarations
    List<DeclarationNode> fieldNodes = new ArrayList<>();
    while (token.type() != Token.Type.RBRACE) {
      expect(Token.Type.VARIABLE);
      Token fieldVar = token;
      advance(); // eat the variable.
      DeclarationNode decl = declaration(fieldVar);
      fieldNodes.add(decl);
    }

    expect(Token.Type.RBRACE);
    advance();
    List<Field> fields =
        fieldNodes
            .stream()
            .map(node -> new Field(node.name(), node.varType()))
            .collect(toImmutableList());
    RecordType recordType = new RecordType(varToken.text(), fields);

    return new DeclarationNode(varToken.text(), recordType, varToken.start());
  }

  /** declaration -> '[' expr ']' */
  private DeclarationNode arrayDecl(Token varToken, VarType baseVarType) {
    expect(Token.Type.LBRACKET);
    advance();
    // The size can be variable.
    ExprNode arraySize = expr();
    ArrayType arrayType = new ArrayType(baseVarType);
    expect(Token.Type.RBRACKET);
    advance();

    return new ArrayDeclarationNode(varToken.text(), arrayType, varToken.start(), arraySize);
  }

  private ProcedureNode procedureDecl(Token varToken) {
    List<Parameter> params = formalParams();

    VarType returnType = VarType.VOID;
    if (token.type() == Token.Type.COLON) {
      returnType = parseVarType();
    }
    BlockNode statements = block();
    return new ProcedureNode(varToken.text(), params, returnType, statements, varToken.start());
  }

  private List<Parameter> formalParams() {
    List<Parameter> params = new ArrayList<>();
    if (token.type() != Token.Type.LPAREN) {
      return params;
    }
    advance(); // eat the left paren

    if (token.type() == Token.Type.RPAREN) {
      advance(); // eat the right paren, and done.
      return params;
    }

    params = commaSeparated(() -> formalParam());
    expect(Token.Type.RPAREN);
    advance(); // eat the right paren.
    return params;
  }

  /**
   * Parses colon followed by var type. Works for INT, BOOL, STRING, record. Does NOT work for
   * arrays or procs yet.
   */
  private VarType parseVarType() {
    expect(Token.Type.COLON);
    advance();
    if (token.type() == Token.Type.VARIABLE) {
      Token typeToken = token;
      advance(); // eat the record type
      return new RecordReferenceType(typeToken.text());
    }

    Token.Type declaredType = token.type();
    VarType paramType = BUILTINS.get(declaredType);
    if (paramType != null && paramType != VarType.PROC) {
      // We have a param type
      advance(); // eat the param type
      return paramType;
    }
    throw new ParseException(
        String.format("Unexpected '%s'; expected INT, BOOL, STRING or record type", token.text()),
        token.start());
  }

  private Parameter formalParam() {
    expect(Token.Type.VARIABLE);

    Token paramName = token;
    advance();
    if (token.type() == Token.Type.COLON) {
      VarType paramType = parseVarType();
      //      advance();
      //      if (!token.type().isKeyword()) {
      //        throw new ParseException(
      //            String.format("Unexpected '%s'; expected INT, BOOL or STRING", token.text()),
      //            token.start());
      //      }
      //      Token.Type declaredType = token.type();
      //      VarType paramType = BUILTINS.get(declaredType);
      //      // TODO: Relax this for records.
      //      if (paramType == null) {
      //        throw new ParseException(
      //            String.format("Unexpected '%s'; expected INT, BOOL or STRING", token.text()),
      //            token.start());
      //      } else {
      //        // We have a param type
      //        advance(); // eat the param type
      return new Parameter(paramName.text(), paramType);
      //      }
    } else {
      // no colon, just an unknown param type
      return new Parameter(paramName.text());
    }
  }

  private PrintNode print(Token printToken) {
    assert (printToken.type() == Token.Type.PRINT || printToken.type() == Token.Type.PRINTLN);
    advance();
    ExprNode expr = expr();
    return new PrintNode(expr, printToken.start(), printToken.type() == Token.Type.PRINTLN);
  }

  private IfNode ifStmt(Token kt) {
    expect(Token.Type.IF);
    advance();

    List<IfNode.Case> cases = new ArrayList<>();

    ExprNode condition = expr();
    BlockNode statements = block();
    cases.add(new IfNode.Case(condition, statements));

    BlockNode elseStatements = null;
    // TODO: This is weird, why do we care if it's a keyword?!
    if (token.type().isKeyword()) {
      Token elseOrElif = token;
      // while elif: get condition, get statements, add to case list.
      while (elseOrElif != null && elseOrElif.type() == Token.Type.ELIF) {
        advance();

        Node elifCondition = expr();
        Node elifStatements = block();
        cases.add(new IfNode.Case(elifCondition, (BlockNode) elifStatements));

        // TODO: What?!
        if (token.type().isKeyword()) {
          elseOrElif = token;
        } else {
          elseOrElif = null;
        }
      }

      if (elseOrElif != null && elseOrElif.type() == Token.Type.ELSE) {
        advance();
        elseStatements = block();
      }
    }

    return new IfNode(cases, elseStatements, kt.start());
  }

  private WhileNode whileStmt(Token kt) {
    expect(Token.Type.WHILE);
    advance();
    ExprNode condition = expr();
    Optional<StatementNode> doStatement = Optional.empty();
    if (token.type() == Token.Type.DO) {
      advance();
      doStatement = Optional.of(statement());
    }
    BlockNode block = block();
    return new WhileNode(condition, doStatement, block, kt.start());
  }

  private CallNode procedureCall(Token varToken, boolean isStatement) {
    expect(Token.Type.LPAREN);
    advance(); // eat the lparen

    List<ExprNode> actuals;
    if (token.type() == Token.Type.RPAREN) {
      actuals = ImmutableList.of();
    } else {
      actuals = commaSeparatedExpressions();
    }

    expect(Token.Type.RPAREN);
    advance(); // eat the rparen

    return new CallNode(varToken.start(), varToken.text(), actuals, isStatement);
  }

  private List<ExprNode> commaSeparatedExpressions() {
    return commaSeparated(() -> expr());
  }

  private <T> List<T> commaSeparated(Supplier<T> nextNode) {
    List<T> nodes = new ArrayList<>();

    T node = nextNode.get();
    nodes.add(node);
    if (token.type() == Token.Type.COMMA) {
      // There's another entry in this list - let's go
      advance();

      while (token.type() != Token.Type.EOF) {
        node = nextNode.get();
        nodes.add(node);

        if (token.type() == Token.Type.COMMA) {
          advance(); // eat the comma
        } else {
          break;
        }
      }
    }
    return nodes;
  }

  /** EXPRESSIONS */
  private ExprNode expr() {
    return boolOr();
  }

  private ExprNode boolOr() {
    return binOpFn(ImmutableSet.of(Token.Type.OR, Token.Type.BIT_OR), () -> boolXor());
  }

  private ExprNode boolXor() {
    return binOpFn(ImmutableSet.of(Token.Type.XOR, Token.Type.BIT_XOR), () -> boolAnd());
  }

  private ExprNode boolAnd() {
    return binOpFn(ImmutableSet.of(Token.Type.AND, Token.Type.BIT_AND), () -> compare());
  }

  private ExprNode compare() {
    // Fun fact, in Java, == and != have higher precedence than <, >, <=, >=
    return binOpFn(
        ImmutableSet.of(
            Token.Type.EQEQ,
            Token.Type.NEQ,
            Token.Type.GT,
            Token.Type.LT,
            Token.Type.GEQ,
            Token.Type.LEQ),
        () -> shift());
  }

  private ExprNode shift() {
    return binOpFn(ImmutableSet.of(Token.Type.SHIFT_LEFT, Token.Type.SHIFT_RIGHT), () -> addSub());
  }

  private ExprNode addSub() {
    return binOpFn(ImmutableSet.of(Token.Type.PLUS, Token.Type.MINUS), () -> mulDiv());
  }

  private ExprNode mulDiv() {
    return binOpFn(ImmutableSet.of(Token.Type.MULT, Token.Type.DIV, Token.Type.MOD), () -> unary());
  }

  /**
   * Parse from the current location, repeatedly call "nextRule", e.g.,:
   *
   * <p>here -> nextRule (tokentype nextRule)*
   *
   * <p>where tokentype is in tokenTypes
   *
   * <p>Example, in the grammar:
   *
   * <p>expr -> term (+- term)*
   */
  private ExprNode binOpFn(Set<Token.Type> tokenTypes, Supplier<ExprNode> nextRule) {
    ExprNode left = nextRule.get();

    while (tokenTypes.contains(token.type())) {
      Token.Type operator = token.type();
      advance();
      ExprNode right = nextRule.get();
      left = new BinOpNode(left, operator, right);
    }

    return left;
  }

  private ExprNode unary() {
    Token unaryToken = token;
    if (token.type() == Token.Type.MINUS
        || token.type() == Token.Type.PLUS
        || token.type() == Token.Type.BIT_NOT
        || token.type() == Token.Type.NOT) {
      advance();
      ExprNode expr = unary(); // should this be expr? unary? atom?

      if (expr.varType() == VarType.INT) {
        // We can simplify now
        if (unaryToken.type() == Token.Type.PLUS) {
          return expr;
        } else if (unaryToken.type() == Token.Type.MINUS) {
          @SuppressWarnings("unchecked")
          ConstNode<Integer> in = (ConstNode<Integer>) expr;
          return new ConstNode<Integer>(-in.value(), VarType.INT, unaryToken.start());
        } else if (unaryToken.type() == Token.Type.BIT_NOT) {
          @SuppressWarnings("unchecked")
          ConstNode<Integer> in = (ConstNode<Integer>) expr;
          return new ConstNode<Integer>(~in.value(), VarType.INT, unaryToken.start());
        }
      } else if (expr.varType() == VarType.BOOL) {
        if (unaryToken.type() == Token.Type.NOT) {
          @SuppressWarnings("unchecked")
          ConstNode<Boolean> cn = (ConstNode<Boolean>) expr;
          return new ConstNode<Boolean>(!cn.value(), VarType.BOOL, unaryToken.start());
        }
      }

      return new UnaryNode(unaryToken.type(), expr, unaryToken.start());
    } else if (isUnaryKeyword(token)) {
      Token keywordToken = unaryToken;

      advance();
      expect(Token.Type.LPAREN);
      advance();
      ExprNode expr = expr();
      expect(Token.Type.RPAREN);
      advance();

      return new UnaryNode(keywordToken.type(), expr, unaryToken.start());
    }
    return arrayGet();
  }

  private static boolean isUnaryKeyword(Token token) {
    return UNARY_KEYWORDS.contains(token.type());
  }

  /**
   * Parse an (optional) array reference.
   *
   * <pre>
   * arrayGet -> atom ('[' expr ']')
   * </pre>
   */
  private ExprNode arrayGet() {
    ExprNode left = atom();

    // TODO(#38): Support multidimensional arrays (switch to "while" instead of "if")
    if (token.type() == Token.Type.LBRACKET) {
      advance();
      ExprNode index = expr();
      expect(Token.Type.RBRACKET);
      advance();

      left = new BinOpNode(left, Token.Type.LBRACKET, index);
    }

    return left;
  }

  /**
   * Parse an atom: a literal, variable or parenthesized expression.
   *
   * <pre>
   * atom -> int | true | false | string | variable | procedureCall | '(' expr ')' |
   *    '[' arrayLiteral ']'
   * </pre>
   */
  private ExprNode atom() {
    if (token.type() == Token.Type.INT) {
      IntToken it = (IntToken) token;
      advance();
      return new ConstNode<Integer>(it.value(), VarType.INT, it.start());
    } else if (token.type() == Token.Type.TRUE || token.type() == Token.Type.FALSE) {
      Token bt = token;
      advance();
      return new ConstNode<Boolean>(bt.type() == Token.Type.TRUE, VarType.BOOL, bt.start());
    } else if (token.type() == Token.Type.STRING) {
      Token st = token;
      advance();
      return new ConstNode<String>(st.text(), VarType.STRING, st.start());
    } else if (token.type() == Token.Type.VARIABLE) {
      Token varToken = token;
      String name = token.text();
      advance();
      if (token.type() == Token.Type.LPAREN) {
        return procedureCall(varToken, false);
      } else {
        return new VariableNode(name, varToken.start());
      }
    } else if (token.type() == Token.Type.LPAREN) {
      advance();
      ExprNode expr = expr();
      expect(Token.Type.RPAREN);
      advance();
      return expr;
    } else if (token.type() == Token.Type.LBRACKET) {
      // array literal
      return arrayLiteral();
    } else if (token.type() == Token.Type.INPUT) {
      Token it = token;
      advance();
      return new InputNode(it.start());
    } else {
      throw new ParseException(
          String.format("Unexpected '%s'; expected literal, variable or '('", token.text()),
          token.start());
    }
  }

  /** Parse an array constant/literal. */
  private ExprNode arrayLiteral() {
    expect(Token.Type.LBRACKET);

    Token openBracket = token;
    advance(); // eat left bracket

    // Future version:
    // List<ExprNode> values = commaSeparatedExpressions();

    // For first iteration: only allow const int[] or const string[] or const bool[]
    if (token.type() != Token.Type.RBRACKET) {
      List<ConstNode<?>> values =
          commaSeparated(
              () -> {
                ExprNode atom = atom();
                if (atom instanceof ConstNode) {
                  return (ConstNode<?>) atom;
                }

                throw new ParseException(
                    String.format(
                        "Illegal entry %s in array literal; only scalar literals allowed",
                        token.text()),
                    token.start());
              });

      expect(Token.Type.RBRACKET);
      advance(); // eat rbracket

      VarType baseType = values.get(0).varType();
      ArrayType arrayType = new ArrayType(baseType);

      // TODO: when the values are expressions, check all values to make sure they're the same type
      // in the static checker.
      for (int i = 1; i < values.size(); ++i) {
        if (!values.get(i).varType().equals(baseType)) {
          throw new ParseException(
              String.format(
                  "Inconsistent types in array literal; first was %s but one was %s",
                  baseType, values.get(i).varType()),
              openBracket.start());
        }
      }
      if (baseType == VarType.BOOL) {
        Boolean[] valuesArray = values.stream().map(node -> node.value()).toArray(Boolean[]::new);
        return new ConstNode<Boolean[]>(valuesArray, arrayType, openBracket.start());
      } else if (baseType == VarType.STRING) {
        String[] valuesArray = values.stream().map(node -> node.value()).toArray(String[]::new);
        return new ConstNode<String[]>(valuesArray, arrayType, openBracket.start());
      } else if (baseType == VarType.INT) {
        Integer[] valuesArray = values.stream().map(node -> node.value()).toArray(Integer[]::new);
        return new ConstNode<Integer[]>(valuesArray, arrayType, openBracket.start());
      }
      throw new ParseException(
          String.format(
              "Illegal type %s in array literal; only scalar literals allowed",
              baseType.toString()),
          openBracket.start());
    } else {
      // empty array constants
      // will this ever be allowed?
      throw new ParseException("Empty array constants are not allowed yet", token.start());
    }
  }
}
