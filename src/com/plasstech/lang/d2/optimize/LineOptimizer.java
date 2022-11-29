package com.plasstech.lang.d2.optimize;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.google.common.flogger.LogSites;
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
import com.plasstech.lang.d2.codegen.il.Nop;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.OpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.ProcEntry;
import com.plasstech.lang.d2.codegen.il.ProcExit;
import com.plasstech.lang.d2.codegen.il.Return;
import com.plasstech.lang.d2.codegen.il.Stop;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.type.SymTab;

abstract class LineOptimizer extends DefaultOptimizer implements OpcodeVisitor {
  private final FluentLogger logger = FluentLogger.forEnclosingClass();

  private int ip;

  protected final Level loggingLevel;
  protected List<Op> code;
  protected SymTab symtab;

  LineOptimizer(int debugLevel) {
    this.loggingLevel = toLoggingLevel(debugLevel);
  }

  @Override
  public final ImmutableList<Op> optimize(ImmutableList<Op> input, SymTab symtab) {
    this.symtab = symtab;
    code = new ArrayList<>(input);
    preProcess();
    setChanged(false);
    for (ip = 0; ip < code.size(); ++ip) {
      try {
        code.get(ip).accept(this);
      } catch (ClassCastException e) {
        logger.atSevere().withCause(e).log("Cannot optimize %s", code.get(ip).toString());
        throw e;
      }
    }
    postProcess();
    return ImmutableList.copyOf(code);
  }

  protected void postProcess() {}

  protected void preProcess() {}

  /** Return the opcode at the given IP, if it's in range. Otherwise, return null. */
  protected final Op getOpAt(int theIp) {
    if (theIp < code.size()) {
      return code.get(theIp);
    }
    return null;
  }

  /** Replace the op at the given ip with the given op. */
  protected final void replaceAt(int theIp, Op newOp) {
    setChanged(true);
    logger
        .at(loggingLevel)
        .withInjectedLogSite(LogSites.callerOf(LineOptimizer.class))
        .log("Replacing ip %d: %s with %s", theIp, code.get(theIp), newOp);
    code.set(theIp, newOp);
  }

  /** Replace the current op with the given op. */
  protected final void replaceCurrent(Op newOp) {
    replaceAt(ip, newOp);
  }

  /** Replace the op at the given ip with a nop. */
  protected final void deleteAt(int theIp) {
    Op theOp = code.get(theIp);
    if (!(theOp instanceof Nop)) {
      replaceAt(theIp, new Nop(theOp));
    }
  }

  /** Replace the current op with a nop. */
  protected final void deleteCurrent() {
    deleteAt(ip);
  }

  protected final int ip() {
    return ip;
  }

  protected final <T extends Op> void replaceAllMatching(
      Class<T> opClazz, Predicate<T> pred, Function<T, Op> replacer) {

    for (int index = 0; index < code.size(); ++index) {
      Op op = code.get(index);
      if (op.getClass().equals(opClazz)) {
        T top = (T) op;
        if (pred.test(top)) {
          replaceAt(index, replacer.apply(top));
        }
      }
    }
  }

  @Override
  public void visit(Label op) {}

  @Override
  public void visit(IfOp op) {}

  @Override
  public void visit(Transfer op) {}

  @Override
  public void visit(BinOp op) {}

  @Override
  public void visit(Return op) {}

  @Override
  public void visit(Stop op) {}

  @Override
  public void visit(SysCall op) {}

  @Override
  public void visit(UnaryOp op) {}

  @Override
  public void visit(Goto op) {}

  @Override
  public void visit(Call op) {}

  @Override
  public void visit(ProcExit op) {}

  @Override
  public void visit(ProcEntry op) {}

  @Override
  public void visit(Dec op) {}

  @Override
  public void visit(Inc op) {}

  @Override
  public void visit(AllocateOp op) {}

  @Override
  public void visit(ArrayAlloc op) {}

  @Override
  public void visit(ArraySet op) {}

  @Override
  public void visit(FieldSetOp op) {}
}
