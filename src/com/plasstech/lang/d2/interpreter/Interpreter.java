package com.plasstech.lang.d2.interpreter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;

import com.google.common.flogger.FluentLogger;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.StackLocation;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Call;
import com.plasstech.lang.d2.codegen.il.Dec;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.Goto;
import com.plasstech.lang.d2.codegen.il.IfOp;
import com.plasstech.lang.d2.codegen.il.Inc;
import com.plasstech.lang.d2.codegen.il.Label;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.ProcExit;
import com.plasstech.lang.d2.codegen.il.Return;
import com.plasstech.lang.d2.codegen.il.Stop;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.lex.Token;
import com.plasstech.lang.d2.parse.node.ProcedureNode.Parameter;
import com.plasstech.lang.d2.type.ProcSymbol;
import com.plasstech.lang.d2.type.SymTab;
import com.plasstech.lang.d2.type.SymbolStorage;

public class Interpreter extends DefaultOpcodeVisitor {
  private static final int MAX_ITERATIONS = 10000000;

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final List<Op> code;
  private final boolean interactive;
  private int ip;
  private int iterations;
  private boolean running = true;
  private final SymTab table;
  private final Environment rootEnv = new Environment();
  private final Stack<Integer> ipStack = new Stack<>();
  private final Stack<Environment> envs = new Stack<>();

  private Level loggingLevel;

  public Interpreter(List<Op> code, SymTab table, boolean interactive) {
    this.code = code;
    this.table = table;
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

  public Environment execute() {
    while (running) {
      Op op = code.get(ip);
      logger.at(loggingLevel).log("Current op: ip: %d: %s. Env: %s", ip, op, envs.peek());
      ip++;
      try {
        op.accept(this);
      } catch (NullPointerException re) {
        logger.atSevere().withCause(re).log("Exception at ip %d: %s; env: %s", ip, op, envs);
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
    return rootEnv;
  }

  @Override
  public void visit(Transfer op) {
    Object rhsVal = resolve(op.source());
    if (rhsVal != null) {
      setValue(op.destination(), rhsVal);
    } else {
      throw new IllegalStateException(String.format("RHS %s has no value", op.source()));
    }
  }

  @Override
  public void visit(IfOp ifOp) {
    Object cond = resolve(ifOp.condition());
    if (cond.equals(1)) {
      String dest = ifOp.destination();
      gotoLabel(dest);
    }
  }

  @Override
  public void visit(Goto op) {
    gotoLabel(op.label());
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
    Object left = resolve(op.left());
    Object right = resolve(op.right());

    Object result;
    if (left instanceof Integer && right instanceof Integer) {
      result = visitBinOp(op, (Integer) left, (Integer) right);
    } else if (left instanceof String && right instanceof String) {
      result = visitBinOp(op, (String) left, (String) right);
    } else if (left instanceof String && right instanceof Integer) {
      result = visitBinOp(op, (String) left, (Integer) right);
    } else if (left.getClass().isArray() && right instanceof Integer) {
      result = visitBinOp(op, (Object[]) left, (Integer) right);
    } else {
      result = -42;
    }

    setValue(op.destination(), result);
  }

  @Override
  public void visit(Inc op) {
    Object target = resolve(op.target());
    int previous = 0;
    if (target != null) {
      previous = (Integer) target;
    }
    setValue(op.target(), previous + 1);
  }

  @Override
  public void visit(Dec op) {
    Object target = resolve(op.target());
    int previous = 0;
    if (target != null) {
      previous = (Integer) target;
    }
    setValue(op.target(), previous - 1);
  }

  private Object visitBinOp(BinOp op, Object[] left, int right) {
    if (op.operator() == Token.Type.LBRACKET) {
      return left[right];
    }
    throw new IllegalStateException("Unknown array/int binop " + op.operator());
  }

  private Object visitBinOp(BinOp op, String left, Integer right) {
    if (op.operator() == Token.Type.LBRACKET) {
      return "" + left.charAt(right);
    }
    throw new IllegalStateException("Unknown string/int binop " + op.operator());
  }

  private Object visitBinOp(BinOp op, String left, String right) {
    switch (op.operator()) {
      case EQEQ:
        return left.equals(right) ? 1 : 0;
      case GEQ:
        return (left.compareTo(right) >= 0) ? 1 : 0;
      case GT:
        return (left.compareTo(right) > 0) ? 1 : 0;
      case LEQ:
        return (left.compareTo(right) <= 0) ? 1 : 0;
      case LT:
        return (left.compareTo(right) < 0) ? 1 : 0;
      case NEQ:
        return left.equals(right) ? 0 : 1;
      case PLUS:
        return left + right;

      default:
        throw new IllegalStateException("Unknown string binop " + op.operator());
    }
  }

  private int visitBinOp(BinOp op, int left, int right) {
    switch (op.operator()) {
      case AND:
        return ((left != 0) && (right != 0)) ? 1 : 0;
      case OR:
        return ((left != 0) || (right != 0)) ? 1 : 0;
      case XOR:
        return ((left != 0) ^ (right != 0)) ? 1 : 0;
      case DIV:
        return left / right;
      case EQEQ:
        return (left == right) ? 1 : 0;
      case GEQ:
        return (left >= right) ? 1 : 0;
      case GT:
        return (left > right) ? 1 : 0;
      case LEQ:
        return (left <= right) ? 1 : 0;
      case LT:
        return (left < right) ? 1 : 0;
      case MINUS:
        return left - right;
      case MOD:
        return left % right;
      case MULT:
        return left * right;
      case NEQ:
        return (left != right) ? 1 : 0;
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

  @Override
  public void visit(UnaryOp op) {
    Object rhs = resolve(op.operand());
    Object result = null;
    if (rhs instanceof Boolean || rhs instanceof Integer) {
      result = visitUnaryInt(op, rhs);
    } else if (rhs instanceof String) {
      result = visitUnaryString(op, (String) rhs);
    } else if (rhs != null && rhs.getClass().isArray()) {
      result = visitUnaryArray(op, (Object[]) rhs);
    } else {
      throw new IllegalStateException(
          "Unknown unary op operand " + rhs + " for op " + op.toString());
    }
    setValue(op.destination(), result);
  }

  private Object visitUnaryArray(UnaryOp op, Object[] rhs) {
    switch (op.operator()) {
      case LENGTH:
        return rhs.length;
      default:
        throw new IllegalStateException("Unknown array unaryop " + op.operator());
    }
  }

  private Object visitUnaryString(UnaryOp op, String rhs) {
    switch (op.operator()) {
      case LENGTH:
        return rhs.length();
      case ASC:
        return (int) rhs.charAt(0);
      default:
        throw new IllegalStateException("Unknown string unaryop " + op.operator());
    }
  }

  private Object visitUnaryInt(UnaryOp op, Object rhs) {
    int r1;
    if (rhs == Boolean.TRUE) {
      r1 = 1;
    } else if (rhs == Boolean.FALSE) {
      r1 = 0;
    } else {
      r1 = (Integer) rhs;
    }
    switch (op.operator()) {
      case MINUS:
        return 0 - r1;
      case NOT:
        return (r1 == 0) ? 1 : 0;
      case BIT_NOT:
        return ~r1;
      case CHR:
        return "" + (char) r1;
      default:
        throw new IllegalStateException("Unknown bool/int unaryop " + op.operator());
    }
  }

  private Object resolve(Operand operand) {
    Object value;
    if (operand instanceof ConstantOperand) {
      value = ((ConstantOperand) operand).value();
    } else {
      // symbol
      String name = ((Location) operand).name();
      value = envs.peek().getValue(name);
    }
    if (value instanceof Boolean) {
      return (value == Boolean.TRUE) ? 1 : 0;
    }
    return value;
  }

  @Override
  public void visit(SysCall op) {
    switch (op.call()) {
      case PRINT:
        if (interactive) {
          System.out.print(String.valueOf(resolve(op.arg())));
        }
        rootEnv.addOutput(String.valueOf(resolve(op.arg())));
        break;
      case MESSAGE:
        if (interactive) {
          System.err.println("SYSTEM ERROR: " + resolve(op.arg()));
        }
        rootEnv.addOutput("SYSTEM ERROR: " + resolve(op.arg()));
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
    // 1. push return location onto stack (NOTE, not ip, which is the next op already)
    ipStack.push(ip - 1);

    // 2. spawn environment
    Environment childEnv = envs.peek().spawn();

    // 3. look up each actual parameter in old environment and add to child environment as formal
    // name from symbol table
    ProcSymbol procSymbol = (ProcSymbol) table.get(op.functionToCall());
    for (int i = 0; i < op.actualLocations().size(); ++i) {
      Operand actualSource = op.actualLocations().get(i);
      Parameter formalParam = procSymbol.node().parameters().get(i);
      StackLocation formal = new StackLocation(formalParam.name());
      childEnv.setValue(formal, resolve(actualSource));
    }

    // 4. update environment to be child environment
    envs.push(childEnv);

    // 5. goto destination
    gotoLabel(procSymbol.name());
  }

  @Override
  public void visit(ProcExit op) {
    // if we get here, it means the method should have returned void. It may be an error, shrug.
    logger.atWarning().log("ProcExit reached - doing return void instead");
    visit(new Return());
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

  private void setValue(Location location, Object value) {
    // If it's a global symbol, write into root environment.
    if (location.storage() == SymbolStorage.GLOBAL) {
      rootEnv.setValue(location, value);
      return;
    }
    // Write into "current" environment instead.
    envs.peek().setValue(location, value);
  }
}
