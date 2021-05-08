package com.plasstech.lang.d2.type;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.plasstech.lang.d2.common.DefaultVisitor;
import com.plasstech.lang.d2.lex.Token;
import com.plasstech.lang.d2.parse.AssignmentNode;
import com.plasstech.lang.d2.parse.BinOpNode;
import com.plasstech.lang.d2.parse.IfNode;
import com.plasstech.lang.d2.parse.Node;
import com.plasstech.lang.d2.parse.BlockNode;
import com.plasstech.lang.d2.parse.UnaryNode;
import com.plasstech.lang.d2.parse.VariableNode;

public class StaticChecker extends DefaultVisitor {
  private static final Set<Token.Type> COMPARISION_OPERATORS = ImmutableSet.of(Token.Type.AND,
          Token.Type.OR, Token.Type.EQEQ, Token.Type.LT, Token.Type.GT, Token.Type.LEQ,
          Token.Type.GEQ, Token.Type.NEQ);

  private final BlockNode root;
  private final SymTab symbolTable = new SymTab();
  private String error;

  public StaticChecker(BlockNode root) {
    this.root = root;
  }

  public TypeCheckResult execute() {
    root.accept(this);
    if (error != null) {
      return new TypeCheckResult(error);
    }
    return new TypeCheckResult(symbolTable);
  }

  @Override
  public void visit(AssignmentNode node) {
    if (error != null) {
      return;
    }
    // Make sure that the left = right
    VariableNode variable = node.variable();
    VarType existingType = variable.varType();
    if (existingType.isUnknown()) {
      existingType = symbolTable.lookup(variable.name());
      if (!existingType.isUnknown()) {
        variable.setVarType(existingType);
      }
    }

    Node right = node.expr();
    right.accept(this);
    if (error != null) {
      return;
    }
    if (right.varType().isUnknown()) {
      // this is bad.
      error = String.format("Type error at %s: Indeterminable type for %s", right.position(),
              right);
      return;
    }

    if (variable.varType().isUnknown()) {
      variable.setVarType(right.varType());
      // Enter it in the local symbol table
      symbolTable.add(variable.name(), right.varType());
    } else if (variable.varType() != right.varType()) {
      error = String.format("Type mismatch at %s: lhs (%s) is %s; rhs (%s) is %s",
              variable.position(), variable, variable.varType(), right, right.varType());
    }
  }

  @Override
  public void visit(VariableNode node) {
    if (error != null) {
      return;
    }
    if (node.varType().isUnknown()) {
      // Look up variable in the (local) symbol table, and set it in the node.
      VarType existingType = symbolTable.lookup(node.name());
      if (!existingType.isUnknown()) {
        node.setVarType(existingType);
      }
    }
  }

  @Override
  public void visit(BinOpNode binOpNode) {
    if (error != null) {
      return;
    }
    // Make sure that the left = right
    Node left = binOpNode.left();
    left.accept(this);
    if (error != null) {
      return;
    }

    Node right = binOpNode.right();
    right.accept(this);
    if (error != null) {
      return;
    }

    if (left.varType().isUnknown()) {
      error = String.format("Type error at %s: Indeterminable type for %s", left.position(), left);
      return;
    }
    if (right.varType().isUnknown()) {
      error = String.format("Type error at %s: Indeterminable type for %s", right.position(),
              right);
      return;
    }

    if (left.varType() != right.varType()) {
      error = String.format("Type mismatch at %s: lhs %s is %s; rhs %s is %s", left.position(),
              left, left.varType(), right, right.varType());
      return;
    }

    // Check that they're not trying to, for example, multiply booleans
    if (left.varType() == VarType.BOOL && !COMPARISION_OPERATORS.contains(binOpNode.operator())) {
      error = String.format("Type mismatch at %s: Cannot apply %s operator to boolean expression",
              left.position(), binOpNode.operator());
      return;
    }
    if (left.varType() == VarType.INT && COMPARISION_OPERATORS.contains(binOpNode.operator())) {
      binOpNode.setVarType(VarType.BOOL);
    } else {
      binOpNode.setVarType(left.varType());
    }
  }

  @Override
  public void visit(UnaryNode unaryNode) {
    if (error != null) {
      return;
    }
    Node expr = unaryNode.expr();
    expr.accept(this);
    if (error != null) {
      return;
    }

    if (expr.varType().isUnknown()) {
      error = String.format("Type error at %s: Indeterminable type for %s", expr.position(), expr);
      return;
    }

    // Check that they're not trying to negate a boolean or "not" an int.
    if (expr.varType() == VarType.BOOL && unaryNode.operator() != Token.Type.NOT) {
      error = String.format("Type mismatch at %s: Cannot apply %s operator to boolean expression",
              expr.position(), unaryNode.operator());
      return;
    }
    if (expr.varType() == VarType.INT && unaryNode.operator() == Token.Type.NOT) {
      error = String.format("Type mismatch at %s: Cannot apply %s operator to int expression",
              expr.position(), unaryNode.operator());
      return;
    }
    unaryNode.setVarType(expr.varType());
  }

  @Override
  public void visit(IfNode node) {
    if (error != null) {
      return;
    }
    for (IfNode.Case ifCase : node.cases()) {
      Node condition = ifCase.condition();
      condition.accept(this);
      if (error != null) {
        return;
      }
      if (condition.varType() != VarType.BOOL) {
        error = String.format(
                "Type mismatch at %s: Condition for 'if' or 'elif' must be boolean; was %s",
                condition.position(), condition.varType());
        return;
      }
      ifCase.block().statements().forEach(stmt -> {
        if (error == null) {
          stmt.accept(this);
        }
      });
    }
    if (node.elseBlock() != null) {
      node.elseBlock().statements().forEach(stmt -> {
        if (error == null) {
          stmt.accept(this);
        }
      });
    }
  }
}
