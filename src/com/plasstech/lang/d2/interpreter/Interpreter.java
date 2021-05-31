package com.plasstech.lang.d2.interpreter;

import java.util.List;
import java.util.Stack;

import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Call;
import com.plasstech.lang.d2.codegen.il.ConstantOperand;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.Goto;
import com.plasstech.lang.d2.codegen.il.IfOp;
import com.plasstech.lang.d2.codegen.il.Label;
import com.plasstech.lang.d2.codegen.il.Location;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Operand;
import com.plasstech.lang.d2.codegen.il.Return;
import com.plasstech.lang.d2.codegen.il.StackLocation;
import com.plasstech.lang.d2.codegen.il.Stop;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.parse.ProcedureNode.Parameter;
import com.plasstech.lang.d2.type.ProcSymbol;
import com.plasstech.lang.d2.type.SymTab;

public class Interpreter extends DefaultOpcodeVisitor {

  private final List<Op> code;
  private int ip;
  private int iterations;
  private boolean running = true;
  private final SymTab table;
  private final Environment rootEnv = new Environment();
  private final Stack<Integer> ipStack = new Stack<>();
  private final Stack<Environment> envs = new Stack<>();

  public Interpreter(List<Op> code, SymTab table) {
    this.code = code;
    this.table = table;
    envs.push(rootEnv);
  }

  public Environment execute() {
    while (running) {
      Op op = code.get(ip);
      ip++;
      try {
      op.accept(this);
      } catch (RuntimeException re) {
        System.err.println("Exception at " + op);
        System.err.println(envs.peek());
        throw re;
      }
      iterations++;
      if (iterations > 10000) {
        System.err.println("ERROR: Terminated after too many iterations");
        break;
      }
    }
    if (!ipStack.isEmpty()) {
      System.err.println("Stack not empty");
    }
    return envs.peek();
  }

  @Override
  public void visit(Transfer op) {
//    System.err.printf("%d: Visit %s\n", ip, op);
    Object rhsVal = resolve(op.source());
    if (rhsVal != null) {
      setValue(op.destination(), rhsVal);
    } else {
      throw new IllegalStateException(String.format("RHS %s has no value", op.source()));
    }
  }

  @Override
  public void visit(IfOp ifOp) {
//    System.err.printf("%d: Visit %s\n", ip, ifOp);
    Object cond = resolve(ifOp.condition());
    if (cond.equals(1)) {
      String dest = ifOp.destination();
      gotoLabel(dest);
    }
  }

  @Override
  public void visit(Goto op) {
//    System.err.printf("%d: Visit %s\n", ip, gotoOp);
    gotoLabel(op.label());
  }

  private void gotoLabel(String dest) {
    for (int i = 0; i < code.size(); ++i) {
      Op op = code.get(i);
      if (op instanceof Label) {
        Label label = (Label) op;
        if (label.label().equals(dest)) {
//            System.err.println("Going to " + label.label());
          ip = i;
          return;
        }
      }
    }
    throw new IllegalStateException("Could not find destination label " + dest);
  }

  @Override
  public void visit(BinOp op) {
//    System.err.printf("%d: Visit %s\n", ip, op);
    int r1 = (Integer) resolve(op.rhs1());
    int r2 = (Integer) resolve(op.rhs2());
    int result;
    switch (op.operator()) {
      case AND:
        result = ((r1 != 0) && (r2 != 0)) ? 1 : 0;
        break;
      case OR:
        result = ((r1 != 0) || (r2 != 0)) ? 1 : 0;
        break;
      case DIV:
        result = r1 / r2;
        break;
      case EQEQ:
        result = (r1 == r2) ? 1 : 0;
        break;
      case GEQ:
        result = (r1 >= r2) ? 1 : 0;
        break;
      case GT:
        result = (r1 > r2) ? 1 : 0;
        break;
      case LEQ:
        result = (r1 <= r2) ? 1 : 0;
        break;
      case LT:
        result = (r1 < r2) ? 1 : 0;
        break;
      case MINUS:
        result = r1 - r2;
        break;
      case MOD:
        result = r1 % r2;
        break;
      case MULT:
        result = r1 * r2;
        break;
      case NEQ:
        result = (r1 != r2) ? 1 : 0;
        break;
      case PLUS:
        result = r1 + r2;
        break;
      default:
        throw new IllegalStateException("Unknown binop " + op.operator());
    }
    setValue(op.destination(), result);
  }

  @Override
  public void visit(UnaryOp op) {
//    System.err.printf("%d: Visit %s\n", ip, op);
    Object rhs = resolve(op.rhs());
    int r1;
    if (rhs == Boolean.TRUE) {
      r1 = 1;
    } else if (rhs == Boolean.FALSE) {
      r1 = 0;
    } else {
      r1 = (Integer) rhs;
    }
    Object result;
    switch (op.operator()) {
      case MINUS:
        result = 0 - r1;
        break;
      case NOT:
        result = (r1 == 0) ? 1 : 0;
        break;
      default:
        throw new IllegalStateException("Unknown unaryop " + op.operator());
    }
    setValue(op.destination(), result);
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
//    System.err.printf("%d: Visit %s\n", ip, op);
    Object val = resolve(op.arg());
    // TODO: don't assume all system calls are prints.
    rootEnv.addOutput(String.valueOf(val));
  }

  @Override
  public void visit(Stop op) {
//    System.err.printf("%d: Visit %s\n", ip, op);
    running = false;
  }

  @Override
  public void visit(Call op) {
//    System.err.printf("%d: Visit %s\n", ip, op);

    // 1. push return location onto stack (NOTE, not ip, which is the next op already)
    ipStack.push(ip - 1);

    // 2. spawn environment
    Environment childEnv = envs.peek().spawn();

    // 3. look up each actual parameter in old environment and add to child environment as formal
    // name from symbol table
    ProcSymbol procSymbol = (ProcSymbol) table.get(op.functionToCall());
    for (int i = 0; i < op.actualLocations().size(); ++i) {
      Location actualSource = op.actualLocations().get(i);
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
  public void visit(Return op) {
//    System.err.printf("%d: Visit %s\n", ip, op);

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
      setValue(callOp.destination(), retValue);
    }
    ip = oldIp + 1;
  }

  private void setValue(Location location, Object value) {
    envs.peek().setValue(location, value);
  }
}
