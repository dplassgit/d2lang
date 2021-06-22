package com.plasstech.lang.d2.parse;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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
import com.plasstech.lang.d2.parse.node.ExprNode;
import com.plasstech.lang.d2.parse.node.IfNode;
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
import com.plasstech.lang.d2.type.VarType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

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

  private ProgramNode program() {
    // Read statements until EOF or "main"
    BlockNode statements = statements(matchesEofOrMain());

    // It's restrictive: must have main at the bottom of the file. Sorry/not sorry.
    if (token.type() == Token.Type.EOF) {
      return new ProgramNode(statements);
    } else if (token.type().isKeyword()) {
      // if main, process main
      if (token.type() == Token.Type.MAIN) {
        Token start = token;
        advance(); // eat the main
        // TODO: parse arguments
        BlockNode mainBlock = block();
        if (token.type() == Token.Type.EOF) {
          MainNode mainProc = new MainNode(mainBlock, start.start());
          return new ProgramNode(statements, mainProc);
        }
        throw new ParseException(
            String.format("Unexpected '%s'; expected EOF", token.text()), token.start());
      }
    }
    throw new ParseException(
        String.format("Unexpected '%s'; expected 'main' or EOF", token.text()), token.start());
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

  // This is a statements node surrounded by braces.
  private BlockNode block() {
    if (token.type() != Token.Type.LBRACE) {
      throw new ParseException(
          String.format("Unexpected '%s'; expected '{'", token.text()), token.start());
    }
    advance();

    BlockNode statements = statements(token -> token.type() == Token.Type.RBRACE);
    advance();
    return statements;
  }

  private StatementNode statement() {
    if (token.type().isKeyword()) {
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

        default:
          break;
      }
    } else if (token.type() == Token.Type.VARIABLE) {
      return assignmentDeclarationProcCall();
    }

    throw new ParseException(
        String.format("Unexpected start of statement '%s'", token.text()), token.start());
  }

  private ReturnNode returnStmt(Position start) {
    // either it's the start of an expression, or else it's void.
    if (EXPRESSION_STARTS.contains(token.type())) {
      return new ReturnNode(start, expr());
    }
    // return void, probably.
    return new ReturnNode(start);
  }

  private StatementNode assignmentDeclarationProcCall() {
    assert (token.type() == Token.Type.VARIABLE);

    Token varToken = token;
    advance();
    if (token.type() == Token.Type.EQ) {
      advance();
      VariableNode var = new VariableNode(varToken.text(), varToken.start());
      ExprNode expr = expr();
      return new AssignmentNode(var, expr);
    } else if (token.type() == Token.Type.COLON) {
      advance();
      if (token.type().isKeyword()) {
        Token.Type declaredType = token.type();

        // TODO: this may have to be relaxed when we have records
        VarType varType = BUILTINS.get(declaredType);
        if (varType != null) {
          advance();
          if (varType == VarType.PROC) {
            return procedure(varToken);
          } else {
            // See if it's an array declaration and build a "compound type" from the
            // declaration, e.g., "array of int"
            if (token.type() == Token.Type.LBRACKET) {
              return arrayDeclaration(varToken, varType);
            }
            return new DeclarationNode(varToken.text(), varType, varToken.start());
          }
        }
      }
      throw new ParseException(
          String.format("Unexpected '%s'; expected INT, BOOL, STRING or PROC", token.text()),
          token.start());
    } else if (token.type() == Token.Type.LPAREN) {
      return procedureCall(varToken, true);
    }
    throw new ParseException(
        String.format("Unexpected '%s'; expected '=' or ':'", token.text()), token.start());
  }

  private DeclarationNode arrayDeclaration(Token varToken, VarType baseVarType) {
    assert (token.type() == Token.Type.LBRACKET);
    advance();
    // The size can be variable.
    ExprNode arraySize = expr();
    ArrayType arrayType = new ArrayType(baseVarType);
    if (token.type() != Token.Type.RBRACKET) {
      throw new ParseException(
          String.format("Unexpected '%s'; expected '['", token.text()), token.start());
    }
    advance();
    return new ArrayDeclarationNode(varToken.text(), arrayType, varToken.start(), arraySize);
  }

  private ProcedureNode procedure(Token varToken) {
    List<Parameter> params = formalParams();

    VarType returnType = VarType.VOID;
    if (token.type() == Token.Type.COLON) {
      advance();
      if (!token.type().isKeyword()) {
        throw new ParseException(
            String.format("Unexpected '%s'; expected INT, BOOL or STRING", token.text()),
            token.start());
      }
      Token.Type declaredType = token.type();
      returnType = BUILTINS.get(declaredType);
      if (returnType == null || returnType == VarType.PROC) {
        throw new ParseException(
            String.format("Unexpected '%s'; expected INT, BOOL or STRING", token.text()),
            token.start());
      }
      advance(); // eat the return type
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

    params =
        commaSeparated(
            new NextNode<Parameter>() {
              @Override
              public Parameter call() {
                return formalParam();
              }
            });
    if (token.type() == Token.Type.RPAREN) {
      advance();
    } else {
      throw new ParseException(
          String.format("Unexpected '%s'; expected ',' or ')'", token.text()), token.start());
    }
    return params;
  }

  private Parameter formalParam() {
    if (token.type() != Token.Type.VARIABLE) {
      throw new ParseException(
          String.format("Unexpected '%s'; expected variable", token.text()), token.start());
    }

    Token paramName = token;
    advance();
    if (token.type() == Token.Type.COLON) {
      advance();
      if (!token.type().isKeyword()) {
        throw new ParseException(
            String.format("Unexpected '%s'; expected INT, BOOL or STRING", token.text()),
            token.start());
      }
      Token.Type declaredType = token.type();
      VarType paramType = BUILTINS.get(declaredType);
      if (paramType == null) {
        throw new ParseException(
            String.format("Unexpected '%s'; expected INT, BOOL or STRING", token.text()),
            token.start());
      } else {
        // We have a param type
        advance(); // eat the param type
        return new Parameter(paramName.text(), paramType);
      }
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
    assert (kt.type() == Token.Type.IF);
    advance();

    List<IfNode.Case> cases = new ArrayList<>();

    Node condition = expr();
    Node statements = block();
    cases.add(new IfNode.Case(condition, (BlockNode) statements));

    Node elseStatements = null;
    if (token.type().isKeyword()) {
      Token elseOrElif = token;
      // while elif: get condition, get statements, add to case list.
      while (elseOrElif != null && elseOrElif.type() == Token.Type.ELIF) {
        advance();

        Node elifCondition = expr();
        Node elifStatements = block();
        cases.add(new IfNode.Case(elifCondition, (BlockNode) elifStatements));

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

    return new IfNode(cases, (BlockNode) elseStatements, kt.start());
  }

  private WhileNode whileStmt(Token kt) {
    assert (kt.type() == Token.Type.WHILE);
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
    assert (token.type() == Token.Type.LPAREN);
    advance(); // eat the lparen

    List<ExprNode> actuals;
    if (token.type() == Token.Type.RPAREN) {
      actuals = ImmutableList.of();
    } else {
      actuals = commaSeparatedExpressions();
    }

    if (token.type() == Token.Type.RPAREN) {
      advance(); // eat the rparen
    } else {
      throw new ParseException(
          String.format("Unexpected '%s'; expected ')'", token.text()), token.start());
    }
    return new CallNode(varToken.start(), varToken.text(), actuals, isStatement);
  }

  private List<ExprNode> commaSeparatedExpressions() {
    return commaSeparated(
        new NextNode<ExprNode>() {
          @Override
          public ExprNode call() {
            return expr();
          }
        });
  }

  private <T> List<T> commaSeparated(NextNode<T> nextNode) {
    List<T> nodes = new ArrayList<>();

    T node = nextNode.call();
    nodes.add(node);
    if (token.type() == Token.Type.COMMA) {
      // There's another entry in this list - let's go
      advance();

      while (token.type() != Token.Type.EOF) {
        node = nextNode.call();
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

  private interface NextNode<T> {
    T call();
  }

  /** EXPRESSIONS */
  private ExprNode expr() {
    return boolOr();
  }

  private ExprNode boolOr() {
    return new BinOpFn(Token.Type.OR) {
      @Override
      ExprNode nextRule() {
        return boolAnd();
      }
    }.parse();
  }

  private ExprNode boolAnd() {
    return new BinOpFn(Token.Type.AND) {
      @Override
      ExprNode nextRule() {
        return compareTerm();
      }
    }.parse();
  }

  private ExprNode compareTerm() {
    return new BinOpFn(
        ImmutableSet.of(
            Token.Type.EQEQ,
            Token.Type.NEQ,
            Token.Type.GT,
            Token.Type.LT,
            Token.Type.GEQ,
            Token.Type.LEQ)) {
      @Override
      ExprNode nextRule() {
        return shiftTerm();
      }
    }.parse();
  }

  private ExprNode shiftTerm() {
    return new BinOpFn(ImmutableSet.of(Token.Type.SHIFT_LEFT, Token.Type.SHIFT_RIGHT)) {
      @Override
      ExprNode nextRule() {
        return addSubTerm();
      }
    }.parse();
  }

  private ExprNode addSubTerm() {
    return new BinOpFn(ImmutableSet.of(Token.Type.PLUS, Token.Type.MINUS)) {
      @Override
      ExprNode nextRule() {
        return mulDivTerm();
      }
    }.parse();
  }

  private ExprNode mulDivTerm() {
    return new BinOpFn(ImmutableSet.of(Token.Type.MULT, Token.Type.DIV, Token.Type.MOD)) {
      @Override
      public ExprNode nextRule() {
        return unary();
      }
    }.parse();
  }

  /** Parses a binary operation function. */
  private abstract class BinOpFn {
    private final Set<Token.Type> tokenTypes;

    BinOpFn(Set<Token.Type> tokenTypes) {
      this.tokenTypes = tokenTypes;
    }

    BinOpFn(Token.Type tokenType) {
      this.tokenTypes = ImmutableSet.of(tokenType);
    }

    /** Call the next method, e.g., mulDivTerm */
    abstract ExprNode nextRule();

    /**
     * Parse from the current location, repeatedly call "function", e.g.,:
     *
     * <p>here -> function (tokentype function)*
     *
     * <p>where tokentype is in tokenTypes
     *
     * <p>In the grammar:
     *
     * <p>expr -> term (+- term)*
     */
    ExprNode parse() {
      ExprNode left = nextRule();

      while (tokenTypes.contains(token.type())) {
        Token.Type operator = token.type();
        advance();
        ExprNode right = nextRule();
        left = new BinOpNode(left, operator, right);
      }

      return left;
    }
  }

  private ExprNode unary() {
    Token unaryToken = token;
    if (token.type() == Token.Type.MINUS
        || token.type() == Token.Type.PLUS
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
        }
      } else if (expr.varType() == VarType.BOOL) {
        if (unaryToken.type() == Token.Type.NOT) {
          @SuppressWarnings("unchecked")
          ConstNode<Boolean> cn = (ConstNode<Boolean>) expr;
          return new ConstNode<Boolean>(!cn.value(), VarType.BOOL, unaryToken.start());
        }
      }

      // TODO: Optimize this further, before the code generator is called.
      // For example, --(expr) == +(expr) == (expr) if expr is ultimately integer.
      // However, at this point we don't have types in the expr tree yet so we can't
      // do that exact optimization yet.
      return new UnaryNode(unaryToken.type(), expr, unaryToken.start());
    } else if (isUnaryKeyword(token)) {
      Token keywordToken = unaryToken;
      advance();
      if (token.type() != Token.Type.LPAREN) {
        throw new ParseException(String.format("Unexpected '%s'; expected '('", token), token.start());
      }
      advance();
      ExprNode hopefullyAString = expr();
      if (token.type() != Token.Type.RPAREN) {
        throw new ParseException(String.format("Unexpected '%s'; expected ')'", token), token.start());
      }
      advance();
      return new UnaryNode(keywordToken.type(), hopefullyAString, unaryToken.start());
    }
    return arrayGet();
  }

  private static boolean isUnaryKeyword(Token token) {
    return UNARY_KEYWORDS.contains(token.type());
  }

  private ExprNode arrayGet() {
    ExprNode left = atom();

    // TODO(#38): Support multidimensional arrays (switch to "while" instead of "if")
    if (token.type() == Token.Type.LBRACKET) {
      advance();
      ExprNode index = expr();
      if (token.type() == Token.Type.RBRACKET) {
        advance();
        left = new BinOpNode(left, Token.Type.LBRACKET, index);
      } else {
        throw new ParseException(
            String.format("Unexpected '%s'; expected ']'", token), token.start());
      }
    }

    return left;
  }

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
      if (token.type() == Token.Type.RPAREN) {
        advance();
        return expr;
      } else {
        throw new ParseException(
            String.format("Unexpected '%s'; expected ')'", token.text()), token.start());
      }
    } else if (token.type() == Token.Type.LBRACKET) {
      // array literal
      return arrayLiteral();
    } else {
      throw new ParseException(
          String.format("Unexpected '%s'; expected literal, variable or '('", token.text()),
          token.start());
    }
  }

  private ExprNode arrayLiteral() {
    assert (token.type() == Token.Type.LBRACKET);

    Token openBracket = token;
    // Array constant/literal.
    advance(); // eat left bracket

    // Future version:
    // List<ExprNode> values = commaSeparatedExpressions();

    // For first iteration: only allow const int[] or const string[] or const bool[]
    if (token.type() != Token.Type.RBRACKET) {
      List<ConstNode<?>> values =
          commaSeparated(
              new NextNode<ConstNode<?>>() {
                @Override
                public ConstNode<?> call() {
                  ExprNode atom = atom();
                  if (atom instanceof ConstNode) {
                    return (ConstNode<?>) atom;
                  }

                  throw new ParseException(
                      String.format(
                          "Illegal entry %s in array literal; only scalar literals allowed",
                          token.text()),
                      token.start());
                }
              });

      if (token.type() != Token.Type.RBRACKET) {
        throw new ParseException(
            String.format("Unexpected '%s'; expected ']'", token.text()), token.start());
      }
      advance(); // eat rbracket

      VarType baseType = values.get(0).varType();
      ArrayType arrayType = new ArrayType(baseType);

      // TODO: check all values to make sure they're the same type
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

  private static Function<Token, Boolean> matchesEofOrMain() {
    return token -> {
      if (token.type() == Token.Type.EOF) {
        return true;
      }
      return token.type() == Token.Type.MAIN;
    };
  }
}
