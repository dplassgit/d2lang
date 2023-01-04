package com.plasstech.lang.d2.interpreter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.logging.Level;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.StackLocation;
import com.plasstech.lang.d2.codegen.il.AllocateOp;
import com.plasstech.lang.d2.codegen.il.ArrayAlloc;
import com.plasstech.lang.d2.codegen.il.ArraySet;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Call;
import com.plasstech.lang.d2.codegen.il.Dec;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.FieldSetOp;
import com.plasstech.lang.d2.codegen.il.Goto;
import com.plasstech.lang.d2.codegen.il.IfOp;
import com.plasstech.lang.d2.codegen.il.Inc;
import com.plasstech.lang.d2.codegen.il.Label;
import com.plasstech.lang.d2.codegen.il.Nop;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.ProcExit;
import com.plasstech.lang.d2.codegen.il.Return;
import com.plasstech.lang.d2.codegen.il.Stop;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.common.D2RuntimeException;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.type.ParamSymbol;
import com.plasstech.lang.d2.type.ProcSymbol;
import com.plasstech.lang.d2.type.RecordSymbol.ArrayField;
import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VarType;

public class Interpreter extends DefaultOpcodeVisitor {
  private static final int MAX_ITERATIONS = 10000000;

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final State state;
  private final ImmutableList<Op> code;
  private final boolean interactive;

  private final Environment rootEnv = new Environment();
  private final Stack<Integer> ipStack = new Stack<>();
  private final Stack<Environment> envs = new Stack<>();
  private int ip;
  private int iterations;
  private boolean running = true;

  private Level loggingLevel;
  private InterpreterResult result;

  public Interpreter(State state, boolean interactive) {
    this.state = state;
    this.code = state.lastIlCode();
    this.interactive = interactive;
    envs.push(rootEnv);
  }

  public void setDebugLevel(int debugInt) {
    switch (debugInt) {
      case 1:
        loggingLevel = Level.CONFIG;
        break;
      case 2:
        loggingLevel = Level.INFO;
        break;
      default:
      case 0:
        loggingLevel = Level.FINE;
        break;
    }
  }

  public InterpreterResult execute() {
    result = new InterpreterResult(state, rootEnv);
    while (running) {
      Op op = code.get(ip);
      if (!(op instanceof Nop) && !(op instanceof Label)) {
        result.incInstructionCycle();
      }
      logger.atFine().log(
          "Current op: ip: %d: %s. cycle %d st %s", ip, op, result.instructionCycles(), envs);
      ip++;
      try {
        op.accept(this);
      } catch (RuntimeException re) {
        logger.atSevere().withCause(re).log("Exception at ip %d: %s; env: %s", ip, op, envs);
        // Yeah yeah I know.
        throw re;
      }
      iterations++;
      if (iterations > MAX_ITERATIONS) {
        logger.atSevere().log("Terminated after too many iterations (%d)", MAX_ITERATIONS);
        break;
      }
    }
    if (!ipStack.isEmpty()) {
      logger.atSevere().log("Stack not empty");
    }
    logger.at(loggingLevel).log("Interpreter ran for %d iterations", iterations);
    return result;
  }

  @Override
  public void visit(Transfer op) {
    Object rhsVal = resolve(op.source());
    // Previously it would fail if rhsValue was null, but that needed to be relaxed to support
    // you know, nulls.
    setValue(op.destination(), rhsVal);
  }

  @Override
  public void visit(ArrayAlloc op) {
    int sizeVal = (Integer) resolve(op.sizeLocation());
    VarType baseType = op.arrayType().baseType();
    Object emptyArray = createEmptyArray(baseType, sizeVal);
    setValue(op.destination(), emptyArray);
  }

  private static Object[] createEmptyArray(VarType baseType, int sizeVal) {
    if (baseType == VarType.BOOL) {
      Boolean[] booleans = new Boolean[sizeVal];
      Arrays.setAll(booleans, (index) -> false);
      return booleans;
    } else if (baseType == VarType.INT) {
      Integer[] integers = new Integer[sizeVal];
      Arrays.setAll(integers, (index) -> 0);
      return integers;
    } else if (baseType == VarType.BYTE) {
      Byte[] bytes = new Byte[sizeVal];
      Arrays.setAll(bytes, (index) -> (byte) 0);
      return bytes;
    } else if (baseType == VarType.DOUBLE) {
      Double[] doubles = new Double[sizeVal];
      Arrays.setAll(doubles, (index) -> 0.0);
      return doubles;
    } else if (baseType == VarType.STRING) {
      String[] strings = new String[sizeVal];
      Arrays.setAll(strings, (index) -> "");
      return strings;
    } else if (baseType.isRecord()) {
      // Cannot make array of map
      Object[] records = new Object[sizeVal];
      for (int i = 0; i < sizeVal; ++i) {
        records[i] = null;
      }
      return records;
    }
    throw new IllegalStateException("Cannot create array of " + baseType);
  }

  @Override
  public void visit(ArraySet op) {
    int index = (Integer) resolve(op.index());
    Object array = resolve(op.array());
    if (array instanceof Object[]) {
      Object[] arrayValue = (Object[]) array;
      arrayValue[index] = resolve(op.source());
    } else {
      List<Object> arrayList = (List<Object>) array;
      arrayList.set(index, resolve(op.source()));
    }
  }

  @Override
  public void visit(FieldSetOp op) {
    Map<String, Object> recordObject = (Map<String, Object>) resolve(op.recordLocation());
    Object rhsVal = resolve(op.source());
    recordObject.put(op.field(), rhsVal);
  }

  @Override
  public void visit(IfOp ifOp) {
    Object cond = resolve(ifOp.condition());
    if ((cond.equals(Boolean.TRUE) && !ifOp.isNot())
        || (cond.equals(Boolean.FALSE) && ifOp.isNot())) {
      String dest = ifOp.destination();
      gotoLabel(dest);
      result.incBranchesTaken();
    } else {
      result.incBranchesNotTaken();
    }
  }

  @Override
  public void visit(Goto op) {
    gotoLabel(op.label());
    result.incGotos();
  }

  private void gotoLabel(String dest) {
    for (int i = 0; i < code.size(); ++i) {
      Op op = code.get(i);
      if (op instanceof Label) {
        Label label = (Label) op;
        if (label.label().equals(dest)) {
          ip = i;
          return;
        }
      }
    }
    throw new IllegalStateException("Could not find destination label " + dest);
  }

  @Override
  public void visit(BinOp op) {
    Operand leftOperand = op.left();
    Object left = resolve(leftOperand);
    Operand rightOperand = op.right();
    Object right = resolve(rightOperand);

    Object result;
    if (op.operator() == TokenType.DOT) {
      result = visitDotOp(left, (String) right);
    } else if (left instanceof Integer && right instanceof Integer) {
      result = visitIntBinOp(op, (Integer) left, (Integer) right);
    } else if (left instanceof Byte && right instanceof Byte) {
      result = visitByteBinOp(op, (Byte) left, (Byte) right);
    } else if (left instanceof Double && right instanceof Double) {
      result = visitDoubleBinOp(op, (Double) left, (Double) right);
    } else if (left instanceof Boolean && right instanceof Boolean) {
      result = visitBoolBinOp(op, (Boolean) left, (Boolean) right);
    } else if (left instanceof String && (right instanceof String || right == null)) {
      result = visitStringBinOp(op, (String) left, (String) right);
    } else if (left instanceof String && right instanceof Integer) {
      result = visitBinOp(op, (String) left, (Integer) right);
    } else if (left != null && left.getClass().isArray()) {
      result = visitArrayBinOp(op, (Object[]) left, right);
    } else if (left instanceof ArrayList && right instanceof Integer) {
      result = visitLiteralArrayBinOp(op, left, (Integer) right);
    } else if (leftOperand.type().isRecord()
        && (rightOperand.type().isRecord() || rightOperand == null)) {
      result = visitRecordBinop(op, left, right);
    } else if (left == null || right == null) {
      result = visitBinOpNulls(op, left, right);
    } else {
      throw new IllegalStateException(
          String.format(
              "Not sure what to do with %s; left %s (%s) right %s (%s)",
              op, left, left.getClass().getSimpleName(), right, right.getClass().getSimpleName()));
    }

    setValue(op.destination(), result);
  }

  private Object visitRecordBinop(BinOp op, Object left, Object right) {
    switch (op.operator()) {
      case EQEQ:
        return Objects.equal(left, right);
      case NEQ:
        return !Objects.equal(left, right);
      default:
        throw new IllegalStateException(
            String.format(
                "Not sure what to do with %s; left %s (%s) right %s (%s)",
                op,
                left,
                left.getClass().getSimpleName(),
                right,
                right.getClass().getSimpleName()));
    }
  }

  private Object visitBinOpNulls(BinOp op, Object left, Object right) {
    boolean leftNull = left == null;
    boolean rightNull = right == null;
    switch (op.operator()) {
      case EQEQ:
      case GEQ:
      case LEQ:
        return leftNull == rightNull;
      case NEQ:
        return leftNull != rightNull;
      case GT:
        return !leftNull && rightNull;
      case LT:
        return false;
      default:
        throw new IllegalStateException("Unknown null binop " + op);
    }
  }

  private Object visitDotOp(Object left, String right) {
    if (left == null) {
      throw new D2RuntimeException("Dereferencing null record", null, "Interpreter");
    }
    if (!(left instanceof Map)) {
      logger.atSevere().log("Not sure what to do with %s, should be record type", left);
    }
    @SuppressWarnings("unchecked")
    Map<String, Object> leftAsMap = (Map<String, Object>) left;
    return leftAsMap.get(right);
  }

  private Object visitLiteralArrayBinOp(BinOp op, Object left, int right) {
    if (op.operator() == TokenType.LBRACKET) {
      List array = (List) left;
      return array.get(right);
    }
    throw new IllegalStateException("Unknown array/int binop " + op.operator());
  }

  private Object visitArrayBinOp(BinOp op, Object[] left, Object right) {
    List<Object> leftList = Arrays.asList(left);
    switch (op.operator()) {
      case EQEQ:
      case NEQ:
        List<Object> rightList = Arrays.asList((Object[]) right);
        boolean same = leftList.equals(rightList);
        return same == (op.operator() == TokenType.EQEQ);

      case LBRACKET:
        int index = (Integer) right;
        return left[index];

      default:
        throw new IllegalStateException(
            String.format(
                "Not sure what to do with %s; left %s (%s) right %s (%s)",
                op,
                leftList,
                left.getClass().getSimpleName(),
                right,
                right.getClass().getSimpleName()));
    }
  }

  private Object visitBinOp(BinOp op, String left, Integer right) {
    if (op.operator() == TokenType.LBRACKET) {
      return "" + left.charAt(right);
    }
    throw new IllegalStateException("Unknown string/int binop " + op.operator());
  }

  private Object visitStringBinOp(BinOp op, String left, String right) {
    if (right == null) {
      switch (op.operator()) {
        case EQEQ:
        case LEQ:
        case GEQ:
          // can only be equal if left == right
          return left == null;
        case GT:
          // not null > null
        case NEQ:
          // not null != null
          return left != null;
        case LT:
          // nothing can be < null.
          return false;
        default:
          throw new IllegalStateException("Unknown string binop " + op.operator());
      }
    }
    switch (op.operator()) {
      case EQEQ:
        return left.equals(right);
      case GEQ:
        return left.compareTo(right) >= 0;
      case GT:
        return left.compareTo(right) > 0;
      case LEQ:
        return left.compareTo(right) <= 0;
      case LT:
        return left.compareTo(right) < 0;
      case NEQ:
        return !left.equals(right);
      case PLUS:
        return left + right;

      default:
        throw new IllegalStateException("Unknown string binop " + op.operator());
    }
  }

  private Object visitIntBinOp(BinOp op, int left, int right) {
    switch (op.operator()) {
      case DIV:
        return left / right;
      case EQEQ:
        return left == right;
      case GEQ:
        return left >= right;
      case GT:
        return left > right;
      case LEQ:
        return left <= right;
      case LT:
        return left < right;
      case MINUS:
        return left - right;
      case MOD:
        return left % right;
      case MULT:
        return left * right;
      case NEQ:
        return left != right;
      case PLUS:
        return left + right;
      case SHIFT_LEFT:
        return left << right;
      case SHIFT_RIGHT:
        return left >> right;
      case BIT_AND:
        return left & right;
      case BIT_OR:
        return left | right;
      case BIT_XOR:
        return left ^ right;
      default:
        throw new IllegalStateException("Unknown int binop " + op.operator());
    }
  }

  private Object visitByteBinOp(BinOp op, byte left, byte right) {
    switch (op.operator()) {
      case DIV:
        return (byte) (left / right);
      case EQEQ:
        return left == right;
      case GEQ:
        return left >= right;
      case GT:
        return left > right;
      case LEQ:
        return left <= right;
      case LT:
        return left < right;
      case MINUS:
        return (byte) (left - right);
      case MOD:
        return (byte) (left % right);
      case MULT:
        return (byte) (left * right);
      case NEQ:
        return left != right;
      case PLUS:
        return (byte) (left + right);
      case SHIFT_LEFT:
        return (byte) (left << right);
      case SHIFT_RIGHT:
        return (byte) (left >> right);
      case BIT_AND:
        return (byte) (left & right);
      case BIT_OR:
        return (byte) (left | right);
      case BIT_XOR:
        return (byte) (left ^ right);
      default:
        throw new IllegalStateException("Unknown byte binop " + op.operator());
    }
  }

  private Object visitDoubleBinOp(BinOp op, double left, double right) {
    switch (op.operator()) {
      case DIV:
        return left / right;
      case EQEQ:
        return left == right;
      case GEQ:
        return left >= right;
      case GT:
        return left > right;
      case LEQ:
        return left <= right;
      case LT:
        return left < right;
      case MINUS:
        return left - right;
      case MULT:
        return left * right;
      case NEQ:
        return left != right;
      case PLUS:
        return left + right;
      default:
        throw new IllegalStateException("Unknown double binop " + op.operator());
    }
  }

  private Object visitBoolBinOp(BinOp op, Boolean left, Boolean right) {
    switch (op.operator()) {
      case AND:
        return left && right;
      case OR:
        return left || right;
      case XOR:
        return left ^ right;
      case EQEQ:
        return left == right;
      case NEQ:
        return left != right;
      case LT:
        return !left && right;
      case GT:
        return left && !right;
      case LEQ:
        return (!left && right) || left == right;
      case GEQ:
        return (left && !right) || left == right;
      default:
        throw new IllegalStateException("Unknown bool binop " + op.operator());
    }
  }

  @Override
  public void visit(Inc op) {
    Object target = resolve(op.target());
    if (target instanceof Integer) {
      int previous = (int) target;
      setValue(op.target(), previous + 1);
    } else {
      byte previous = (byte) target;
      setValue(op.target(), (byte) (previous + 1));
    }
  }

  @Override
  public void visit(Dec op) {
    Object target = resolve(op.target());
    if (target instanceof Integer) {
      int previous = (int) target;
      setValue(op.target(), previous - 1);
    } else {
      byte previous = (byte) target;
      setValue(op.target(), (byte) (previous - 1));
    }
  }

  @Override
  public void visit(UnaryOp op) {
    Object rhs = resolve(op.operand());
    Object result = null;
    // FFS use op.source.type

    if (rhs instanceof Boolean || rhs instanceof Integer) {
      result = visitUnaryInt(op, rhs);
    } else if (rhs instanceof Byte) {
      result = visitUnaryByte(op, rhs);
    } else if (rhs instanceof Double) {
      result = visitUnaryDouble(op, rhs);
    } else if (op.operand().type() == VarType.STRING) {
      result = visitUnaryString(op, (String) rhs);
    } else if (rhs != null && rhs.getClass().isArray()) {
      result = visitUnaryArray(op, (Object[]) rhs);
    } else {
      throw new IllegalStateException(
          "Unknown unary op operand " + rhs + " for op " + op.toString());
    }
    setValue(op.destination(), result);
  }

  private Object visitUnaryByte(UnaryOp op, Object rhs) {
    byte r1 = (byte) rhs;
    switch (op.operator()) {
      case PLUS:
        return r1;
      case MINUS:
        return (byte) -r1;
      case BIT_NOT:
        return (byte) ~r1;
      default:
        throw new IllegalStateException("Unknown byte unaryop " + op.operator());
    }
  }

  private static Object visitUnaryArray(UnaryOp op, Object[] rhs) {
    switch (op.operator()) {
      case LENGTH:
        return rhs.length;
      default:
        throw new IllegalStateException("Unknown array unaryop " + op.operator());
    }
  }

  private static Object visitUnaryString(UnaryOp op, String rhs) {
    switch (op.operator()) {
      case LENGTH:
        return rhs.length();
      case ASC:
        return (int) rhs.charAt(0);
      default:
        throw new IllegalStateException("Unknown string unaryop " + op.operator());
    }
  }

  private static Object visitUnaryInt(UnaryOp op, Object rhs) {
    if (op.operator() == TokenType.NOT) {
      return rhs.equals(Boolean.FALSE);
    }
    int r1;
    if (rhs instanceof Integer) {
      r1 = (int) rhs;
    } else {
      r1 = (byte) rhs;
    }
    switch (op.operator()) {
      case PLUS:
        return r1;
      case MINUS:
        return 0 - r1;
      case BIT_NOT:
        return ~r1;
      case CHR:
        return "" + (char) r1;
      default:
        throw new IllegalStateException("Unknown bool/int unaryop " + op.operator());
    }
  }

  private static Object visitUnaryDouble(UnaryOp op, Object rhs) {
    double r1 = (double) rhs;
    if (op.operator() == TokenType.PLUS) {
      return r1;
    } else if (op.operator() == TokenType.MINUS) {
      return 0.0 - r1;
    }
    throw new IllegalStateException("Unknown double unaryop " + op.operator());
  }

  private Object resolve(Operand operand) {
    Object value;
    if (operand.isConstant()) {
      // int, string, bool.
      value = ((ConstantOperand<?>) operand).value();
    } else {
      // symbol
      String name = ((Location) operand).name();
      value = envs.peek().getValue(name);
    }
    return value;
  }

  @Override
  public void visit(SysCall op) {
    Object resolved = resolve(op.arg());
    switch (op.call()) {
      case PRINT:
      case PRINTLN:
        String val;
        if (op.arg().type() == VarType.DOUBLE) {
          double resolvedd = (double) resolved;
          val = String.format("%.6f", resolvedd);
        } else {
          val = String.valueOf(resolved);
        }
        if (interactive) {
          if (op.call() == SysCall.Call.PRINTLN) {
            System.out.println(val);
          } else {
            System.out.print(val);
          }
        }
        rootEnv.addOutput(val);
        if (op.call() == SysCall.Call.PRINTLN) {
          rootEnv.addOutput("\n");
        }
        break;
      case MESSAGE:
        if (interactive) {
          System.err.println("ERROR: " + resolved);
        }
        rootEnv.addOutput("ERROR: " + resolved);
        rootEnv.addOutput("\n");
        break;
      case INPUT:
        assert (op.arg() instanceof Location);
        String input = "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
          String line = reader.readLine();
          while (line != null) {
            input += line + "\n";
            line = reader.readLine();
          }
        } catch (IOException e) {
          throw new InterpreterException("Could not read standard in", e.getMessage());
        }

        setValue((Location) op.arg(), input);
        break;
      default:
        break;
    }
  }

  @Override
  public void visit(Stop op) {
    ipStack.clear();
    running = false;
  }

  @Override
  public void visit(Call op) {
    result.incCalls();

    // 1. push return location onto stack (NOTE, not ip, which is the next op already)
    ipStack.push(ip - 1);

    // 2. spawn environment
    Environment childEnv = envs.peek().spawn();

    // 3. look up each actual parameter in old environment and add to child environment as formal
    // name from symbol table
    ProcSymbol procSymbol = op.procSym();
    for (int i = 0; i < op.actuals().size(); ++i) {
      Operand actualSource = op.actuals().get(i);
      ParamSymbol formalParam = procSymbol.formal(i);
      // formals are not necessarily on the stack...
      Location formal = new StackLocation(formalParam.name(), formalParam.varType(), 0);
      childEnv.setValue(formal, resolve(actualSource));
    }

    // 4. update environment to be child environment
    envs.push(childEnv);

    // 5. goto destination
    gotoLabel(procSymbol.mungedName());
  }

  @Override
  public void visit(ProcExit op) {
    // if we get here, it means the method should have returned void. It may be an error, shrug.
    logger.atWarning().log("ProcExit reached - doing return void instead");
    visit(new Return(op.procName()));
  }

  @Override
  public void visit(Return op) {
    // 1. if there's a return value, look it up in environment
    Object retValue = null;
    if (op.returnValueLocation().isPresent()) {
      retValue = resolve(op.returnValueLocation().get());
    }

    // 2. pop environment
    envs.pop();

    // 3. pop ip
    int oldIp = ipStack.pop();
    // 4. look at call op - if there's a destination, set it in environment
    if (retValue != null) {
      Op callOpAsOp = code.get(oldIp);
      Call callOp = (Call) callOpAsOp;
      if (callOp.destination().isPresent()) {
        setValue(callOp.destination().get(), retValue);
      }
    }
    ip = oldIp + 1;
  }

  @Override
  public void visit(AllocateOp op) {
    Map<String, Object> recordAsMap = new HashMap<>();
    for (String fieldName : op.record().fieldNames()) {
      VarType type = op.record().fieldType(fieldName);
      if (type == VarType.INT || type == VarType.BYTE) {
        recordAsMap.put(fieldName, 0);
      } else if (type == VarType.STRING) {
        recordAsMap.put(fieldName, null);
      } else if (type == VarType.BOOL) {
        recordAsMap.put(fieldName, false);
      } else if (type == VarType.DOUBLE) {
        recordAsMap.put(fieldName, 0.0);
      } else if (type.isArray()) {
        ArrayField arrayField = op.record().getArrayField(fieldName);
        // TODO(#38) support multidimensional arrays
        int size = arrayField.sizes().get(0);
        Object[] emptyArray = createEmptyArray(arrayField.baseType(), size);
        recordAsMap.put(fieldName, emptyArray);
      } else if (type.isRecord()) {
        // it's cool, leave as null
        recordAsMap.put(fieldName, null);
      } else {
        // ???
        throw new IllegalStateException("Cannot initialize field of type " + type);
      }
    }
    setValue(op.destination(), recordAsMap);
  }

  private void setValue(Location location, Object value) {
    // If it's a global symbol, write into root environment.
    if (location.storage() == SymbolStorage.GLOBAL || location.storage() == SymbolStorage.HEAP) {
      rootEnv.setValue(location, value);
      return;
    }
    // Write into "current" environment instead.
    envs.peek().setValue(location, value);
  }
}
