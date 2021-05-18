package com.plasstech.lang.d2.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.google.common.collect.ImmutableSet;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.lex.BoolToken;
import com.plasstech.lang.d2.lex.IntToken;
import com.plasstech.lang.d2.lex.KeywordToken;
import com.plasstech.lang.d2.lex.KeywordToken.KeywordType;
import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.lex.Token;
import com.plasstech.lang.d2.parse.ProcedureNode.Parameter;
import com.plasstech.lang.d2.type.VarType;

public class Parser {

  private final Lexer lexer;
  private Token token;
  private int inWhile;

  public Parser(Lexer lexer) {
    this.lexer = lexer;
    this.advance();
  }

  private Token advance() {
    token = lexer.nextToken();
    return token;
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
    } else if (token.type() == Token.Type.KEYWORD) {
      // if main, process main
      KeywordToken kt = (KeywordToken) token;
      if (kt.keyword() == KeywordType.MAIN) {
        advance(); // eat the main
        // TODO: parse arguments
        BlockNode mainBlock = block();
        if (token.type() == Token.Type.EOF) {
          MainNode mainProc = new MainNode(mainBlock, kt.start());
          return new ProgramNode(statements, mainProc);
        }
        throw new ParseException(String.format("Unexpected %s; expected EOF", token.text()),
                token.start());
      }
    }
    throw new ParseException(String.format("Unexpected %s; expected 'main' or EOF", token.text()),
            token.start());
  }

  private Function<Token, Boolean> matchesEofOrMain() {
    return token -> {
      if (token.type() == Token.Type.EOF) {
        return true;
      }
      return matchesKeyword(token, KeywordType.MAIN);
    };
  }

  private static Boolean matchesKeyword(Token token, KeywordType type) {
    if (token.type() == Token.Type.KEYWORD) {
      KeywordToken kt = (KeywordToken) token;
      return kt.keyword() == type;
    }
    return false;
  }

  // TODO: do not allow functions in blocks?
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
      throw new ParseException(String.format("Unexpected %s; expected {", token.text()),
              token.start());
    }
    advance();

    BlockNode statements = statements(token -> token.type() == Token.Type.RBRACE);
    advance();
    return statements;
  }

  private StatementNode statement() {
    if (token.type() == Token.Type.KEYWORD) {

      KeywordToken kt = (KeywordToken) token;
      switch (kt.keyword()) {
        case PRINT:
        case PRINTLN:
          return print(kt, kt.keyword() == KeywordType.PRINTLN);

        case IF:
          return ifStmt(kt);

        case WHILE: {
          inWhile++;
          WhileNode whileStmt = whileStmt(kt);
          inWhile--;
          return whileStmt;
        }

        case BREAK:
          if (inWhile == 0) {
            throw new ParseException("BREAK keyword not in while block", kt.start());
          }
          advance();
          return new BreakNode(kt.start());

        case CONTINUE:
          if (inWhile == 0) {
            throw new ParseException("CONTINUE keyword not in while block", kt.start());
          }
          advance();
          return new ContinueNode(kt.start());

        case RETURN:
          advance();
          Node expr = expr();
          return new ReturnNode(kt.start(), expr);

        default:
          break;
      }
    } else if (token.type() == Token.Type.VARIABLE) {
      return assignmentOrDeclaration();
    }

    throw new ParseException(String
            .format("Unexpected %s; expected 'print', assignment, 'if' or 'while'", token.text()),
            token.start());
  }

  private StatementNode assignmentOrDeclaration() {
    if (token.type() != Token.Type.VARIABLE) {
      throw new ParseException(String.format("Unexpected %s; expected variable", token.text()),
              token.start());
    }
    Token varToken = token;
    advance();
    if (token.type() == Token.Type.EQ) {
      advance();
      VariableNode var = new VariableNode(varToken.text(), varToken.start());
      Node expr = expr();
      return new AssignmentNode(var, expr);
    } else if (token.type() == Token.Type.COLON) {
      advance();
      if (token.type() == Token.Type.KEYWORD) {
        KeywordType declaredType = ((KeywordToken) token).keyword();
        VarType varType = DeclarationNode.BUILTINS.get(declaredType);
        if (varType != null) {
          advance();
          if (varType == VarType.PROC) {
            return procedure(varToken);
          } else {
            return new DeclarationNode(varToken.text(), varType, varToken.start());
          }
        }
      }
      throw new ParseException(
              String.format("Unexpected %s; expected INT, BOOL, STRING or PROC", token.text()),
              token.start());
    }
    throw new ParseException(String.format("Unexpected %s; expected '=' or ':'", token.text()),
            token.start());
  }

  private ProcedureNode procedure(Token varToken) {
    List<Parameter> params = formalParams();

    VarType returnType = VarType.VOID;
    if (matchesKeyword(token, KeywordType.RETURNS)) {
      advance();
      if (token.type() != Token.Type.KEYWORD) {
        throw new ParseException(
                String.format("Unexpected %s; expected INT, BOOL or STRING", token.text()),
                token.start());
      }
      KeywordType declaredType = ((KeywordToken) token).keyword();
      // this allows returns proc, which I don't like.
      VarType varType = DeclarationNode.BUILTINS.get(declaredType);
      if (varType == null) {
        throw new ParseException(
                String.format("Unexpected %s; expected INT, BOOL or STRING", token.text()),
                token.start());
      }
      returnType = varType;
      advance(); // eat the return type
    }
    if (token.type() != Token.Type.LBRACE) {
      throw new ParseException(String.format("Unexpected %s; expected {", token.text()),
              token.start());
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
    // Must be variable
    while (token.type() != Token.Type.RPAREN && token.type() != Token.Type.EOF) {
      if (token.type() != Token.Type.VARIABLE) {
        throw new ParseException("expected variable", token.start());
      }
      Token paramName = token;
      advance();
      if (token.type() == Token.Type.COLON) {
        advance();
        if (token.type() != Token.Type.KEYWORD) {
          throw new ParseException(
                  String.format("Unexpected %s; expected INT, BOOL or STRING", token.text()),
                  token.start());
        }
        KeywordType declaredType = ((KeywordToken) token).keyword();
        VarType paramType = DeclarationNode.BUILTINS.get(declaredType);
        if (paramType == null) {
          throw new ParseException(
                  String.format("Unexpected %s; expected INT, BOOL or STRING", token.text()),
                  token.start());
        } else {
          // We have a param type
          Parameter param = new Parameter(paramName.text(), paramType);
          params.add(param);
          advance(); // eat the param type
        }
      } else {
        // no colon, just an unknown param type
        Parameter param = new Parameter(paramName.text());
        params.add(param);
      }

      // if it's a comma, eat it
      if (token.type() == Token.Type.COMMA) {
        advance();
      }
    }

    if (token.type() != Token.Type.RPAREN) {
      throw new ParseException("no right paren", token.start());
    }
    advance();
    return params;
  }

  private AssignmentNode assignment() {
    if (token.type() != Token.Type.VARIABLE) {
      throw new ParseException(String.format("Unexpected %s; expected variable", token.text()),
              token.start());
    }

    VariableNode var = new VariableNode(token.text(), token.start());
    advance();
    if (token.type() != Token.Type.EQ) {
      throw new ParseException(String.format("Unexpected %s; expected '='", token.text()),
              token.start());
    }
    advance();
    Node expr = expr();
    return new AssignmentNode(var, expr);
  }

  private PrintNode print(KeywordToken kt, boolean println) {
    assert (kt.keyword() == KeywordType.PRINT || kt.keyword() == KeywordType.PRINTLN);
    advance();
    Node expr = expr();
    return new PrintNode(expr, kt.start(), println);
  }

  private IfNode ifStmt(KeywordToken kt) {
    assert (kt.keyword() == KeywordType.IF);
    advance();

    List<IfNode.Case> cases = new ArrayList<>();

    Node condition = expr();
    Node statements = block();
    cases.add(new IfNode.Case(condition, (BlockNode) statements));

    Node elseStatements = null;
    if (token.type() == Token.Type.KEYWORD) {
      KeywordToken elseOrElif = (KeywordToken) token;
      // while elif: get condition, get statements, add to case list.
      while (elseOrElif != null && elseOrElif.keyword() == KeywordType.ELIF) {
        advance();

        Node elifCondition = expr();
        Node elifStatements = block();
        cases.add(new IfNode.Case(elifCondition, (BlockNode) elifStatements));

        if (token.type() == Token.Type.KEYWORD) {
          elseOrElif = (KeywordToken) token;
        } else {
          elseOrElif = null;
        }
      }

      if (elseOrElif != null && elseOrElif.keyword() == KeywordType.ELSE) {
        advance();
        elseStatements = block();
      }
    }

    return new IfNode(cases, (BlockNode) elseStatements, kt.start());
  }

  private WhileNode whileStmt(KeywordToken kt) {
    assert (kt.keyword() == KeywordType.WHILE);
    advance();
    Node condition = expr();
    Optional<AssignmentNode> assignment = Optional.empty();
    if (matchesKeyword(token, KeywordType.DO)) {
      advance();
      assignment = Optional.of(assignment());
    }
    BlockNode block = block();
    return new WhileNode(condition, assignment, block, kt.start());
  }

  private Node expr() {
    return boolOr();
  }

  private Node boolOr() {
    return new BinOpFn(Token.Type.OR) {
      @Override
      Node nextRule() {
        return boolAnd();
      }
    }.parse();
  }

  private Node boolAnd() {
    return new BinOpFn(Token.Type.AND) {
      @Override
      Node nextRule() {
        return compareTerm();
      }
    }.parse();
  }

  private Node compareTerm() {
    return new BinOpFn(ImmutableSet.of(Token.Type.EQEQ, Token.Type.NEQ, Token.Type.GT,
            Token.Type.LT, Token.Type.GEQ, Token.Type.LEQ)) {
      @Override
      Node nextRule() {
        return addSubTerm();
      }
    }.parse();
  }

  private Node addSubTerm() {
    return new BinOpFn(ImmutableSet.of(Token.Type.PLUS, Token.Type.MINUS)) {
      @Override
      Node nextRule() {
        return mulDivTerm();
      }
    }.parse();
  }

  private Node mulDivTerm() {
    return new BinOpFn(ImmutableSet.of(Token.Type.MULT, Token.Type.DIV, Token.Type.MOD)) {
      @Override
      public Node nextRule() {
        return unary();
      }
    }.parse();
  }

  private Node unary() {
    if (token.type() == Token.Type.MINUS || token.type() == Token.Type.PLUS
            || token.type() == Token.Type.NOT) {
      Token unaryToken = token;
      advance();
      Node expr = unary(); // should this be expr? unary? atom?

      if (expr.nodeType() == Node.Type.INT) {
        // We can simplify now
        if (unaryToken.type() == Token.Type.PLUS) {
          return expr;
        } else if (unaryToken.type() == Token.Type.MINUS) {
          @SuppressWarnings("unchecked")
          ConstNode<Integer> in = (ConstNode<Integer>) expr;
          return new ConstNode<Integer>(Node.Type.INT, -in.value(), VarType.INT,
                  unaryToken.start());
        }
      } else if (expr.nodeType() == Node.Type.BOOL) {
        if (unaryToken.type() == Token.Type.NOT) {
          @SuppressWarnings("unchecked")
          ConstNode<Boolean> cn = (ConstNode<Boolean>) expr;
          return new ConstNode<Boolean>(Node.Type.BOOL, !cn.value(), VarType.BOOL,
                  unaryToken.start());
        }
      }

      // TODO: Optimize this further, before the code generator is called.
      // For example, --(expr) == +(expr) == (expr) if expr is ultimately integer.
      // However, at this point we don't have types in the expr tree yet so we can't
      // do that exact optimization yet.
      return new UnaryNode(unaryToken.type(), expr, unaryToken.start());
    }
    return atom();
  }

  private Node atom() {
    if (token.type() == Token.Type.INT) {
      IntToken it = (IntToken) token;
      advance();
      return new ConstNode<Integer>(Node.Type.INT, it.value(), VarType.INT, it.start());
    } else if (token.type() == Token.Type.VARIABLE) {
      Token varToken = token;
      String name = token.text();
      advance();
      return new VariableNode(name, varToken.start());
    } else if (token.type() == Token.Type.BOOL) {
      BoolToken bt = (BoolToken) token;
      advance();
      return new ConstNode<Boolean>(Node.Type.BOOL, bt.value(), VarType.BOOL, bt.start());
    } else if (token.type() == Token.Type.STRING) {
      Token st = token;
      advance();
      return new ConstNode<String>(Node.Type.STRING, st.text(), VarType.STRING, st.start());
    } else if (token.type() == Token.Type.LPAREN) {
      advance();
      Node expr = expr();
      if (token.type() == Token.Type.RPAREN) {
        advance();
        return expr;
      } else {
        throw new ParseException(String.format("Unexpected %s; expected ')'", token.text()),
                token.start());
      }
    } else {
      throw new ParseException(
              String.format("Unexpected %s; expected literal, variable or '('", token.text()),
              token.start());
    }
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
    abstract Node nextRule();

    /**
     * Parse from the current location, repeatedly call "function", e.g.,:
     *
     * here -> function (tokentype function)*
     *
     * where tokentype is in tokenTypes
     *
     * In the grammar:
     *
     * expr -> term (+- term)*
     */
    Node parse() {
      Node left = nextRule();

      while (tokenTypes.contains(token.type())) {
        Token.Type operator = token.type();
        advance();
        Node right = nextRule();
        left = new BinOpNode(left, operator, right);
      }

      return left;
    }
  }
}
