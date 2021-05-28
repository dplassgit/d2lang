package com.plasstech.lang.d2.interpreter;

import java.util.List;

import com.plasstech.lang.d2.codegen.il.Assignment;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.Goto;
import com.plasstech.lang.d2.codegen.il.IfOp;
import com.plasstech.lang.d2.codegen.il.Label;
import com.plasstech.lang.d2.codegen.il.Load;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Stop;
import com.plasstech.lang.d2.codegen.il.Store;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.type.SymTab;

public class Interpreter extends DefaultOpcodeVisitor {

  private final List<Op> code;
  private final Environment env = new Environment();
  private int ip;
  private int iterations;
  private boolean running = true;

  public Interpreter(List<Op> code, SymTab table) {
    this.code = code;
  }

  public Environment execute() {
    while (running) {
      Op op = code.get(ip);
      ip++;
      op.accept(this);
      iterations++;
      if (iterations > 10000) {
        env.addOutput("ERROR: Terminated after too many iterations");
        break;
      }
    }
    return env;
  }

  @Override
  public void visit(Assignment op) {
//    System.err.printf("%d: Visit %s\n", ip, op);
    Object rhsVal = resolve(op.rhs());
    env.setValue(op.lhs(), rhsVal);
  }

  @Override
  public void visit(Load op) {
//    System.err.printf("%d: Visit %s\n", ip, op);
    Object rhsVal = env.getValue(op.sourceAddress());
    if (rhsVal != null) {
      env.setValue(op.destRegister(), rhsVal);
    } else {
      throw new IllegalStateException(String.format("RHS %s has no value", op.sourceAddress()));
    }
  }

  @Override
  public void visit(Store op) {
//    System.err.printf("%d: Visit %s\n", ip, op);
    Object rhsVal = env.getValue(op.sourceRegister());
    if (rhsVal != null) {
      env.setValue(op.destAddress(), rhsVal);
    } else {
      throw new IllegalStateException(String.format("RHS %s has no value", op.sourceRegister()));
    }
  }

  @Override
  public void visit(IfOp ifOp) {
//    System.err.printf("%d: Visit %s\n", ip, ifOp);
    Object cond = env.getValue(ifOp.condition());
    if (cond.equals(1)) {
      for (int i = 0; i < code.size(); ++i) {
        Op op = code.get(i);
        if (op instanceof Label) {
          Label label = (Label) op;
          if (label.label().equals(ifOp.destination())) {
//            System.err.println("Going to " + label.label());
            ip = i;
            return;
          }
        }
      }
      throw new IllegalStateException("Could not find destination label " + ifOp.destination());
    }
  }

  @Override
  public void visit(Goto gotoOp) {
//    System.err.printf("%d: Visit %s\n", ip, gotoOp);
    for (int i = 0; i < code.size(); ++i) {
      Op op = code.get(i);
      if (op instanceof Label) {
        Label label = (Label) op;
        if (label.label().equals(gotoOp.label())) {
//          System.err.println("Going to " + label.label());
          ip = i;
          return;
        }
      }
    }
    throw new IllegalStateException("Could not find destination label " + gotoOp.label());
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
    env.setValue(op.lhs(), result);
  }

  @Override
  public void visit(UnaryOp op) {
//    System.err.printf("%d: Visit %s\n", ip, op);
    int r1 = (Integer) resolve(op.rhs());
    int result;
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
    env.setValue(op.lhs(), result);
  }

  private Object resolve(Object oval) {
    String val = oval.toString();
    if (val.equals("true")) {
      return 1;
    } else if (val.equals("false")) {
      return 0;
    } else if (val.startsWith("t")) {
      // register/temp
      return env.getValue(val);
    } else {
      try {
        return Integer.parseInt(val);
      } catch (NumberFormatException e) {
        return val;
      }
    }
  }

  @Override
  public void visit(SysCall op) {
//    System.err.printf("%d: Visit %s\n", ip, op);
    Object val = resolve(op.arg());
    // assume all system calls are prints.
    env.addOutput(String.valueOf(val));
  }

  @Override
  public void visit(Stop op) {
//    System.err.printf("%d: Visit %s\n", ip, op);
    running = false;
  }
}
