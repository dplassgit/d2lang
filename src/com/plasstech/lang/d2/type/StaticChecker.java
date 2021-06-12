package com.plasstech.lang.d2.type;

import static com.google.common.collect.ImmutableList.toImmutableList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import com.google.common.collect.ImmutableSet;
import com.plasstech.lang.d2.lex.Token;
import com.plasstech.lang.d2.parse.node.ArrayDeclarationNode;
import com.plasstech.lang.d2.parse.node.AssignmentNode;
import com.plasstech.lang.d2.parse.node.BinOpNode;
import com.plasstech.lang.d2.parse.node.BlockNode;
import com.plasstech.lang.d2.parse.node.CallNode;
import com.plasstech.lang.d2.parse.node.DeclarationNode;
import com.plasstech.lang.d2.parse.node.DefaultVisitor;
import com.plasstech.lang.d2.parse.node.ExprNode;
import com.plasstech.lang.d2.parse.node.IfNode;
import com.plasstech.lang.d2.parse.node.IfNode.Case;
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

public class StaticChecker extends DefaultVisitor {
  private static final Set<Token.Type> COMPARISION_OPERATORS = ImmutableSet.of(Token.Type.AND,
          Token.Type.OR, Token.Type.EQEQ, Token.Type.LT, Token.Type.GT, Token.Type.LEQ,
          Token.Type.GEQ, Token.Type.NEQ);

  private static final Set<Token.Type> INT_OPERATORS = //
          ImmutableSet.of(Token.Type.EQEQ, Token.Type.LT, Token.Type.GT, Token.Type.LEQ,
                  Token.Type.GEQ, Token.Type.NEQ, Token.Type.DIV, Token.Type.MINUS, Token.Type.MOD,
                  Token.Type.MULT, Token.Type.PLUS);

  private static final Set<Token.Type> STRING_OPERATORS = //
          ImmutableSet.of(Token.Type.EQEQ, Token.Type.LT, Token.Type.GT, Token.Type.LEQ,
                  Token.Type.GEQ, Token.Type.NEQ, Token.Type.PLUS, Token.Type.LBRACKET
          // , Token.Type.MOD // eventually
          );

  private static final Set<Token.Type> BOOLEAN_OPERATORS = ImmutableSet.of(Token.Type.EQEQ,
          Token.Type.LT, Token.Type.GT, Token.Type.NEQ, Token.Type.AND, Token.Type.OR);

  private static final Set<Token.Type> ARRAY_OPERATORS = ImmutableSet.of(Token.Type.EQEQ,
          Token.Type.NEQ, Token.Type.LBRACKET);

  private final ProgramNode root;
  private final SymTab symbolTable = new SymTab();

  private Stack<ProcSymbol> procedures = new Stack<>();
  private Set<ProcSymbol> needsReturn = new HashSet<>();

  public StaticChecker(ProgramNode root) {
    this.root = root;
  }

  public TypeCheckResult execute() {
    try {
      root.accept(this);
      if (!procedures.isEmpty()) {
        ProcSymbol top = procedures.peek();
        throw new TypeException(
                String.format("Still in procedure %s. (This should never happen)", top),
                top.node().position());
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
    return procedures.peek().symTab();
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
    if (right.varType() == VarType.VOID) {
      // this is bad.
      throw new TypeException(String.format("Cannot assign value of void expression %s", right),
              right.position());
    }

    Symbol sym = symbolTable().getRecursive(variable.name());
    if (sym == null) {
      // brand new in all scopes. Assign in curren scope.
      symbolTable().assign(variable.name(), right.varType());
    } else {
      // already known in some scope. Update.
      if (sym.type().isUnknown()) {
        sym.setType(right.varType());
      } else if (!sym.type().equals(right.varType())) {
        // It was already in the symbol table. Possible that it's wrong
        throw new TypeException(
                String.format("Type mismatch: '%s' declared as %s but RHS (%s) is %s", variable,
                sym.type(), right, right.varType()), variable.position());
      }

      sym.setAssigned();
    }

    // all is good.
    // TODO: why do we need variable.vartype, node.vartype and symbol.type?
    variable.setVarType(right.varType());
    node.setVarType(right.varType());
  }

  @Override
  public void visit(VariableNode node) {
    if (node.varType().isUnknown()) {
      // Look up variable in the (current) symbol table, and set it in the node.
      VarType existingType = symbolTable().lookup(node.name(), true);
      if (!existingType.isUnknown()) {
        // BUG- parameters can be referenced without being assigned...
        Symbol symbol = symbolTable().getRecursive(node.name());
        if (symbol.storage() == SymbolStorage.GLOBAL && !procedures.isEmpty()) {
          // Globals can be referenced inside a proc without being assigned.
        } else if (!symbolTable().isAssigned(node.name())) {
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
      if (formal.type().isUnknown()) {
        if (actual.varType().isUnknown()) {
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
      if (!formal.type().equals(actual.varType())) {
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
  public void visit(BinOpNode node) {
    // Make sure that the left type = right type, mostly.
    Node left = node.left();
    left.accept(this);

    Node right = node.right();
    right.accept(this);

    if (left.varType().isUnknown()) {
      throw new TypeException(String.format("Indeterminable type for %s", left), left.position());
    }
    if (right.varType().isUnknown()) {
      throw new TypeException(String.format("Indeterminable type for %s", right), right.position());
    }

    // Check that they're not trying to, for example, multiply booleans
    if (left.varType() == VarType.BOOL && !BOOLEAN_OPERATORS.contains(node.operator())) {
      throw new TypeException(
              String.format("Cannot apply %s operator to boolean expression", node.operator()),
              left.position());
    }
    if (left.varType() == VarType.INT && !INT_OPERATORS.contains(node.operator())) {
      throw new TypeException(
              String.format("Cannot apply %s operator to int expression", node.operator()),
              left.position());
    }
    if (left.varType() == VarType.STRING && !STRING_OPERATORS.contains(node.operator())) {
      throw new TypeException(
              String.format("Cannot apply %s operator to string expression", node.operator()),
              left.position());
    }
    if (left.varType() == VarType.STRING && node.operator() == Token.Type.LBRACKET) {
      if (right.varType() != VarType.INT) {
        throw new TypeException(
                String.format("Type mismatch: string index must be INT; was %s", right.varType()),
                right.position());
      }
      node.setVarType(VarType.STRING);
      return;
    } else if (left.varType().isArray() && node.operator() == Token.Type.LBRACKET) {
      if (right.varType() != VarType.INT) {
        throw new TypeException(
                String.format("Type mismatch: array index must be INT; was %s", right.varType()),
                right.position());
      }
      // I hate this.
      ArrayType arrayType = (ArrayType) left.varType();
      node.setVarType(arrayType.baseType());
      return;
    } else if (!left.varType().equals(right.varType())) {
      throw new TypeException(String.format("Type mismatch: %s is %s; %s is %s", left,
              left.varType(), right, right.varType()), left.position());
    }

    if ((left.varType() == VarType.INT || left.varType() == VarType.STRING)
            && COMPARISION_OPERATORS.contains(node.operator())) {
      node.setVarType(VarType.BOOL);
    } else {
      node.setVarType(left.varType());
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
    if (boolNode.doStatement().isPresent()) {
      boolNode.doStatement().get().accept(this);
    }
    boolNode.block().accept(this);
  }

  @Override
  public void visit(MainNode node) {
    // TODO: something about arguments? probably add to local symbol table
    node.block().accept(this);
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
  public void visit(ArrayDeclarationNode node) {
    VarType existingType = symbolTable().lookup(node.name(), false);
    if (!existingType.isUnknown()) {
      throw new TypeException(
              String.format("Variable '%s' already declared as %s, cannot be redeclared as %s",
                      node.name(), existingType.name(), node.varType()),
              node.position());
    }

    // Make sure size type is int
    ExprNode arraySizeExpr = node.sizeExpr();
    if (arraySizeExpr.varType().isUnknown()) {
      throw new TypeException("Indeterminable type for array size; must be INT",
              arraySizeExpr.position());
    }
    if (arraySizeExpr.varType() != VarType.INT) {
      throw new TypeException(
              String.format("Array size must be INT; was %s", arraySizeExpr.varType()),
              arraySizeExpr.position());
    }

    symbolTable().declare(node.name(), node.varType());
  }

  @Override
  public void visit(ProcedureNode node) {
    // 1. make sure no duplicate arg names
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
      throw new TypeException(String.format("Duplicate parameter names: %s in procedure %s",
              duplicates.toString(), node.name()), node.position());
    }

    // Add this procedure to the symbol table
    ProcSymbol procSymbol = symbolTable().declareProc(node);

    // 2. spawn symbol table & assign to the node.
    SymTab child = symbolTable().spawn();
    procSymbol.setSymTab(child);

    // 3. push current procedure onto a stack, for symbol table AND return value checking
    procedures.push(procSymbol);
    if (node.returnType() != VarType.VOID) {
      needsReturn.add(procSymbol);
    }

    // 4. add all args to symbol table
    for (Parameter param : node.parameters()) {
      symbolTable().declareParam(param.name(), param.type());
    }

    // 5. process the statements in the procedure:
    node.block().accept(this);

    // 6. make sure args all have a type
    for (Parameter param : node.parameters()) {
      VarType type = symbolTable().get(param.name()).type();
      if (type.isUnknown()) {
        throw new TypeException(
                String.format("Could not determine type of parameter %s of procedure %s",
                        param.name(), node.name()),
                node.position());
      }
    }

    if (node.returnType() != VarType.VOID) {
      if (needsReturn.contains(procSymbol)) {
        // no return statement seen.
        throw new TypeException(
                String.format("No 'return' statement for procedure %s ", node.name()),
                node.position());

      }
      // make sure that all *codepaths* have a return
      if (!checkAllPathsHaveReturn(node)) {
        throw new TypeException(
                String.format("Not all codepaths end with 'return' for procedure %s ", node.name()),
                node.position());
      }
    }
    procedures.pop();
  }

  private boolean checkAllPathsHaveReturn(ProcedureNode node) {
    return checkAllPathsHaveReturn(node.block());
  }

  private boolean checkAllPathsHaveReturn(BlockNode node) {
    /**
     * <pre>
     * If there's a top-level "return" in this block, return true 
     * Else for each statement in the block: 
     *    If it's an "ifNode": 
     *      Ok = true 
     *      For each "case": ok = ok & check "case" block 
     *      If "else" exists, ok = ok & check "else" block
     *      if no "else", ok = false
     *      if ok == true return true
     * return false
     * </pre>
     */
    for (StatementNode stmt : node.statements()) {
      if (stmt instanceof ReturnNode) {
        return true;
      }
    }

    // for each 'if' find if there's a
    for (StatementNode stmt : node.statements()) {
      if (stmt instanceof IfNode) {
        IfNode ifNode = (IfNode) stmt;
        boolean ok = true;
        for (Case ifCase : ifNode.cases()) {
          // make sure all the arms have a return
          ok &= checkAllPathsHaveReturn(ifCase.block());
        }
        BlockNode elseBlock = ifNode.elseBlock();
        if (elseBlock != null) {
          // make sure the else has a return
          ok &= checkAllPathsHaveReturn(elseBlock);
        } else {
          // no "else" - no good.
          ok = false;
        }
        if (ok) {
          // all arms of the "if" had a "return"
          return true;
        }
      }
    }

    return false;
  }

  @Override
  public void visit(ReturnNode node) {
    if (procedures.isEmpty()) {
      throw new TypeException("Cannot return from outside a procedure", node.position());
    }
    if (node.expr().isPresent()) {
      ExprNode expr = node.expr().get();
      expr.accept(this);
      node.setVarType(expr.varType());
    }

    ProcSymbol proc = procedures.peek();
    needsReturn.remove(proc);
    VarType declaredReturnType = proc.returnType();
    VarType actualReturnType = node.varType();

    if (actualReturnType.isUnknown()) {
      throw new TypeException(String.format("Indeterminable type for return statement %s", node),
              node.position());
    }

    if (!proc.node().varType().equals(actualReturnType)) {
      throw new TypeException(
              String.format("Type mismatch: %s declared to return %s but returned %s", proc.name(),
                      declaredReturnType, actualReturnType),
              node.position());
    }
  }
}
