package com.plasstech.lang.d2.type;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.plasstech.lang.d2.common.DefaultVisitor;
import com.plasstech.lang.d2.lex.Token;
import com.plasstech.lang.d2.parse.AssignmentNode;
import com.plasstech.lang.d2.parse.BinOpNode;
import com.plasstech.lang.d2.parse.DeclarationNode;
import com.plasstech.lang.d2.parse.IfNode;
import com.plasstech.lang.d2.parse.MainNode;
import com.plasstech.lang.d2.parse.Node;
import com.plasstech.lang.d2.parse.PrintNode;
import com.plasstech.lang.d2.parse.ProgramNode;
import com.plasstech.lang.d2.parse.UnaryNode;
import com.plasstech.lang.d2.parse.VariableNode;
import com.plasstech.lang.d2.parse.WhileNode;

public class StaticChecker extends DefaultVisitor {
  private static final Set<Token.Type> COMPARISION_OPERATORS = ImmutableSet.of(Token.Type.AND,
          Token.Type.OR, Token.Type.EQEQ, Token.Type.LT, Token.Type.GT, Token.Type.LEQ,
          Token.Type.GEQ, Token.Type.NEQ);

  private static final Set<Token.Type> STRING_OPERATORS = ImmutableSet.of(Token.Type.EQEQ,
          Token.Type.LT, Token.Type.GT, Token.Type.LEQ, Token.Type.GEQ, Token.Type.NEQ,
          Token.Type.PLUS, Token.Type.MINUS);

  private final ProgramNode root;
  private final SymTab symbolTable = new SymTab();

  public StaticChecker(ProgramNode root) {
    this.root = root;
  }

  public TypeCheckResult execute() {
    try {
      root.accept(this);
      return new TypeCheckResult(symbolTable);
    } catch (TypeException e) {
      return new TypeCheckResult(e.toString());
    }
  }

  @Override
  public void visit(PrintNode node) {
    Node expr = node.expr();
    expr.accept(this);
    if (expr.varType().isUnknown()) {
      // this is bad.
      throw new TypeException(String.format("Indeterminable type for %s", expr), expr.position());
    }
  }

  @Override
  public void visit(AssignmentNode node) {
    // Make sure that the left = right
    VariableNode variable = node.variable();

    Node right = node.expr();
    right.accept(this);
    if (right.varType().isUnknown()) {
      // this is bad.
      throw new TypeException(String.format("Indeterminable type for %s", right), right.position());
    }

    VarType existingType = symbolTable.lookup(variable.name());
    if (existingType.isUnknown()) {
      symbolTable.assign(variable.name(), right.varType());
    } else if (existingType != right.varType()) {
      // It was already in the symbol table. Possible that it's wrong
      throw new TypeException(String.format("Type mismatch: lhs (%s) is %s; rhs (%s) is %s",
              variable, existingType, right, right.varType()), variable.position());
    }
    if (!symbolTable.isAssigned(variable.name())) {
      symbolTable.assign(variable.name(), right.varType());
    }
    // all is good.
    variable.setVarType(right.varType());
    node.setVarType(right.varType());
  }

  @Override
  public void visit(VariableNode node) {
    if (node.varType().isUnknown()) {
      // Look up variable in the (local) symbol table, and set it in the node.
      VarType existingType = symbolTable.lookup(node.name());

      if (!existingType.isUnknown()) {
        if (!symbolTable.isAssigned(node.name())) {
          // can't use it
          throw new TypeException(
                  String.format("Variable '%s' used before assignment", node.name()),
                  node.position());
        }
        node.setVarType(existingType);
      }
    }
  }

  @Override
  public void visit(BinOpNode binOpNode) {
    // Make sure that the left = right
    Node left = binOpNode.left();
    left.accept(this);

    Node right = binOpNode.right();
    right.accept(this);

    if (left.varType().isUnknown()) {
      throw new TypeException(String.format("Indeterminable type for %s", left), left.position());
    }
    if (right.varType().isUnknown()) {
      throw new TypeException(String.format("Indeterminable type for %s", right), right.position());
    }

    if (left.varType() != right.varType()) {
      throw new TypeException(String.format("Type mismatch: lhs %s is %s; rhs %s is %s", left,
              left.varType(), right, right.varType()), left.position());
    }

    // Check that they're not trying to, for example, multiply booleans
    if (left.varType() == VarType.BOOL && !COMPARISION_OPERATORS.contains(binOpNode.operator())) {
      throw new TypeException(
              String.format("Cannot apply %s operator to boolean expression", binOpNode.operator()),
              left.position());
    }
    if (left.varType() == VarType.STRING && !STRING_OPERATORS.contains(binOpNode.operator())) {
      throw new TypeException(
              String.format("Cannot apply %s operator to string expression", binOpNode.operator()),
              left.position());
    }
    if ((left.varType() == VarType.INT || left.varType() == VarType.STRING)
            && COMPARISION_OPERATORS.contains(binOpNode.operator())) {
      binOpNode.setVarType(VarType.BOOL);
    } else {
      binOpNode.setVarType(left.varType());
    }
  }

  @Override
  public void visit(UnaryNode unaryNode) {
    Node expr = unaryNode.expr();
    expr.accept(this);

    if (expr.varType().isUnknown()) {
      throw new TypeException(String.format("Indeterminable type for %s", expr), expr.position());
    }

    // Check that they're not trying to negate a boolean or "not" an int.
    if (expr.varType() == VarType.BOOL && unaryNode.operator() != Token.Type.NOT) {
      throw new TypeException(
              String.format("Cannot apply %s operator to boolean expression", unaryNode.operator()),
              expr.position());
    }
    if (expr.varType() == VarType.INT && unaryNode.operator() == Token.Type.NOT) {
      throw new TypeException(
              String.format("Cannot apply %s operator to int expression", unaryNode.operator()),
              expr.position());
    }
    unaryNode.setVarType(expr.varType());
  }

  @Override
  public void visit(IfNode node) {
    for (IfNode.Case ifCase : node.cases()) {
      Node condition = ifCase.condition();
      condition.accept(this);
      if (condition.varType() != VarType.BOOL) {
        throw new TypeException(
                String.format("Condition for 'if' or 'elif' must be boolean; was %s",
                        condition.varType()),
                condition.position());
      }
      ifCase.block().statements().forEach(stmt -> {
        stmt.accept(this);
      });
    }
    if (node.elseBlock() != null) {
      node.elseBlock().statements().forEach(stmt -> {
        stmt.accept(this);
      });
    }
  }

  @Override
  public void visit(WhileNode boolNode) {
    Node condition = boolNode.condition();
    condition.accept(this);
    if (condition.varType() != VarType.BOOL) {
      throw new TypeException(
              String.format("Condition for 'while' must be boolean; was %s", condition.varType()),
              condition.position());
    }
    if (boolNode.assignment().isPresent()) {
      boolNode.assignment().get().accept(this);
    }
    boolNode.block().accept(this);
  }

  @Override
  public void visit(MainNode node) {
    // TODO: something about arguments? probably add to local symbol table
    // Also TODO: how to reference arguments
    if (node.statements() != null) {
      node.statements().accept(this);
    }
  }

  @Override
  public void visit(DeclarationNode node) {
    VarType existingType = symbolTable.lookup(node.name());
    if (existingType != VarType.UNKNOWN) {
      throw new TypeException(
              String.format("Variable '%s' already declared as %s, cannot be redeclared as %s",
                      node.name(), existingType.name(), node.varType()),
              node.position());
    }
    symbolTable.declare(node.name(), node.varType());
  }
}
