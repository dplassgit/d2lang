package com.plasstech.lang.d2.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.lex.IntToken;
import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.lex.Token;
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
import com.plasstech.lang.d2.parse.node.FieldSetNode;
import com.plasstech.lang.d2.parse.node.IfNode;
import com.plasstech.lang.d2.parse.node.InputNode;
import com.plasstech.lang.d2.parse.node.MainNode;
import com.plasstech.lang.d2.parse.node.NewNode;
import com.plasstech.lang.d2.parse.node.Node;
import com.plasstech.lang.d2.parse.node.PrintNode;
import com.plasstech.lang.d2.parse.node.ProcedureNode;
import com.plasstech.lang.d2.parse.node.ProcedureNode.Parameter;
import com.plasstech.lang.d2.parse.node.ProgramNode;
import com.plasstech.lang.d2.parse.node.RecordDeclarationNode;
import com.plasstech.lang.d2.parse.node.ReturnNode;
import com.plasstech.lang.d2.parse.node.StatementNode;
import com.plasstech.lang.d2.parse.node.UnaryNode;
import com.plasstech.lang.d2.parse.node.VariableNode;
import com.plasstech.lang.d2.parse.node.VariableSetNode;
import com.plasstech.lang.d2.parse.node.WhileNode;
import com.plasstech.lang.d2.phase.Phase;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.type.ArrayType;
import com.plasstech.lang.d2.type.RecordReferenceType;
import com.plasstech.lang.d2.type.VarType;

public class Parser implements Phase {

  private static final ImmutableMap<TokenType, VarType> BUILTINS =
      ImmutableMap.of(
          TokenType.INT, VarType.INT,
          TokenType.BOOL, VarType.BOOL,
          TokenType.STRING, VarType.STRING,
          TokenType.PROC, VarType.PROC,
          TokenType.NULL, VarType.NULL);

  private static final Set<TokenType> EXPRESSION_STARTS =
      ImmutableSet.of(
          TokenType.VARIABLE,
          TokenType.LPAREN,
          TokenType.MINUS,
          TokenType.PLUS,
          TokenType.NOT,
          TokenType.BIT_NOT,
          TokenType.INT,
          TokenType.STRING,
          TokenType.BOOL,
          TokenType.TRUE,
          TokenType.FALSE,
          TokenType.LENGTH,
          TokenType.ASC,
          TokenType.CHR,
          TokenType.NULL,
          TokenType.NEW);

  private static Set<TokenType> UNARY_KEYWORDS =
      ImmutableSet.of(TokenType.LENGTH, TokenType.ASC, TokenType.CHR);

  private final Lexer lexer;
  private Token token;
  private int inWhile;

  public Parser(Lexer lexer) {
    this.lexer = lexer;
    this.advance();
  }

  private Token advance() {
    Token prev = token;
    token = lexer.nextToken();
    return prev;
  }

  @Override
  public State execute(State input) {
    try {
      ProgramNode node = parse();
      return input.addProgramNode(node);
    } catch (ParseException pe) {
      return input.addException(pe);
    }
  }

  private ProgramNode parse() {
    return program();
  }

  private void expect(TokenType first, TokenType... rest) {
    ImmutableList<TokenType> expected = ImmutableList.copyOf(Lists.asList(first, rest));
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
    if (token.type() == TokenType.EOF) {
      return new ProgramNode(statements);
    } else if (token.type() == TokenType.MAIN) {
      // if main, process main
      Token start = token;
      advance(); // eat the main
      // TODO: parse arguments
      BlockNode mainBlock = block();
      expect(TokenType.EOF);
      MainNode mainProc = new MainNode(mainBlock, start.start());
      return new ProgramNode(statements, mainProc);
    }
    expect(TokenType.MAIN, TokenType.EOF);
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
      if (token.type() == TokenType.EOF) {
        return true;
      }
      return token.type() == TokenType.MAIN;
    };
  }

  // This is a statements node surrounded by braces.
  private BlockNode block() {
    expect(TokenType.LBRACE);
    advance();

    BlockNode statements = statements(token -> token.type() == TokenType.RBRACE);
    expect(TokenType.RBRACE);
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
    expect(TokenType.VARIABLE);

    Token variable = advance(); // eat the variable name
    switch (token.type()) {
        // Assignment (variable=expression)
      case EQ:
        advance(); // eat the =
        VariableSetNode var = new VariableSetNode(variable.text(), variable.start());
        ExprNode expr = expr();
        return new AssignmentNode(var, expr);

        // Declaration (variable:type)
      case COLON:
        return declaration(variable);

        // Procedure call statement (variable(comma-separated-list))
      case LPAREN:
        return procedureCall(variable, true);

        // for record field set (field.name=expression)
      case DOT:
        return fieldAssignment(variable);

        //  bracket for array slot assignment (field[expression] = expression)
      case LBRACKET:
        return arraySlotAssignment(variable);

      default:
        break;
    }
    throw new ParseException(
        String.format("Unexpected '%s'; expected '=' or ':'", token.text()), token.start());
  }

  private StatementNode arraySlotAssignment(Token variable) {
    expect(TokenType.LBRACKET);
    advance(); // eat the [
    // now get an expression
    ExprNode indexNode = expr();
    ArraySetNode asn = new ArraySetNode(variable.text(), indexNode, variable.start());

    expect(TokenType.RBRACKET);
    advance(); // eat the ]

    expect(TokenType.EQ);
    advance();
    ExprNode rhs = expr();

    return new AssignmentNode(asn, rhs);
  }

  private StatementNode fieldAssignment(Token variable) {
    expect(TokenType.DOT);
    advance(); // eat the .

    expect(TokenType.VARIABLE);
    Token fieldName = advance();
    FieldSetNode fsn = new FieldSetNode(variable.text(), fieldName.text(), variable.start());
    expect(TokenType.EQ);
    advance();
    ExprNode rhs = expr();

    return new AssignmentNode(fsn, rhs);
  }

  private DeclarationNode declaration(Token varToken) {
    expect(TokenType.COLON);
    advance(); // eat the colon
    if (token.type().isKeyword()) {
      TokenType declaredType = token.type();

      if (declaredType == TokenType.RECORD) {
        return parseRecordDeclaration(varToken);
      }

      VarType varType = BUILTINS.get(declaredType);
      if (varType != null) {
        advance(); // int, string, bool, proc
        if (varType == VarType.PROC) {
          return procedureDecl(varToken);
        } else {
          // See if it's an array declaration and build a "compound type" from the
          // declaration, e.g., "array of int"
          if (token.type() == TokenType.LBRACKET) {
            return arrayDecl(varToken, varType);
          }
          return new DeclarationNode(varToken.text(), varType, varToken.start());
        }
      }
    } else if (token.type() == TokenType.VARIABLE) {
      Token typeToken = advance(); // eat the variable type record reference
      // TODO: allow arrays of records!
      return new DeclarationNode(
          varToken.text(), new RecordReferenceType(typeToken.text()), varToken.start());
    }
    throw new ParseException(
        String.format(
            "Unexpected '%s' in declaration; expected INT, BOOL, STRING, PROC or RECORD",
            token.text()),
        token.start());
  }

  private DeclarationNode parseRecordDeclaration(Token varToken) {
    expect(TokenType.RECORD);
    advance(); // eat "record"
    expect(TokenType.LBRACE);
    advance();

    // read field declarations
    List<DeclarationNode> fieldNodes = new ArrayList<>();
    while (token.type() != TokenType.RBRACE) {
      expect(TokenType.VARIABLE);
      Token fieldVar = advance(); // eat the variable.
      DeclarationNode decl = declaration(fieldVar);
      fieldNodes.add(decl);
    }

    expect(TokenType.RBRACE);
    advance();
    return new RecordDeclarationNode(varToken.text(), fieldNodes, varToken.start());
  }

  /** declaration -> '[' expr ']' */
  private DeclarationNode arrayDecl(Token varToken, VarType baseVarType) {
    /** while... (dimensions) */
    expect(TokenType.LBRACKET);
    advance();
    // The size can be variable.
    ExprNode arraySize = expr();
    ArrayType arrayType = new ArrayType(baseVarType);
    expect(TokenType.RBRACKET);
    advance();

    return new ArrayDeclarationNode(varToken.text(), arrayType, varToken.start(), arraySize);
  }

  private ProcedureNode procedureDecl(Token varToken) {
    List<Parameter> params = formalParams();

    VarType returnType = VarType.VOID;
    if (token.type() == TokenType.COLON) {
      returnType = parseVarType();
    }
    BlockNode statements = block();
    return new ProcedureNode(varToken.text(), params, returnType, statements, varToken.start());
  }

  private List<Parameter> formalParams() {
    List<Parameter> params = new ArrayList<>();
    if (token.type() != TokenType.LPAREN) {
      return params;
    }
    advance(); // eat the left paren

    if (token.type() == TokenType.RPAREN) {
      advance(); // eat the right paren, and done.
      return params;
    }

    params = commaSeparated(() -> formalParam());
    expect(TokenType.RPAREN);
    advance(); // eat the right paren.
    return params;
  }

  /**
   * Parses colon followed by var type. Works for INT, BOOL, STRING, record. Does NOT work for
   * arrays or procs yet.
   */
  private VarType parseVarType() {
    expect(TokenType.COLON);
    advance();
    if (token.type() == TokenType.VARIABLE) {
      Token typeToken = advance(); // eat the record type
      return new RecordReferenceType(typeToken.text());
    }

    TokenType declaredType = token.type();
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
    expect(TokenType.VARIABLE);

    Token paramName = advance();
    if (token.type() == TokenType.COLON) {
      VarType paramType = parseVarType();
      return new Parameter(paramName.text(), paramType, paramName.start());
    } else {
      // no colon, just an unknown param type
      return new Parameter(paramName.text(), paramName.start());
    }
  }

  private PrintNode print(Token printToken) {
    assert (printToken.type() == TokenType.PRINT || printToken.type() == TokenType.PRINTLN);
    advance();
    ExprNode expr = expr();
    return new PrintNode(expr, printToken.start(), printToken.type() == TokenType.PRINTLN);
  }

  private IfNode ifStmt(Token kt) {
    expect(TokenType.IF);
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
      while (elseOrElif != null && elseOrElif.type() == TokenType.ELIF) {
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

      if (elseOrElif != null && elseOrElif.type() == TokenType.ELSE) {
        advance();
        elseStatements = block();
      }
    }

    return new IfNode(cases, elseStatements, kt.start());
  }

  private WhileNode whileStmt(Token kt) {
    expect(TokenType.WHILE);
    advance();
    ExprNode condition = expr();
    Optional<StatementNode> doStatement = Optional.empty();
    if (token.type() == TokenType.DO) {
      advance();
      doStatement = Optional.of(statement());
    }
    BlockNode block = block();
    return new WhileNode(condition, doStatement, block, kt.start());
  }

  private CallNode procedureCall(Token varToken, boolean isStatement) {
    expect(TokenType.LPAREN);
    advance(); // eat the lparen

    List<ExprNode> actuals;
    if (token.type() == TokenType.RPAREN) {
      actuals = ImmutableList.of();
    } else {
      actuals = commaSeparatedExpressions();
    }

    expect(TokenType.RPAREN);
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
    if (token.type() == TokenType.COMMA) {
      // There's another entry in this list - let's go
      advance();

      while (token.type() != TokenType.EOF) {
        node = nextNode.get();
        nodes.add(node);

        if (token.type() == TokenType.COMMA) {
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
    return binOpFn(ImmutableSet.of(TokenType.OR, TokenType.BIT_OR), () -> boolXor());
  }

  private ExprNode boolXor() {
    return binOpFn(ImmutableSet.of(TokenType.XOR, TokenType.BIT_XOR), () -> boolAnd());
  }

  private ExprNode boolAnd() {
    return binOpFn(ImmutableSet.of(TokenType.AND, TokenType.BIT_AND), () -> compare());
  }

  private ExprNode compare() {
    // Fun fact, in Java, == and != have higher precedence than <, >, <=, >=
    return binOpFn(
        ImmutableSet.of(
            TokenType.EQEQ,
            TokenType.NEQ,
            TokenType.GT,
            TokenType.LT,
            TokenType.GEQ,
            TokenType.LEQ),
        () -> shift());
  }

  private ExprNode shift() {
    return binOpFn(ImmutableSet.of(TokenType.SHIFT_LEFT, TokenType.SHIFT_RIGHT), () -> addSub());
  }

  private ExprNode addSub() {
    return binOpFn(ImmutableSet.of(TokenType.PLUS, TokenType.MINUS), () -> mulDiv());
  }

  private ExprNode mulDiv() {
    return binOpFn(ImmutableSet.of(TokenType.MULT, TokenType.DIV, TokenType.MOD), () -> unary());
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
  private ExprNode binOpFn(Set<TokenType> tokenTypes, Supplier<ExprNode> nextRule) {
    ExprNode left = nextRule.get();

    while (tokenTypes.contains(token.type())) {
      TokenType operator = token.type();
      advance();
      ExprNode right = nextRule.get();
      left = new BinOpNode(left, operator, right);
    }

    return left;
  }

  private ExprNode unary() {
    Token unaryToken = token;
    if (token.type() == TokenType.MINUS
        || token.type() == TokenType.PLUS
        || token.type() == TokenType.BIT_NOT
        || token.type() == TokenType.NOT) {
      advance();
      ExprNode expr = unary(); // should this be expr? unary? atom?

      if (expr.varType() == VarType.INT) {
        // We can simplify now
        if (unaryToken.type() == TokenType.PLUS) {
          return expr;
        } else if (unaryToken.type() == TokenType.MINUS) {
          @SuppressWarnings("unchecked")
          ConstNode<Integer> in = (ConstNode<Integer>) expr;
          return new ConstNode<Integer>(-in.value(), VarType.INT, unaryToken.start());
        } else if (unaryToken.type() == TokenType.BIT_NOT) {
          @SuppressWarnings("unchecked")
          ConstNode<Integer> in = (ConstNode<Integer>) expr;
          return new ConstNode<Integer>(~in.value(), VarType.INT, unaryToken.start());
        }
      } else if (expr.varType() == VarType.BOOL) {
        if (unaryToken.type() == TokenType.NOT) {
          @SuppressWarnings("unchecked")
          ConstNode<Boolean> cn = (ConstNode<Boolean>) expr;
          return new ConstNode<Boolean>(!cn.value(), VarType.BOOL, unaryToken.start());
        }
      }

      return new UnaryNode(unaryToken.type(), expr, unaryToken.start());
    } else if (isUnaryKeyword(token)) {
      Token keywordToken = unaryToken;

      advance();
      expect(TokenType.LPAREN);
      advance();
      ExprNode expr = expr();
      expect(TokenType.RPAREN);
      advance();

      return new UnaryNode(keywordToken.type(), expr, unaryToken.start());
    } else if (token.type() == TokenType.NEW) {
      Position start = token.start();
      advance();
      expect(TokenType.VARIABLE);
      Token recordTypeName = advance();
      return new NewNode(recordTypeName.text(), start);
    }

    return compositeDereference();
  }

  private static boolean isUnaryKeyword(Token token) {
    return UNARY_KEYWORDS.contains(token.type());
  }

  /**
   * Parse an (optional) composite dereference.
   *
   * <pre>
   * composite dereference -> atom ('[' expr ']') | atom ('.' expr)
   * <p>here -> nextRule (tokentype nextRule)*
   * </pre>
   */
  private ExprNode compositeDereference() {
    ExprNode left = atom();

    // TODO(#38): Support multidimensional arrays (switch to "while" instead of "if")
    if (token.type() == TokenType.LBRACKET) {
      advance();
      ExprNode index = expr();
      expect(TokenType.RBRACKET);
      advance();

      left = new BinOpNode(left, TokenType.LBRACKET, index);
    } else if (token.type() == TokenType.DOT) {

      while (token.type() == TokenType.DOT || token.type() == TokenType.LBRACKET) {
        TokenType operator = token.type();
        advance();
        // this is restrictive but I'm too dumb to figure out how to make it left-associative
        ExprNode right = atom();
        if (operator == TokenType.LBRACKET) {
          expect(TokenType.RBRACKET);
          advance();
        }
        left = new BinOpNode(left, operator, right);
      }
    }

    return left;
  }

  /**
   * Parse an atom: a literal, variable or parenthesized expression.
   *
   * <pre>
   * atom -> int | true | false | string | null | variable | procedureCall | '(' expr ')' |
   *    '[' arrayLiteral ']'
   * </pre>
   */
  private ExprNode atom() {
    if (token.type() == TokenType.INT) {
      IntToken it = (IntToken) token;
      advance();
      return new ConstNode<Integer>(it.value(), VarType.INT, it.start());
    } else if (token.type() == TokenType.TRUE || token.type() == TokenType.FALSE) {
      Token bt = advance();
      return new ConstNode<Boolean>(bt.type() == TokenType.TRUE, VarType.BOOL, bt.start());
    } else if (token.type() == TokenType.STRING) {
      Token st = advance();
      return new ConstNode<String>(st.text(), VarType.STRING, st.start());
    } else if (token.type() == TokenType.NULL) {
      Token st = advance();
      return new ConstNode<Void>(null, VarType.NULL, st.start());
    } else if (token.type() == TokenType.VARIABLE) {
      Token varToken = advance();
      if (token.type() == TokenType.LPAREN) {
        return procedureCall(varToken, false);
      } else {
        String name = varToken.text();
        return new VariableNode(name, varToken.start());
      }
    } else if (token.type() == TokenType.LPAREN) {
      advance();
      ExprNode expr = expr();
      expect(TokenType.RPAREN);
      advance();
      return expr;
    } else if (token.type() == TokenType.LBRACKET) {
      // array literal
      return arrayLiteral();
    } else if (token.type() == TokenType.INPUT) {
      Token it = advance();
      return new InputNode(it.start());
    } else {
      throw new ParseException(
          String.format("Unexpected '%s'; expected literal, variable or '('", token.text()),
          token.start());
    }
  }

  /** Parse an array constant/literal. */
  private ExprNode arrayLiteral() {
    expect(TokenType.LBRACKET);

    Token openBracket = advance(); // eat left bracket

    List<ExprNode> values = commaSeparatedExpressions();
    if (values.isEmpty()) {
      // will this ever be allowed?
      throw new ParseException("Empty array constants are not allowed yet", token.start());
    }

    expect(TokenType.RBRACKET);
    advance(); // eat rbracket

    // First implementation: find the first non-unknown value and use it
    Optional<VarType> baseType =
        values
            .stream()
            .map(Node::varType)
            .filter(varType -> varType != VarType.UNKNOWN)
            .findFirst();
    if (!baseType.isPresent()) {
      throw new ParseException(
          "Cannot determine type of array; all elements are UNKNOWN", openBracket.start());
    }
    // None are unknown now!
    for (ExprNode element : values) {
      if (element.varType() == VarType.UNKNOWN) {
        element.setVarType(baseType.get());
      }
    }

    return new ArrayLiteralNode(openBracket.start(), values, baseType.get());
  }
}
