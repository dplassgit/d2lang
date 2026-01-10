package com.plasstech.lang.d2.codegen.asm;

import java.io.File;
import java.io.IOException;

import com.plasstech.lang.d2.common.D2Options;

public interface Assembler {
  /**
   * Assembles asmFile into an executable using some of the D2Options. If the D2Options specify that
   * an executable should be generated, its path is returned. Otherwise Null is returned.
   */
  File assemble(D2Options options, String baseName, File asmFile)
      throws IOException, InterruptedException;
}
