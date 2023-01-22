package com.plasstech.lang.d2.codegen;

import static com.google.common.collect.ImmutableList.toImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.il.AllocateOp;
import com.plasstech.lang.d2.codegen.il.ArrayAlloc;
import com.plasstech.lang.d2.codegen.il.ArraySet;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Call;
import com.plasstech.lang.d2.codegen.il.Dec;
import com.plasstech.lang.d2.codegen.il.FieldSetOp;
import com.plasstech.lang.d2.codegen.il.Goto;
import com.plasstech.lang.d2.codegen.il.IfOp;
import com.plasstech.lang.d2.codegen.il.Inc;
import com.plasstech.lang.d2.codegen.il.Label;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.ProcEntry;
import com.plasstech.lang.d2.codegen.il.ProcExit;
import com.plasstech.lang.d2.codegen.il.Return;
import com.plasstech.lang.d2.codegen.il.Stop;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.common.D2RuntimeException;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.parse.node.ArrayDeclarationNode;
import com.plasstech.lang.d2.parse.node.ArrayLiteralNode;
import com.plasstech.lang.d2.parse.node.ArraySetNode;
import com.plasstech.lang.d2.parse.node.AssignmentNode;
import com.plasstech.lang.d2.parse.node.BinOpNode;
import com.plasstech.lang.d2.parse.node.BreakNode;
import com.plasstech.lang.d2.parse.node.CallNode;
import com.plasstech.lang.d2.parse.node.ConstNode;
import com.plasstech.lang.d2.parse.node.ContinueNode;
import com.plasstech.lang.d2.parse.node.DefaultNodeVisitor;
import com.plasstech.lang.d2.parse.node.ExitNode;
import com.plasstech.lang.d2.parse.node.ExprNode;
import com.plasstech.lang.d2.parse.node.FieldSetNode;
import com.plasstech.lang.d2.parse.node.IfNode;
import com.plasstech.lang.d2.parse.node.IncDecNode;
import com.plasstech.lang.d2.parse.node.InputNode;
import com.plasstech.lang.d2.parse.node.LValueNode;
import com.plasstech.lang.d2.parse.node.MainNode;
import com.plasstech.lang.d2.parse.node.NewNode;
import com.plasstech.lang.d2.parse.node.Node;
import com.plasstech.lang.d2.parse.node.PrintNode;
import com.plasstech.lang.d2.parse.node.ProcedureNode;
import com.plasstech.lang.d2.parse.node.ProgramNode;
import com.plasstech.lang.d2.parse.node.ReturnNode;
import com.plasstech.lang.d2.parse.node.UnaryNode;
import com.plasstech.lang.d2.parse.node.VariableNode;
import com.plasstech.lang.d2.parse.node.VariableSetNode;
import com.plasstech.lang.d2.parse.node.WhileNode;
import com.plasstech.lang.d2.phase.Phase;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.type.ArrayType;
import com.plasstech.lang.d2.type.LocalSymbol;
import com.plasstech.lang.d2.type.ParamSymbol;
import com.plasstech.lang.d2.type.ProcSymbol;
import com.plasstech.lang.d2.type.RecordSymbol;
import com.plasstech.lang.d2.type.RecordSymbol.ArrayField;
import com.plasstech.lang.d2.type.SymTab;
import com.plasstech.lang.d2.type.Symbol;
import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VarType;

public class ILCodeGenerator extends DefaultNodeVisitor implements Phase {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private SymTab symbolTable;

  private final List<Op> operations = new ArrayList<>();
  private final Stack<String> whileBreaks = new Stack<>();
  private final Stack<String> whileContinues = new Stack<>();

  private int id;
  private Stack<ProcSymbol> procedures = new Stack<>();

  @Override
  public State execute(State input) {
    symbolTable = input.symbolTable();
    try {
      ImmutableList<Op> code = generate(input.programNode());
      return input.addIlCode(code);
    } catch (D2RuntimeException re) {
      return input.addException(re);
    }
  }

  private ImmutableList<Op> generate(ProgramNode root) {
    root.accept(this);
    emit(new Stop());
    return ImmutableList.copyOf(operations);
  }

  private void emit(Op op) {
    logger.atFine().log("%s", op.toString());
    operations.add(op);
  }

  private SymTab symbolTable() {
    if (procedures.isEmpty()) {
      return symbolTable;
    }
    return procedures.peek().symTab();
  }

  private String nextLabel(String prefix) {
    // leading underscore is an illegal character so this will never conflict.
    return String.format("__%s_%d", prefix, ++id);
  }

  private TempLocation allocateTemp(VarType varType) {
    String name = String.format("__temp%d", ++id);
    symbolTable().declareTemp(name, varType);
    return new TempLocation(name, varType);
  }

  private Location lookupLocation(String name) {
    Symbol variable = symbolTable().getRecursive(name);
    if (variable == null) {
      throw new IllegalStateException(
          String.format("Cannot find location of %s in symbol table", name));
    }
    switch (variable.storage()) {
      case HEAP:
      case GLOBAL:
        return new MemoryAddress(name, variable.varType());
      case LOCAL:
        LocalSymbol local = (LocalSymbol) variable;
        // captures its offset
        return new StackLocation(local.name(), local.varType(), local.offset());
      case PARAM:
        ParamSymbol param = (ParamSymbol) variable;
        return new ParamLocation(name, variable.varType(), param.index(), param.offset());
      default:
        throw new IllegalStateException(
            String.format(
                "Cannot create location of %s of type %s in symbol table",
                name, variable.storage()));
    }
  }

  private static <T> ConstantOperand<T> toConstOperand(ConstNode<T> node) {
    return new ConstantOperand<T>(node.value(), node.varType());
  }

  @Override
  public void visit(MainNode node) {
    node.block().accept(this);
  }

  @Override
  public void visit(PrintNode node) {
    Node expr = node.expr();
    if (expr.varType() == VarType.STRING && expr instanceof BinOpNode) {
      BinOpNode binop = (BinOpNode) expr;
      if (binop.operator() == TokenType.PLUS) {
        // If adding two strings, print each one individually
        ExprNode left = binop.left();
        visit(new PrintNode(left, left.position(), false));
        ExprNode right = binop.right();
        visit(new PrintNode(right, right.position(), node.isPrintln()));
        return;
      }
    }
    expr.accept(this);
    if (expr.varType().isArray()) {
      // Need 2 globals: index and length. Can't use temps, because they're one-time-use.
      if (symbolTable.lookup("array_print_index") == VarType.UNKNOWN) {
        symbolTable.declare("array_print_index", VarType.INT);
      }
      Location index = lookupLocation("array_print_index");
      ArrayType arrayType = (ArrayType) expr.varType();
      emit(new SysCall(SysCall.Call.PRINT, ConstantOperand.of("[")));
      emit(new Transfer(index, ConstantOperand.of(0), node.position()));
      // length = calculate length of array
      if (symbolTable.lookup("array_print_length") == VarType.UNKNOWN) {
        symbolTable.declare("array_print_length", VarType.INT);
      }
      Location length = lookupLocation("array_print_length");
      TempLocation tempLength = allocateTemp(VarType.INT);
      // can't assign right to the global because it barfs on the 2nd line:
      // mov RBX, [__arr] ; get array location into reg
      // mov [__length], [RBX + 1] ; get length from first dimension
      emit(new UnaryOp(tempLength, TokenType.LENGTH, expr.location(), node.position()));
      emit(new Transfer(length, tempLength, null));
      String loopLabel = nextLabel("array_print_loop");
      // loop:
      emit(new Label(loopLabel));
      String endLoopLabel = nextLabel("array_print_loop_end");
      //   compare index to length
      TempLocation compare = allocateTemp(VarType.BOOL);
      emit(new BinOp(compare, index, TokenType.EQEQ, length, node.position()));
      //   if equal, go to end
      emit(new IfOp(compare, endLoopLabel, false, node.position()));
      TempLocation item = allocateTemp(arrayType.baseType());
      //   item = get "index"th item
      emit(new BinOp(item, expr.location(), TokenType.LBRACKET, index, node.position()));
      //   print item
      emit(new SysCall(SysCall.Call.PRINT, item));
      // this adds a trailing comma but who cares.
      emit(new SysCall(SysCall.Call.PRINT, ConstantOperand.of(",")));
      //   inc index
      emit(new Inc(index, node.position()));
      //   goto loop
      emit(new Goto(loopLabel));
      // end:
      emit(new Label(endLoopLabel));
      if (node.isPrintln()) {
        emit(new SysCall(SysCall.Call.PRINTLN, ConstantOperand.of("]")));
      } else {
        emit(new SysCall(SysCall.Call.PRINT, ConstantOperand.of("]")));
      }
    } else {
      if (node.isPrintln()) {
        emit(new SysCall(SysCall.Call.PRINTLN, expr.location()));
      } else {
        emit(new SysCall(SysCall.Call.PRINT, expr.location()));
      }
    }
  }

  @Override
  public void visit(ArrayLiteralNode node) {
    // 1. allocate an array of the desired type and length
    // 2. set each value of the array to what we want.
    TempLocation destination = allocateTemp(node.varType());
    node.setLocation(destination);

    ArrayType arrayType = node.arrayType();
    emit(
        new ArrayAlloc(
            destination, arrayType, ConstantOperand.of(node.elements().size()), node.position()));

    int i = 0;
    for (ExprNode element : node.elements()) {
      element.accept(ILCodeGenerator.this);
      emit(
          new ArraySet(
              destination,
              arrayType,
              ConstantOperand.of(i++),
              element.location(),
              true,
              node.position()));
    }
  }

  @Override
  public void visit(AssignmentNode node) {
    Node rhs = node.expr();
    Operand rhsLocation;
    if (rhs.isConstant()) {
      // Just assign a=constant
      ConstNode<?> rhsConst = (ConstNode<?>) rhs;
      rhsLocation = toConstOperand(rhsConst);
    } else {
      rhs.accept(this);
      rhsLocation = rhs.location();
    }
    LValueNode lvalue = node.lvalue();
    lvalue.accept(
        new LValueNode.Visitor() {

          @Override
          public void visit(VariableSetNode variableNode) {
            Location dest = lookupLocation(variableNode.name());
            variableNode.setLocation(dest);
            emit(new Transfer(dest, rhsLocation, variableNode.position()));
          }

          @Override
          public void visit(FieldSetNode fsn) {
            Symbol sym = symbolTable().getRecursive(fsn.variableName());
            if (sym != null) {
              Location recordLocation = lookupLocation(fsn.variableName());
              VarType varType = sym.varType();
              if (!varType.isRecord()) {
                throw new D2RuntimeException(
                    String.format("Can't set field on non-record type; was %s", varType.name()),
                    fsn.position(),
                    "Internal");
              }
              Symbol hopefullyRecordSymbol = symbolTable().getRecursive(varType.name());

              String fieldName = fsn.fieldName();

              RecordSymbol recordSymbol = (RecordSymbol) hopefullyRecordSymbol;
              emit(
                  new FieldSetOp(
                      recordLocation, recordSymbol, fieldName, rhsLocation, fsn.position()));
            } else {
              throw new D2RuntimeException(
                  String.format("Could not find record symbol %s in symtab", fsn.variableName()),
                  fsn.position(),
                  "Internal");
            }
          }

          @Override
          public void visit(ArraySetNode asn) {
            // Look up storage in current symbol table
            Symbol sym = symbolTable().getRecursive(asn.variableName());
            if (sym != null) {
              Location arrayLocation = lookupLocation(asn.variableName());

              Node indexNode = asn.indexNode();
              indexNode.accept(ILCodeGenerator.this);
              Operand indexLocation = indexNode.location();

              emit(
                  new ArraySet(
                      arrayLocation,
                      (ArrayType) asn.varType(),
                      indexLocation,
                      rhsLocation,
                      false,
                      asn.position()));
            } else {
              throw new RuntimeException(
                  String.format("Could not find record symbol %s in symtab", asn.variableName()));
            }
          }
        });
  }

  @Override
  public void visit(ArrayDeclarationNode node) {
    node.sizeExpr().accept(this);
    Location dest = lookupLocation(node.name());
    node.setLocation(dest);
    emit(new ArrayAlloc(dest, node.arrayType(), node.sizeExpr().location(), node.position()));
  }

  @Override
  public void visit(NewNode node) {
    RecordSymbol symbol = (RecordSymbol) symbolTable().getRecursive(node.recordName());
    TempLocation recordLocation = allocateTemp(symbol.varType());
    node.setLocation(recordLocation);
    emit(new AllocateOp(recordLocation, symbol, node.position()));

    // Generate array allocation node for each array field
    List<ArrayField> arrayFields = symbol.arrayFields();
    for (ArrayField af : arrayFields) {
      ArrayType at = af.type();
      // 1. allocate a new array to a temp
      TempLocation fieldTemp = allocateTemp(at);
      // TODO(#38): Support multidimensional arrays
      int size = af.sizes().get(0);
      emit(new ArrayAlloc(fieldTemp, at, ConstantOperand.of(size), node.position()));
      // 2. set the field to the temp
      emit(new FieldSetOp(recordLocation, symbol, af.name(), fieldTemp, node.position()));
    }
  }

  @Override
  public <T> void visit(ConstNode<T> node) {
    TempLocation destination = allocateTemp(node.varType());

    // TODO: Should this be a thing? should we just set the "location" of the node to a constant
    // "location"?
    node.setLocation(destination);

    ConstantOperand<T> constOperand = toConstOperand(node);
    Transfer transfer = new Transfer(destination, constOperand, node.position());
    emit(transfer);
  }

  /**
   * This winds up generating code that may be difficult to translate to assembly, e.g.,
   *
   * <pre>
   * __temp32 = a + b
   * </pre>
   *
   * where both a and b are stack or globals.
   *
   * <p>The only reason I did this was to get the loop invariant optimizer to work with records,
   * because this is the code it used to generate:
   *
   * <pre>
   * __temp1 = rec
   * __temp2 = __temp1.fieldname
   * </pre>
   *
   * This was causing the loop optimizer to move BOTH ops out of the loop, because it thought
   * __temp1, and therefore __temp2 was invariant.
   *
   * <p>So now this version generates:
   *
   * <pre>
   * __temp2 = rec.fieldname
   * </pre>
   *
   * which makes the optimizer work.
   */
  @Override
  public void visit(VariableNode node) {
    // This may be a global or a local/parameter (stack)
    Location source = lookupLocation(node.name());
    node.setLocation(source);
  }

  @Override
  public void visit(InputNode node) {
    TempLocation destination = allocateTemp(VarType.STRING);
    node.setLocation(destination);
    emit(new SysCall(SysCall.Call.INPUT, destination));
  }

  @Override
  public void visit(BinOpNode node) {
    // Calculate the value and put it somewhere
    Node left = node.left();
    // Source for the value of left - either a register or memory location or a constant value.
    Operand leftSrc;
    // if left is a constant, just get it.
    if (left.isConstant()) {
      ConstNode<?> simpleLeft = (ConstNode<?>) left;
      leftSrc = toConstOperand(simpleLeft);
    } else {
      left.accept(this);
      leftSrc = left.location();
    }

    TempLocation destination = allocateTemp(node.varType());
    node.setLocation(destination);

    String resultIsFalseLabel = null;
    String resultIsTrueLabel = null;

    // if node.operator == AND, generate:
    //    if !left, goto resultisfalse
    //      generate right,
    //      value is right // we know left is true, so value is true AND right = right
    //      goto valueIsSetLabel
    //    resultisfalse:
    //      value is false
    //    valueIsSetLabel:

    // if node.operator == OR: generate:
    //    if left: goto resultistrue
    //      generate right
    //      value = right // we know left is false, so value is false OR right = right
    //      goto valueissetlabel
    //    resultistrue:
    //      value is true
    //    valuissetlabel

    if (node.operator() == TokenType.AND) {
      resultIsFalseLabel = nextLabel("short_circuit_result_false");
      emit(new IfOp(leftSrc, resultIsFalseLabel, true, node.position()));
      // ... to be continued
    } else if (node.operator() == TokenType.OR) {
      resultIsTrueLabel = nextLabel("short_circuit_result_true");
      emit(new IfOp(leftSrc, resultIsTrueLabel, false, node.position()));
      // ... to be continued
    }

    // Calculate the value and put it somewhere
    Node right = node.right();
    // Source for the value of right - either a register or memory location or a constant value.
    Operand rightSrc;

    if (node.operator() == TokenType.DOT) {
      // the RHS is a field reference
      VariableNode rightVarNode = (VariableNode) right;
      rightSrc = ConstantOperand.of(rightVarNode.name());
    } else {
      // if right is a constant, just get it.
      if (right.isConstant()) {
        ConstNode<?> simpleRight = (ConstNode<?>) right;
        rightSrc = toConstOperand(simpleRight);
      } else {
        right.accept(this);
        rightSrc = right.location();
      }

      String valueIsSetLabel = nextLabel("short_circuit_value_is_set");
      if (node.operator() == TokenType.AND) {
        // value = right (we know left is true, therefore value is true AND right = right)
        emit(new Transfer(destination, rightSrc, node.position()));
        // goto valueIsSetLabel
        emit(new Goto(valueIsSetLabel));

        // resultisfalse:
        emit(new Label(resultIsFalseLabel));
        //   value=false
        emit(new Transfer(destination, ConstantOperand.FALSE, node.position()));

        // valueIsSetLabel: (continue)
        emit(new Label(valueIsSetLabel));

      } else if (node.operator() == TokenType.OR) {
        // value = right (we know left is false, so value is false OR right = right)
        emit(new Transfer(destination, rightSrc, node.position()));
        // goto valueIsSetLabel
        emit(new Goto(valueIsSetLabel));

        // resultistrue:
        emit(new Label(resultIsTrueLabel));
        //   value=true
        emit(new Transfer(destination, ConstantOperand.TRUE, node.position()));
        // valueIsSetLabel:
        emit(new Label(valueIsSetLabel));
      }
    }

    // do not do this for AND or OR
    if (node.operator() != TokenType.AND && node.operator() != TokenType.OR) {
      emit(new BinOp(destination, leftSrc, node.operator(), rightSrc, node.position()));
    }
  }

  @Override
  public void visit(UnaryNode node) {
    Node rhs = node.expr();
    rhs.accept(this);

    // calculate the value and put it somewhere.
    switch (node.operator()) {
      case MINUS:
      case BIT_NOT:
      case NOT:
      case LENGTH:
      case ASC:
      case CHR:
        TempLocation destination = allocateTemp(node.varType());
        node.setLocation(destination);
        emit(new UnaryOp(destination, node.operator(), rhs.location(), node.position()));
        break;
      case PLUS:
        // tiny optimization: a=+b -> a=b
        node.setLocation(rhs.location());
        break;
      default:
        logger.atSevere().log("No code generated for node %s", node);
        break;
    }
  }

  @Override
  public void visit(IfNode node) {
    String after = nextLabel("after_if");

    for (IfNode.Case ifCase : node.cases()) {
      String nextLabel = nextLabel("elif");

      Node cond = ifCase.condition();
      cond.accept(this);

      // if !cond.location goto nextLabel
      emit(new IfOp(cond.location(), nextLabel, true, node.position()));

      ifCase.block().accept(this);
      // We're in a block , now jump completely after.
      emit(new Goto(after));

      emit(new Label(nextLabel));
    }
    node.elseBlock().ifPresent(block -> block.accept(this));

    emit(new Label(after));
  }

  @Override
  public void visit(WhileNode node) {
    // pattern is: if (condition) { do loop while condition}

    // emit test
    // if while condition is false goto loop_end
    // loop_begin:
    // ..emit loop code
    // loop_increment: ("continue" target)
    // ..emit increment code
    // ..emit test
    // ..if while condition is true, goto loop_begin
    // loop_end: ("break" target)

    String before = nextLabel(Label.LOOP_BEGIN_PREFIX);
    String increment = nextLabel(Label.LOOP_INCREMENT_PREFIX);
    String after = nextLabel(Label.LOOP_END_PREFIX);
    whileContinues.push(increment);
    whileBreaks.push(after);

    // Pre-check
    Node condition = node.condition();
    condition.accept(this);
    // if not condition.location() goto after
    emit(new IfOp(condition.location(), after, true, node.position()));

    emit(new Label(before));

    node.block().accept(this);

    emit(new Label(increment));
    if (node.doStatement().isPresent()) {
      node.doStatement().get().accept(this);
    }

    // post-increment test
    condition.setLocation(null);
    condition.accept(this);
    // if condition.location() goto before
    emit(new IfOp(condition.location(), before, false));

    emit(new Label(after));
    whileBreaks.pop();
    whileContinues.pop();
  }

  @Override
  public void visit(BreakNode node) {
    // break = go to the end
    String after = whileBreaks.peek();
    emit(new Goto(after));
  }

  @Override
  public void visit(ContinueNode node) {
    // continue = go to the "increment" label
    String before = whileContinues.peek();
    emit(new Goto(before));
  }

  @Override
  public void visit(ExitNode node) {
    int code = 0;
    if (node.exitMessage().isPresent()) {
      code = -1;
      node.exitMessage().get().accept(this);
      Location messageLocation = node.exitMessage().get().location();
      emit(new SysCall(SysCall.Call.MESSAGE, messageLocation));
    }
    emit(new Stop(code));
  }

  @Override
  public void visit(ReturnNode node) {
    String procedureName = procedures.peek().name();
    if (node.expr().isPresent()) {
      node.expr().get().accept(this);
      // copy the location of the result to the current procedure's destination.
      Location exprLoc = node.expr().get().location();
      emit(new Return(procedureName, exprLoc));
    } else {
      emit(new Return(procedureName));
    }
  }

  @Override
  public void visit(ProcedureNode node) {
    // Look up procedure in current scope
    ProcSymbol procSym = (ProcSymbol) symbolTable().getRecursive(node.name());
    assert (procSym != null);

    procedures.push(procSym);
    // Guard to prevent just falling into this method
    String afterLabel = nextLabel("after_proc_" + node.name());

    emit(new Goto(afterLabel));

    // This is the real entry point.
    emit(new Label(procSym.mungedName()));
    SymTab symTab = procSym.symTab();
    ImmutableList<Symbol> locals =
        symTab
            .entries()
            .values()
            .stream()
            .filter(symbol -> symbol.storage() == SymbolStorage.LOCAL)
            .collect(ImmutableList.toImmutableList());
    int localBytes = 0;
    for (Symbol symbol : locals) {
      LocalSymbol localSymbol = (LocalSymbol) symbol;
      localBytes += localSymbol.varType().size();
      localSymbol.setOffset(localBytes);
    }

    ImmutableList<ParamSymbol> formals = procSym.formals();
    int paramBytes = 8; // 8 for the return address on the stack
    for (int i = 0; i < formals.size(); ++i) {
      if (i > 3) {
        ParamSymbol formal = formals.get(i);
        paramBytes += 8; // parameters go on the stack and they must always be at a multiple of 8.
        formal.setOffset(paramBytes);
      }
    }

    emit(new ProcEntry(node.name(), formals, localBytes));

    node.block().accept(this);

    // there should have already been a regular "return" with the value.
    if (node.returnType() == VarType.VOID) {
      // This is needed for the inline optimizer.
      emit(new Return(node.name()));
    }

    emit(new ProcExit(node.name(), localBytes));
    emit(new Label(afterLabel));

    procedures.pop();
  }

  @Override
  public void visit(CallNode node) {
    // 1. generate each actual
    for (ExprNode actual : node.actuals()) {
      actual.accept(this);
    }

    // 2. emit call(parameters)
    Call call;
    ImmutableList<Operand> actuals =
        node.actuals().stream().map(Node::location).collect(toImmutableList());

    // look up the procedure node to get its parameter list to put into the
    // call object
    Symbol symbol = symbolTable().getRecursive(node.procName());
    if (!(symbol instanceof ProcSymbol)) {
      throw new RuntimeException(
          "proc " + node.procName() + " not found in symtab " + symbolTable());
    }
    ProcSymbol procSym = (ProcSymbol) symbol;
    ImmutableList<Location> formals = procSymFormals(procSym);
    if (node.isStatement()) {
      // No return value
      call = new Call(procSym, actuals, formals, node.position());
    } else {
      // 3. put result location into node.location
      Location location = allocateTemp(node.varType());
      node.setLocation(location);
      call = new Call(Optional.of(location), procSym, actuals, formals, node.position());
    }
    emit(call);
  }

  private ImmutableList<Location> procSymFormals(ProcSymbol procSym) {
    ImmutableList<ParamSymbol> formals = procSym.formals();

    ImmutableList.Builder<Location> formalLocations = ImmutableList.builder();
    int i = 0;
    for (ParamSymbol formal : formals) {
      // TODO(bug #194): Why is this here, and also in lookupLocation, where it *really* sets the
      // offset? Maybe these should just be the symbols?
      formalLocations.add(new ParamLocation(formal.name(), formal.varType(), i++, -1));
    }
    return formalLocations.build();
  }

  @Override
  public void visit(IncDecNode node) {
    Location dest = lookupLocation(node.name());
    node.setLocation(dest);
    if (node.isIncrement()) {
      emit(new Inc(dest, node.position()));
    } else {
      emit(new Dec(dest, node.position()));
    }
  }
}
