package com.plasstech.lang.d2.codegen;

import com.plasstech.lang.d2.type.SymbolStorage;

public class StackLocation extends Location {

  // TODO: capture "offset" into stack frame
  public StackLocation(String name) {
    super(name);
  }

  @Override
  public SymbolStorage storage() {
    // TODO: might be a parameter (but still on the stack, shrug)
    return SymbolStorage.LOCAL;
  }
}
