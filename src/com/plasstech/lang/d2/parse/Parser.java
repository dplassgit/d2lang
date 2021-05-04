package com.plasstech.lang.d2.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
    return statements();
  }

  private Node statements() {
    Node child = statement();
    if (child.isError() || !(child instanceof StatementNode)) {
      return child;
    }
    List<StatementNode> children = new ArrayList<>();
    children.add((StatementNode) child);
    while (token.type() != Token.Type.EOF) {
      child = statement();
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
      }
    } else if (token.type() == Token.Type.VARIABLE) {
      return assignment();
    }
    return new ErrorNode(
            String.format("Unexpected token %s; expected print or assignment", token.toString()),
            token.start());
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

  private Node expr() {
    return addSubTerm();
  }

  private Node addSubTerm() {
    return new BinOpFn() {
      @Override
      Node function() {
        return mulDivTerm();
      }
    }.call(ImmutableSet.of(Token.Type.PLUS, Token.Type.MINUS));
  }

  private Node mulDivTerm() {
    return new BinOpFn() {
      @Override
      public Node function() {
        return unary();
      }
    }.call(ImmutableSet.of(Token.Type.MULT, Token.Type.DIV));
  }

  private Node unary() {
    if (token.type() == Token.Type.MINUS || token.type() == Token.Type.PLUS /* or not */) {
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
        } else {
          return new IntNode(-((IntNode) expr).value(), unaryToken.start());
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
    /** Call the next method, e.g., mulDivTerm */
    abstract Node function();

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
    Node call(Set<Token.Type> tokenTypes) {
      Node left = function();
      if (left.isError()) {
        return left;
      }

      while (tokenTypes.contains(token.type())) {
        Token.Type operator = token.type();
        advance();
        Node right = function();
        if (right.isError()) {
          return right;
        }
        left = new BinOpNode(left, operator, right);
      }

      return left;
    }
  }
}
