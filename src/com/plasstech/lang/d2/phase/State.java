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

  public State addException(D2RuntimeException exception) {
    return toBuilder().setException(exception).build();
  }

  public boolean error() {
    return exception() != null;
  }

  @Nullable
  public abstract String filename();

  public State addFilename(String filename) {
    return toBuilder().setFilename(filename).build();
  }

  @Nullable
  public abstract String sourceCode();

  public State addSourceCode(String sourceCode) {
    return toBuilder().setSourceCode(sourceCode).build();
  }

  @Nullable
  public abstract ProgramNode programNode();

  public State addProgramNode(ProgramNode node) {
    return toBuilder().setProgramNode(node).build();
  }

  @Nullable
  public abstract SymTab symbolTable();

  public State addSymbolTable(SymTab table) {
    return toBuilder().setSymbolTable(table).build();
  }

  @Nullable
  public abstract TypeCheckResult typeCheckResult();

  public State addTypecheckResult(TypeCheckResult result) {
    return toBuilder().setTypeCheckResult(result).build();
  }

  public abstract ImmutableList<Op> ilCode();

  public State addIlCode(ImmutableList<Op> ilCode) {
    return toBuilder().setIlCode(ilCode).build();
  }

  public abstract ImmutableList<Op> optimizedIlCode();

  public State addOptimizedCode(ImmutableList<Op> optimizedIlCode) {
    return toBuilder().setOptimizedIlCode(optimizedIlCode).build();
  }

  public abstract ImmutableList<String> asmCode();

  public State addAsmCode(ImmutableList<String> asmCode) {
    return toBuilder().setAsmCode(asmCode).build();
  }

  abstract Builder toBuilder();

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
}
