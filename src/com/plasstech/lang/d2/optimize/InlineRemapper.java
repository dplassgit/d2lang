package com.plasstech.lang.d2.optimize;

import java.util.ArrayList;
import java.util.List;

import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.TempLocation;
import com.plasstech.lang.d2.codegen.il.AllocateOp;
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
import com.plasstech.lang.d2.codegen.il.Return;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.type.SymTab;
import com.plasstech.lang.d2.type.VarType;

/** For each opcode, remap temps with a new id. Remap any stack variables too. */
class InlineRemapper extends DefaultOpcodeVisitor {
  private static int global_counter = 0;

  private final List<Op> code;
  private final String suffix;
  private final SymTab symtab;

  private int ip;

  InlineRemapper(List<Op> code, SymTab symtab) {
    this.symtab = symtab;
    this.suffix = "__inline__" + (global_counter++);
    this.code = new ArrayList<>(code);
  }

  Location remapFormal(String name, VarType type) {
    String fullName = "__" + name + suffix;
    if (symtab.get(fullName) == null) {
      symtab.declare(fullName, type);
    }
    System.err.printf("Remapping formal to %s (type %s)\n", fullName, type);
    // This is messed up because temps are read once, so dead code optimizer and constant
    // propagation optimizer both create invalid code.
    return new TempLocation(fullName, type);
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
    code.set(ip, new BinOp((Location) dest, left, op.operator(), right, op.position()));
  }

  @Override
  public void visit(UnaryOp op) {
    Operand operand = remap(op.operand());
    Operand dest = remap(op.destination());
    if (dest == op.destination() && operand == op.operand()) {
      // no change
      return;
    }
    code.set(ip, new UnaryOp((Location) dest, op.operator(), operand, op.position()));
  }

  @Override
  public void visit(Inc op) {
    Operand operand = remap(op.target());
    if (operand == op.target()) {
      return;
    }
    code.set(ip, new Inc((Location) operand, op.position()));
  }

  @Override
  public void visit(Dec op) {
    Operand operand = remap(op.target());
    if (operand == op.target()) {
      return;
    }
    code.set(ip, new Dec((Location) operand, op.position()));
  }

  @Override
  public void visit(Transfer op) {
    Operand source = remap(op.source());
    Operand destination = remap(op.destination());
    if (source == op.source() && destination == op.destination()) {
      return;
    }
    code.set(ip, new Transfer((Location) destination, source, op.position()));
  }

  @Override
  public void visit(ArraySet op) {
    Operand source = remap(op.source());
    Operand index = remap(op.index());
    Location destination = (Location) remap(op.array());
    if (source == op.source() && destination == op.array() && index == op.index()) {
      return;
    }
    code.set(ip, new ArraySet(destination, op.arrayType(), index, source, false, op.position()));
  }

  @Override
  public void visit(FieldSetOp op) {
    Operand source = remap(op.source());
    Location destination = (Location) remap(op.recordLocation());
    if (source == op.source() && destination == op.recordLocation()) {
      return;
    }
    code.set(ip, new FieldSetOp(destination, op.recordSymbol(), op.field(), source, op.position()));
  }

  @Override
  public void visit(AllocateOp op) {
    Location destination = (Location) remap(op.destination());
    if (destination == op.destination()) {
      return;
    }
    code.set(ip, new AllocateOp(destination, op.record(), op.position()));
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

  @Override
  public void visit(Return op) {
    if (!op.returnValueLocation().isPresent()) {
      // no return value, at least at this location. nuke it.
      code.set(ip, new Nop(op));
    } else {
      code.set(ip, new Return(op.procName(), remap(op.returnValueLocation().get())));
    }
  }

  private Operand remap(Operand operand) {
    if (operand.isConstant()) {
      return operand;
    }
    Location location = (Location) operand;
    switch (location.storage()) {
      case TEMP:
      case LOCAL:
      case PARAM:
        String fullName = "__" + location.name() + suffix;
        if (symtab.get(fullName) == null) {
          symtab.declare(fullName, location.type());
        }
        // This is messed up because temps are read once, so dead code optimizer and constant
        // propagation optimizer both create invalid code.
        return new TempLocation(fullName, location.type());

      default:
        return operand;
    }
  }
}
