package com.plasstech.lang.d2.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.plasstech.lang.d2.lex.IntToken;
import com.plasstech.lang.d2.lex.KeywordToken;
import com.plasstech.lang.d2.lex.KeywordToken.KeywordType;
import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.lex.Token;

public class Parser {

  private final Lexer lexer;
  private Token token;

  public Parser(Lexer lexer) {
    this.lexer = lexer;
    this.advance();
  }

  private Token advance() {
    token = lexer.nextToken();
    return token;
  }

  public Node parse() {
    // TODO: do not allow functions in blocks
    return block(Token.Type.EOF);
  }

  private Node block(Token.Type blockEnd) {
    List<StatementNode> children = new ArrayList<>();
    while (token.type() != blockEnd) {
      Node child = statement();
      if (child.isError() || !(child instanceof StatementNode)) {
        return child;
      }
      children.add((StatementNode) child);
    }
    return new StatementsNode(children);
  }

  private Node statement() {
    if (token.type() == Token.Type.KEYWORD) {
      KeywordToken kt = (KeywordToken) token;
      // TODO: use a switch
      if (kt.keyword() == KeywordType.PRINT) {
        return print(kt);
      } else if (kt.keyword() == KeywordType.IF) {
        return ifStmt(kt);
      }
    } else if (token.type() == Token.Type.VARIABLE) {
      return assignment();
    }
    return new ErrorNode(String.format("Unexpected token %s; expected print, assignment or if",
            token.toString()), token.start());
  }

  private Node assignment() {
    VariableNode var = new VariableNode(token.text(), token.start());
    advance();
    if (token.type() != Token.Type.EQ) {
      return new ErrorNode(String.format("Unexpected token %s; expected '='", token.toString()),
              token.start());
    }
    advance();
    Node expr = expr();
    if (expr.isError()) {
      return expr;
    }
    return new AssignmentNode(var, expr);
  }

  private Node print(KeywordToken kt) {
    assert (kt.keyword() == KeywordType.PRINT);
    advance();
    Node expr = expr();
    if (expr.isError()) {
      return expr;
    }
    return new PrintNode(expr, kt.start());
  }

  private Node ifStmt(KeywordToken kt) {
    assert (kt.keyword() == KeywordType.IF);
    advance();

    List<IfNode.Case> cases = new ArrayList<>();

    Node condition = expr();
    if (condition.isError()) {
      return condition;
    }
    List<Node> statements = parseStatementList();
    if (statements.size() == 1 && statements.get(0).isError()) {
      return statements.get(0);
    }
    cases.add(new IfNode.Case(condition, statements));

    List<Node> elseStatements = ImmutableList.of();
    if (token.type() == Token.Type.KEYWORD) {
      KeywordToken elseOrElif = (KeywordToken) token;
      // while elif: get condition, get statements, add to case list.
      while (elseOrElif != null && elseOrElif.keyword() == KeywordType.ELIF) {
        advance();

        Node elifCondition = expr();
        if (elifCondition.isError()) {
          return elifCondition;
        }
        List<Node> elifStatements = parseStatementList();
        if (elifStatements.size() == 1 && elifStatements.get(0).isError()) {
          return elifStatements.get(0);
        }
        cases.add(new IfNode.Case(elifCondition, elifStatements));

        if (token.type() == Token.Type.KEYWORD) {
          elseOrElif = (KeywordToken) token;
        } else {
          elseOrElif = null;
        }
      }

      if (elseOrElif != null && elseOrElif.keyword() == KeywordType.ELSE) {
        advance();
        elseStatements = parseStatementList();
        if (elseStatements.size() == 1 && elseStatements.get(0).isError()) {
          return elseStatements.get(0);
        }
      }
    }

    return new IfNode(cases, elseStatements, kt.start());
  }

  private List<Node> parseStatementList() {
    if (token.type() != Token.Type.LBRACE) {
      return ImmutableList.of(new ErrorNode(
              String.format("Unexpected token %s; expected {", token.toString()), token.start()));
    }
    advance();

    List<Node> statements = new ArrayList<>();
    while (token.type() != Token.Type.RBRACE) {
      Node statement = statement();
      if (statement.isError()) {
        return ImmutableList.of(statement);
      }
      statements.add(statement);
    }
    advance();
    return statements;
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
    return new BinOpFn(
        ImmutableSet.of(Token.Type.EQEQ, Token.Type.NEQ, Token.Type.GT, Token.Type.LT, 
          Token.Type.GEQ, Token.Type.LEQ)) {
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
      if (expr.isError()) {
        return expr;
      }

      if (expr.nodeType() == Node.Type.INT) {
        // We can simplify now
        if (unaryToken.type() == Token.Type.PLUS) {
          return expr;
        } else if (unaryToken.type() == Token.Type.MINUS) {
          return new IntNode(-((IntNode) expr).value(), unaryToken.start());
        }
      } else if (expr.nodeType() == Node.Type.BOOL) {
        if (unaryToken.type() == Token.Type.NOT) {
          return new BoolNode(!((BoolNode) expr).value(), unaryToken.start());
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
      return new IntNode(it.value(), it.start());
    } else if (token.type() == Token.Type.VARIABLE) {
      Token varToken = token;
      String name = token.text();
      advance();
      return new VariableNode(name, varToken.start());
    } else if (token.type() == Token.Type.KEYWORD) {
      KeywordToken kt = (KeywordToken) token;
      if (kt.keyword() == KeywordType.TRUE || kt.keyword() == KeywordType.FALSE) {
        advance();
        return new BoolNode(kt.keyword() == KeywordType.TRUE, kt.end());
      }
      return new ErrorNode(
              String.format("Unexpected keyword at %s: Found %s, expected literal, variable or '('",
                      token.start(), token.text()),
              token.start());

    } else if (token.type() == Token.Type.LPAREN) {
      advance();
      Node expr = expr();
      if (token.type() == Token.Type.RPAREN) {
        advance();
        return expr;
      } else {
        return new ErrorNode(String.format("Unexpected string at %s: Found %s, expected ')'",
                token.start(), token.text()), token.start());
      }
    } else {
      return new ErrorNode(
              String.format("Unexpected string at %s: Found %s, expected literal, variable or '('",
                      token.start(), token.text()),
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
      if (left.isError()) {
        return left;
      }

      while (tokenTypes.contains(token.type())) {
        Token.Type operator = token.type();
        advance();
        Node right = nextRule();
        if (right.isError()) {
          return right;
        }
        left = new BinOpNode(left, operator, right);
      }

      return left;
    }
  }
}
