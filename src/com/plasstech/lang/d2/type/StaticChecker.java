package com.plasstech.lang.d2.type;

import static com.google.common.collect.ImmutableList.toImmutableList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import com.google.common.collect.ImmutableSet;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.lex.Token;
import com.plasstech.lang.d2.lex.Token.Type;
import com.plasstech.lang.d2.parse.node.ArrayDeclarationNode;
import com.plasstech.lang.d2.parse.node.AssignmentNode;
import com.plasstech.lang.d2.parse.node.BinOpNode;
import com.plasstech.lang.d2.parse.node.BlockNode;
import com.plasstech.lang.d2.parse.node.CallNode;
import com.plasstech.lang.d2.parse.node.DeclarationNode;
import com.plasstech.lang.d2.parse.node.DefaultVisitor;
import com.plasstech.lang.d2.parse.node.ExitNode;
import com.plasstech.lang.d2.parse.node.ExprNode;
import com.plasstech.lang.d2.parse.node.FieldSetNode;
import com.plasstech.lang.d2.parse.node.IfNode;
import com.plasstech.lang.d2.parse.node.IfNode.Case;
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

public class StaticChecker extends DefaultVisitor {
  private static final Set<Token.Type> COMPARISION_OPERATORS =
      ImmutableSet.of(
          Token.Type.AND,
          Token.Type.OR,
          Token.Type.EQEQ,
          Token.Type.LT,
          Token.Type.GT,
          Token.Type.LEQ,
          Token.Type.GEQ,
          Token.Type.NEQ);

  private static final Set<Token.Type> INT_OPERATORS =
      ImmutableSet.of(
          Token.Type.EQEQ,
          Token.Type.LT,
          Token.Type.GT,
          Token.Type.LEQ,
          Token.Type.GEQ,
          Token.Type.NEQ,
          Token.Type.DIV,
          Token.Type.MINUS,
          Token.Type.MOD,
          Token.Type.MULT,
          Token.Type.PLUS,
          Token.Type.SHIFT_LEFT,
          Token.Type.SHIFT_RIGHT,
          Token.Type.BIT_OR,
          Token.Type.BIT_XOR,
          Token.Type.BIT_AND);

  private static final Set<Token.Type> STRING_OPERATORS =
      ImmutableSet.of(
          Token.Type.EQEQ,
          Token.Type.LT,
          Token.Type.GT,
          Token.Type.LEQ,
          Token.Type.GEQ,
          Token.Type.NEQ,
          Token.Type.PLUS,
          Token.Type.LBRACKET
          // , Token.Type.MOD // eventually
          );

  private static final Set<Token.Type> RECORD_COMPARATORS =
      ImmutableSet.of(Token.Type.EQEQ, Token.Type.NEQ);

  private static final Set<Token.Type> BOOLEAN_OPERATORS =
      ImmutableSet.of(
          Token.Type.EQEQ,
          Token.Type.LT,
          Token.Type.GT,
          Token.Type.NEQ,
          Token.Type.AND,
          Token.Type.OR,
          Token.Type.XOR);

  // TODO(#14): implement EQEQ and NEQ for arrays
  private static final Set<Token.Type> ARRAY_OPERATORS =
      ImmutableSet.of(Token.Type.EQEQ, Token.Type.NEQ, Token.Type.LBRACKET);

  private final Node root;
  private final SymTab symbolTable = new SymTab();

  private Stack<ProcSymbol> procedures = new Stack<>();
  private Set<ProcSymbol> needsReturn = new HashSet<>();

  public StaticChecker(Node root) {
    this.root = root;
  }

  public TypeCheckResult execute() {
    NodeVisitor procGatherer = new ProcGatherer(symbolTable);
    try {
      root.accept(procGatherer);
    } catch (TypeException e) {
      return new TypeCheckResult(e);
    }

    NodeVisitor recordGatherer = new RecordGatherer(symbolTable);
    try {
      root.accept(recordGatherer);
    } catch (TypeException e) {
      return new TypeCheckResult(e);
    }

    try {
      root.accept(this);
      if (!procedures.isEmpty()) {
        ProcSymbol top = procedures.peek();
        throw new TypeException(
            String.format("Still in PROC '%s'. (This should never happen)", top),
            top.node().position());
      }
      return new TypeCheckResult(symbolTable);
    } catch (TypeException e) {
      e.printStackTrace();
      //      throw e;
      return new TypeCheckResult(e);
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
      throw new TypeException(String.format("Indeterminable type for %s", expr), expr.position());
    }
  }

  @Override
  public void visit(AssignmentNode node) {
    // Make sure that the left = right
    LValueNode variable = node.variable();

    Node right = node.expr();
    right.accept(this);
    if (right.varType().isUnknown()) {
      throw new TypeException(String.format("Indeterminable type for %s", right), right.position());
    }
    if (right.varType() == VarType.VOID) {
      throw new TypeException(
          String.format("Cannot assign value of void expression %s", right), right.position());
    }

    variable.accept(
        new LValueNode.Visitor() {
          @Override
          public void visit(FieldSetNode fsn) {
            //
            // Now get the record from the symbol table.
            Symbol variableSymbol = symbolTable().getRecursive(fsn.variableName());
            if (variableSymbol == null || !variableSymbol.varType().isRecord()) {
              throw new TypeException(
                  String.format("Variable '%s' not a known RECORD", variable.name()),
                  variable.position());
            }

            Symbol recordSymbol = symbolTable().getRecursive(variableSymbol.varType().name());
            if (recordSymbol == null || !(recordSymbol instanceof RecordSymbol)) {
              throw new TypeException(
                  String.format("Cannot apply DOT operator to %s expression", fsn.varType()),
                  fsn.position());
            }

            RecordSymbol record = (RecordSymbol) recordSymbol;
            // make sure string after the dot is a field in record
            String fieldName = fsn.fieldName();
            VarType fieldType = record.fieldType(fieldName);
            if (fieldType == VarType.UNKNOWN) {
              throw new TypeException(
                  String.format(
                      "Unknown field '%s' referenced in RECORD type '%s'",
                      fieldName, record.name()),
                  fsn.position());
            } else if (!fieldType.compatibleWith(right.varType())) {
              throw new TypeException(
                  String.format(
                      "field '%s' of RECORD '%s' declared as %s but RHS (%s) is %s",
                      fieldName, record.name(), fieldType, right, right.varType()),
                  variable.position());
            }
            variable.setVarType(fieldType);
          }

          @Override
          public void visit(VariableSetNode node) {
            Symbol symbol = symbolTable().getRecursive(variable.name());
            if (symbol == null) {
              // Brand new symbol in all scopes. Assign in current scope.
              symbolTable().assign(variable.name(), right.varType());
            } else {
              // Already known in some scope. Update.
              if (symbol.varType().isUnknown()) {
                symbol.setVarType(right.varType());
              } else if (!symbol.varType().compatibleWith(right.varType())) {
                // It was already in the symbol table. Possible that it's wrong
                throw new TypeException(
                    String.format(
                        "variable '%s' declared as %s but RHS (%s) is %s",
                        variable.name(), symbol.varType(), right, right.varType()),
                    variable.position());
              }

              symbol.setAssigned();
            }

            variable.setVarType(right.varType());
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
          throw new TypeException(
              String.format("Variable '%s' used before assignment", node.name()), node.position());
        }
        node.setVarType(existingType);
      }
    }
  }

  @Override
  public void visit(CallNode node) {
    // 1. make sure the function is really a function
    Symbol maybeProc = symbolTable().getRecursive(node.functionToCall());
    if (maybeProc == null || maybeProc.varType() != VarType.PROC) {
      throw new TypeException(
          String.format("PROC '%s' is unknown", node.functionToCall()), node.position());
    }
    // 2. make sure the arg length is right.
    ProcSymbol proc = (ProcSymbol) maybeProc;
    if (proc.node().parameters().size() != node.actuals().size()) {
      throw new TypeException(
          String.format(
              "Wrong number of arguments to PROC '%s': found %d, expected %d",
              node.functionToCall(), node.actuals().size(), proc.node().parameters().size()),
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
              String.format(
                  "Indeterminable type for parameter '%s' of PROC '%s'",
                  formal.name(), proc.name()),
              node.position());
        } else {
          formal.setVarType(actual.varType());
        }
      }
      // 5. make sure expr types == param types
      if (!formal.type().compatibleWith(actual.varType())) {
        throw new TypeException(
            String.format(
                "Type mismatch for parameter '%s' to PROC '%s': found %s, expected %s",
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

    // Only care if RHS is unknown if it's not DOT, because fields are not exactly like variables
    Type operator = node.operator();
    if (operator != Token.Type.DOT && right.varType().isUnknown()) {
      throw new TypeException(String.format("Indeterminable type for %s", right), right.position());
    }

    if (operator == Token.Type.DOT) {
      if (!left.varType().isRecord()) {
        // this is probably already handled, /shrug.
        throw new TypeException(
            String.format("Cannot apply DOT operator to %s expression", left.varType()),
            left.position());
      }
      if (!(right instanceof VariableNode)) {
        throw new TypeException(
            String.format("Invalid field reference %s (must be just field name)", right.toString()),
            right.position());
      }

      // Now get the record from the symbol table.
      String recordName = left.varType().name();
      Symbol symbol = symbolTable().getRecursive(recordName);
      if (symbol == null || !symbol.varType().isRecord()) {
        throw new TypeException(
            String.format("Unknown RECORD type '%s'", recordName), left.position());
      }

      RecordSymbol record = (RecordSymbol) symbol;
      // make sure RHS is a field in record
      String fieldName = ((VariableNode) right).name();
      VarType fieldType = record.fieldType(fieldName);
      if (fieldType == VarType.UNKNOWN) {
        throw new TypeException(
            String.format(
                "Unknown field '%s' referenced in RECORD type '%s'", fieldName, recordName),
            right.position());
      }
      node.setVarType(fieldType);
      // NOTE: RETURN
      return;
    }

    // Check that they're not trying to, for example, multiply booleans
    // TODO: CLEAN THIS UP
    if (left.varType() == VarType.BOOL && !BOOLEAN_OPERATORS.contains(operator)) {
      throw new TypeException(
          String.format("Cannot apply %s operator to BOOL expression", operator.name()),
          left.position());
    }
    if (left.varType() == VarType.INT && !INT_OPERATORS.contains(operator)) {
      throw new TypeException(
          String.format("Cannot apply %s operator to INT expression", operator.name()),
          left.position());
    }
    if (left.varType() == VarType.STRING && !STRING_OPERATORS.contains(operator)) {
      throw new TypeException(
          String.format("Cannot apply %s operator to STRING expression", operator.name()),
          left.position());
    }
    if (left.varType().isArray() && !ARRAY_OPERATORS.contains(operator)) {
      throw new TypeException(
          String.format("Cannot apply %s operator to ARRAY expression", operator.name()),
          left.position());
    }

    // string[int] and array[int]
    if (left.varType() == VarType.STRING && operator == Token.Type.LBRACKET) {
      if (right.varType() != VarType.INT) {
        throw new TypeException(
            String.format("STRING index must be INT; was %s", right.varType()), right.position());
      }
      node.setVarType(VarType.STRING);
      // NOTE RETURN
      return;

    } else if (left.varType().isArray() && operator == Token.Type.LBRACKET) {
      if (right.varType() != VarType.INT) {
        throw new TypeException(
            String.format("ARRAY index must be INT; was %s", right.varType()), right.position());
      }
      // I hate this.
      ArrayType arrayType = (ArrayType) left.varType();
      node.setVarType(arrayType.baseType());
      // NOTE RETURN
      return;

    } else if (!left.varType().compatibleWith(right.varType())) {
      throw new TypeException(
          String.format(
              "Type mismatch: %s is %s; %s is %s", left, left.varType(), right, right.varType()),
          left.position());
    }

    if (((left.varType() == VarType.INT || left.varType() == VarType.STRING)
            && COMPARISION_OPERATORS.contains(operator))
        || ((left.varType().isRecord() || left.varType().isNull())
            && RECORD_COMPARATORS.contains(operator))) {
      node.setVarType(VarType.BOOL);
    } else {
      node.setVarType(left.varType());
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

    if (expr.varType().isUnknown()) {
      throw new TypeException(String.format("Indeterminable type for %s", expr), expr.position());
    }

    // Check that they're not trying to negate a boolean or "not" an int.
    if (expr.varType() == VarType.BOOL && node.operator() != Token.Type.NOT) {
      throw new TypeException(
          String.format("Cannot apply %s operator to BOOL expression", node.operator().name()),
          expr.position());
    }

    // General checks for each operator:
    switch (node.operator()) {
      case MINUS:
        if (expr.varType() != VarType.INT) {
          throw new TypeException(
              String.format("MINUS must take INT; was %s", expr.varType()), expr.position());
        }
        break;
      case NOT:
        if (expr.varType() != VarType.BOOL) {
          throw new TypeException(
              String.format("NOT must take BOOL; was %s", expr.varType()), expr.position());
        }
        break;
      case BIT_NOT:
        if (expr.varType() != VarType.INT) {
          throw new TypeException(
              String.format("! (binary not) must take INT; was %s", expr.varType()),
              expr.position());
        }
        break;
      case LENGTH:
        if (expr.varType() != VarType.STRING && !expr.varType().isArray()) {
          throw new TypeException(
              String.format("LENGTH must take STRING or ARRAY; was %s", expr.varType()),
              expr.position());
        }
        node.setVarType(VarType.INT);
        // NOTE RETURN
        return;
      case ASC:
        if (expr.varType() != VarType.STRING) {
          throw new TypeException(
              String.format("ASC must take STRING; was %s", expr.varType()), expr.position());
        }
        node.setVarType(VarType.INT);
        // NOTE RETURN
        return;
      case CHR:
        if (expr.varType() != VarType.INT) {
          throw new TypeException(
              String.format("CHR must take INT; was %s", expr.varType()), expr.position());
        }
        node.setVarType(VarType.STRING);
        // NOTE RETURN
        return;
      default:
        break;
    }
    // Output type is the same as input type, unless overridden in a "case", above.
    node.setVarType(expr.varType());
  }

  @Override
  public void visit(IfNode node) {
    for (IfNode.Case ifCase : node.cases()) {
      Node condition = ifCase.condition();
      condition.accept(this);
      if (condition.varType() != VarType.BOOL) {
        throw new TypeException(
            String.format("Condition for IF or ELIF must be BOOL; was %s", condition.varType()),
            condition.position());
      }
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
      throw new TypeException(
          String.format("Condition for WHILE must be BOOL; was %s", condition.varType()),
          condition.position());
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
      throw new TypeException(
          String.format(
              "Variable '%s' already declared as %s, cannot be redeclared as %s",
              node.name(), existingType.name(), node.varType()),
          node.position());
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
        throw new TypeException(
            String.format("Cannot declare '%s' as unknown RECORD '%s'", name, recordName),
            position);
      }
      return symbol;
    }
    return null;
  }

  @Override
  public void visit(ArrayDeclarationNode node) {
    VarType existingType = symbolTable().lookup(node.name(), false);
    if (!existingType.isUnknown()) {
      throw new TypeException(
          String.format(
              "Variable '%s' already declared as %s, cannot be redeclared as %s",
              node.name(), existingType.name(), node.varType()),
          node.position());
    }

    // Make sure size type is int
    ExprNode arraySizeExpr = node.sizeExpr();
    if (arraySizeExpr.varType().isUnknown()) {
      throw new TypeException(
          "Indeterminable type for array size; must be INT", arraySizeExpr.position());
    }
    if (arraySizeExpr.varType() != VarType.INT) {
      throw new TypeException(
          String.format("Array size must be INT; was %s", arraySizeExpr.varType()),
          arraySizeExpr.position());
    }

    symbolTable().declare(node.name(), node.varType());
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
        // Make sure it exists.
        RecordReferenceType rrt = (RecordReferenceType) fieldType;
        String recordTypeName = rrt.name();
        Symbol putativeRecordSymbol = symbolTable().getRecursive(recordTypeName);
        if (putativeRecordSymbol == null) {
          throw new TypeException(
              String.format("Unknown RECORD type '%s'", recordTypeName), field.position());
        }
      }
    }
  }

  @Override
  public void visit(ProcedureNode node) {
    // 1. make sure no duplicate arg names
    List<String> paramNames =
        node.parameters().stream().map(Parameter::name).collect(toImmutableList());
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
      throw new TypeException(
          String.format(
              "Duplicate formal parameter(s) '%s' declared in PROC '%s'",
              duplicates.toString(), node.name()),
          node.position());
    }

    // Add this procedure to the symbol table
    Symbol sym = symbolTable().get(node.name());
    boolean innerProc = sym == null;
    ProcSymbol procSymbol = null;
    if (sym == null) {
      // nested proc; spawn symbol table & assign to the node.
      procSymbol = symbolTable().declareProc(node);
      SymTab child = symbolTable().spawn();
      procSymbol.setSymTab(child);
    } else if (sym.varType() != VarType.PROC) {
      throw new TypeException(
          String.format("Cannot define PROC '%s' with the same name as a global", node.name()),
          node.position());
    } else {
      procSymbol = (ProcSymbol) sym;
    }

    // 3. push current procedure onto a stack, for symbol table AND return value checking
    procedures.push(procSymbol);
    if (node.returnType() != VarType.VOID) {
      needsReturn.add(procSymbol);
    }

    // 4. add all args to local symbol table
    if (innerProc) {
      for (Parameter param : node.parameters()) {
        symbolTable().declareParam(param.name(), param.type());
      }
    }

    // 5. process the statements in the procedure:
    node.block().accept(this);

    // 6. make sure args all have a type
    for (Parameter param : node.parameters()) {
      VarType type = symbolTable().get(param.name()).varType();
      validatePossibleRecordType(param.name(), type, node.position());
      if (type.isUnknown()) {
        throw new TypeException(
            String.format(
                "Could not determine type of formal parameter '%s' of PROC '%s'",
                param.name(), node.name()),
            node.position());
      }
    }

    if (node.returnType() != VarType.VOID) {
      if (needsReturn.contains(procSymbol)) {
        // no return statement seen.
        throw new TypeException(
            String.format("No RETURN statement for PROC '%s'", node.name()), node.position());
      }
      // make sure that all *codepaths* have a return
      if (!checkAllPathsHaveReturn(node)) {
        throw new TypeException(
            String.format("Not all codepaths end with RETURN for PROC '%s'", node.name()),
            node.position());
      }
      validatePossibleRecordType("return type", node.returnType(), node.position());
    }
    procedures.pop();
  }

  private boolean checkAllPathsHaveReturn(ProcedureNode node) {
    return checkAllPathsHaveReturn(node.block());
  }

  private boolean checkAllPathsHaveReturn(BlockNode node) {
    /**
     *
     *
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
      throw new TypeException("Cannot RETURN from outside a PROC", node.position());
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
      throw new TypeException(
          String.format("Indeterminable type for RETURN statement %s", node), node.position());
    }

    if (!proc.node().varType().compatibleWith(actualReturnType)) {
      throw new TypeException(
          String.format(
              "PROC '%s' declared to return %s but returned %s",
              proc.name(), declaredReturnType, actualReturnType),
          node.position());
    }
  }

  @Override
  public void visit(ExitNode node) {
    if (node.exitMessage().isPresent()) {
      ExprNode message = node.exitMessage().get();
      message.accept(this);
      VarType actualMessageType = message.varType();
      if (actualMessageType.isUnknown()) {
        throw new TypeException(
            String.format("Indeterminable type for EXIT message %s", node), node.position());
      }
      if (message.varType() != VarType.STRING) {
        throw new TypeException(
            String.format("EXIT message must be STRING; was %s", message.varType()),
            message.position());
      }
    }
    if (!procedures.isEmpty()) {
      ProcSymbol proc = procedures.peek();
      if (proc != null) {
        needsReturn.remove(proc);
      }
    }
  }
}
