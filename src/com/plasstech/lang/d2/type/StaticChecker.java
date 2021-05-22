package com.plasstech.lang.d2.type;

import static com.google.common.collect.ImmutableList.toImmutableList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import com.google.common.collect.ImmutableSet;
import com.plasstech.lang.d2.common.DefaultVisitor;
import com.plasstech.lang.d2.lex.Token;
import com.plasstech.lang.d2.parse.AssignmentNode;
import com.plasstech.lang.d2.parse.BinOpNode;
import com.plasstech.lang.d2.parse.CallNode;
import com.plasstech.lang.d2.parse.DeclarationNode;
import com.plasstech.lang.d2.parse.ExprNode;
import com.plasstech.lang.d2.parse.IfNode;
import com.plasstech.lang.d2.parse.MainNode;
import com.plasstech.lang.d2.parse.Node;
import com.plasstech.lang.d2.parse.PrintNode;
import com.plasstech.lang.d2.parse.ProcedureNode;
import com.plasstech.lang.d2.parse.ProcedureNode.Parameter;
import com.plasstech.lang.d2.parse.ProgramNode;
import com.plasstech.lang.d2.parse.ReturnNode;
import com.plasstech.lang.d2.parse.UnaryNode;
import com.plasstech.lang.d2.parse.VariableNode;
import com.plasstech.lang.d2.parse.WhileNode;

public class StaticChecker extends DefaultVisitor {
  private static final Set<Token.Type> COMPARISION_OPERATORS = ImmutableSet.of(Token.Type.AND,
          Token.Type.OR, Token.Type.EQEQ, Token.Type.LT, Token.Type.GT, Token.Type.LEQ,
          Token.Type.GEQ, Token.Type.NEQ);

  private static final Set<
          Token.Type> STRING_OPERATORS = ImmutableSet.of(Token.Type.EQEQ, Token.Type.LT,
                  Token.Type.GT, Token.Type.LEQ, Token.Type.GEQ, Token.Type.NEQ, Token.Type.PLUS
  // , Token.Type.MOD // eventually
  );

  private final ProgramNode root;
  private final SymTab symbolTable = new SymTab();

  private Stack<ProcedureNode> procedures = new Stack<>();
  private Set<ProcedureNode> needsReturn = new HashSet<>();

  public StaticChecker(ProgramNode root) {
    this.root = root;
  }

  public TypeCheckResult execute() {
    try {
      root.accept(this);
      if (!procedures.isEmpty()) {
        ProcedureNode top = procedures.peek();
        throw new TypeException(
                String.format("Still in procedure %s. (This should never happen)", top),
                top.position());
      }
      return new TypeCheckResult(symbolTable);
    } catch (TypeException e) {
//      throw e;
      return new TypeCheckResult(e.toString());
    }
  }

  private SymTab symbolTable() {
    if (procedures.isEmpty()) {
      return symbolTable;
    }
    return procedures.peek().symbolTable();
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

    VarType existingType = symbolTable().lookup(variable.name(), true);
    if (existingType.isUnknown()) {
      symbolTable().assign(variable.name(), right.varType());
    } else if (existingType != right.varType()) {
      // It was already in the symbol table. Possible that it's wrong
      throw new TypeException(String.format("Type mismatch: lhs (%s) is %s; rhs (%s) is %s",
              variable, existingType, right, right.varType()), variable.position());
    }
    if (!symbolTable().isAssigned(variable.name())) {
      symbolTable().assign(variable.name(), right.varType());
    }
    // all is good.
    variable.setVarType(right.varType());
    node.setVarType(right.varType());
  }

  @Override
  public void visit(VariableNode node) {
    if (node.varType().isUnknown()) {
      // Look up variable in the (current) symbol table, and set it in the node.
      VarType existingType = symbolTable().lookup(node.name(), true);

      if (!existingType.isUnknown()) {
        // BUG- parameters can be referenced without being assigned
        if (!symbolTable().isAssigned(node.name())) {
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
  public void visit(CallNode node) {
    // 1. make sure the function is really a function
    Symbol maybeProc = symbolTable().getRecursive(node.functionToCall());
    if (maybeProc == null || maybeProc.type() != VarType.PROC) {
      throw new TypeException(String.format("Procedure %s is unknown", node.functionToCall()),
              node.position());
    }
    // 2. make sure the arg length is right.
    ProcSymbol proc = (ProcSymbol) maybeProc;
    if (proc.node().parameters().size() != node.actuals().size()) {
      throw new TypeException(
              String.format("Wrong number of arguments to procedure %s: found %d, expected %d",
                      node.functionToCall(), node.actuals().size(),
                      proc.node().parameters().size()),
              node.position());
    }
    // 3. eval parameter expressions.
    node.actuals().forEach(actual -> actual.accept(this));

    // 4. for each param, if param type is unknown, set it from the expr if possible
    for (int i = 0; i < node.actuals().size(); ++i) {
      Parameter formal = proc.node().parameters().get(i);
      ExprNode actual = node.actuals().get(i);
      if (formal.type() == VarType.UNKNOWN) {
        if (actual.varType() == VarType.UNKNOWN) {
          // wah.
          throw new TypeException(
                  String.format("Indeterminable type for parameter %s of procedure %s",
                          formal.name(), proc.name()),
                  node.position());
        } else {
          formal.setVarType(actual.varType());
        }
      }
      // 5. make sure expr types == param types
      if (formal.type() != actual.varType()) {
        throw new TypeException(
                String.format(
                        "Type mismatch for parameter %s of procedure %s: found %s, expected %s",
                        formal.name(), proc.name(), actual.varType(), formal.type()),
                node.position());
      }
    }
    // 6. set the type of the expression to the return type of the node
    node.setVarType(proc.node().returnType());
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
    if (node.block() != null) {
      node.block().accept(this);
    }
  }

  @Override
  public void visit(DeclarationNode node) {
    // Don't go up to the parent symbol table; this allows scoping
    VarType existingType = symbolTable().lookup(node.name(), false);
    if (!existingType.isUnknown()) {
      throw new TypeException(
              String.format("Variable '%s' already declared as %s, cannot be redeclared as %s",
                      node.name(), existingType.name(), node.varType()),
              node.position());
    }
    symbolTable().declare(node.name(), node.varType());
  }

  @Override
  public void visit(ProcedureNode node) {
    // 1. make sure no duplicate arg names (this can be in the parser?)
    List<String> paramNames = node.parameters().stream().map(Parameter::name)
            .collect(toImmutableList());
    Set<String> duplicates = new HashSet<>();
    Set<String> uniques = new HashSet<>();
    for (String param : paramNames) {
      if (uniques.contains(param)) {
        uniques.remove(param);
        duplicates.add(param);
      } else {
        uniques.add(param);
      }
    }
    if (!duplicates.isEmpty()) {
      throw new TypeException(String.format("Duplicate parameters %s in procedure declaration",
              duplicates.toString()), node.position());
    }

    // Add this procedure to the symbol table
    symbolTable().declareProc(node);

    // 2. spawn symbol table & assign to the node.
    SymTab child = symbolTable().spawn();
    node.setSymbolTable(child);

    // 3. push current procedure onto a stack, for symbol table AND return value checking
    procedures.push(node);
    if (node.returnType() != VarType.VOID) {
      needsReturn.add(node);
    }

    // 4. add all args to symbol table
    for (Parameter param : node.parameters()) {
      symbolTable().declareParam(param.name(), param.type());
    }

    // 5. this:
    if (node.block() != null) {
      node.block().accept(this);
    }

    // 6. make sure args all have a type
    for (Parameter param : node.parameters()) {
      VarType type = symbolTable().get(param.name()).type();
      if (type.isUnknown()) {
        throw new TypeException(
                String.format("Could not determine type of parameter %s ", param.name()),
                node.position());
      }
    }

    if (node.returnType() != VarType.VOID) {
      if (needsReturn.contains(node)) {
        // no return statement seen.
        throw new TypeException(
                String.format("No 'return' statement for procedure %s ", node.name()),
                node.position());

      }
    }
    procedures.pop();
  }

  @Override
  public void visit(ReturnNode returnNode) {
    if (procedures.isEmpty()) {
      throw new TypeException("Cannot return from outside a procedure", returnNode.position());
    }
    ExprNode expr = returnNode.expr();
    expr.accept(this);

    ProcedureNode proc = procedures.peek();
    needsReturn.remove(proc);
    VarType declared = proc.returnType();
    VarType actual = returnNode.expr().varType();

    if (actual.isUnknown()) {
      throw new TypeException(String.format("Indeterminable type for return statement %s", expr),
              expr.position());
    }

    if (proc.varType() != expr.varType()) {
      throw new TypeException(
              String.format("Type mismatch: %s declared to return %s but returned %s", proc.name(),
                      declared, actual),
              returnNode.position());
    }
  }
}
