package com.plasstech.lang.d2.parse;

import java.util.ArrayList;
import java.util.List;

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
    Node left = term();
    if (left.isError()) {
      return left;
    }

    while (token.type() == Token.Type.PLUS || token.type() == Token.Type.MINUS) {
      Token.Type operator = token.type();
      advance();
      Node right = term();
      left = new BinOpNode(left, operator, right);
    }

    return left;
  }

  private Node term() {
    Node left = atom();
    if (left.isError()) {
      return left;
    }

    while (token.type() == Token.Type.MULT || token.type() == Token.Type.DIV) {
      Token.Type operator = token.type();
      advance();
      Node right = atom();
      left = new BinOpNode(left, operator, right);
    }

    return left;
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
              String.format("Unexpected token at %s: Found %s, expected literal, variable or '('",
                      token.start(), token.text()),
              token.start());

    } else if (token.type() == Token.Type.LPAREN) {
      advance();
      Node expr = expr();
      if (token.type() == Token.Type.RPAREN) {
        advance();
        return expr;
      } else {
        return new ErrorNode(
        String.format("Unexpected token at %s: Found %s, expected ')'",
                token.start(), token.text()), token.start());
      }
    } else {
      return new ErrorNode(
              String.format("Unexpected token at %s: Found %s, expected literal, variable or '('",
                      token.start(), token.text()),
              token.start());
    }
  }
}
