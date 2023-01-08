package com.plasstech.lang.d2.type;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.plasstech.lang.d2.common.D2RuntimeException;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.parse.node.ArrayDeclarationNode;
import com.plasstech.lang.d2.parse.node.ArrayLiteralNode;
import com.plasstech.lang.d2.parse.node.ArraySetNode;
import com.plasstech.lang.d2.parse.node.AssignmentNode;
import com.plasstech.lang.d2.parse.node.BinOpNode;
import com.plasstech.lang.d2.parse.node.BlockNode;
import com.plasstech.lang.d2.parse.node.CallNode;
import com.plasstech.lang.d2.parse.node.ConstNode;
import com.plasstech.lang.d2.parse.node.DeclarationNode;
import com.plasstech.lang.d2.parse.node.DefaultNodeVisitor;
import com.plasstech.lang.d2.parse.node.ExitNode;
import com.plasstech.lang.d2.parse.node.ExprNode;
import com.plasstech.lang.d2.parse.node.ExternProcedureNode;
import com.plasstech.lang.d2.parse.node.FieldSetNode;
import com.plasstech.lang.d2.parse.node.IfNode;
import com.plasstech.lang.d2.parse.node.IfNode.Case;
import com.plasstech.lang.d2.parse.node.IncDecNode;
import com.plasstech.lang.d2.parse.node.LValueNode;
import com.plasstech.lang.d2.parse.node.MainNode;
import com.plasstech.lang.d2.parse.node.NewNode;
import com.plasstech.lang.d2.parse.node.Node;
import com.plasstech.lang.d2.parse.node.NodeVisitor;
import com.plasstech.lang.d2.parse.node.PrintNode;
import com.plasstech.lang.d2.parse.node.ProcedureNode;
import com.plasstech.lang.d2.parse.node.ProcedureNode.Parameter;
import com.plasstech.lang.d2.parse.node.RecordDeclarationNode;
import com.plasstech.lang.d2.parse.node.ReturnNode;
import com.plasstech.lang.d2.parse.node.StatementNode;
import com.plasstech.lang.d2.parse.node.UnaryNode;
import com.plasstech.lang.d2.parse.node.VariableNode;
import com.plasstech.lang.d2.parse.node.VariableSetNode;
import com.plasstech.lang.d2.parse.node.WhileNode;
import com.plasstech.lang.d2.phase.Errors;
import com.plasstech.lang.d2.phase.Phase;
import com.plasstech.lang.d2.phase.State;

public class StaticChecker extends DefaultNodeVisitor implements Phase {
  // also works for bytes
  private static final Set<TokenType> INT_OPERATORS =
      ImmutableSet.of(
          TokenType.EQEQ,
          TokenType.LT,
          TokenType.GT,
          TokenType.LEQ,
          TokenType.GEQ,
          TokenType.NEQ,
          TokenType.DIV,
          TokenType.MINUS,
          TokenType.MOD,
          TokenType.MULT,
          TokenType.PLUS,
          TokenType.SHIFT_LEFT,
          TokenType.SHIFT_RIGHT,
          TokenType.BIT_OR,
          TokenType.BIT_XOR,
          TokenType.BIT_AND);

  private static final Set<TokenType> DOUBLE_OPERATORS =
      ImmutableSet.of(
          TokenType.EQEQ,
          TokenType.LT,
          TokenType.GT,
          TokenType.LEQ,
          TokenType.GEQ,
          TokenType.NEQ,
          TokenType.DIV,
          TokenType.MINUS,
          TokenType.MULT,
          TokenType.PLUS);

  private static final Set<TokenType> STRING_OPERATORS =
      ImmutableSet.of(
          TokenType.EQEQ,
          TokenType.LT,
          TokenType.GT,
          TokenType.LEQ,
          TokenType.GEQ,
          TokenType.NEQ,
          TokenType.PLUS,
          TokenType.LBRACKET
          // , Token.Type.MOD // eventually
          );

  private static final Set<TokenType> BOOL_OPERATORS =
      ImmutableSet.of(
          TokenType.LT,
          TokenType.LEQ,
          TokenType.EQEQ,
          TokenType.GT,
          TokenType.GEQ,
          TokenType.NEQ,
          TokenType.AND,
          TokenType.OR,
          TokenType.XOR);

  private static final Set<TokenType> NULL_OPERATORS =
      ImmutableSet.of(TokenType.EQEQ, TokenType.NEQ);

  // NOTE: Does not include arrays or records.
  private static final Map<VarType, Set<TokenType>> OPERATORS_BY_LEFT_VARTYPE =
      ImmutableMap.<VarType, Set<TokenType>>builder()
          .put(VarType.BOOL, BOOL_OPERATORS)
          .put(VarType.BYTE, INT_OPERATORS)
          .put(VarType.DOUBLE, DOUBLE_OPERATORS)
          .put(VarType.INT, INT_OPERATORS)
          .put(VarType.LONG, INT_OPERATORS)
          .put(VarType.NULL, NULL_OPERATORS)
          .put(VarType.STRING, STRING_OPERATORS)
          .build();

  private static final ImmutableSet<TokenType> INT_UNARY_OPERATORS =
      ImmutableSet.of(TokenType.MINUS, TokenType.PLUS, TokenType.BIT_NOT, TokenType.CHR);

  private static final ImmutableSet<TokenType> DOUBLE_UNARY_OPERATORS =
      ImmutableSet.of(TokenType.MINUS, TokenType.PLUS);

  private static final ImmutableSet<TokenType> STRING_UNARY_OPERATORS =
      ImmutableSet.of(TokenType.LENGTH, TokenType.ASC);

  private static final ImmutableSet<TokenType> BOOL_UNARY_OPERATORS =
      ImmutableSet.of(TokenType.NOT);

  private static final Map<VarType, ImmutableSet<TokenType>> UNARY_OPERATORS_BY_VARTYPE =
      ImmutableMap.<VarType, ImmutableSet<TokenType>>builder()
          .put(VarType.BOOL, BOOL_UNARY_OPERATORS)
          .put(VarType.BYTE, INT_UNARY_OPERATORS)
          .put(VarType.DOUBLE, DOUBLE_UNARY_OPERATORS)
          .put(VarType.INT, INT_UNARY_OPERATORS)
          .put(VarType.LONG, INT_UNARY_OPERATORS)
          .put(VarType.STRING, STRING_UNARY_OPERATORS)
          .build();

  private static final Set<TokenType> RECORD_COMPARATORS =
      ImmutableSet.of(TokenType.EQEQ, TokenType.NEQ);

  private static final ImmutableSet<TokenType> COMPARISION_OPERATORS =
      ImmutableSet.of(
          TokenType.EQEQ, TokenType.LT, TokenType.GT, TokenType.LEQ, TokenType.GEQ, TokenType.NEQ);

  /** VarTypes that can be compared using COMPARISON_OPERATORS */
  private static final ImmutableSet<VarType> COMPARABLE_VARTYPES =
      ImmutableSet.of(VarType.BYTE, VarType.DOUBLE, VarType.INT, VarType.LONG, VarType.STRING);

  private static final Set<TokenType> ARRAY_OPERATORS =
      ImmutableSet.of(TokenType.EQEQ, TokenType.NEQ, TokenType.LBRACKET);

  private Node root;
  private final SymTab symbolTable = new SymTab();

  private final Stack<ProcSymbol> procedures = new Stack<>();
  private final Set<ProcSymbol> needsReturn = new HashSet<>();
  private final Errors errors = new Errors();

  @Override
  public State execute(State input) {
    assert input.programNode() != null;
    root = input.programNode();
    TypeCheckResult result = execute();
    return input.addTypecheckResult(result);
  }

  private TypeCheckResult execute() {
    NodeVisitor procGatherer = new ProcGatherer(symbolTable);
    try {
      root.accept(procGatherer);
    } catch (D2RuntimeException e) {
      return new TypeCheckResult(e);
    }

    NodeVisitor recordGatherer = new RecordGatherer(symbolTable);
    try {
      root.accept(recordGatherer);
    } catch (D2RuntimeException e) {
      return new TypeCheckResult(e);
    }

    try {
      root.accept(this);
      if (!procedures.isEmpty()) {
        ProcSymbol top = procedures.peek();
        errors.add(
            new TypeException(
                String.format("Still in PROC '%s'. (This should never happen)", top.name()),
                top.position()));
      }
      if (errors.hasErrors()) {
        return new TypeCheckResult(errors);
      }
      return new TypeCheckResult(symbolTable);
    } catch (D2RuntimeException e) {
      return new TypeCheckResult(errors);
    } catch (IllegalArgumentException e) {
      // Closes #217 once and for all. BUT... ONCE AND FOR ALL.
      e.printStackTrace();
      errors.add(new D2RuntimeException(e.getMessage(), root.position(), "INTERNAL ERROR"));
      return new TypeCheckResult(errors);
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
      errors.add(
          new TypeException(String.format("Indeterminable type for %s", expr), expr.position()));
    }
  }

  @Override
  public void visit(ArrayLiteralNode node) {
    // Make sure all element types are the same.
    ArrayType arrayType = node.arrayType();
    VarType baseType = arrayType.baseType();
    int i = 0;
    for (ExprNode element : node.elements()) {
      element.accept(this);
      if (element.varType().isUnknown()) {
        errors.add(
            new TypeException(
                String.format("Indeterminable type for %s", element), element.position()));
      }

      // this may fail for records, bleah.
      if (element.varType() != baseType) {
        errors.add(
            new TypeException(
                String.format(
                    "Inconsistent type in array literal; expected %s but element %d was %s",
                    baseType, i, element.varType()),
                element.position()));
      }
      i++;
    }
  }

  @Override
  public void visit(AssignmentNode node) {
    // Make sure that the left = right

    Node right = node.expr();
    right.accept(this);
    if (right.varType().isUnknown()) {
      // TODO: Can we infer anything from this?
      errors.add(
          new TypeException(String.format("Indeterminable type for %s", right), right.position()));
      return;
    }
    if (right.varType() == VarType.VOID) {
      errors.add(
          new TypeException(
              String.format("Cannot assign value of VOID expression %s", right), right.position()));
      return;
    }

    LValueNode lvalue = node.lvalue();
    lvalue.accept(
        new LValueNode.Visitor() {
          @Override
          public void visit(FieldSetNode fsn) {
            // Get the record from the symbol table.
            Symbol variableSymbol = symbolTable().getRecursive(fsn.variableName());
            if (variableSymbol.varType() == VarType.PROC) {
              // can't assign to a proc
              errors.add(
                  new TypeException(
                      String.format(
                          "Cannot dereference '%s' as RECORD; already declared as PROC",
                          lvalue.name()),
                      lvalue.position()));
              return;
            }
            VarType varType = variableSymbol.varType();
            if (variableSymbol == null || !varType.isRecord()) {
              errors.add(
                  new TypeException(
                      String.format(
                          "Cannot set field of variable '%s' of type %s; not a known RECORD",
                          lvalue.name(), varType),
                      lvalue.position()));
              return;
            }

            Symbol recordSymbol = symbolTable().getRecursive(varType.name());
            if (recordSymbol == null || !(recordSymbol instanceof RecordSymbol)) {
              // this should never happen because the varType.isRecord, above, should have caught
              // it.
              errors.add(
                  new TypeException(
                      String.format(
                          "Cannot set field of variable '%s' of type %s; not a known RECORD",
                          lvalue.name(), varType),
                      fsn.position()));
              return;
            }

            RecordSymbol record = (RecordSymbol) recordSymbol;
            // make sure string after the dot is a field in record
            String fieldName = fsn.fieldName();
            VarType fieldType = record.fieldType(fieldName);
            if (fieldType == VarType.UNKNOWN) {
              errors.add(
                  new TypeException(
                      String.format(
                          "Cannot set unknown field %s of RECORD type %s",
                          fieldName, record.name()),
                      fsn.position()));
              return;
            } else if (!fieldType.compatibleWith(right.varType())) {
              errors.add(
                  new TypeException(
                      String.format(
                          "Field %s of RECORD type %s declared as %s but expression is %s",
                          fieldName, record.name(), fieldType, right.varType()),
                      lvalue.position()));
            }
            lvalue.setVarType(fieldType);
          }

          @Override
          public void visit(VariableSetNode node) {
            // simple this=that
            Symbol symbol = symbolTable().getRecursive(lvalue.name());
            if (symbol == null) {
              // Brand new symbol in all scopes. Assign in current scope.
              symbolTable().assign(lvalue.name(), right.varType());
            } else {
              // Already known in some scope. Update.
              if (symbol.varType().isUnknown()) {
                symbol.setVarType(right.varType());
              } else if (symbol.varType() == VarType.PROC) {
                // can't assign to a proc
                errors.add(
                    new TypeException(
                        String.format(
                            "Cannot assign '%s' as %s; already declared as PROC",
                            lvalue.name(), right.varType()),
                        lvalue.position()));
                return;
              } else if (!symbol.varType().compatibleWith(right.varType())) {
                // It was already in the symbol table. Possible that it's wrong.
                errors.add(
                    new TypeException(
                        String.format(
                            "Cannot convert variable '%s' from declared type %s to %s",
                            lvalue.name(), symbol.varType(), right.varType()),
                        lvalue.position()));
                return;
              }

              // TODO(#38): if arrays, the # of dimensions must match.

              symbol.setAssigned();
            }

            lvalue.setVarType(right.varType());
          }

          @Override
          public void visit(ArraySetNode asn) {
            // 1. lhs must be known array
            String variableName = asn.variableName();
            Symbol symbol = symbolTable().getRecursive(variableName);
            if (symbol == null) {
              // this should never happen?
              errors.add(
                  new TypeException(
                      String.format("Unknown variable '%s' used as ARRAY", variableName),
                      lvalue.position()));
              return;
            }
            if (!symbol.varType().isArray()) {
              errors.add(
                  new TypeException(
                      String.format(
                          "Variable '%s' used as ARRAY; was %s", variableName, symbol.varType()),
                      lvalue.position()));
              return;
            }
            asn.setVarType(symbol.varType());

            // 2. index must be int
            ExprNode indexNode = asn.indexNode();
            indexNode.accept(StaticChecker.this);
            VarType indexType = indexNode.varType();
            if (indexType != VarType.INT) {
              errors.add(
                  new TypeException(
                      String.format("ARRAY index must be INT; was %s", indexType),
                      indexNode.position()));
              return;
            }
            if (indexNode.isConstant()) {
              ConstNode<Integer> index = (ConstNode<Integer>) indexNode;
              if (index.value() < 0) {
                errors.add(
                    new TypeException(
                        String.format("ARRAY index must be non-negative; was %d", index.value()),
                        indexNode.position()));
              }
            }

            ArrayType arrayType = (ArrayType) symbol.varType();

            if (right.varType().isUnknown()) {
              // should we infer it?
            }
            // 3. rhs must match lhs
            if (!arrayType.baseType().compatibleWith(right.varType())) {
              errors.add(
                  new TypeException(
                      String.format(
                          "Variable '%s' declared as ARRAY of %s but expression resolved to %s",
                          variableName, arrayType.baseType(), right.varType()),
                      lvalue.position()));
            }
          }
        });

    // TODO: why do we need variable.vartype, node.vartype and symbol.type?
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
          errors.add(
              new TypeException(
                  String.format("Variable '%s' used before assignment", node.name()),
                  node.position()));
          return;
        }
        node.setVarType(existingType);
      }
    } else if (node.name().equals("ARGS")) {
      VarType type = symbolTable().lookup(node.name());
      if (type == VarType.UNKNOWN) {
        // put it in the global symbol table
        symbolTable.declare(node.name(), node.varType());
      }
    }
  }

  @Override
  public void visit(CallNode node) {
    // 1. make sure the function is really a function
    Symbol maybeProc = symbolTable().getRecursive(node.procName());
    if (maybeProc == null || maybeProc.varType() != VarType.PROC) {
      errors.add(
          new TypeException(
              String.format("PROC '%s' is undefined", node.procName()), node.position()));
      return;
    }
    // 2. make sure the arg length is right.
    ProcSymbol proc = (ProcSymbol) maybeProc;
    if (proc.formals().size() != node.actuals().size()) {
      errors.add(
          new TypeException(
              String.format(
                  "Wrong number of arguments in call to PROC '%s': found %d, expected %d",
                  node.procName(), node.actuals().size(), proc.formals().size()),
              node.position()));
    }
    // 3. eval parameter expressions.
    node.actuals().forEach(actual -> actual.accept(this));

    // 4. for each param, if param type is unknown, set it from the expr if possible
    for (int i = 0; i < Math.min(proc.formals().size(), node.actuals().size()); ++i) {
      ParamSymbol formal = proc.formal(i);
      ExprNode actual = node.actuals().get(i);
      if (formal.varType().isUnknown()) {
        if (actual.varType().isUnknown()) {
          errors.add(
              new TypeException(
                  String.format(
                      "Indeterminable type for parameter '%s' of PROC '%s'",
                      formal.name(), proc.name()),
                  node.position()));
        } else {
          formal.setVarType(actual.varType());
        }
      }
      // 5. make sure expr types == param types
      if (!formal.varType().compatibleWith(actual.varType())) {
        errors.add(
            new TypeException(
                String.format(
                    "Incorrect type of parameter '%s' to PROC '%s': found %s, expected %s",
                    formal.name(), proc.name(), actual.varType(), formal.varType()),
                node.position()));
        return;
      }
    }
    // 6. set the type of the expression to the return type of the node
    node.setVarType(proc.returnType());
  }

  @Override
  public void visit(BinOpNode node) {
    // Make sure that the left type = right type, mostly.
    ExprNode left = node.left();
    left.accept(this);

    ExprNode right = node.right();
    right.accept(this);

    VarType leftType = left.varType();
    if (leftType.isUnknown()) {
      errors.add(
          new TypeException(
              String.format("Indeterminable type for expression %s", left), left.position()));
      // stop here, because it's only going to get worse
      return;
    }

    // Only care if RHS is unknown if it's not DOT, because fields are not exactly like variables
    TokenType operator = node.operator();
    VarType rightType = right.varType();
    if (operator != TokenType.DOT && rightType.isUnknown()) {
      errors.add(
          new TypeException(
              String.format("Indeterminable type for expression %s", right), right.position()));
      // stop here, because it's only going to get worse
    }

    // Check that they're not trying to, for example, multiply booleans
    Set<TokenType> operators = OPERATORS_BY_LEFT_VARTYPE.get(leftType);
    if (operators != null && !operators.contains(operator)) {
      errors.add(
          new TypeException(
              String.format(
                  "Cannot apply %s operator to left operand of type %s", operator, leftType),
              left.position()));
      return;
    }

    if (operator == TokenType.DOT) {
      // Now get the record from the symbol table.
      String recordName = leftType.name();
      Symbol symbol = symbolTable().getRecursive(recordName);
      if (symbol == null || !symbol.varType().isRecord()) {
        errors.add(
            new TypeException(
                // this might be better
                String.format("Unknown RECORD type '%s'", recordName), left.position()));
        return;
      }
      if (!(right instanceof VariableNode)) {
        errors.add(
            new TypeException(
                String.format(
                    "Cannot use expression %s to get field of RECORD type %s", right, recordName),
                right.position()));
        return;
      }
      // make sure RHS is a field in record
      String fieldName = ((VariableNode) right).name();
      RecordSymbol recordSymbol = (RecordSymbol) symbol;
      VarType fieldType = recordSymbol.fieldType(fieldName);
      if (fieldType == VarType.UNKNOWN) {
        errors.add(
            new TypeException(
                String.format(
                    "Cannot get unknown field %s of RECORD type %s",
                    fieldName, recordSymbol.name()),
                right.position()));
        return;
      }
      node.setVarType(fieldType);
      // NOTE: RETURN
      return;
    }

    if (leftType.isArray() && !ARRAY_OPERATORS.contains(operator)) {
      errors.add(
          new TypeException(
              String.format("Cannot apply %s operator to ARRAY expression", operator),
              left.position()));
      return;
    }

    if (leftType == VarType.STRING && operator == TokenType.PLUS && rightType.isNull()) {
      // Ugh, this is such a random one-off...
      errors.add(new TypeException("Cannot add NULL to STRING", right.position()));
    }

    if (operator == TokenType.LBRACKET) {
      // string[int] and array[int]
      if (rightType != VarType.INT) {
        errors.add(
            new TypeException(
                String.format("Index of ARRAY variable '%s' must be INT; was %s", left, rightType),
                right.position()));
        return;
      }
      if (right.isConstant()) {
        ConstNode<Integer> index = (ConstNode<Integer>) right;
        if (index.value() < 0) {
          errors.add(
              new TypeException(
                  String.format(
                      "Index of ARRAY variable '%s' must be non-negative; was %d",
                      left, index.value()),
                  right.position()));
        }
      }
      if (leftType == VarType.STRING) {
        node.setVarType(VarType.STRING);
        // NOTE RETURN
        return;
      }
      if (leftType.isUnknown()) {
        errors.add(
            new TypeException(
                String.format("Indeterminable type used as ARRAY: %s", left), left.position()));
        // note return; can't do anything.
        return;
      }

      // bug #214
      if (!(leftType instanceof ArrayType)) {
        errors.add(
            new TypeException(
                String.format("%s cannot be used as ARRAY", leftType), left.position()));
        return;
      }
      // TODO: Generalize this, e.g., have a "baseType" method on VarType
      ArrayType arrayType = (ArrayType) leftType;
      node.setVarType(arrayType.baseType());
      // NOTE RETURN
      return;

    } else if (!leftType.compatibleWith(rightType)) {
      errors.add(
          new TypeException(
              String.format(
                  "Incompatible types for operator %s; left operand is %s but right is %s",
                  operator, leftType, rightType),
              left.position()));
    }

    if ((COMPARABLE_VARTYPES.contains(leftType) && COMPARISION_OPERATORS.contains(operator))
        || (leftType.isArray() && ARRAY_OPERATORS.contains(operator))
        || ((leftType.isRecord() || leftType.isNull()) && RECORD_COMPARATORS.contains(operator))) {
      node.setVarType(VarType.BOOL);
    } else {
      if (leftType.isUnknown()) {
        // Can't do much more. Fixed bug #204
        return;
      }

      node.setVarType(leftType);
    }
  }

  @Override
  public void visit(NewNode node) {
    validatePossibleRecordType(node.recordName(), node.varType(), node.position());
  }

  @Override
  public void visit(UnaryNode node) {
    Node expr = node.expr();
    expr.accept(this);

    VarType exprType = expr.varType();
    if (exprType.isUnknown()) {
      errors.add(
          new TypeException(
              String.format("Indeterminable type for expression %s", expr), expr.position()));
      return;
    }
    if (exprType.isNull()) {
      errors.add(
          new TypeException(
              String.format("Cannot apply %s operator to NULL expression", node.operator()),
              expr.position()));
      return;
    }

    // Check that they're not trying to negate a boolean or "not" an int.
    ImmutableSet<TokenType> operators = UNARY_OPERATORS_BY_VARTYPE.get(exprType);
    if (operators != null && !operators.contains(node.operator())) {
      errors.add(
          new TypeException(
              String.format("Cannot apply %s operator to %s expression", node.operator(), exprType),
              node.position()));
      return;
    }

    // Extra checks for some operators:
    switch (node.operator()) {
      case LENGTH:
        if (exprType != VarType.STRING && !exprType.isArray()) {
          errors.add(
              new TypeException(
                  String.format("Cannot apply LENGTH function to %s expression", exprType),
                  expr.position()));
        }
        node.setVarType(VarType.INT);
        break;
      case ASC:
        node.setVarType(VarType.INT);
        break;
      case CHR:
        node.setVarType(VarType.STRING);
        break;
      default:
        // Output type is the same as input type, except for cases, above.
        node.setVarType(exprType);
        break;
    }
  }

  @Override
  public void visit(IfNode node) {
    Set<Node> conditions = new HashSet<>();
    for (IfNode.Case ifCase : node.cases()) {
      Node condition = ifCase.condition();
      condition.accept(this);
      if (condition.varType() != VarType.BOOL) {
        errors.add(
            new TypeException(
                String.format(
                    "Cannot use %s expression in IF or ELIF; must be BOOL", condition.varType()),
                condition.position()));
      }
      if (conditions.contains(condition)) {
        errors.add(
            new TypeException(
                String.format("Duplicate expression %s in IF/ELIF", condition),
                condition.position()));
      }
      conditions.add(condition);
      ifCase
          .block()
          .statements()
          .forEach(
              stmt -> {
                stmt.accept(this);
              });
    }
    if (node.elseBlock() != null) {
      node.elseBlock()
          .statements()
          .forEach(
              stmt -> {
                stmt.accept(this);
              });
    }
  }

  @Override
  public void visit(WhileNode node) {
    Node condition = node.condition();
    condition.accept(this);
    if (condition.varType() != VarType.BOOL) {
      errors.add(
          new TypeException(
              String.format("Cannot use %s expression in WHILE; must be BOOL", condition.varType()),
              condition.position()));
    }
    if (node.doStatement().isPresent()) {
      node.doStatement().get().accept(this);
    }
    node.block().accept(this);
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
      errors.add(
          new TypeException(
              String.format(
                  "Variable '%s' already declared as %s, cannot be redeclared as %s",
                  node.name(), existingType.name(), node.varType()),
              node.position()));
      return;
    }
    // gotta make sure it exists
    validatePossibleRecordType(node.name(), node.varType(), node.position());

    symbolTable().declare(node.name(), node.varType());
  }

  private Symbol validatePossibleRecordType(String name, VarType type, Position position) {
    if (type.isRecord()) {
      String recordName = ((RecordReferenceType) type).name();
      Symbol symbol = symbolTable().getRecursive(recordName);
      if (symbol == null || !symbol.varType().isRecord()) {
        errors.add(
            new TypeException(
                String.format(
                    "Cannot declare variable '%s' as unknown RECORD type %s", name, recordName),
                position));
      }
      return symbol;
    }
    return null;
  }

  @Override
  public void visit(ArrayDeclarationNode node) {
    VarType existingType = symbolTable().lookup(node.name(), false);
    if (!existingType.isUnknown()) {
      errors.add(
          new TypeException(
              String.format(
                  "Variable '%s' already declared as %s, cannot be redeclared as %s",
                  node.name(), existingType.name(), node.varType()),
              node.position()));
      return;
    }

    // Make sure size type is int
    ExprNode arraySizeExpr = node.sizeExpr();
    arraySizeExpr.accept(this);
    if (arraySizeExpr.varType().isUnknown()) {
      errors.add(
          new TypeException(
              String.format(
                  "Indeterminable size for ARRAY variable '%s'; must be INT", node.name()),
              arraySizeExpr.position()));
      return;
    }
    if (arraySizeExpr.varType() != VarType.INT) {
      errors.add(
          new TypeException(
              String.format(
                  "Size of ARRAY variable '%s' must be INT; was %s",
                  node.name(), arraySizeExpr.varType()),
              arraySizeExpr.position()));
      return;
    }
    if (arraySizeExpr.isConstant()) {
      ConstNode<Integer> size = (ConstNode<Integer>) arraySizeExpr;
      if (size.value() <= 0) {
        // Peephole optimization
        errors.add(
            new D2RuntimeException(
                String.format(
                    "Size of ARRAY variable '%s' must be positive; was %d",
                    node.name(), size.value()),
                arraySizeExpr.position(),
                "Invalid value"));
        return;
      }
    }

    // declaring the array actually assigns it.
    symbolTable().assign(node.name(), node.varType());
  }

  @Override
  public void visit(RecordDeclarationNode node) {
    Symbol sym = symbolTable().get(node.name());
    if (sym == null) {
      // Updates the symbol table with this symbol. It might be null because
      // it's defining a local record (in a procedure). Which, shrug, I'm not even sure
      // the rest of the codebase supports...
      new RecordGatherer(symbolTable()).visit(node);
      sym = symbolTable().get(node.name());
    }

    // If we got this far, we know that the record doesn't have duplicate fields, or
    // invalid field types. Now we have to make sure any record fields are valid.
    for (DeclarationNode field : node.fields()) {
      VarType fieldType = field.varType();
      if (fieldType.isRecord()) {
        // field that is a record.
        // Make sure it exists.
        RecordReferenceType rrt = (RecordReferenceType) fieldType;
        String recordTypeName = rrt.name();
        Symbol putativeRecordSymbol = symbolTable().getRecursive(recordTypeName);
        if (putativeRecordSymbol == null) {
          errors.add(
              new TypeException(
                  String.format(
                      "Field '%s' of RECORD type %s declared as unknown RECORD type %s",
                      field.name(), node.name(), recordTypeName),
                  field.position()));
        }
      }
    }
  }

  @Override
  public void visit(ExternProcedureNode node) {
    // make sure args all have a type
    for (Parameter param : node.parameters()) {
      VarType type = param.varType();
      validatePossibleRecordType(param.name(), type, node.position());
      if (type.isUnknown()) {
        errors.add(
            new TypeException(
                String.format(
                    "Could not determine type of parameter '%s' of EXTERN PROC '%s'",
                    param.name(), node.name()),
                node.position()));
      }
    }
  }

  @Override
  public void visit(ProcedureNode node) {
    // Add this procedure to the symbol table if it's a nested proc.
    Symbol sym = symbolTable().get(node.name());
    boolean innerProc = sym == null;
    ProcSymbol procSymbol = null;
    if (sym == null) {
      // nested proc; spawn symbol table & assign to the node.
      procSymbol = symbolTable().declareProc(node);
    } else if (sym.varType() != VarType.PROC) {
      errors.add(
          new TypeException(
              String.format("Cannot define PROC '%s' with the same name as a global", node.name()),
              node.position()));
    } else {
      procSymbol = (ProcSymbol) sym;
    }

    // push current procedure onto a stack, for symbol table AND return value checking
    procedures.push(procSymbol);
    if (node.returnType() != VarType.VOID) {
      needsReturn.add(procSymbol);
    }

    // add all parameters to local symbol table
    if (innerProc) {
      int i = 0;
      for (Parameter param : node.parameters()) {
        procSymbol.declareParam(param.name(), param.varType(), i++);
      }
    }

    // process the statements in the procedure:
    node.block().accept(this);

    // make sure args all have a type
    for (Parameter param : node.parameters()) {
      VarType type = symbolTable().get(param.name()).varType();
      validatePossibleRecordType(param.name(), type, node.position());
      if (type.isUnknown()) {
        errors.add(
            new TypeException(
                String.format(
                    "Could not determine type of parameter '%s' of PROC '%s'",
                    param.name(), node.name()),
                node.position()));
      }
    }

    // make sure that all codepaths have a return
    if (node.returnType() != VarType.VOID) {
      if (needsReturn.contains(procSymbol)) {
        // no return statement seen.
        errors.add(
            new TypeException(
                String.format("Not all codepaths end with RETURN for PROC '%s'", node.name()),
                node.position()));
      }
      if (!checkAllPathsHaveReturn(node)) {
        errors.add(
            new TypeException(
                String.format("Not all codepaths end with RETURN for PROC '%s'", node.name()),
                node.position()));
      } else {
        validatePossibleRecordType("return type", node.returnType(), node.position());
      }
    }
    procedures.pop();
  }

  private boolean checkAllPathsHaveReturn(ProcedureNode node) {
    return checkAllPathsHaveReturn(node.block());
  }

  private boolean checkAllPathsHaveReturn(BlockNode node) {
    /*
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
      if (stmt instanceof ReturnNode || stmt instanceof ExitNode) {
        return true;
      }
    }

    for (StatementNode stmt : node.statements()) {
      if (stmt instanceof IfNode) {
        // for each 'if':
        IfNode ifNode = (IfNode) stmt;
        boolean ok = true;
        for (Case ifCase : ifNode.cases()) {
          // make sure all the "case" arms have a return
          ok &= checkAllPathsHaveReturn(ifCase.block());
        }
        BlockNode elseBlock = ifNode.elseBlock();
        if (elseBlock != null) {
          // make sure the "else" has a return
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
      errors.add(new TypeException("Cannot RETURN from outside a PROC", node.position()));
      return;
    }
    if (node.expr().isPresent()) {
      ExprNode expr = node.expr().get();
      expr.accept(this);
      if (expr.varType().isUnknown()) {
        errors.add(
            new TypeException(
                String.format("Indeterminable type for RETURN statement: %s", node),
                node.position()));
        ProcSymbol proc = procedures.peek();
        needsReturn.remove(proc);
        return;
      }
      node.setVarType(expr.varType());
    }

    ProcSymbol proc = procedures.peek();
    needsReturn.remove(proc);
    VarType declaredReturnType = proc.returnType();
    VarType actualReturnType = node.varType();

    if (actualReturnType.isUnknown()) {
      errors.add(
          new TypeException(
              String.format("Indeterminable type for RETURN statement: %s", node),
              node.position()));
    }

    if (!proc.returnType().compatibleWith(actualReturnType)) {
      errors.add(
          new TypeException(
              String.format(
                  "PROC '%s' declared to return %s but RETURN statement was of type %s",
                  proc.name(), declaredReturnType, actualReturnType),
              node.position()));
    }
  }

  @Override
  public void visit(ExitNode node) {
    if (node.exitMessage().isPresent()) {
      ExprNode message = node.exitMessage().get();
      message.accept(this);
      VarType actualMessageType = message.varType();
      if (actualMessageType.isUnknown()) {
        errors.add(
            new TypeException(
                String.format("Indeterminable type for EXIT message: %s", message),
                node.position()));
        return;
      }
      if (message.varType() != VarType.STRING) {
        errors.add(
            new TypeException(
                String.format(
                    "Cannot use %s expression as EXIT message; must be STRING", message.varType()),
                message.position()));
      }
    }
    if (!procedures.isEmpty()) {
      ProcSymbol proc = procedures.peek();
      if (proc != null) {
        needsReturn.remove(proc);
      }
    }
  }

  @Override
  public void visit(IncDecNode node) {
    // make sure variable is a byte, int, long.
    Symbol symbol = symbolTable().getRecursive(node.name());
    if (symbol == null || symbol.varType().isUnknown() || !symbol.isAssigned()) {
      // cannot do this, we do not know what type it is.
      errors.add(
          new TypeException(
              String.format(
                  "Cannot %screment '%s'; type is unknown",
                  node.isIncrement() ? "in" : "de", node.name()),
              node.position()));
      return;
    }
    VarType type = symbol.varType();
    if (type == VarType.PROC) {
      // can't assign to a proc
      errors.add(
          new TypeException(
              String.format(
                  "Cannot %screment '%s'; already declared as PROC",
                  node.isIncrement() ? "in" : "de", node.name()),
              node.position()));
      return;
    }
    if (!VarType.INTEGRAL_TYPES.contains(type)) {
      // It was already in the symbol table, but not aintegral
      errors.add(
          new TypeException(
              String.format(
                  "Cannot %screment variable '%s'; already declared as %s",
                  node.isIncrement() ? "in" : "de", node.name(), type),
              node.position()));
      return;
    }
    node.setVarType(type);
  }
}
