package com.plasstech.lang.d2.interpreter;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.Nop;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.type.SymTab;

public class ExecutionResult {

  private final ImmutableList<Op> code;
  private final Environment environment;
  private final SymTab symbolTable;
  private int instructionCycles;
  private int linesOfCode;
  private int gotos;
  private int branchesTaken;
  private int branchesNotTaken;
  private int calls;

  public ExecutionResult(ImmutableList<Op> code, Environment environment, SymTab symbolTable) {
    this.code = code;
    this.environment = environment;
    this.symbolTable = symbolTable;
    this.linesOfCode = (int) code.stream().filter(op -> !(op instanceof Nop)).count();
  }

  public ImmutableList<Op> code() {
    return code;
  }

  public Environment environment() {
    return environment;
  }

  public SymTab symbolTable() {
    return symbolTable;
  }

  public int instructionCycles() {
    return instructionCycles;
  }

  public void incInstructionCycle() {
    instructionCycles++;
  }

  public int linesOfCode() {
    return linesOfCode;
  }

  public int gotos() {
    return gotos;
  }

  public void incGotos() {
    gotos++;
  }

  public int branchesTaken() {
    return branchesTaken;
  }

  public void incBranchesTaken() {
    branchesTaken++;
  }

  public int branchesNotTaken() {
    return branchesNotTaken;
  }

  public void incBranchesNotTaken() {
    branchesNotTaken++;
  }

  public int calls() {
    return calls;
  }

  public void incCalls() {
    this.calls++;
  }
}
