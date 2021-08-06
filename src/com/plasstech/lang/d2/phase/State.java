package com.plasstech.lang.d2.phase;

import javax.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.common.D2RuntimeException;
import com.plasstech.lang.d2.parse.node.ProgramNode;
import com.plasstech.lang.d2.type.SymTab;
import com.plasstech.lang.d2.type.TypeCheckResult;

@AutoValue
public abstract class State {

  public static Builder create(String sourceCode) {
    return new AutoValue_State.Builder().setSourceCode(sourceCode);
  }

  @Nullable
  public abstract D2RuntimeException exception();

  public boolean error() {
    return exception() != null;
  }

  @Nullable
  public abstract String filename();

  @Nullable
  public abstract String sourceCode();

  @Nullable
  public abstract ProgramNode programNode();

  @Nullable
  public abstract SymTab symbolTable();

  @Nullable
  public abstract TypeCheckResult typeCheckResult();

  @Nullable
  public abstract ImmutableList<Op> ilCode();

  @Nullable
  public abstract ImmutableList<Op> optimizedIlCode();

  public ImmutableList<Op> lastIlCode() {
    if (optimizedIlCode() != null) {
      return optimizedIlCode();
    }
    return ilCode();
  }

  @Nullable
  public abstract ImmutableList<String> asmCode();

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setException(D2RuntimeException exception);

    public abstract Builder setFilename(String filename);

    public abstract Builder setSourceCode(String code);

    public abstract Builder setProgramNode(ProgramNode node);

    public abstract Builder setSymbolTable(SymTab symbolTable);

    public abstract Builder setTypeCheckResult(TypeCheckResult result);

    public abstract Builder setIlCode(ImmutableList<Op> code);

    public abstract Builder setOptimizedIlCode(ImmutableList<Op> code);

    public abstract Builder setAsmCode(ImmutableList<String> asmCode);

    public abstract State build();
  }

  public State addException(D2RuntimeException exception) {
    return toBuilder().setException(exception).build();
  }

  public State addFilename(String filename) {
    return toBuilder().setFilename(filename).build();
  }

  public State addSourceCode(String sourceCode) {
    return toBuilder().setSourceCode(sourceCode).build();
  }

  public State addProgramNode(ProgramNode node) {
    return toBuilder().setProgramNode(node).build();
  }

  public State addSymbolTable(SymTab table) {
    return toBuilder().setSymbolTable(table).build();
  }

  public State addTypecheckResult(TypeCheckResult result) {
    return toBuilder().setTypeCheckResult(result).build();
  }

  public State addIlCode(ImmutableList<Op> ilCode) {
    return toBuilder().setIlCode(ilCode).build();
  }

  public State addOptimizedCode(ImmutableList<Op> optimizedIlCode) {
    return toBuilder().setOptimizedIlCode(optimizedIlCode).build();
  }

  public State addAsmCode(ImmutableList<String> asmCode) {
    return toBuilder().setAsmCode(asmCode).build();
  }

  abstract Builder toBuilder();

}
