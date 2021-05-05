package com.plasstech.lang.d2.codegen;

import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.common.DefaultVisitor;
import com.plasstech.lang.d2.parse.StatementsNode;
import com.plasstech.lang.d2.type.SymTab;

public class ILCodeGenerator extends DefaultVisitor implements CodeGenerator<Op> {

  private final StatementsNode root;
  private final SymTab symTab;

  public ILCodeGenerator(StatementsNode root, SymTab symTab) {
    this.root = root;
    this.symTab = symTab;
  }

  @Override
  public void generate() {
  }

  @Override
  public void emit(Op op) {
  }

}
