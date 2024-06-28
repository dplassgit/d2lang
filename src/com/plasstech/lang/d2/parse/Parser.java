package com.plasstech.lang.d2.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.lex.ConstToken;
import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.lex.ScannerException;
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
import com.plasstech.lang.d2.parse.node.ExternProcedureNode;
import com.plasstech.lang.d2.parse.node.FieldSetNode;
import com.plasstech.lang.d2.parse.node.IfNode;
import com.plasstech.lang.d2.parse.node.IncDecNode;
import com.plasstech.lang.d2.parse.node.InputNode;
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
import com.plasstech.lang.d2.type.UnboundType;
import com.plasstech.lang.d2.type.VarType;

public class Parser implements Phase {

  private static final ImmutableMap<TokenType, VarType> VARIABLE_TYPES =
      ImmutableMap.<TokenType, VarType>builder()
          .put(TokenType.BOOL, VarType.BOOL)
          .put(TokenType.BYTE, VarType.BYTE)
          .put(TokenType.DOUBLE, VarType.DOUBLE)
          .put(TokenType.INT, VarType.INT)
          .put(TokenType.LONG, VarType.LONG)
          .put(TokenType.STRING, VarType.STRING)
          .put(TokenType.RANGE, VarType.RANGE)
          .build();

  private static final ImmutableMap<TokenType, VarType> RETURN_TYPES =
      ImmutableMap.<TokenType, VarType>builder().putAll(VARIABLE_TYPES)
          .put(TokenType.VOID, VarType.VOID)
          .build();

  private static final Set<TokenType> EXPRESSION_STARTS =
      ImmutableSet.of(
          TokenType.ARGS,
          TokenType.ASC,
          TokenType.BIT_NOT,
          TokenType.CHR,
          TokenType.FALSE,
          TokenType.INPUT,
          TokenType.LENGTH,
          TokenType.LITERAL,
          TokenType.LPAREN,
          TokenType.MINUS,
          TokenType.NEW,
          TokenType.NOT,
          TokenType.NULL,
          TokenType.PLUS,
          TokenType.TRUE,
          TokenType.VARIABLE);

  private static final Set<TokenType> UNARY_KEYWORDS =
      ImmutableSet.of(TokenType.LENGTH, TokenType.ASC, TokenType.CHR);

  private static final ImmutableMap<TokenType, TokenType> OP_EQ_TO_OP =
      ImmutableMap.of(TokenType.PLUS_EQ, TokenType.PLUS,
          TokenType.MINUS_EQ, TokenType.MINUS,
          TokenType.MULT_EQ, TokenType.MULT,
          TokenType.DIV_EQ, TokenType.DIV);

  private final Lexer lexer;
  private Token token;
  private int inWhile;
  private int inProc;

  public Parser(Lexer lexer) {
    this.lexer = lexer;
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
    } catch (ScannerException se) {
      return input.addException(se);
    } catch (ParseException pe) {
      return input.addException(pe);
    }
  }

  private ProgramNode parse() {
    this.advance();
    return program();
  }

  private Token expectToken(TokenType allowed) {
    if (allowed == token.type()) {
      return advance();
    }
    throw new ParseException(
        String.format("Unexpected '%s'; expected %s", token.text(), allowed),
        token.start());
  }

  private ProgramNode program() {
    // Read statements until EOF
    BlockNode statements = statements(matchesEof());

    return new ProgramNode(statements);
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

  private static Function<Token, Boolean> matchesEof() {
    return token -> token.type() == TokenType.EOF;
  }

  // This is a statements node surrounded by braces.
  private BlockNode block() {
    expectToken(TokenType.LBRACE);

    BlockNode statements = statements(token -> token.type() == TokenType.RBRACE);
    expectToken(TokenType.RBRACE);
    return statements;
  }

  private StatementNode statement() {
    switch (token.type()) {
      case BREAK:
        if (inWhile == 0) {
          throw new ParseException("BREAK found outside of WHILE block", token.start());
        }
        advance();
        return new BreakNode(token.start());

      case CONTINUE:
        if (inWhile == 0) {
          throw new ParseException("CONTINUE found outside of WHILE block", token.start());
        }
        advance();
        return new ContinueNode(token.start());

      case EXIT:
        return exitStmt(token.start());

      case IF:
        return ifStmt(token);

      case PRINT:
      case PRINTLN:
        return print(token);

      case RETURN:
        return returnStmt(token.start());

      case VARIABLE:
        return startsWithVariableStmt();

      case WHILE:
        inWhile++;
        WhileNode whileStmt = whileStmt(token);
        inWhile--;
        return whileStmt;

      default:
        throw new ParseException(
            String.format("Unexpected start of statement '%s'", token.text()), token.start());
    }
  }

  private ReturnNode returnStmt(Position start) {
    expectToken(TokenType.RETURN);
    if (inProc == 0) {
      throw new ParseException("Cannot RETURN from outside a PROC", start);
    }

    // If it's the start of an expression, read the whole expression...
    if (EXPRESSION_STARTS.contains(token.type())) {
      return new ReturnNode(start, expr());
    }
    // ...else it returns void.
    return new ReturnNode(start);
  }

  private ExitNode exitStmt(Position start) {
    expectToken(TokenType.EXIT);
    // If it's the start of an expression, read the whole expression...
    // (Except INPUT, which is forbidden.)
    if (token.type() == TokenType.INPUT) {
      throw new ParseException("Use of INPUT is not allowed in EXIT statements", token.start());
    }
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
    Token variable = expectToken(TokenType.VARIABLE);
    switch (token.type()) {
      // Assignment: variable=expression
      case ASSIGN:
        advance(); // eat the =
        VariableSetNode var = new VariableSetNode(variable.text(), variable.start());
        ExprNode expr = expr();
        return new AssignmentNode(var, expr);

      // Declaration: variable:type
      case COLON:
        return declaration(variable);

      case DECREMENT:
      case INCREMENT:
        boolean inc = token.type() == TokenType.INCREMENT;
        advance(); // eat the ++ or --
        VariableSetNode incVar = new VariableSetNode(variable.text(), variable.start());
        return new IncDecNode(incVar, inc);

      case PLUS_EQ:
      case MINUS_EQ:
      case MULT_EQ:
      case DIV_EQ:
        Token opEq = token;
        advance(); // eat the token
        VariableSetNode vsn = new VariableSetNode(variable.text(), variable.start());
        ExprNode rhs = expr();
        ExprNode left = new VariableNode(variable.text(), variable.start());
        // left += rhs => left = left + rhs
        ExprNode variableModifiedByRhs = new BinOpNode(left, OP_EQ_TO_OP.get(opEq.type()), rhs);
        return new AssignmentNode(vsn, variableModifiedByRhs);

      // for record field set: field.name=expression
      case DOT:
        return fieldAssignment(variable);

      // bracket for array slot assignment: field[expression] = expression
      case LBRACKET:
        return arraySlotAssignment(variable);

      // Procedure call: variable(comma-separated-list)
      case LPAREN:
        return procedureCall(variable, true);

      default:
        break;
    }
    throw new ParseException(
        String.format("Unexpected '%s'; expected '=' or ':'", token.text()), token.start());
  }

  private StatementNode arraySlotAssignment(Token variable) {
    expectToken(TokenType.LBRACKET);
    // now get an expression
    ExprNode indexNode = expr();
    ArraySetNode asn = new ArraySetNode(variable.text(), indexNode, variable.start());

    expectToken(TokenType.RBRACKET);

    expectToken(TokenType.ASSIGN);
    ExprNode rhs = expr();

    return new AssignmentNode(asn, rhs);
  }

  private StatementNode fieldAssignment(Token variable) {
    expectToken(TokenType.DOT);

    Token fieldName = expectToken(TokenType.VARIABLE);
    FieldSetNode fsn = new FieldSetNode(variable.text(), fieldName.text(), variable.start());
    expectToken(TokenType.ASSIGN);
    ExprNode rhs = expr();

    return new AssignmentNode(fsn, rhs);
  }

  private DeclarationNode declaration(Token varToken) {
    expectToken(TokenType.COLON);
    TokenType declaredType = token.type();

    switch (declaredType) {
      case RECORD:
        return parseRecordDeclaration(varToken);

      case PROC:
        return procedureDecl(varToken);

      case EXTERN:
        return externDecl(varToken);

      case BOOL:
      case BYTE:
      case DOUBLE:
      case INT:
      case LONG:
      case STRING:
      case RANGE:
        VarType varType = VARIABLE_TYPES.get(declaredType);
        assert (varType != null); // it should always find the varType in the map.
        advance();
        // See if it's an array declaration and build a "compound type" from the
        // declaration, e.g., "array of int"
        if (token.type() == TokenType.LBRACKET) {
          return arrayDecl(varToken, varType);
        }
        // Non-array declaration.
        return new DeclarationNode(varToken.text(), varType, varToken.start());

      case VARIABLE:
        Token typeToken = advance(); // eat the variable type record reference

        RecordReferenceType recordReference = new RecordReferenceType(typeToken.text());
        if (token.type() == TokenType.LBRACKET) {
          // Array of records!
          return arrayDecl(varToken, recordReference);
        }
        // Non-array declaration
        return new DeclarationNode(varToken.text(), recordReference, varToken.start());

      default:
        throw new ParseException(
            String.format(
                "Unexpected '%s' in declaration; expected built-in type, PROC or RECORD",
                token.text()),
            token.start());
    }
  }

  private DeclarationNode parseRecordDeclaration(Token varToken) {
    expectToken(TokenType.RECORD);

    List<String> formalTypeVariables = ImmutableList.of();
    if (token.type() == TokenType.LT) {
      advance(); // eat the <
      formalTypeVariables = commaSeparated(() -> {
        Token next = expectToken(TokenType.VARIABLE);
        String name = next.text();
        return name;
      });
      expectToken(TokenType.GT);
    }
    expectToken(TokenType.LBRACE);

    // read field declarations
    List<DeclarationNode> fieldNodes = new ArrayList<>();
    while (token.type() != TokenType.RBRACE) {
      Token fieldVar = expectToken(TokenType.VARIABLE);
      DeclarationNode decl = fieldDeclaration(fieldVar, formalTypeVariables);
      fieldNodes.add(decl);
    }

    expectToken(TokenType.RBRACE);
    return new RecordDeclarationNode(varToken.text(), fieldNodes, varToken.start(),
        formalTypeVariables);
  }

  private DeclarationNode fieldDeclaration(Token varToken, List<String> formalTypeVariables) {
    expectToken(TokenType.COLON);
    if (token.type().isKeyword()) {
      TokenType declaredType = token.type();
      VarType varType = VARIABLE_TYPES.get(declaredType);
      if (varType != null) {
        advance(); // int, string, bool, etc.
        // See if it's an array declaration and build a "compound type" from the
        // declaration, e.g., "array of int"
        if (token.type() == TokenType.LBRACKET) {
          return arrayDecl(varToken, varType);
        }
        return new DeclarationNode(varToken.text(), varType, varToken.start());
      }
    }
    if (token.type() == TokenType.VARIABLE) {
      Token typeToken = advance(); // eat the variable type record reference
      String name = typeToken.text();
      if (formalTypeVariables.contains(name)) {
        // Field type is from formal type parameter
        VarType varType = new UnboundType(name);
        // TODO: Arrays of unbound types not allowed yet
        return new DeclarationNode(varToken.text(), varType, varToken.start());
      }
      RecordReferenceType recordReference = new RecordReferenceType(name);
      if (token.type() == TokenType.LBRACKET) {
        // Array of records!
        return arrayDecl(varToken, recordReference);
      }
      return new DeclarationNode(varToken.text(), recordReference, varToken.start());
    }
    throw new ParseException(
        String.format(
            "Unexpected '%s' in RECORD declaration; expected built-in type or RECORD reference",
            token.text()),
        token.start());
  }

  /** declaration -> '[' expr ']' */
  private DeclarationNode arrayDecl(Token varToken, VarType baseVarType) {
    ArrayType arrayType = new ArrayType(baseVarType, 1);
    /** while... (dimensions) */
    expectToken(TokenType.LBRACKET);
    if (token.type() == TokenType.RBRACKET) {
      // array declaration (without size)
      expectToken(TokenType.RBRACKET);
      return new DeclarationNode(varToken.text(), arrayType, varToken.start());
    }
    // The size can be variable.
    ExprNode arraySize = expr();
    // TODO(#38): support multidimensional arrays
    expectToken(TokenType.RBRACKET);

    return new ArrayDeclarationNode(varToken.text(), arrayType, varToken.start(), arraySize);
  }

  private ProcedureNode procedureDecl(Token varToken) {
    inProc++;
    expectToken(TokenType.PROC);
    List<Parameter> params = formalParams();

    VarType returnType = VarType.VOID;
    if (token.type() == TokenType.COLON) {
      returnType = parseVarType(RETURN_TYPES);
    }
    BlockNode statements = block();
    inProc--;
    return new ProcedureNode(varToken.text(), params, returnType, statements, varToken.start());
  }

  private DeclarationNode externDecl(Token varToken) {
    expectToken(TokenType.EXTERN);
    expectToken(TokenType.PROC);

    List<Parameter> params = formalParams();

    VarType returnType = VarType.VOID;
    if (token.type() == TokenType.COLON) {
      returnType = parseVarType(RETURN_TYPES);
    }
    return new ExternProcedureNode(varToken.text(), params, returnType, varToken.start());
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
    expectToken(TokenType.RPAREN);
    return params;
  }

  /**
   * Parses colon followed by var type.
   */
  private VarType parseVarType(ImmutableMap<TokenType, VarType> allowedVarTypeMap) {
    expectToken(TokenType.COLON);
    if (token.type() == TokenType.VARIABLE) {
      // Record type.
      Token typeToken = expectToken(TokenType.VARIABLE); // eat the record type
      // TODO: optional < followed by comma-separated vartypes - OR NOT...they might still be
      // unbound type variables...
      return new RecordReferenceType(typeToken.text());
    }

    TokenType declaredType = token.type();
    VarType paramType = allowedVarTypeMap.get(declaredType);
    if (paramType != null) {
      // We have a param type
      advance(); // eat the param type

      // possibly an array. see if there's an open and close bracket
      if (token.type() == TokenType.LBRACKET) {
        expectToken(TokenType.LBRACKET);
        expectToken(TokenType.RBRACKET);
        // TODO(#38): Support multidimensional arrays
        return new ArrayType(paramType, 1);
      }
      return paramType;
    }
    throw new ParseException(
        String.format("Unexpected '%s'; expected built-in or record type", token.text()),
        token.start());
  }

  private Parameter formalParam() {
    Token paramName = expectToken(TokenType.VARIABLE);
    if (token.type() == TokenType.COLON) {
      VarType paramType = parseVarType(VARIABLE_TYPES);
      return new Parameter(paramName.text(), paramType, paramName.start());
    }
    // no colon, just an unknown param type (which will fail type checking(?))
    return new Parameter(paramName.text(), paramName.start());
  }

  private PrintNode print(Token printToken) {
    advance();
    ExprNode expr = expr();
    return new PrintNode(expr, printToken.start(), printToken.type() == TokenType.PRINTLN);
  }

  private IfNode ifStmt(Token kt) {
    expectToken(TokenType.IF);

    List<IfNode.Case> cases = new ArrayList<>();

    ExprNode condition = expr();
    BlockNode statements = block();
    cases.add(new IfNode.Case(condition, statements));

    // while elif: get condition, get statements, add to case list.
    while (token.type() == TokenType.ELIF) {
      expectToken(TokenType.ELIF);

      Node elifCondition = expr();
      Node elifStatements = block();
      cases.add(new IfNode.Case(elifCondition, (BlockNode) elifStatements));
    }

    Optional<BlockNode> elseBlock = Optional.empty();
    if (token.type() == TokenType.ELSE) {
      expectToken(TokenType.ELSE);
      elseBlock = Optional.of(block());
    }

    return new IfNode(cases, elseBlock, kt.start());
  }

  private WhileNode whileStmt(Token kt) {
    expectToken(TokenType.WHILE);

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
    expectToken(TokenType.LPAREN);

    List<ExprNode> actuals;
    if (token.type() == TokenType.RPAREN) {
      actuals = ImmutableList.of();
    } else {
      actuals = commaSeparatedExpressions();
    }

    expectToken(TokenType.RPAREN);

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
    return range();
  }

  private ExprNode range() {
    ExprNode left = boolOr();

    // NOT a "while" because ranges cannot be chained.
    if (token.type() == TokenType.COLON) {
      TokenType operator = token.type();
      advance();
      ExprNode right = boolOr();
      left = new BinOpNode(left, operator, right);
    }

    return left;
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
   * <p>
   * here -> nextRule (tokentype nextRule)*
   *
   * <p>
   * where tokentype is in tokenTypes
   *
   * <p>
   * Example, in the grammar:
   *
   * <p>
   * expr -> term (+- term)*
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

  private static final BiMap<TokenType, TokenType> NOTTED_OPS =
      ImmutableBiMap.of(
          TokenType.EQEQ, TokenType.NEQ,
          TokenType.LT, TokenType.GEQ,
          TokenType.GT, TokenType.LEQ);

  private ExprNode unary() {
    Token unaryToken = token;
    if (token.type() == TokenType.MINUS
        || token.type() == TokenType.PLUS
        || token.type() == TokenType.BIT_NOT
        || token.type() == TokenType.NOT) {
      advance();
      ExprNode operand = unary();

      if (unaryToken.type() == TokenType.PLUS) {
        // unary +(anything) = the thing
        return operand;
      }

      if (unaryToken.type() == TokenType.NOT || unaryToken.type() == TokenType.BIT_NOT) {
        if (operand instanceof BinOpNode) {
          // optimize 'not (a==b)' to 'a!=b'
          BinOpNode child = (BinOpNode) operand;
          TokenType newOperator = NOTTED_OPS.get(child.operator());
          if (newOperator == null) {
            newOperator = NOTTED_OPS.inverse().get(child.operator());
          }
          if (newOperator != null) {
            return new BinOpNode(child.left(), newOperator, child.right());
          }
        }
        if (operand instanceof UnaryNode) {
          // optimize 'not not x' to 'x'. I hate this.
          UnaryNode child = (UnaryNode) operand;
          TokenType secondOp = child.operator();
          if (secondOp == unaryToken.type()) {
            return child.expr();
          }
        }
      }

      if (operand.isConstant()) {
        // We don't really have to check for constant, because at parsing time,
        // we only know the vartypes of constants, not variables. But it doesn't hurt.
        VarType varType = operand.varType();
        if (varType.isNumeric() && unaryToken.type() == TokenType.MINUS) {
          // unary -(constant) = -constant
          @SuppressWarnings("unchecked")
          ConstNode<? extends Number> cn = (ConstNode<? extends Number>) operand;
          Number number = cn.valueAsNumber();
          if (varType.isIntegral()) {
            return ConstNode.fromValue(-number.longValue(), varType, unaryToken.start());
          }
          double value = number.doubleValue();
          return new ConstNode<Double>(-value, varType, unaryToken.start());
        }
        if (varType.isIntegral() && unaryToken.type() == TokenType.BIT_NOT) {
          // unary ~(constant) = ~constant
          @SuppressWarnings("unchecked")
          ConstNode<? extends Number> cn = (ConstNode<? extends Number>) operand;
          Number number = cn.valueAsNumber();
          return ConstNode.fromValue(~number.longValue(), varType, unaryToken.start());
        }
        if (operand.varType() == VarType.BOOL && unaryToken.type() == TokenType.NOT) {
          // not (constant) to not constant
          @SuppressWarnings("unchecked")
          ConstNode<Boolean> cn = (ConstNode<Boolean>) operand;
          return new ConstNode<Boolean>(!cn.value(), VarType.BOOL, unaryToken.start());
        }
      }

      return new UnaryNode(unaryToken.type(), operand, unaryToken.start());
    }

    if (isUnaryKeyword(token)) {
      Token keywordToken = unaryToken;

      advance();
      expectToken(TokenType.LPAREN);
      ExprNode expr = expr();
      expectToken(TokenType.RPAREN);

      return new UnaryNode(keywordToken.type(), expr, keywordToken.start());
    }

    if (token.type() == TokenType.NEW) {
      Position start = token.start();
      expectToken(TokenType.NEW);

      Token recordTypeName = expectToken(TokenType.VARIABLE);
      // optionally allow < and comma-separated types then >
      // then create a new type from the record.
      List<VarType> actualTypes = ImmutableList.of();
      if (token.type() == TokenType.LT) {
        advance();
        actualTypes = commaSeparated(() -> {
          if (token.type() == TokenType.VARIABLE) {
            // Record type.
            Token typeToken = advance(); // eat the record type
            return new RecordReferenceType(typeToken.text());
          }

          TokenType declaredType = token.type();
          VarType paramType = VARIABLE_TYPES.get(declaredType);
          if (paramType != null) {
            // We have a param type
            advance(); // eat the param type
            // possibly an array. see if there's an open and close bracket
            return paramType;
          }
          throw new ParseException(
              String.format("Unexpected '%s'; expected built-in or record type", token.text()),
              token.start());
        });
        expectToken(TokenType.GT);
      }
      return new NewNode(recordTypeName.text(), actualTypes, start);
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
   * composite dereference -> (atom ('[' range ']') | ('.' atom)) *
   * <p>here -> nextRule (tokentype nextRule)*
   * </pre>
   */
  private ExprNode compositeDereference() {
    ExprNode left = atom();

    while (token.type() == TokenType.DOT || token.type() == TokenType.LBRACKET) {
      TokenType operator = token.type();
      advance();
      ExprNode right;
      // TODO(#38): Support multidimensional arrays (switch to "while" instead of "if")
      if (operator == TokenType.LBRACKET) {
        right = expr();
        expectToken(TokenType.RBRACKET);
      } else {
        // dot operator.
        right = atom();
      }
      left = new BinOpNode(left, operator, right);
    }

    return left;
  }

  private static <T> ConstNode<T> toConstNode(ConstToken<T> constToken) {
    VarType varType = VARIABLE_TYPES.get(constToken.literalType());
    assert (varType != null); // it should always find the varType in the map.

    return new ConstNode<T>(constToken.value(), varType, constToken.start());
  }

  /**
   * Parse an atom: a literal, variable or parenthesized expression.
   *
   * <pre>
   * atom -> ARGS | literal constant | FALSE | TRUE | NULL | variable | procedureCall |
   *     INPUT | '(' expr ')' | '[' arrayLiteral ']'
   * </pre>
   */
  private ExprNode atom() {
    switch (token.type()) {
      case ARGS:
        Token argsToken = token;
        advance();
        VariableNode argsNode = new VariableNode(argsToken.type().name(), argsToken.start());
        argsNode.setVarType(new ArrayType(VarType.STRING, 1));
        return argsNode;

      case FALSE:
      case TRUE:
        Token booleanToken = token;
        advance();
        return new ConstNode<Boolean>(
            booleanToken.type() == TokenType.TRUE, VarType.BOOL, booleanToken.start());

      case INPUT:
        Token inputToken = token;
        advance();
        return new InputNode(inputToken.start());

      case LBRACKET:
        return arrayLiteral();

      case LITERAL:
        ConstToken<?> ct = (ConstToken<?>) token;
        switch (ct.literalType()) {
          case BYTE:
          case DOUBLE:
          case INT:
          case LONG:
          case STRING:
            advance();
            return toConstNode(ct);

          default:
            throw new ParseException(
                String.format("Unexpected '%s'; expected literal, variable, or '('", token.text()),
                token.start());
        }
        // no break needed

      case LPAREN:
        expectToken(TokenType.LPAREN);
        ExprNode expr = expr();
        expectToken(TokenType.RPAREN);
        return expr;

      case NULL:
        Token nt = token;
        advance();
        return new ConstNode<Void>(null, VarType.NULL, nt.start());

      case VARIABLE:
        Token varToken = token;
        advance();
        if (token.type() == TokenType.LPAREN) {
          return procedureCall(varToken, false);
        } else {
          return new VariableNode(varToken.text(), varToken.start());
        }
      default:
        throw new ParseException(
            String.format("Unexpected '%s'; expected literal, variable, or '('", token.text()),
            token.start());
    }
  }

  /** Parse an array constant/literal. */
  private ExprNode arrayLiteral() {
    Token openBracket = expectToken(TokenType.LBRACKET);
    List<ExprNode> values = commaSeparatedExpressions();
    if (values.isEmpty()) {
      // will this ever be allowed?
      throw new ParseException("Empty array constants are not allowed yet", openBracket.start());
    }

    expectToken(TokenType.RBRACKET);

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

    return new ArrayLiteralNode(openBracket.start(), values, baseType.get());
  }
}
