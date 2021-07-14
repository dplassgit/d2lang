package com.plasstech.lang.d2.codegen;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Call;
import com.plasstech.lang.d2.codegen.il.Dec;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.Goto;
import com.plasstech.lang.d2.codegen.il.IfOp;
import com.plasstech.lang.d2.codegen.il.Inc;
import com.plasstech.lang.d2.codegen.il.Label;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;

/** For each opcode, remap temps with a new id. Remap any stack variables too. */
class InlineRemapper extends DefaultOpcodeVisitor {
  private static int global_counter = 0;

  private final ImmutableSet<String> formals;
  private final List<Op> code;
  private final String suffix;

  private int ip;

  InlineRemapper(List<Op> code, ImmutableList<String> formals) {
    this.suffix = "__inline__" + (global_counter++);
    this.formals = ImmutableSet.copyOf(formals);
    this.code = new ArrayList<>(code);
  }

  List<Op> remap() {
    for (ip = 0; ip < code.size(); ++ip) {
      code.get(ip).accept(this);
    }
    return code;
  }

  @Override
  public void visit(BinOp op) {
    Operand left = remap(op.left());
    Operand right = remap(op.right());
    Operand dest = remap(op.destination());
    if (dest == op.destination() && left == op.left() && right == op.right()) {
      // no change
      return;
    }
    code.set(ip, new BinOp((Location) dest, left, op.operator(), right));
  }

  @Override
  public void visit(UnaryOp op) {
    Operand operand = remap(op.operand());
    Operand dest = remap(op.destination());
    if (dest == op.destination() && operand == op.operand()) {
      // no change
      return;
    }
    code.set(ip, new UnaryOp((Location) dest, op.operator(), operand));
  }

  @Override
  public void visit(Inc op) {
    Operand operand = remap(op.target());
    if (operand == op.target()) {
      return;
    }
    code.set(ip, new Inc((Location) operand));
  }

  @Override
  public void visit(Dec op) {
    Operand operand = remap(op.target());
    if (operand == op.target()) {
      return;
    }
    code.set(ip, new Dec((Location) operand));
  }

  @Override
  public void visit(Transfer op) {
    Operand source = remap(op.source());
    Operand destination = remap(op.destination());
    if (source == op.source() && destination == op.destination()) {
      return;
    }
    code.set(ip, new Transfer((Location) destination, source));
  }
  
  @Override
  public void visit(SysCall op) {
    Operand arg = remap(op.arg());
    if (arg == op.arg()) {
      return;
    }
    code.set(ip, new SysCall(op.call(), arg));
  }

  @Override
  public void visit(Goto op) {
    // we don't know if this jumps outside this block so we can't just blindly remap it.
    throw new IllegalStateException("Cannot handle gotos");
  }

  @Override
  public void visit(IfOp op) {
    // we don't know if this jumps outside this block so we can't just blindly remap it.
    throw new IllegalStateException("Cannot handle ifs");
  }

  @Override
  public void visit(Label op) {
    // we don't know if something outside this block jumps here so we can't just blindly remap it.
    throw new IllegalStateException("Cannot handle labels");
  }

  @Override
  public void visit(Call op) {
    throw new IllegalStateException("Cannot handle calls");
  }
  
  private Operand remap(Operand operand) {
    if (operand.isConstant()) {
      return operand;
    }
    Location location = (Location) operand;
    if (formals.contains(location.name())) {
      return operand;
    }
    switch (location.storage()) {
      case TEMP:
        return new TempLocation(location.name() + suffix);
      case LOCAL:
      case PARAM:
        return new StackLocation(location.name() + suffix);
      default:
        return operand;
    }
  }
}
