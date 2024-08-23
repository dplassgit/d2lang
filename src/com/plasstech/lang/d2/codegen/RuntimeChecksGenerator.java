package com.plasstech.lang.d2.codegen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.il.ArrayAlloc;
import com.plasstech.lang.d2.codegen.il.ArraySet;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Dec;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.FieldSetOp;
import com.plasstech.lang.d2.codegen.il.IfOp;
import com.plasstech.lang.d2.codegen.il.Label;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Stop;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.common.ArraySizeException;
import com.plasstech.lang.d2.common.D2RuntimeException;
import com.plasstech.lang.d2.common.DivisionByZeroException;
import com.plasstech.lang.d2.common.InvalidIndexException;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.common.Range;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.phase.Phase;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.type.StaticChecker;
import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VarType;
import com.plasstech.lang.d2.type.VariableSymbol;

/**
 * For certain ops, add runtime checks: NPE and index checks. Much of this used to be in
 * ILCodeGenerator but was split out so we can optimize first (and after!)
 */
public class RuntimeChecksGenerator extends DefaultOpcodeVisitor implements Phase {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private static final String DIV_BY_0 = "Division by 0 error at line %d, column %d";
  private static final String NULL_POINTER = "Null pointer error at line %d, column %d";

  private static final String ARRAY_INDEX_NEGATIVE_ERR =
      "Invalid index error at line %d, column %d: ARRAY index must be non-negative; was %d";
  private static final String ARRAY_INDEX_OOB_ERR =
      "Invalid index error at line %d, column %d: ARRAY index out of bounds (length %d); was %d";
  private static final String STRING_INDEX_NEGATIVE_ERR =
      "Invalid index error at line %d, column %d: STRING index must be non-negative; was %d";
  private static final String STRING_INDEX_OOB_ERR =
      "Invalid index error at line %d, column %d: STRING index out of bounds (length %d); was %d";
  private static final String RANGE_INDEX_OOB_ERR =
      "Invalid index error at line %d, column %d: RANGE index must be 0 or 1; was %d";
  private static final String ARRAY_SIZE_NEGATIVE_ERR =
      "Invalid array size error at line %d, column %d: ARRAY size must be non-negative; was %d";

  private final List<Op> augmentedCode = new ArrayList<>();
  // Maps a temp to its corresponding long temp
  private final Map<Operand, LongTempLocation> tempToLongTemp = new HashMap<>();

  private int id;

  @Override
  public State execute(State input) {
    try {
      tempToLongTemp.clear();
      // reads either the optimized or pre-optimized (if no optimized code)
      augment(input.lastIlCode());
      return input.setIlCode(ImmutableList.copyOf(augmentedCode));
    } catch (D2RuntimeException re) {
      return input.addException(re);
    }
  }

  private ImmutableList<Op> augment(ImmutableList<Op> code) {
    for (Op op : code) {
      int length = augmentedCode.size();
      op.accept(this);
      if (length == augmentedCode.size()) {
        // The "accept/visit" didn't change anything; add this op manually.
        augmentedCode.add(op);
      }
    }
    return code;
  }

  @Override
  public void visit(BinOp op) {
    var position = op.position();
    Location destination = op.destination();
    Operand left = remapTemp(op.left());
    Operand right = remapTemp(op.right());
    var operator = op.operator();
    switch (operator) {
      case DIV:
      case MOD:
        right = divBy0Check(right, position);
        break;

      case DOT:
        left = npeCheck(left, position);
        break;

      case LBRACKET:
        if (left.type().compatibleWith(VarType.NULL)) {
          left = npeCheck(left, position);
        }
        right = indexChecks(left, right, position);
        break;

      case PLUS:
        // NOTE we do NOT check string plus for nulls because it was already done in ILCodeGenerator
        // have to update for remapped temps
        break;

      default:
        return;
    }

    // Replace the op with the new right, even if it's the same.
    emit(new BinOp(destination, left, operator, right, position));
  }

  private Operand remapTemp(Operand temp) {
    LongTempLocation maybeLongTemp = tempToLongTemp.get(temp);
    if (maybeLongTemp != null) {
      return maybeLongTemp;
    }

    return temp;
  }

  @Override
  public void visit(ArraySet op) {
    if (op.isArrayLiteral() || op.array().type() == VarType.RANGE) {
      return;
    }
    var position = op.position();
    Location arrayLocation = npeCheck(op.array(), position);
    Operand indexLocation = indexChecks(arrayLocation, op.index(), position);
    // Replace the op with the new array and index, even if they're the same
    emit(new ArraySet(arrayLocation, op.arrayType(), indexLocation,
        remapTemp(op.source()), op.isArrayLiteral(), position));
  }

  @Override
  public void visit(FieldSetOp op) {
    var position = op.position();
    Location recordLocation = npeCheck(op.recordLocation(), position);
    emit(new FieldSetOp(recordLocation, op.recordSymbol(), op.field(), remapTemp(op.source()),
        position));
  }

  @Override
  public void visit(UnaryOp op) {
    Operand source = remapTemp(op.operand());
    var position = op.position();
    var operator = op.operator();
    switch (operator) {
      case LENGTH:
      case ASC:
        if (source.type().compatibleWith(VarType.NULL)) {
          source = npeCheck(source, position);
        } else {
          return;
        }
        break;

      default:
        return;
    }
    // Replace the op with the new right, even if it's the same.
    emit(new UnaryOp(op.destination(), operator, source, position));
  }

  @Override
  public void visit(Transfer op) {
    emit(new Transfer(op.destination(), remapTemp(op.source()), op.position()));
  }

  @Override
  public void visit(IfOp op) {
    emit(new IfOp(remapTemp(op.condition()), op.destination(), op.isNot(), op.position()));
  }

  @Override
  public void visit(ArrayAlloc op) {
    Operand size = op.sizeLocation();
    if (size.isConstant()) {
      int constantSize = ConstantOperand.valueFromConstOperand(size).intValue();
      if (constantSize >= 0) {
        return;
      }
      throw new ArraySizeException("ARRAY size must be non-negative; was " + constantSize,
          op.position());
    }
    Location nonNegativeIndex = allocateTemp(VarType.BOOL);
    Position position = op.position();
    size = copyTempToLongTemp(size, position);
    emit(new BinOp(nonNegativeIndex, size, TokenType.GEQ, ConstantOperand.of(0), position));
    // if nonnegativeindex: goto good
    String nonNegativeIndexLabel = nextLabel("non_negative_index");
    emit(new IfOp(nonNegativeIndex, nonNegativeIndexLabel, false, position));
    emit(new SysCall(ARRAY_SIZE_NEGATIVE_ERR,
        ImmutableList.of(ConstantOperand.of(position.line()),
            ConstantOperand.of(position.column()), size)));
    emit(new Stop());
    emit(new Label(nonNegativeIndexLabel));

    emit(new ArrayAlloc(op.destination(), op.arrayType(), size, position));
  }

  private void emit(Op op) {
    logger.atFine().log("%s", op.toString());
    augmentedCode.add(op);
  }

  private String nextLabel(String prefix) {
    // leading underscore is an illegal character so this will never conflict.
    return String.format("__rt%s_%d", prefix, ++id);
  }

  private TempLocation allocateTemp(VarType varType) {
    Preconditions.checkArgument(!varType.isRecord(), "Cannot allocate temp with a record");
    String name = String.format("__rttemp%d", ++id);
    VariableSymbol symbol = new VariableSymbol(null, name, SymbolStorage.TEMP);
    symbol.setVarType(varType);
    return new TempLocation(symbol);
  }

  private Location allocateLongTemp(VarType varType) {
    Preconditions.checkArgument(!varType.isRecord(), "Cannot allocate longtemp with a record");
    String name = String.format("__rtlongtemp%d", ++id);
    VariableSymbol symbol = new VariableSymbol(null, name, SymbolStorage.LONG_TEMP);
    symbol.setVarType(varType);
    return new LongTempLocation(symbol);
  }

  private Operand copyTempToLongTemp(Operand maybeTemp, Position position) {
    if (!maybeTemp.isTemp()) {
      return maybeTemp;
    }
    if (tempToLongTemp.containsKey(maybeTemp)) {
      // Asked and answered your honor. We already have a longtemp for this temp.
      // It's important that we don't make another one, because reasons.
      return tempToLongTemp.get(maybeTemp);
    }
    VariableLocation temp = (VariableLocation) maybeTemp;

    // make a new symbol
    String name = String.format("__rtlongtemp%d", ++id);
    VariableSymbol symbol = tempSymbolToLongTempSymbol(temp.symbol(), name);
    LongTempLocation longTemp = new LongTempLocation(symbol);

    tempToLongTemp.put(maybeTemp, longTemp);
    emit(new Transfer(longTemp, maybeTemp, position));
    return longTemp;
  }

  private static VariableSymbol tempSymbolToLongTempSymbol(VariableSymbol tempSymbol,
      String newName) {
    VariableSymbol symbol =
        new VariableSymbol(tempSymbol.symbolTable(), newName, SymbolStorage.LONG_TEMP);
    symbol.setVarType(tempSymbol.varType());
    return symbol;
  }

  private Operand divBy0Check(Operand right, Position position) {
    if (right.isConstant()) {
      if (ConstantOperand.isAnyZero(right)) {
        throw new DivisionByZeroException(position);
      }
      return right;
    }
    // Copy right to a long lived temp so we can re-use it
    right = copyTempToLongTemp(right, position);
    TempLocation divBy0Bool = allocateTemp(VarType.BOOL);
    ConstantOperand<? extends Number> zero = ConstantOperand.zeroOf(right.type());
    emit(new BinOp(divBy0Bool, right, TokenType.EQEQ, zero, position));
    String continueLabel = Labels.nextLabel("not_div_by_0");
    emit(new IfOp(divBy0Bool, continueLabel, true));
    emit(new SysCall(DIV_BY_0,
        ImmutableList.of(ConstantOperand.of(position.line()),
            ConstantOperand.of(position.column()))));
    emit(new Stop(-1));
    emit(new Label(continueLabel));

    return right;
  }

  private <T extends Operand> T npeCheck(T operand, Position position) {
    // Copy operand to a long lived temp so we can re-use it
    operand = (T) copyTempToLongTemp(operand, position);
    TempLocation nullRecordBool = allocateTemp(VarType.BOOL);
    emit(
        new BinOp(
            nullRecordBool,
            operand,
            TokenType.EQEQ,
            new ConstantOperand<Void>(null, operand.type()),
            position));
    String continueLabel = Labels.nextLabel("not_null");
    emit(new IfOp(nullRecordBool, continueLabel, true));
    emit(new SysCall(NULL_POINTER,
        ImmutableList.of(ConstantOperand.of(position.line()),
            ConstantOperand.of(position.column()))));
    emit(new Stop(-1));
    emit(new Label(continueLabel));
    // This may be different now
    return operand;
  }

  private Operand indexChecks(Operand thingWithIndex, Operand index, Position position) {
    if (index.type() == VarType.RANGE) {
      // Taking a slice of a string
      if (index.isConstant()) {
        Range range = ConstantOperand.rangeValueFromConstOperand(index);
        ConstantOperand<Integer> rangeMin = ConstantOperand.of(range.start());
        indexChecks(thingWithIndex, rangeMin, position);
        if (range.start() == range.end()) {
          // if start and end are the same, only need to test one
          return index;
        }
        ConstantOperand<Integer> rangeMax = ConstantOperand.of(range.end());
        indexChecks(thingWithIndex, rangeMax, position);
        return index;
      }

      // rangemin = index[0]
      // Copy index to a long lived temp so we can re-use it
      index = copyTempToLongTemp(index, position);
      // r=index[0]
      Location rangeMin = allocateTemp(VarType.INT);
      emit(new BinOp(rangeMin, index, TokenType.LBRACKET, ConstantOperand.ZERO, position));
      indexChecks(thingWithIndex, rangeMin, position);

      // r=index[1]-1
      Location rangeMax = allocateLongTemp(VarType.INT);
      emit(new BinOp(rangeMax, index, TokenType.LBRACKET, ConstantOperand.ONE, position));
      emit(new Dec(rangeMax, position));
      indexChecks(thingWithIndex, rangeMax, position);
      return index;
    }

    if (thingWithIndex.type() == VarType.RANGE && index.isConstant()) {
      // We can do it right here, right now.
      int indexNum = ConstantOperand.valueFromConstOperand(index).intValue();
      if (indexNum < 0 || indexNum > 1) {
        throw new InvalidIndexException(
            String.format(
                StaticChecker.RANGE_INDEX_OUT_OF_RANGE,
                thingWithIndex.toString(), indexNum),
            position);
      }
      return index;
    }

    // Copy index to a long lived temp so we can re-use it
    index = copyTempToLongTemp(index, position);
    // len = length(array)
    Location length = allocateLongTemp(VarType.INT);
    if (thingWithIndex.type() == VarType.RANGE) {
      emit(new Transfer(length, ConstantOperand.of(2), position));
    } else {
      // TODO: arrays might have a known size, so this doesn't need to be done
      emit(new UnaryOp(length, TokenType.LENGTH, thingWithIndex, position));
    }

    // indexInBounds = index < length
    Location indexInBounds = allocateTemp(VarType.BOOL);

    emit(new BinOp(indexInBounds, index, TokenType.LT, length, position));
    // if indexInBounds, goto good
    String indexInBoundsLabel = nextLabel("index_in_bounds");
    emit(new IfOp(indexInBounds, indexInBoundsLabel, false, position));
    if (thingWithIndex.type() == VarType.STRING) {
      emit(new SysCall(STRING_INDEX_OOB_ERR,
          ImmutableList.of(ConstantOperand.of(position.line()),
              ConstantOperand.of(position.column()), length, index)));
    } else if (thingWithIndex.type() == VarType.RANGE) {
      emit(new SysCall(RANGE_INDEX_OOB_ERR,
          ImmutableList.of(ConstantOperand.of(position.line()),
              ConstantOperand.of(position.column()), index)));
    } else {
      emit(new SysCall(ARRAY_INDEX_OOB_ERR,
          ImmutableList.of(ConstantOperand.of(position.line()),
              ConstantOperand.of(position.column()), length, index)));
    }
    emit(new Stop());

    emit(new Label(indexInBoundsLabel));

    if (index.isConstant()) {
      int val = ConstantOperand.valueFromConstOperand(index).intValue();
      if (val >= 0) {
        return index;
      }
    }
    // nonNegativeIndex = index >= 0
    Location nonNegativeIndex = allocateTemp(VarType.BOOL);
    emit(new BinOp(nonNegativeIndex, index, TokenType.GEQ, ConstantOperand.of(0), position));
    // if nonnegativeindex: goto good
    String nonNegativeIndexLabel = nextLabel("non_negative_index");
    emit(new IfOp(nonNegativeIndex, nonNegativeIndexLabel, false, position));
    if (thingWithIndex.type() == VarType.STRING) {
      emit(new SysCall(STRING_INDEX_NEGATIVE_ERR,
          ImmutableList.of(ConstantOperand.of(position.line()),
              ConstantOperand.of(position.column()), index)));
    } else if (thingWithIndex.type() == VarType.RANGE) {
      emit(new SysCall(RANGE_INDEX_OOB_ERR,
          ImmutableList.of(ConstantOperand.of(position.line()),
              ConstantOperand.of(position.column()), index)));
    } else {
      emit(new SysCall(ARRAY_INDEX_NEGATIVE_ERR,
          ImmutableList.of(ConstantOperand.of(position.line()),
              ConstantOperand.of(position.column()), index)));
    }
    emit(new Stop());

    emit(new Label(nonNegativeIndexLabel));

    return index;
  }
}
